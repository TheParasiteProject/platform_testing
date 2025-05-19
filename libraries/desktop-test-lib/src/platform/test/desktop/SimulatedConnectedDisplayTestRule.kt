/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package platform.test.desktop

import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.DisplayListener
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Display.TYPE_OVERLAY
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] to manage multiple simulated connected overlay displays.
 *
 * @param initDisplayCount the number of displays to be set up immediately. Alternatively, it's
 *   possible to setup new displays at a later stage (on-demand) by calling [setupTestDisplay] or
 *   [setupTestDisplays].
 */
class SimulatedConnectedDisplayTestRule(val initDisplayCount: Int = 0) : TestRule {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val displayManager = context.getSystemService(DisplayManager::class.java)
    var addedDisplays = listOf<Int>()
        private set

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                try {
                    if (initDisplayCount > 0) {
                        setupTestDisplays(initDisplayCount)
                    }
                    base.evaluate()
                } finally {
                    teardown()
                }
            }
        }

    private fun teardown() {
        cleanupTestDisplays()
    }

    /**
     * Adds multiple overlay displays with specified dimensions. Any existing overlay displays will
     * be removed before adding the new ones.
     *
     * @param displays A list of [Point] objects, where each [Point] represents the width and height
     *   of a simulated display.
     * @return List of displayIds of added displays.
     */
    fun setupTestDisplays(displays: List<Point>): List<Int> = runBlocking {
        // Cleanup any existing overlay displays.
        cleanupTestDisplays()

        if (displays.isEmpty()) {
            Log.w(TAG, "setupTestDisplays called with an empty list. No displays created.")
            return@runBlocking emptyList()
        }

        val displayAddedFlow: Flow<Int> = callbackFlow {
            val listener =
                object : DisplayListener {
                    override fun onDisplayAdded(displayId: Int) {
                        trySend(displayId)
                    }

                    override fun onDisplayRemoved(displayId: Int) {}

                    override fun onDisplayChanged(displayId: Int) {}
                }

            val handler = Handler(Looper.getMainLooper())
            displayManager.registerDisplayListener(listener, handler)

            // `disable_window_interaction` is used to let interaction not get obstructed by
            // OverlayDisplayWindow and let it go through to the window or surface behind it.
            val displaySettings =
                displays.joinToString(separator = ";") { size ->
                    "${size.x}x${size.y}/$DEFAULT_DENSITY,disable_window_interaction"
                }

            // Add the overlay displays
            Settings.Global.putString(
                context.contentResolver,
                Settings.Global.OVERLAY_DISPLAY_DEVICES,
                displaySettings,
            )

            awaitClose { displayManager.unregisterDisplayListener(listener) }
        }

        addedDisplays = buildList {
            withTimeoutOrNull(TIMEOUT) {
                displayAddedFlow.take(displays.size).collect { displayId ->
                    this@buildList += displayId
                }
            } ?: error("Timed out waiting for displays to be added.")
        }
        addedDisplays
    }

    /**
     * Adds multiple overlay displays with default dimensions. Any existing overlay displays will be
     * removed before adding the new ones.
     *
     * @param count number of displays to add.
     * @return List of displayIds of added displays.
     */
    fun setupTestDisplays(count: Int): List<Int> {
        val displays = List(count) { Point(DEFAULT_WIDTH, DEFAULT_HEIGHT) }
        return setupTestDisplays(displays)
    }

    /** Add a single overlay display. */
    fun setupTestDisplay(width: Int = DEFAULT_WIDTH, height: Int = DEFAULT_HEIGHT): Int =
        setupTestDisplays(listOf(Point(width, height)))[0]

    private fun cleanupTestDisplays() = runBlocking {
        val existingDisplays =
            displayManager.displays.filter { it.type == TYPE_OVERLAY }.map { it.displayId }
        if (existingDisplays.isEmpty()) {
            return@runBlocking
        }

        val displayRemovedFlow: Flow<Int> = callbackFlow {
            val listener =
                object : DisplayListener {
                    override fun onDisplayAdded(displayId: Int) {}

                    override fun onDisplayRemoved(displayId: Int) {
                        trySend(displayId)
                    }

                    override fun onDisplayChanged(displayId: Int) {}
                }
            val handler = Handler(Looper.getMainLooper())
            displayManager.registerDisplayListener(listener, handler)

            // Remove overlay displays. We'll execute this regardless of addedDisplays just to
            // ensure all overlay displays are removed before and after the test.
            // Note: If we want to restore the original overlay display added before this test (and
            // its topology), it will be complicated as re-adding overlay display would lead to
            // different displayId and topology could not be restored easily.
            Settings.Global.putString(
                context.contentResolver,
                Settings.Global.OVERLAY_DISPLAY_DEVICES,
                null,
            )
            awaitClose { displayManager.unregisterDisplayListener(listener) }
        }

        val removedDisplays = buildList {
            withTimeoutOrNull(TIMEOUT) {
                displayRemovedFlow.take(existingDisplays.size).collect { displayId ->
                    this@buildList += displayId
                }
            } ?: error("Timed out waiting for displays to be removed: $existingDisplays")
        }
        addedDisplays = existingDisplays - removedDisplays
    }

    private companion object {
        const val DEFAULT_WIDTH = 1280
        const val DEFAULT_HEIGHT = 720
        const val DEFAULT_DENSITY = 160
        const val TAG = "SimulatedConnectedDisplayTestRule"
        val TIMEOUT = 10.seconds
    }
}

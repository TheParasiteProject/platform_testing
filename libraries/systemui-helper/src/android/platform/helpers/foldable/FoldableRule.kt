/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.platform.helpers.foldable

import android.os.SystemClock
import android.platform.test.rule.TestWatcher
import android.platform.uiautomator_helpers.WaitUtils.ensureThat
import androidx.annotation.FloatRange
import java.util.concurrent.TimeUnit
import org.junit.rules.TestRule
import org.junit.runner.Description

/**
 * Provides an interface to use foldable specific features.
 *
 * Should be used as [ClassRule]. To start a test folded or unfolded, use [foldBeforeTestRule] and
 * [unfoldBeforeTestRule].
 *
 * Example:
 *
 * ```
 *  companion object {
 *      @get:ClassRule val foldable = FoldableRule()
 *  }
 *  @get:Rule val foldRule = foldable.foldBeforeTestRule
 * ```
 */
class FoldableRule(private val ensureScreenOn: Boolean = false) : TestWatcher() {

    private val controller = FoldableDeviceController()
    private var initialized = false

    override fun starting(description: Description?) {
        controller.init()
        initialized = true
    }

    override fun finished(description: Description?) {
        if (initialized) {
            controller.uninit()
            initialized = false
        }
    }

    fun fold() {
        check(!controller.isFolded) { "Trying to fold when already folded" }
        if (ensureScreenOn) {
            ensureThat("screen is on before folding") { screenOn }
        }
        val initialScreenSurface = displaySurface

        controller.fold()
        SystemClock.sleep(ANIMATION_TIMEOUT) // Let's wait for the unfold animation to finish.

        ensureThat("screen is off after folding") { !screenOn }
        ensureThat("screen surface decreases after folding") {
            displaySurface < initialScreenSurface
        }
    }

    fun unfold() {
        check(controller.isFolded) { "Trying to unfold when already unfolded" }
        if (ensureScreenOn) {
            ensureThat("screen is on before unfolding") { screenOn }
        }
        val initialScreenSurface = displaySurface

        controller.unfold()
        SystemClock.sleep(ANIMATION_TIMEOUT) // Let's wait for the unfold animation to finish.

        ensureThat("screen is on after unfolding") { screenOn }
        ensureThat("screen surface increases after unfolding") {
            displaySurface > initialScreenSurface
        }
    }

    fun setHingeAngle(@FloatRange(from = 0.0, to = 180.0) angle: Float) {
        controller.setHingeAngle(angle)
    }

    val foldBeforeTestRule: TestRule = FoldControlRule(FoldableState.FOLDED)
    val unfoldBeforeTestRule: TestRule = FoldControlRule(FoldableState.UNFOLDED)

    private inner class FoldControlRule(private val startState: FoldableState) : TestWatcher() {
        override fun starting(description: Description?) {
            check(initialized) { "Initialize of FoldableRule needed before this." }
            if (currentState == startState) {
                return
            }
            when (startState) {
                FoldableState.FOLDED -> fold()
                FoldableState.UNFOLDED -> unfold()
                FoldableState.HALF_FOLDED -> TODO("Not yet supported.")
            }
        }
    }

    private val currentState: FoldableState
        get() =
            when (controller.isFolded) {
                true -> FoldableState.FOLDED
                false -> FoldableState.UNFOLDED
            }

    private val screenOn: Boolean
        get() = uiDevice.isScreenOn

    private val displaySurface: Int
        get() = uiDevice.displayWidth * uiDevice.displayHeight
}

private val ANIMATION_TIMEOUT = TimeUnit.SECONDS.toMillis(3)

private enum class FoldableState {
    FOLDED,
    UNFOLDED,
    HALF_FOLDED
}

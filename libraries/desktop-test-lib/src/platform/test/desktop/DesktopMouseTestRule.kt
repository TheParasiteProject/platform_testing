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

import android.Manifest
import android.companion.virtual.VirtualDeviceManager
import android.companion.virtual.VirtualDeviceParams
import android.graphics.PointF
import android.graphics.RectF
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayTopology
import android.hardware.display.DisplayTopologyGraph
import android.hardware.input.InputManager
import android.hardware.input.VirtualMouse
import android.hardware.input.VirtualMouseConfig
import android.os.Handler
import android.os.Looper
import android.platform.uiautomatorhelpers.AdoptShellPermissionsRule
import android.util.Log
import android.util.SizeF
import android.view.Display.DEFAULT_DISPLAY
import androidx.annotation.VisibleForTesting
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] to support [VirtualMouse] move and drag within a single display / crossing across
 * displays.
 */
class DesktopMouseTestRule() : TestRule {
    private val adoptShellPermissionsTestRule = AdoptShellPermissionsRule(*PERMISSIONS)
    private val fakeAssociationRule = FakeAssociationRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val displayManager = context.getSystemService(DisplayManager::class.java)
    private val resourceTracker = ResourceTracker(fakeAssociationRule, displayManager)
    private val ruleChain =
        RuleChain.outerRule(adoptShellPermissionsTestRule)
            .around(fakeAssociationRule)
            .around(resourceTracker)

    override fun apply(base: Statement, description: Description) =
        ruleChain.apply(base, description)

    /**
     * Internal test rule that tracks created virtual mouse and modified display topology and
     * ensures they are properly closed / restored after the test.
     */
    private class ResourceTracker(
        private val fakeAssociationRule: FakeAssociationRule,
        private val displayManager: DisplayManager,
    ) : ExternalResource() {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        private val inputManager = context.getSystemService(InputManager::class.java)
        private val virtualDeviceManager =
            context.getSystemService(VirtualDeviceManager::class.java)

        // TODO: b/392534769 - Refactor this to use UinputMouse.
        private var virtualDevice: VirtualDeviceManager.VirtualDevice? = null
        private var virtualMouse: VirtualMouse? = null
        private val displayIdsWithMouseScalingDisabled = mutableListOf<Int>()

        val requireVirtualMouse: VirtualMouse
            get() = checkNotNull(virtualMouse) { "Failed to initialize VirtualMouse" }

        /**
         * Sets up a virtual mouse that will start at [DEFAULT_DISPLAY].
         *
         * Note: This will move any existing cursor in the same topology as [DEFAULT_DISPLAY], to
         * [DEFAULT_DISPLAY]. If the mouse needs to start at different display, first call
         * `DesktopMouseTestRule#move(displayId, x, y)`.
         */
        override fun before() = runBlocking {
            val createdVirtualDevice =
                virtualDeviceManager.createVirtualDevice(
                    fakeAssociationRule.associationInfo.id,
                    VirtualDeviceParams.Builder().build(),
                )
            virtualDevice = createdVirtualDevice

            val inputDeviceFlow = callbackFlow {
                val inputDeviceListener =
                    object : InputManager.InputDeviceListener {
                        override fun onInputDeviceAdded(deviceId: Int) {
                            val device = inputManager.getInputDevice(deviceId) ?: return
                            if (
                                device.vendorId == VIRTUAL_MOUSE_VENDOR_ID &&
                                    device.productId == VIRTUAL_MOUSE_PRODUCT_ID
                            ) {
                                trySend(deviceId)
                                close()
                            }
                        }

                        override fun onInputDeviceRemoved(deviceId: Int) {}

                        override fun onInputDeviceChanged(deviceId: Int) {}
                    }
                val handler = Handler(Looper.getMainLooper())
                inputManager.registerInputDeviceListener(inputDeviceListener, handler)
                awaitClose { inputManager.unregisterInputDeviceListener(inputDeviceListener) }
            }

            // Note: AssociatedDisplayId is not actually used in connected displays, since cursor
            // will be able to cross displays and no longer gets "associated" anymore, with an
            // exception if display is excluded from the current DisplayTopology.
            virtualMouse =
                createdVirtualDevice.createVirtualMouse(
                    VirtualMouseConfig.Builder()
                        .setVendorId(VIRTUAL_MOUSE_VENDOR_ID)
                        .setProductId(VIRTUAL_MOUSE_PRODUCT_ID)
                        .setInputDeviceName("VirtualMouse_ConnectedDisplaysTest")
                        .setAssociatedDisplayId(DEFAULT_DISPLAY)
                        .build()
                )

            withTimeoutOrNull(TIMEOUT) { inputDeviceFlow.take(1) }
                ?: error("Timed out waiting for input device to be added.")

            disableMouseScaling()
            // TODO: b/399523818 - Ensure cursor starts on DEFAULT_DISPLAY
            // TODO: b/380001108 - Adds util method (move, drag) for test writer to interact with
            //  the VirtualMouse
        }

        private fun disableMouseScaling() {
            for (display in displayManager.displays) {
                displayIdsWithMouseScalingDisabled += display.displayId
                inputManager.setMouseScalingEnabled(false, display.displayId)
            }
        }

        override fun after() {
            for (displayId in displayIdsWithMouseScalingDisabled) {
                try {
                    inputManager.setMouseScalingEnabled(true, displayId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore mouse scaling for display#$displayId", e)
                }
            }
            virtualMouse?.close()
            virtualDevice?.close()
            virtualMouse = null
            virtualDevice = null
            super.after()
        }
    }

    /**
     * Representing details on how to cross from one display to another based on their adjacency
     * relation.
     *
     * @property targetPointDp The center of an intersection edge of 2 adjacent displays. This will
     *   be the target (x, y) to move to, wherever the current cursor is.
     * @property toCrossDeltaDp The small (dx, dy) required to cross display boundary when cursor is
     *   already on the `targetPointDp.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    data class DisplayCrossingDetail(val targetPointDp: PointF, private val toCrossDeltaDp: SizeF) {

        // Sample explanation
        //
        //   +-------------------------------+
        //   |           Display 2           |
        //   |                               |
        //   +=========*****X*****===========+
        //             R    |    R Bottom of D2 / Top of D1
        //              +-------+
        //              |Display|
        //              |   1   |
        //              |       |
        //              |       |
        //              +-------+
        //  R = range of the intersection
        //  X = center of the intersection (`targetPointDp`)

        val toCrossDxDp: Float
            get() = toCrossDeltaDp.width

        val toCrossDyDp: Float
            get() = toCrossDeltaDp.height
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    data class AdjacentDisplay(val displayId: Int, val position: Position) {
        // Side of the other display which touches this display.
        enum class Position {
            LEFT,
            RIGHT,
            TOP,
            BOTTOM;

            companion object {
                fun from(@DisplayTopology.TreeNode.Position value: Int): Position {
                    return when (value) {
                        DisplayTopology.TreeNode.POSITION_LEFT -> LEFT
                        DisplayTopology.TreeNode.POSITION_TOP -> TOP
                        DisplayTopology.TreeNode.POSITION_RIGHT -> RIGHT
                        DisplayTopology.TreeNode.POSITION_BOTTOM -> BOTTOM
                        else ->
                            throw IllegalArgumentException(
                                "Invalid integer value for Position: $value"
                            )
                    }
                }
            }
        }
    }

    class NoPathFoundException(message: String) : Exception(message)

    companion object {

        /**
         * Finds a path of adjacent display IDs from `startId` to `endId` using BFS.
         *
         * Returned path doesn't include the start node. If `startId` == `endId`, then empty list
         * will be returned
         */
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        fun findPath(
            startId: Int,
            endId: Int,
            displayTopologyGraph: DisplayTopologyGraph,
        ): List<AdjacentDisplay> {
            if (startId == endId) return listOf()
            val adjacencyGraph = displayTopologyGraph.displayNodes.associateBy { it.displayId }
            val queue = ArrayDeque<Int>().apply { add(startId) }
            val visited = mutableSetOf(startId)
            // Maps display id to its parent in the context of `startId`->`endIf` traversal.
            val parentMap = mutableMapOf<Int, AdjacentDisplay>()

            while (queue.isNotEmpty()) {
                val currentId = queue.removeFirst()
                if (currentId == endId) {
                    // Path found, reconstruct path
                    val path = ArrayDeque<AdjacentDisplay>()
                    var backtrackId = endId
                    while (backtrackId != startId) {
                        val parentNode =
                            parentMap[backtrackId]
                                ?: throw IllegalStateException(
                                    "Path should exist since start node can be reached"
                                )
                        path.addFirst(AdjacentDisplay(backtrackId, parentNode.position))
                        backtrackId = parentNode.displayId
                    }
                    return path
                }

                val currentNode = adjacencyGraph[currentId] ?: continue
                // Check neighbors
                for (adjacentDisplay in currentNode.adjacentDisplays()) {
                    val neighborId = adjacentDisplay.displayId()
                    val position = adjacentDisplay.position()
                    if (neighborId in visited) continue
                    visited.add(neighborId)
                    parentMap[neighborId] =
                        AdjacentDisplay(currentId, AdjacentDisplay.Position.from(position))
                    queue.add(neighborId)
                }
            }
            throw NoPathFoundException("No path found from $startId to $endId")
        }

        /**
         * Calculates crossing details (intersection-midpoint, crossing-delta) in DP, between
         * adjacent bounds.
         *
         * @see [DisplayCrossingDetail] for details
         */
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        fun calculateCrossingDetailsDp(
            source: RectF,
            target: RectF,
            position: AdjacentDisplay.Position,
        ): DisplayCrossingDetail {
            val overlapTop = max(source.top, target.top)
            val overlapBottom = min(source.bottom, target.bottom)
            val overlapLeft = max(source.left, target.left)
            val overlapRight = min(source.right, target.right)
            val offset = MOUSE_CROSS_DISPLAY_OFFSET_DP

            return when (position) {
                AdjacentDisplay.Position.RIGHT -> // target is to the right of source
                DisplayCrossingDetail(
                        PointF(source.right, (overlapTop + overlapBottom) / 2f),
                        SizeF(offset, 0f),
                    )
                AdjacentDisplay.Position.LEFT -> // target is to the left of source
                DisplayCrossingDetail(
                        PointF(source.left, (overlapTop + overlapBottom) / 2f),
                        SizeF(-offset, 0f),
                    )
                AdjacentDisplay.Position.BOTTOM -> // target is below source
                DisplayCrossingDetail(
                        PointF((overlapLeft + overlapRight) / 2f, source.bottom),
                        SizeF(0f, offset),
                    )
                AdjacentDisplay.Position.TOP -> // target is above source
                DisplayCrossingDetail(
                        PointF((overlapLeft + overlapRight) / 2f, source.top),
                        SizeF(0f, -offset),
                    )
            }
        }

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val MOUSE_CROSS_DISPLAY_OFFSET_DP = 5.0f
        private const val VIRTUAL_MOUSE_VENDOR_ID = 123
        private const val VIRTUAL_MOUSE_PRODUCT_ID = 456
        private const val TAG = "DesktopMouseTestRule"
        private val TIMEOUT: Duration = 10.seconds
        private val PERMISSIONS =
            arrayOf(
                Manifest.permission.ASSOCIATE_COMPANION_DEVICES,
                "android.permission.MANAGE_COMPANION_DEVICES",
                Manifest.permission.CREATE_VIRTUAL_DEVICE,
                Manifest.permission.INJECT_EVENTS,
                "android.permission.MANAGE_DISPLAYS",
                Manifest.permission.SET_POINTER_SPEED,
            )
    }
}

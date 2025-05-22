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
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayTopology
import android.hardware.display.DisplayTopology.dpToPx
import android.hardware.display.DisplayTopologyGraph
import android.hardware.input.InputManager
import android.hardware.input.VirtualMouse
import android.hardware.input.VirtualMouseButtonEvent
import android.hardware.input.VirtualMouseButtonEvent.ACTION_BUTTON_PRESS
import android.hardware.input.VirtualMouseButtonEvent.ACTION_BUTTON_RELEASE
import android.hardware.input.VirtualMouseButtonEvent.BUTTON_PRIMARY
import android.hardware.input.VirtualMouseConfig
import android.hardware.input.VirtualMouseRelativeEvent
import android.os.Handler
import android.os.Looper
import android.platform.uiautomatorhelpers.AdoptShellPermissionsRule
import android.platform.uiautomatorhelpers.WaitUtils
import android.util.Log
import android.view.Display.DEFAULT_DISPLAY
import android.view.DisplayInfo
import androidx.annotation.VisibleForTesting
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
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
    private val uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation()
    private val displayManager = context.getSystemService(DisplayManager::class.java)
    private val inputManager = context.getSystemService(InputManager::class.java)
    private val resourceTracker = ResourceTracker()
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
    private inner class ResourceTracker() : ExternalResource() {
        private val virtualDeviceManager =
            context.getSystemService(VirtualDeviceManager::class.java)

        // TODO: b/392534769 - Refactor this to use UinputMouse.
        private var virtualDevice: VirtualDeviceManager.VirtualDevice? = null
        private var virtualMouse: VirtualMouse? = null
        private val displayIdsWithMouseScalingDisabled = mutableListOf<Int>()
        private val handler = Handler(Looper.getMainLooper())
        private val displayListener =
            object : DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) {
                    disableMouseScaling(displayId)
                }

                override fun onDisplayRemoved(displayId: Int) {}

                override fun onDisplayChanged(displayId: Int) {}
            }

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
                inputManager.registerInputDeviceListener(inputDeviceListener, handler)

                // Note: AssociatedDisplayId is not actually used in connected displays, since
                // cursor will be able to cross displays and no longer gets "associated" anymore,
                // with an exception if display is excluded from the current DisplayTopology.
                virtualMouse =
                    createdVirtualDevice.createVirtualMouse(
                        VirtualMouseConfig.Builder()
                            .setVendorId(VIRTUAL_MOUSE_VENDOR_ID)
                            .setProductId(VIRTUAL_MOUSE_PRODUCT_ID)
                            .setInputDeviceName("VirtualMouse_ConnectedDisplaysTest")
                            .setAssociatedDisplayId(DEFAULT_DISPLAY)
                            .build()
                    )
                awaitClose { inputManager.unregisterInputDeviceListener(inputDeviceListener) }
            }

            withTimeoutOrNull(TIMEOUT) { inputDeviceFlow.first() }
                ?: error("Timed out waiting for input device to be added.")

            for (display in displayManager.displays) {
                disableMouseScaling(display.displayId)
            }
            displayManager.registerDisplayListener(displayListener, handler)
            ensureCursorStartsOnDefaultDisplay()
        }

        private fun disableMouseScaling(displayId: Int) {
            displayIdsWithMouseScalingDisabled += displayId
            inputManager.setMouseScalingEnabled(false, displayId)
        }

        private fun ensureCursorStartsOnDefaultDisplay() {
            val display = displayManager.getDisplay(DEFAULT_DISPLAY)
            move(DEFAULT_DISPLAY, display.width / 2, display.height / 2)
        }

        override fun after() {
            displayManager.unregisterDisplayListener(displayListener)
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

    fun startDrag() {
        resourceTracker.requireVirtualMouse.sendButtonEvent(
            VirtualMouseButtonEvent.Builder()
                .setAction(ACTION_BUTTON_PRESS)
                .setButtonCode(BUTTON_PRIMARY)
                .build()
        )
        Thread.sleep(MOUSE_INPUT_DELAY.inWholeMilliseconds)
    }

    fun stopDrag() {
        resourceTracker.requireVirtualMouse.sendButtonEvent(
            VirtualMouseButtonEvent.Builder()
                .setAction(ACTION_BUTTON_RELEASE)
                .setButtonCode(BUTTON_PRIMARY)
                .build()
        )
        Thread.sleep(MOUSE_INPUT_DELAY.inWholeMilliseconds)
    }

    /**
     * Moves the mouse cursor to the `(targetXPx, targetYPx)` on target display. If the target
     * display is different from the current display, it finds a path and moves the cursor across
     * display(s).
     *
     * NOTE: While InputManager APIs are using PointF for both get/set, the underlying
     * implementation is actually using Int. For example, evdev injection only supports Int.
     * Therefore, it's currently not possible to move to a floating point coordinates.
     *
     * @param targetDisplayId The ID of the destination display.
     * @param targetX The target X (PX) coordinate relative to the target display.
     * @param targetY The target Y (PX) coordinate relative to the target display.
     */
    fun move(targetDisplayId: Int, targetXPx: Int, targetYPx: Int) {
        val currentCursorDisplayId = getCursorDisplayId()

        if (targetDisplayId != currentCursorDisplayId) {
            moveToDisplay(currentCursorDisplayId, targetDisplayId)
        }

        val currentPosition = getCursorPosition(targetDisplayId).roundToInt()
        performSteppedMove(Point(targetXPx, targetYPx) - currentPosition)

        WaitUtils.ensureThat {
            val finalPosition = getCursorPosition(targetDisplayId).roundToInt()
            val delta = finalPosition - Point(targetXPx, targetYPx)
            // As mentioned in the javadoc above, InputManager API doesn't support floating-point
            // movements. Hence, with all the floating-point calculation above, there might be
            // slight difference (within `FLOATING_ROUND_CORRECTION`) in the final cursor position.
            delta.dx <= FLOATING_ROUNDING_CORRECTION && delta.dy <= FLOATING_ROUNDING_CORRECTION
        }
    }

    /**
     * Requests to move the mouse cursor by the specified delta. Different from the [move] method,
     * this does not consider where the current mouse cursor is, and does not ensure that the cursor
     * will move to any target position.
     *
     * @param xPx The delta X (PX) coordinate.
     * @param yPx The delta Y (PX) coordinate.
     */
    fun moveDelta(xPx: Int, yPx: Int) {
        moveInternal(Delta(xPx, yPx))
    }

    private fun moveToDisplay(startingDisplayId: Int, targetDisplayId: Int) {
        var currentCursorDisplayId = startingDisplayId
        val topology =
            checkNotNull(displayManager.displayTopology) { "DisplayTopology must be available." }
        val displayAbsoluteBounds = topology.absoluteBounds
        val topologyGraph = topology.graph
        val path = findPath(currentCursorDisplayId, targetDisplayId, topologyGraph)

        path.forEach { (nextDisplayId, position) ->
            val dpi = getDpiForDisplay(currentCursorDisplayId)
            val currentBounds = displayAbsoluteBounds[currentCursorDisplayId]
            val nextBounds = displayAbsoluteBounds[nextDisplayId]

            // Calculate where to cross and the crossing delta
            val crossingDetail = calculateCrossingDetailsDp(currentBounds, nextBounds, position)
            // Cursor moves in PX, however there's no notion of global PX bounds since density
            // of each displays could be different, and there's only global DP bounds.
            // Therefore, to solve the calculation, first convert globalDP -> localDP, then
            // apply DP->PX conversion.
            val edgeIntersectionPx =
                PointF(
                    dpToPx(crossingDetail.targetPointDp.x - currentBounds.left, dpi),
                    dpToPx(crossingDetail.targetPointDp.y - currentBounds.top, dpi),
                )
            val crossingDeltaPx =
                DeltaF(
                    dpToPx(crossingDetail.toCrossDxDp, dpi),
                    dpToPx(crossingDetail.toCrossDyDp, dpi),
                )

            val currentPosition = getCursorPosition(currentCursorDisplayId)
            // Move to the center of the edge intersection.
            val toBorderDeltaPx = edgeIntersectionPx - currentPosition
            performSteppedMove(toBorderDeltaPx.roundToInt())

            // Perform a small move to cross the boundary
            performSteppedMove(crossingDeltaPx.roundToInt())
            // Validate cursor crossed display
            WaitUtils.ensureThat { getCursorDisplayId() == nextDisplayId }
            currentCursorDisplayId = nextDisplayId

            // InputDevice reconfiguration will happen when cursor changed display, and might jam
            // the input queue. Waiting for input transactions to finish ensure any subsequent
            // `getCursorPosition()` calls return accurate position
            uiAutomation.syncInputTransactions()
        }
    }

    /**
     * Divides delta to multiple small movements
     *
     * @param deltaPx delta movement, either dx or dy must be non-zero
     * @param maxSteps the maximum number of times move events would be sent
     */
    private fun performSteppedMove(deltaPx: Delta, maxSteps: Int = MAX_MOUSE_MOVE_STEPS_COUNT) {
        if (deltaPx.dx == 0 && deltaPx.dy == 0) return
        // Find ideal number of steps to move a number of PX
        val idealSteps = max(abs(deltaPx.dx), abs(deltaPx.dy)) / MIN_PX_PER_STEP

        // Limit the number of steps while ensuring it's not zero
        val steps = max(1, min(maxSteps, idealSteps))
        val stepX = deltaPx.dx / steps
        val stepY = deltaPx.dy / steps
        repeat(steps) { moveInternal(Delta(stepX, stepY)) }

        // Move any remaining delta
        val remainingDx = deltaPx.dx - (stepX * steps)
        val remainingDy = deltaPx.dy - (stepY * steps)
        moveInternal(Delta(remainingDx, remainingDy))
    }

    private fun moveInternal(deltaPx: Delta) {
        resourceTracker.requireVirtualMouse.sendRelativeEvent(
            VirtualMouseRelativeEvent.Builder()
                .setRelativeX(deltaPx.dx.toFloat())
                .setRelativeY(deltaPx.dy.toFloat())
                .build()
        )
        Thread.sleep(MOUSE_INPUT_DELAY.inWholeMilliseconds)
    }

    private fun getCursorPosition(displayId: Int): PointF =
        checkNotNull(inputManager.getCursorPosition(displayId)) {
            "Cursor is not on display#$displayId"
        }

    private fun getCursorDisplayId(): Int {
        // Query cursor position on all displays and find the one with non-null values
        // This is a hack since getCursorPosition API, doesn't directly provide the displayId it's
        // currently at.
        for (display in displayManager.displays) {
            val cursorPosition = inputManager.getCursorPosition(display.displayId)
            if (cursorPosition != null) {
                return display.displayId
            }
        }
        throw NoCursorFoundException("Cursor doesn't exist on any display")
    }

    private fun getDpiForDisplay(displayId: Int): Int {
        val display =
            checkNotNull(displayManager.getDisplay(displayId)) { "Display#$displayId not found" }
        val displayInfo = DisplayInfo()
        display.getDisplayInfo(displayInfo)
        return displayInfo.logicalDensityDpi
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
    data class DisplayCrossingDetail(
        val targetPointDp: PointF,
        private val toCrossDeltaDp: DeltaF,
    ) {

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
            get() = toCrossDeltaDp.dx

        val toCrossDyDp: Float
            get() = toCrossDeltaDp.dy
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

    data class DeltaF(val dx: Float, val dy: Float) {
        fun roundToInt() = Delta(dx.roundToInt(), dy.roundToInt())
    }

    data class Delta(val dx: Int, val dy: Int)

    class NoCursorFoundException(message: String) : Exception(message)

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
            topologyGraph: DisplayTopologyGraph,
        ): List<AdjacentDisplay> {
            if (startId == endId) return listOf()
            val adjacencyGraph = topologyGraph.displayNodes.associateBy { it.displayId }
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
                for (adjacentDisplay in currentNode.adjacentDisplays) {
                    val neighborId = adjacentDisplay.displayId
                    val position = adjacentDisplay.position
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
                        DeltaF(offset, 0f),
                    )
                AdjacentDisplay.Position.LEFT -> // target is to the left of source
                DisplayCrossingDetail(
                        PointF(source.left, (overlapTop + overlapBottom) / 2f),
                        DeltaF(-offset, 0f),
                    )
                AdjacentDisplay.Position.BOTTOM -> // target is below source
                DisplayCrossingDetail(
                        PointF((overlapLeft + overlapRight) / 2f, source.bottom),
                        DeltaF(0f, offset),
                    )
                AdjacentDisplay.Position.TOP -> // target is above source
                DisplayCrossingDetail(
                        PointF((overlapLeft + overlapRight) / 2f, source.top),
                        DeltaF(0f, -offset),
                    )
            }
        }

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val MOUSE_CROSS_DISPLAY_OFFSET_DP = 5.0f
        private const val FLOATING_ROUNDING_CORRECTION = 1f
        private const val MIN_PX_PER_STEP = 1
        private const val MAX_MOUSE_MOVE_STEPS_COUNT = 20
        private const val VIRTUAL_MOUSE_VENDOR_ID = 123
        private const val VIRTUAL_MOUSE_PRODUCT_ID = 456
        private const val TAG = "DesktopMouseTestRule"

        // Mimics UiAutomator delay for injecting MotionEvent
        private val MOUSE_INPUT_DELAY = 5.milliseconds
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

        private fun PointF.roundToInt() = Point(x.roundToInt(), y.roundToInt())

        private operator fun Point.minus(other: Point) = Delta(x - other.x, y - other.y)

        private operator fun PointF.minus(other: PointF) = DeltaF(x - other.x, y - other.y)
    }
}

/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.platform.systemui_tapl.ui

import android.graphics.Point
import android.platform.systemui_tapl.ui.Bubble.Companion.DISMISS_VIEW
import android.platform.uiautomatorhelpers.BetterSwipe
import android.platform.uiautomatorhelpers.DeviceHelpers.context
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForNullableObj
import android.platform.uiautomatorhelpers.PRECISE_GESTURE_INTERPOLATOR
import android.view.WindowInsets
import android.view.WindowManager
import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * The interface to centralize the logic to drag a bubble component.
 *
 * @see Bubble
 * @see BubbleBarHandle
 */
internal interface BubbleDragTarget {

    /** The current position of this bubble component. */
    val visibleCenter: Point

    /**
     * Drags the bubble handle to the other side.
     *
     * If the bubble is at the right of the screen, drag to the left. Otherwise, drag to the right.
     */
    fun dragToTheOtherSide() {
        dragTo(Point(uiDevice.displayWidth - visibleCenter.x, visibleCenter.y))
    }

    /** Dismisses the bubble by dragging it to the Dismiss target. */
    fun dragToDismiss() {
        val windowMetrics =
            context.getSystemService(WindowManager::class.java)!!.currentWindowMetrics
        val insets =
            windowMetrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.mandatorySystemGestures() or
                    WindowInsets.Type.navigationBars() or
                    WindowInsets.Type.displayCutout()
            )
        val destination =
            Point(windowMetrics.bounds.width() / 2, (windowMetrics.bounds.height() - insets.bottom))
        // drag to bottom of the screen, wait for dismiss view to appear, drag to dismiss view
        BetterSwipe.swipe(visibleCenter) {
            to(destination, interpolator = PRECISE_GESTURE_INTERPOLATOR)
            // Make dismiss view optional in case the EDU view was shown and first swipe hid that.
            // We will do a second swipe to actually dismiss.
            val dismissView = waitForNullableObj(DISMISS_VIEW)
            if (dismissView != null) {
                to(dismissView.visibleCenter, interpolator = PRECISE_GESTURE_INTERPOLATOR)
            }
        }
    }

    /** Drags the target to [position]. */
    fun dragTo(position: Point) {
        BetterSwipe.swipe(visibleCenter) {
            to(position, Duration.of(500, ChronoUnit.MILLIS), PRECISE_GESTURE_INTERPOLATOR)
        }
    }
}

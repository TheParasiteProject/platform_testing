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
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForNullableObj
import android.platform.uiautomatorhelpers.PRECISE_GESTURE_INTERPOLATOR
import android.view.WindowInsets
import android.view.WindowManager

/** A helper to consolidate bubble operations among several UI bubble components. */
object BubbleHelper {

    /**
     * Drag the bubble to dismiss view.
     *
     * @param currentPosition the start position to drag.
     */
    fun dragBubbleToDismiss(currentPosition: Point) {
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
        BetterSwipe.swipe(currentPosition) {
            to(destination, interpolator = PRECISE_GESTURE_INTERPOLATOR)
            // Make dismiss view optional in case the EDU view was shown and first swipe hid that.
            // We will do a second swipe to actually dismiss.
            val dismissView = waitForNullableObj(DISMISS_VIEW)
            if (dismissView != null) {
                to(dismissView.visibleCenter, interpolator = PRECISE_GESTURE_INTERPOLATOR)
            }
        }
    }
}

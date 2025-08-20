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
import android.platform.uiautomatorhelpers.BetterSwipe
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import android.platform.uiautomatorhelpers.PRECISE_GESTURE_INTERPOLATOR
import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * The interface to centralize the logic to drag a bubble bar component.
 *
 * @see BubbleBar
 * @see BubbleBarItem
 */
internal interface BubbleBarDragTarget : BubbleDragTarget {

    override fun dragToDismiss() {
        BetterSwipe.swipe(visibleCenter) {
            // It is necessary to hold a period to trigger the drag gesture for bubble bar.
            pause()
            to(
                // We should wait for dismiss view show up before dragging to it.
                waitForObj(BubbleBar.DISMISS_VIEW).visibleCenter,
                Duration.of(500, ChronoUnit.MILLIS),
                PRECISE_GESTURE_INTERPOLATOR,
            )
        }
    }

    override fun dragTo(position: Point) {
        BetterSwipe.swipe(visibleCenter) {
            // It is necessary to hold a period to trigger the drag gesture for bubble bar.
            pause()
            to(position, Duration.of(500, ChronoUnit.MILLIS), PRECISE_GESTURE_INTERPOLATOR)
        }
    }
}

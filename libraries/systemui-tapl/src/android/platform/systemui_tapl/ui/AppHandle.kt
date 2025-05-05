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

import android.animation.TimeInterpolator
import android.graphics.Point
import android.platform.uiautomatorhelpers.BetterSwipe
import android.platform.uiautomatorhelpers.DeviceHelpers
import android.platform.uiautomatorhelpers.PRECISE_GESTURE_INTERPOLATOR
import android.view.Display.DEFAULT_DISPLAY
import java.time.Duration

/** System UI test automation object representing the app handle */
class AppHandle {
    companion object {
        /** Clicks on the app handle to expand the menu and returns [AppHandleMenu]. */
        @JvmStatic
        @JvmOverloads
        fun click(displayId: Int = DEFAULT_DISPLAY): AppHandleMenu {
            val center = getHandleCenter(displayId)
            DeviceHelpers.uiDevice.click(center.x, center.y)
            return AppHandleMenu(displayId)
        }

        /** Drags from the app handle to the specified location. */
        @JvmStatic
        @JvmOverloads
        fun dragTo(
            point: Point,
            duration: Duration = Duration.ofMillis(500),
            interpolator: TimeInterpolator = PRECISE_GESTURE_INTERPOLATOR,
            displayId: Int = DEFAULT_DISPLAY,
        ) {
            BetterSwipe.swipe(getHandleCenter(displayId), displayId) {
                pause()
                to(point, duration, interpolator)
            }
        }

        private fun getHandleCenter(displayId: Int): Point {
            // TODO(b/415121594): find handle by view id
            val x = DeviceHelpers.uiDevice.getDisplayWidth(displayId) / 2
            val y = 10 // A little under the top to prevent opening notification shade
            return Point(x, y)
        }
    }
}

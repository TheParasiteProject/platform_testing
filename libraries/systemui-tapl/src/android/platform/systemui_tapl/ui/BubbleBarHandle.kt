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
import android.platform.systemui_tapl.ui.ExpandedBubbleStack.Companion.FIND_OBJECT_TIMEOUT
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj

/** System UI test automation object representing the bubble bar handle */
class BubbleBarHandle internal constructor() : BubbleDragTarget {
    init {
        BUBBLE_BAR_HANDLE.assertVisible(timeout = FIND_OBJECT_TIMEOUT) {
            "Bubble bar handle must be visible."
        }
    }

    /** The position of this handle. */
    override val visibleCenter: Point
        get() = bubbleBarHandleView.visibleCenter

    /** Opens the bubble bar handle menu. */
    fun openBubbleBarHandleMenu(): BubbleBarHandleMenu {
        bubbleBarHandleView.click()
        return BubbleBarHandleMenu()
    }

    companion object {
        private val BUBBLE_BAR_HANDLE = sysuiResSelector("bubble_bar_handle_view")

        private val bubbleBarHandleView
            get() = waitForObj(BUBBLE_BAR_HANDLE, FIND_OBJECT_TIMEOUT)
    }
}

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

import android.platform.systemui_tapl.ui.ExpandedBubbleStack.Companion.FIND_OBJECT_TIMEOUT
import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.systemui_tapl.utils.SYSUI_PACKAGE
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import android.widget.TextView
import androidx.test.uiautomator.By

/** System UI test automation object representing the bubble bar handle menu */
class BubbleBarHandleMenu internal constructor() {
    init {
        BUBBLE_BAR_HANDLE_MENU_VIEW.assertVisible(timeout = FIND_OBJECT_TIMEOUT) {
            "Bubble bar handle menu must be visible."
        }
    }

    /** Clicks the dismiss menu item to dismiss the bubble. */
    fun dismissBubble() {
        bubbleBarHandleMenu.findObject(DISMISS_BUBBLE_MENU_ITEM).click()
    }

    companion object {
        private val BUBBLE_BAR_HANDLE_MENU_VIEW = sysuiResSelector("bubble_bar_menu_view")
        private val bubbleBarHandleMenu =
            waitForObj(BUBBLE_BAR_HANDLE_MENU_VIEW, FIND_OBJECT_TIMEOUT)
        private val DISMISS_BUBBLE_MENU_ITEM =
            By.pkg(SYSUI_PACKAGE).clazz(TextView::class.java).text("Dismiss bubble")
    }
}

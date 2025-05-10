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

import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.click
import android.view.Display.DEFAULT_DISPLAY

/** System UI test automation object representing the app handle menu. */
class AppHandleMenu
@JvmOverloads
internal constructor(private val displayId: Int = DEFAULT_DISPLAY) {

    init {
        sysuiResSelector(HANDLE_MENU_RES_ID, displayId).assertVisible {
            "App handle menu is not visible"
        }
    }

    fun clickFloating() {
        sysuiResSelector(FLOATING_BUTTON_RES_ID, displayId).click()
    }

    companion object {
        private const val HANDLE_MENU_RES_ID = "handle_menu"
        private const val FLOATING_BUTTON_RES_ID = "floating_button"
    }
}

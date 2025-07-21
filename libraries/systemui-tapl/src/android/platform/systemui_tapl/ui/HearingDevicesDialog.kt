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

import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.view.Display.DEFAULT_DISPLAY
import androidx.test.uiautomator.By

/** System UI test automation object representing the hearing devices dialog. */
class HearingDevicesDialog internal constructor(val displayId: Int = DEFAULT_DISPLAY) {

    private val uiDialogTitle = By.displayId(displayId).textContains(UI_DIALOG_CONTENT)

    init {
        uiDialogTitle.assertVisible()
    }

    /* Dismisses the dialog by the Back gesture */
    fun dismiss() {
        // Press back to dismiss the dialog
        Root.get(displayId).pressBackOnDisplay()
        uiDialogTitle.assertInvisible()
    }

    private companion object {
        const val UI_DIALOG_CONTENT = "Hearing devices"
    }
}

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
import android.platform.uiautomatorhelpers.DeviceHelpers.click
import android.platform.uiautomatorhelpers.DeviceHelpers.uiDevice
import android.platform.uiautomatorhelpers.WaitResult
import android.platform.uiautomatorhelpers.WaitUtils.waitToBecomeTrue
import android.util.Log
import android.view.Display.DEFAULT_DISPLAY
import androidx.test.uiautomator.By

/** Wrapper representing the FlashlightDialog that opens when the QS Tile is clicked */
class FlashlightDialog internal constructor(displayId: Int = DEFAULT_DISPLAY) {
    private val uiDialogTitle = By.text("Flashlight Strength")
    private val doneBtn = By.text("Done")

    // TODO (b/419868372) use the dialog title since we know what it is

    init {
        uiDialogTitle.assertVisible(errorProvider = { "Flashlight dialog title is not visible" })
        doneBtn.assertVisible(errorProvider = { "Flashlight tile done button is not visible" })
    }

    fun assertDialogClosed() =
        uiDialogTitle.assertInvisible(errorProvider = { "Flashlight dialog title is visible" })

    /** Finds the done button, clicks on it and asserts that the dialog has closed. */
    fun clickOnDoneAndClose() {
        doneBtn.click()
        if (waitToBecomeTrue { !uiDevice.hasObject(doneBtn) }.result !is WaitResult.WaitSuccess) {
            Log.d("QuickSettingsTileBase", "Retrying click due to b/339676505")
            doneBtn.click()
        }
        assertDialogClosed()
    }
}

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

package android.platform.systemui_tapl.ui.quicksettings

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
    // https://hsv.googleplex.com/5394301264592896?node=20
    private val uiDialogTitle =
        By.displayId(displayId).res(FLASHLIGHT_TITLE_TAG).text(FLASHLIGHT_TITLE_TEXT)
    // https://hsv.googleplex.com/5394301264592896?node=44
    private val doneBtn =
        By.displayId(displayId)
            .res(FLASHLIGHT_DONE_TAG)
            .hasDescendant(By.text(FLASHLIGHT_DONE_TEXT))
    // https://hsv.googleplex.com/5394301264592896?node=53
    private val offButton =
        By.displayId(displayId).res(FLASHLIGHT_OFF_TAG).hasDescendant(By.text(FLASHLIGHT_OFF_TEXT))

    // TODO (b/419868372) use the dialog title since we know what it is, get rid of test tags.

    init {
        uiDialogTitle.assertVisible(errorProvider = { "Flashlight dialog title is not visible" })
        doneBtn.assertVisible(errorProvider = { "Flashlight done button is not visible" })
        offButton.assertVisible(errorProvider = { "Flashlight off button is not visible" })
    }

    fun assertDialogClosed() {
        uiDialogTitle.assertInvisible(errorProvider = { "Flashlight dialog title is visible" })
        doneBtn.assertInvisible(errorProvider = { "Flashlight dialog done button is visible" })
        offButton.assertInvisible(errorProvider = { "Flashlight dialog off button is visible" })
    }

    /** Finds the done button, clicks on it and asserts that the dialog has closed. */
    fun clickOnDoneAndClose() {
        doneBtn.click()
        if (waitToBecomeTrue { !uiDevice.hasObject(doneBtn) }.result !is WaitResult.WaitSuccess) {
            Log.d("QuickSettingsTileBase", "Retrying click due to b/339676505")
            doneBtn.click()
        }
        assertDialogClosed()
    }

    /** Finds the turn off button, clicks on it and asserts that the dialog has closed. */
    fun clickOnTurnOffAssertClosed() {
        offButton.click()
        if (waitToBecomeTrue { !uiDevice.hasObject(offButton) }.result !is WaitResult.WaitSuccess) {
            Log.d("QuickSettingsTileBase", "Retrying click due to b/339676505")
            offButton.click()
        }
        assertDialogClosed()
    }

    private companion object {
        const val FLASHLIGHT_TITLE_TAG = "flashlight_title"
        const val FLASHLIGHT_TITLE_TEXT = "Flashlight Strength"
        const val FLASHLIGHT_DONE_TAG = "flashlight_done"
        const val FLASHLIGHT_DONE_TEXT = "Done"
        const val FLASHLIGHT_OFF_TAG = "flashlight_off"
        const val FLASHLIGHT_OFF_TEXT = "Turn off"
    }
}

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

import android.platform.systemui_tapl.utils.DeviceUtils.sysuiResSelector
import android.platform.uiautomatorhelpers.DeviceHelpers.assertInvisible
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.click
import androidx.test.uiautomator.By

/** System UI test automation object representing QS edit mode. */
class QSEditMode internal constructor() {
    init {
        CURRENT_TILES_GRID_SELECTOR.assertVisible()
        EDIT_MODE_TITLE_SELECTOR.assertVisible()
    }

    /** Closes edit mode and returns to QS. */
    fun close() {
        BACK_ARROW_SELECTOR.click()
        CURRENT_TILES_GRID_SELECTOR.assertInvisible { "QS edit mode is visible" }
    }

    private companion object {
        // https://hsv.googleplex.com/4784770226585600?node=20
        private val CURRENT_TILES_GRID_SELECTOR = sysuiResSelector("CurrentTilesGrid")

        // https://hsv.googleplex.com/4784770226585600?node=97
        private val EDIT_MODE_TITLE_SELECTOR = By.text("Edit tiles")

        // https://hsv.googleplex.com/4784770226585600?node=95
        private val BACK_ARROW_SELECTOR = By.desc("Navigate up")
    }
}

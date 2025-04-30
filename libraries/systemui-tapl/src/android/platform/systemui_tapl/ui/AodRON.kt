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

import android.platform.systemui_tapl.utils.DeviceUtils.androidResSelector
import android.platform.uiautomatorhelpers.DeviceHelpers.assertVisible
import android.platform.uiautomatorhelpers.DeviceHelpers.waitForObj
import androidx.test.uiautomator.By

class AodRON internal constructor() {
    init {
        LockScreen.RON_AOD_SKELETON.assertVisible { "RON Skeleton is not visible" }
    }

    fun title(title: String): AodRON = also {
        waitForObj(notificationByTitleSelector(title)) { "Notification Title is not visible." }
    }

    private fun notificationByTitleSelector(title: String) =
        By.copy(LockScreen.RON_AOD_SKELETON).hasDescendant(androidResSelector("title").text(title))
}

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

package platform.test.desktop

import android.platform.uiautomatorhelpers.DeviceHelpers.shell
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Rule that sets a shade display policy.
 *
 * See [ShadePrimaryDisplayCommand] for possible policies.
 */
class ShadeDisplayGoesAroundTestRule(private val policy: String = STATUS_BAR_POLICY) :
    TestWatcher() {

    override fun starting(description: Description?) {
        super.starting(description)
        shell("$BASE_COMMAND $policy")
    }

    override fun finished(description: Description?) {
        super.finished(description)
        shell("$BASE_COMMAND reset")
    }

    private companion object {
        const val BASE_COMMAND = "cmd statusbar shade_display_override"
        const val STATUS_BAR_POLICY = "status_bar_latest_touch"
    }
}

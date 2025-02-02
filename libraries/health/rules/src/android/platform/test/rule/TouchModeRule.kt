/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.platform.test.rule

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.Description

/**
 * Ensures that the test is executed with specified touch mode.
 * When device is in touch mode it will show virtual keyboard when focusing input fields,
 * when touch mode is disabled it will rely only on physical keyboard event and won't show
 * virtual keyboard.
 *
 * By default the rule ensures that touch mode is enabled.
 */
class TouchModeRule(private val enabled: Boolean = true) : TestWatcher() {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    override fun starting(description: Description?) {
        instrumentation.setInTouchMode(enabled)
    }

    override fun finished(description: Description?) {
        instrumentation.resetInTouchMode()
    }
}

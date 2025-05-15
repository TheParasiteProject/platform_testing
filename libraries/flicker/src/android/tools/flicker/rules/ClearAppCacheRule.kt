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

package android.tools.flicker.rules

import android.app.Instrumentation
import android.tools.FLICKER_TAG
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class ClearAppCacheRule : TestWatcher() {
    override fun finished(description: Description?) {
        super.finished(description)

        val instr: Instrumentation = InstrumentationRegistry.getInstrumentation()
        val rmCommand = "rm -rf /data/user/0/${instr.targetContext.packageName}/cache/withTracing*"
        Log.d(FLICKER_TAG, "Cleaning up cache directory with $rmCommand")
        instr.uiAutomation.executeShellCommand(rmCommand)
    }
}

/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.tools.testrules

import android.annotation.SuppressLint
import android.tools.flicker.datastore.DataStore
import android.tools.testutils.TEST_SCENARIO
import android.tools.testutils.TEST_SCENARIO_KEY
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@SuppressLint("VisibleForTests")
class DataStoreCleanupRule : TestWatcher() {
    override fun starting(description: Description?) {
        super.starting(description)
        resetDataStore(TEST_SCENARIO.key)
        resetDataStore(TEST_SCENARIO_KEY)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        resetDataStore(TEST_SCENARIO.key)
        resetDataStore(TEST_SCENARIO_KEY)
    }

    private fun resetDataStore(key: String) {
        val backup = DataStore.backup()
        DataStore.clear()

        if (backup.cachedResults.containsKey(key)) {
            backup.cachedResults.remove(key)
        }

        if (backup.cachedFlickerServiceAssertions.containsKey(key)) {
            backup.cachedFlickerServiceAssertions.remove(key)
        }

        DataStore.restore(backup)
    }
}

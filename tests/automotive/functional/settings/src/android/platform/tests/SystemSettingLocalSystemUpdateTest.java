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

package android.platform.tests;

import static junit.framework.Assert.assertTrue;

import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.IAutoSystemSettingsHelper;
import android.platform.helpers.SettingsConstants;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SystemSettingLocalSystemUpdateTest {
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private HelperAccessor<IAutoSystemSettingsHelper> mSystemSettingsHelper;
    private static final String LOG_TAG = SystemSettingLocalSystemUpdateTest.class.getSimpleName();

    public SystemSettingLocalSystemUpdateTest() throws Exception {
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
        mSystemSettingsHelper = new HelperAccessor<>(IAutoSystemSettingsHelper.class);
    }

    @Before
    public void openSystemFacet() {
        Log.i(LOG_TAG, "Act: Open System Setting");
        mSettingHelper.get().openSetting(SettingsConstants.SYSTEM_SETTINGS);
        Log.i(LOG_TAG, "Assert: System Settings is Open");
        assertTrue(
                "System settings did not open",
                mSettingHelper.get().checkMenuExists("Languages & input"));
    }

    @After
    public void goBackToSettingsScreen() {
        mSettingHelper.get().goBackToSettingsScreen();
    }

    @Test
    public void testLocalSystemUpdate() {
        Log.i(LOG_TAG, "Act: Open System Setting");
        mSystemSettingsHelper.get().openLocalSystemUpdate();
        Log.i(LOG_TAG, "Assert: Local System Update is open");
        assertTrue(
                "Local System Update is not open",
                mSettingHelper.get().checkMenuExists("Local System Update"));
        Log.i(LOG_TAG, "Assert: Volumes is present");
        assertTrue(
                "Volumes is not present",
                mSystemSettingsHelper.get().isLocalSystemUpdateVolumesPresent());
    }
}

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

import android.platform.helpers.AutomotiveConfigConstants;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.IAutoSystemSettingsHelper;
import android.platform.helpers.IAutoUISettingsHelper;
import android.platform.helpers.SettingsConstants;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class StorageSettingTest {
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private HelperAccessor<IAutoUISettingsHelper> mSettingsUIHelper;
    private HelperAccessor<IAutoSystemSettingsHelper> mSystemSettingsHelper;
    private static final String LOG_TAG = StorageSettingTest.class.getSimpleName();

    public StorageSettingTest() throws Exception {
        mSettingsUIHelper = new HelperAccessor<>(IAutoUISettingsHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
        mSystemSettingsHelper = new HelperAccessor<>(IAutoSystemSettingsHelper.class);
    }

    @Before
    public void openSystemStorageFacet() {
        Log.i(LOG_TAG, "Act: Open the system settings");
        mSettingHelper.get().openSetting(SettingsConstants.SYSTEM_SETTINGS);
        Log.i(LOG_TAG, "Assert: System settings is opened ");
        assertTrue(
                "System settings did not open",
                mSettingHelper.get().checkMenuExists("Languages & input"));
        Log.i(LOG_TAG, "Act: Open Storage Menu ");
        mSystemSettingsHelper.get().openStorageMenu();
    }

    @After
    public void goBackToSettingsScreen() {
        mSettingHelper.get().goBackToSettingsScreen();
    }

    @Test
    public void testMusicAndAudio() {
        Log.i(LOG_TAG, "Assert: Music and Audio and Usage are Present");
        assertTrue(
                "Music and Audio and Usage are not Present",
                mSystemSettingsHelper
                        .get()
                        .verifyUsageinGB(AutomotiveConfigConstants.STORAGE_MUSIC_AUDIO_SETTINGS));
        Log.i(LOG_TAG, "Assert: Music and Audio are present");
        assertTrue(
                "Music and Audio is not present",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.STORAGE_MUSIC_AUDIO_SETTINGS));
        Log.i(LOG_TAG, "Act: Music Audio Settings");
        mSettingsUIHelper
                .get()
                .openUIOptions(AutomotiveConfigConstants.STORAGE_MUSIC_AUDIO_SETTINGS);
        Log.i(LOG_TAG, "Assert: Music and Audio is open");
        assertTrue(
                "Music and Audio is not open",
                mSettingHelper.get().checkMenuExists("Hide system apps"));
    }

    @Test
    public void testOtherApps() {
        Log.i(LOG_TAG, "Assert: Other apps Usage is Present");
        assertTrue(
                "Other apps Usage is not Present",
                mSystemSettingsHelper
                        .get()
                        .verifyUsageinGB(AutomotiveConfigConstants.STORAGE_OTHER_APPS_SETTINGS));
        Log.i(LOG_TAG, "Assert: Other apps is Present");
        assertTrue(
                "Other apps is not present",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.STORAGE_OTHER_APPS_SETTINGS));
        Log.i(LOG_TAG, "Assert: Other apps Storage settings");
        mSettingsUIHelper
                .get()
                .openUIOptions(AutomotiveConfigConstants.STORAGE_OTHER_APPS_SETTINGS);
        Log.i(LOG_TAG, "Assert: Other apps is open");
        assertTrue(
                "Other apps is not open", mSettingHelper.get().checkMenuExists("Hide system apps"));
    }

    @Test
    public void testFiles() {
        Log.i(LOG_TAG, "Assert: Files Usage is Present in GBs");
        assertTrue(
                "Files Usage is not Present",
                mSystemSettingsHelper
                        .get()
                        .verifyUsageinGB(AutomotiveConfigConstants.STORAGE_FILES_SETTINGS));
        Log.i(LOG_TAG, "Assert: Files is Present under Storage");
        assertTrue(
                "Files Usage is not present",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.STORAGE_FILES_SETTINGS));
        mSettingsUIHelper.get().openUIOptions(AutomotiveConfigConstants.STORAGE_FILES_SETTINGS);
    }

    @Test
    public void testSystem() {
        Log.i(LOG_TAG, "Assert: System Usage is Present");
        assertTrue(
                "System Usage is not Present",
                mSystemSettingsHelper
                        .get()
                        .verifyUsageinGB(AutomotiveConfigConstants.STORAGE_SYSTEM_SETTINGS));
        Log.i(LOG_TAG, "Assert: System is Present under Storage");
        assertTrue(
                "System is not present",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.STORAGE_SYSTEM_SETTINGS));
        mSettingsUIHelper.get().openUIOptions(AutomotiveConfigConstants.STORAGE_SYSTEM_SETTINGS);
    }
}

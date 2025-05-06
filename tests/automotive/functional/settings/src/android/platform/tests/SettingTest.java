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

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import android.platform.helpers.AutomotiveConfigConstants;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoAppInfoSettingsHelper;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.IAutoUISettingsHelper;
import android.platform.helpers.SettingsConstants;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SettingTest {
    private static final int DAY_MODE_VALUE = 0;
    private HelperAccessor<IAutoAppInfoSettingsHelper> mAppInfoSettingsHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private HelperAccessor<IAutoUISettingsHelper> mSettingsUIHelper;
    private static final String LOG_TAG = SettingTest.class.getSimpleName();

    public SettingTest() throws Exception {
        mSettingsUIHelper = new HelperAccessor<>(IAutoUISettingsHelper.class);
        mAppInfoSettingsHelper = new HelperAccessor<>(IAutoAppInfoSettingsHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
    }


    @After
    public void goBackToSettingsScreen() {
        mSettingHelper.get().goBackToSettingsScreen();
    }

    @Test
    public void testDisplaySettings() {
        Log.i(LOG_TAG, "Act: Open the Display Setting");
        mSettingHelper.get().openSetting(SettingsConstants.DISPLAY_SETTINGS);
        Log.i(LOG_TAG, "Assert: Display Setting is open");
        assertTrue(
                "Display Setting did not open",
                mSettingHelper.get().checkMenuExists("Brightness level"));
    }

    @Test
    public void testSoundSettings() {
        Log.i(LOG_TAG, "Act: Open the Sound Setting");
        mSettingHelper.get().openSetting(SettingsConstants.SOUND_SETTINGS);
        Log.i(LOG_TAG, "Assert: Sound Setting is open");
        assertTrue(
                "Sound setting did not open",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.SOUND_SETTING_INCALL));
    }

    @Test
    public void testAppinfoSettings() {
        Log.i(LOG_TAG, "Act: Open the App Setting");
        mSettingHelper.get().openSetting(SettingsConstants.APPS_SETTINGS);
        Log.i(LOG_TAG, "Assert: App Setting is open");
        assertTrue(
                "Apps setting did not open",
                mSettingHelper.get().checkMenuExists("Recently opened")
                        || mSettingHelper.get().checkMenuExists("Reset app grid to A-Z order"));
        mAppInfoSettingsHelper.get().showAllApps();
    }

    @Test
    public void testAccountsSettings() {
        Log.i(LOG_TAG, "Act: Open the Account Setting");
        mSettingHelper.get().openSetting(SettingsConstants.PROFILE_ACCOUNT_SETTINGS);
        Log.i(LOG_TAG, "Assert: Profiles and accounts settings is open");
        assertTrue(
                "Profiles and accounts settings did not open",
                mSettingHelper.get().checkMenuExists("Add a profile"));
    }

    @Test
    public void testSystemSettings() {
        Log.i(LOG_TAG, "Act: Open the System Setting");
        mSettingHelper.get().openSetting(SettingsConstants.SYSTEM_SETTINGS);
        Log.i(LOG_TAG, "Assert: System Setting is opened");
        assertTrue(
                "System settings did not open",
                mSettingHelper.get().checkMenuExists("Languages & input"));
    }

    @Test
    public void testBluetoothSettings() {
        Log.i(LOG_TAG, "Act: Open the Bluetooth Setting");
        mSettingHelper.get().openSetting(SettingsConstants.BLUETOOTH_SETTINGS);
        Log.i(LOG_TAG, "Assert: Bluetooth Setting is opened");
        assertTrue(
                "Bluetooth Setting did not open",
                mSettingHelper.get().checkMenuExists("Pair new device"));
        Log.i(LOG_TAG, "Act: Turn Off the Bluetooth");
        mSettingHelper.get().turnOnOffBluetooth(false);
        Log.i(LOG_TAG, "Assert: Blue Setting is Off");
        assertFalse(mSettingHelper.get().isBluetoothOn());
        Log.i(LOG_TAG, "Act: Turn On the Bluetooth");
        mSettingHelper.get().turnOnOffBluetooth(true);
    }
}

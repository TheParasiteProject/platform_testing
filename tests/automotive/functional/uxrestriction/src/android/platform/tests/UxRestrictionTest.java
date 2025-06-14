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
import android.platform.helpers.IAutoAppGridHelper;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.IAutoVehicleHardKeysHelper;
import android.platform.helpers.IAutoVehicleHardKeysHelper.DrivingState;
import android.platform.helpers.SettingsConstants;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UxRestrictionTest {
    private static HelperAccessor<IAutoSettingHelper> sSettingHelper =
            new HelperAccessor<>(IAutoSettingHelper.class);
    private HelperAccessor<IAutoAppGridHelper> mAppGridHelper;
    private static HelperAccessor<IAutoVehicleHardKeysHelper> sHardKeysHelper =
            new HelperAccessor<>(IAutoVehicleHardKeysHelper.class);
    private static final String LOG_TAG = UxRestrictionTest.class.getSimpleName();

    private static final int SPEED_TWENTY = 20;
    private static final int SPEED_ZERO = 0;

    public UxRestrictionTest() throws Exception {
        mAppGridHelper = new HelperAccessor<>(IAutoAppGridHelper.class);
    }

    @BeforeClass
    public static void enableDrivingMode() {
        Log.i(LOG_TAG, "Act: Set Driving State to Moving");
        sHardKeysHelper.get().setDrivingState(DrivingState.MOVING);
        Log.i(LOG_TAG, "Act: Set Driving speed to twenty");
        sHardKeysHelper.get().setSpeed(SPEED_TWENTY);
    }

    @AfterClass
    public static void disableDrivingMode() {
        Log.i(LOG_TAG, "Act: Back to Settings");
        sSettingHelper.get().goBackToSettingsScreen();
        Log.i(LOG_TAG, "Act: Set Driving speed to twenty");
        sHardKeysHelper.get().setSpeed(SPEED_ZERO);
        Log.i(LOG_TAG, "Act: Set Driving State to Parking");
        sHardKeysHelper.get().setDrivingState(DrivingState.PARKED);
    }

    @Test
    public void testRestrictedSoundSettings() {
        Log.i(LOG_TAG, "Act: Open Sound Setting");
        sSettingHelper.get().openSetting(SettingsConstants.SOUND_SETTINGS);
        Log.i(LOG_TAG, "Act: Get Page Title");
        String currentTitle = sSettingHelper.get().getSettingsPageTitleText();
        Log.i(LOG_TAG, "Assert: Sound Setting is open");
        assertTrue(
                "Sound setting did not open",
                sSettingHelper
                        .get()
                        .scrollAndCheckMenuExists(AutomotiveConfigConstants.SOUND_SETTING_INCALL));
        Log.i(LOG_TAG, "Act: Open In call volume option");
        sSettingHelper.get().openMenuWith("In-call Volume");
        Log.i(LOG_TAG, "Act: Get New Page Title");
        String newTitle = sSettingHelper.get().getSettingsPageTitleText();
        Log.i(LOG_TAG, "Assert: Phone Ringtone Setting is disabled");
        assertTrue("Phone ringtone setting is not disabled", currentTitle.equals(newTitle));
    }

    @Test
    public void testRestrictedNetworkSettings() {
        Log.i(LOG_TAG, "Act: Open Network & Internet Setting");
        sSettingHelper.get().openSetting(SettingsConstants.NETWORK_AND_INTERNET_SETTINGS);
        Log.i(LOG_TAG, "Assert: Network and Internet settings is Open");
        assertTrue(
                "Network and Internet settings did not open",
                sSettingHelper.get().checkMenuExists("Hotspot"));
        Log.i(LOG_TAG, "Act: Get Current Hotspot State");
        Boolean currentHotspotState = sSettingHelper.get().isHotspotOn();
        Log.i(LOG_TAG, "Act: Toggle hotspot on");
        sSettingHelper.get().toggleHotspot();
        Log.i(LOG_TAG, "Act: Get status of Hotspot state");
        Boolean newHotspotState = sSettingHelper.get().isHotspotOn();
        Log.i(LOG_TAG, "Act: Hotspot is working");
        assertFalse("Hotspot is not working", currentHotspotState.equals(newHotspotState));
        Log.i(LOG_TAG, "Act: Toggle hotspot off");
        sSettingHelper.get().toggleHotspot();
    }

    @Test
    public void testRestrictedBluetoothSettings() {
        Log.i(LOG_TAG, "Act: Open Bluetooth Setting");
        sSettingHelper.get().openSetting(SettingsConstants.BLUETOOTH_SETTINGS);
        Log.i(LOG_TAG, "Assert: Bluetooth Setting is On");
        assertTrue(
                "Bluetooth Setting did not open",
                sSettingHelper.get().checkMenuExists("Pair new device"));
        Log.i(LOG_TAG, "Act: Get Page Title text");
        String currentTitle = sSettingHelper.get().getSettingsPageTitleText();
        Log.i(LOG_TAG, "Act: Pair new device");
        sSettingHelper.get().openMenuWith("Pair new device");
        Log.i(LOG_TAG, "Act: Get new Page Title text");
        String newTitle = sSettingHelper.get().getSettingsPageTitleText();
        Log.i(LOG_TAG, "Assert: Bluetooth Setting is disabled ");
        assertTrue("Bluetooth setting is not disabled", currentTitle.equals(newTitle));
    }

    @Test
    public void testRestrictedAppSettings() {
        Log.i(LOG_TAG, "Act: Open Settings screen");
        sSettingHelper.get().openFullSettings();
        Log.i(LOG_TAG, "Act: Get Page Title");
        String currentTitle = sSettingHelper.get().getSettingsPageTitleText();
        Log.i(LOG_TAG, "Act: Open Apps settings");
        sSettingHelper.get().openSetting(SettingsConstants.APPS_SETTINGS);
        Log.i(LOG_TAG, "Assert: Apps is disabled");
        assertFalse("Apps is not disabled", sSettingHelper.get().checkMenuExists("View all"));
        Log.i(LOG_TAG, "Act: Get New Page Title");
        String newTitle = sSettingHelper.get().getSettingsPageTitleText();
        Log.i(LOG_TAG, "Assert: Apps is disabled");
        assertTrue("Apps & notification settings is not disabled", currentTitle.equals(newTitle));
    }

    @Test
    public void testRestrictedProfilesAndAccountsSettings() {
        Log.i(LOG_TAG, "Act: Open Settings");
        sSettingHelper.get().openFullSettings();
        Log.i(LOG_TAG, "Act: Get Page Title");
        String currentTitle = sSettingHelper.get().getSettingsPageTitleText();
        Log.i(LOG_TAG, "Act: Open Profile Account Setting");
        sSettingHelper.get().openSetting(SettingsConstants.PROFILE_ACCOUNT_SETTINGS);
        Log.i(LOG_TAG, "Assert: Profile and Accounts Setting is disabled");
        assertFalse(
                "Profiles and accounts settings is not disabled",
                sSettingHelper.get().checkMenuExists("Add a profile"));
        Log.i(LOG_TAG, "Act: Get New Page Title");
        String newTitle = sSettingHelper.get().getSettingsPageTitleText();
        Log.i(LOG_TAG, "Assert: Profiles and Accounts is disabled");
        assertTrue("Profiles and accounts settings is not disabled", currentTitle.equals(newTitle));
    }

    @Test
    public void testRestrictedSecuritySettings() {
        Log.i(LOG_TAG, "Act: Open Settings");
        sSettingHelper.get().openFullSettings();
        Log.i(LOG_TAG, "Act: Get Page Title");
        String currentTitle = sSettingHelper.get().getSettingsPageTitleText();
        Log.i(LOG_TAG, "Act: Open Security Setting");
        sSettingHelper.get().openSetting(SettingsConstants.SECURITY_SETTINGS);
        Log.i(LOG_TAG, "Act: Security Setting is disabled");
        assertFalse(
                "Security settings is not disabled",
                sSettingHelper.get().checkMenuExists("Profile lock"));
        Log.i(LOG_TAG, "Act: Get New Page Title");
        String newTitle = sSettingHelper.get().getSettingsPageTitleText();
        Log.i(LOG_TAG, "Assert: Secutiy setting is disabled");
        assertTrue("Security settings is not disabled", currentTitle.equals(newTitle));
    }

    @Test
    public void testRestrictedSystemSettings() {
        Log.i(LOG_TAG, "Act: Open Settings");
        sSettingHelper.get().openFullSettings();
        Log.i(LOG_TAG, "Act: Get Page Title");
        String currentTitle = sSettingHelper.get().getSettingsPageTitleText();
        Log.i(LOG_TAG, "Act: Open System Setting");
        sSettingHelper.get().openSetting(SettingsConstants.SYSTEM_SETTINGS);
        Log.i(LOG_TAG, "Assert: System Setting is disabled");
        assertFalse(
                "System settings is not disabled",
                sSettingHelper.get().checkMenuExists("Languages & input"));
        Log.i(LOG_TAG, "Act: Get New Page Title");
        String newTitle = sSettingHelper.get().getSettingsPageTitleText();
        Log.i(LOG_TAG, "Assert: System Setting is disabled");
        assertTrue("System settings is not disabled", currentTitle.equals(newTitle));
    }
}

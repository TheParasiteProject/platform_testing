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
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.IAutoUISettingsHelper;
import android.platform.helpers.SettingsConstants;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WifiSettingTest {
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private HelperAccessor<IAutoUISettingsHelper> mSettingsUIHelper;
    private static final String LOG_TAG = WifiSettingTest.class.getSimpleName();

    public WifiSettingTest() throws Exception {
        mSettingsUIHelper = new HelperAccessor<>(IAutoUISettingsHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
    }


    @Before
    public void openNetworkSetting() {
        mSettingHelper.get().openSetting(SettingsConstants.NETWORK_AND_INTERNET_SETTINGS);
        assertTrue(
                "Network and Internet settings did not open",
                mSettingHelper.get().checkMenuExists("Hotspot"));
    }

    @After
    public void goBackToSettingsScreen() {
        mSettingHelper.get().goBackToSettingsScreen();
    }

    @Test
    public void testWifiSettings() {
        Log.i(LOG_TAG, "Act: Turn Off Wifi");
        mSettingHelper.get().turnOnOffWifi(false);
        Log.i(LOG_TAG, "Assert: Wifi is turned off");
        assertFalse(mSettingHelper.get().isWifiOn());
        Log.i(LOG_TAG, "Act: Turn On Wifi");
        mSettingHelper.get().turnOnOffWifi(true);
        Log.i(LOG_TAG, "Assert: Wifi is turned on");
        assertTrue(mSettingHelper.get().isWifiOn());
    }

    @Test
    public void testTurnOnOffHotspot() {
        Log.i(LOG_TAG, "Act: Turn On Hotspot");
        mSettingHelper.get().turnOnOffHotspot(true);
        Log.i(LOG_TAG, "Assert: Hotspot is On");
        assertTrue(mSettingHelper.get().isHotspotOn());
        Log.i(LOG_TAG, "Act: Turn Off Hotspot");
        mSettingHelper.get().turnOnOffHotspot(false);
        Log.i(LOG_TAG, "Assert: Hotspot is Off");
        assertFalse(mSettingHelper.get().isHotspotOn());
    }

    @Test
    public void testWifiPreferences() {
        Log.i(LOG_TAG, "Assert: Wi-Fi Preferences option is displayed");
        assertTrue(
                "Wi-Fi Preferences option is not displayed",
                mSettingsUIHelper.get().hasUIElement(AutomotiveConfigConstants.WIFI_PREFERENCES));
        Log.i(LOG_TAG, "Act: Open Wifi preferences");
        mSettingHelper.get().openMenuWith("Wi‑Fi preferences");
        Log.i(LOG_TAG, "Assert: Turn on Wi‑Fi automatically is displayed");
        assertTrue(
                "Turn on Wi‑Fi automatically is not displayed",
                mSettingHelper.get().checkMenuExists("Turn on Wi‑Fi automatically"));
        Log.i(LOG_TAG, "Assert: Turn on Wi-Fi automatically toggle is displayed");
        assertTrue(
                "Turn on Wi-Fi automatically toggle is not displayed",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.TURN_WIFI_AUTOMATICALLY_TOGGLE));
    }

    @Test
    public void testJoinOtherNetwork() {
        Log.i(LOG_TAG, "Assert: Join other network option is displayed");
        assertTrue(
                "Join other network option is not displayed",
                mSettingsUIHelper.get().hasUIElement(AutomotiveConfigConstants.JOIN_OTHER_NETWORK));
    }
}

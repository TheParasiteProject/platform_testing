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

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WifiSettingTest {
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private HelperAccessor<IAutoUISettingsHelper> mSettingsUIHelper;

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
        mSettingHelper.get().turnOnOffWifi(false);
        assertFalse(mSettingHelper.get().isWifiOn());
        mSettingHelper.get().turnOnOffWifi(true);
        assertTrue(mSettingHelper.get().isWifiOn());
    }

    @Test
    public void testTurnOnOffHotspot() {
        mSettingHelper.get().turnOnOffHotspot(true);
        assertTrue(mSettingHelper.get().isHotspotOn());
        mSettingHelper.get().turnOnOffHotspot(false);
    }

    @Test
    public void testWifiPreferences() {
        assertTrue(
                "Wi-Fi Preferences option is not displayed",
                mSettingsUIHelper.get().hasUIElement(AutomotiveConfigConstants.WIFI_PREFERENCES));
        mSettingHelper.get().openMenuWith("Wi‑Fi preferences");
        assertTrue(
                "Turn on Wi‑Fi automatically is not displayed",
                mSettingHelper.get().checkMenuExists("Turn on Wi‑Fi automatically"));
        assertTrue(
                "Turn on Wi-Fi automatically toggle is not displayed",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.TURN_WIFI_AUTOMATICALLY_TOGGLE));
    }

    @Test
    public void testJoinOtherNetwork() {
        assertTrue(
                "Join other network option is not displayed",
                mSettingsUIHelper.get().hasUIElement(AutomotiveConfigConstants.JOIN_OTHER_NETWORK));
    }
}

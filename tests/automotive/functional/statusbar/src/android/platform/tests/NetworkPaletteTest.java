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
import android.platform.helpers.IAutoStatusBarHelper;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class NetworkPaletteTest {

    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private HelperAccessor<IAutoStatusBarHelper> mStatusBarHelper;
    private static final String HOTSPOT = AutomotiveConfigConstants.NETWORK_PALETTE_HOTSPOT;
    private static final String WIFI = AutomotiveConfigConstants.NETWORK_PALETTE_WIFI;
    private static final String LOG_TAG = NetworkPaletteTest.class.getSimpleName();

    public NetworkPaletteTest() {
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
        mStatusBarHelper = new HelperAccessor<>(IAutoStatusBarHelper.class);
    }

    @Before
    public void openNetworkPalette() {
        Log.i(LOG_TAG, "Act: Open Network Palette.");
        mStatusBarHelper.get().openNetworkPalette();
        Log.i(LOG_TAG, "Assert: Bluetooth Palette is open");
        assertTrue(
                "Network Palette is not open",
                mSettingHelper.get().checkMenuExists("Network & internet settings"));
    }

    @After
    public void goBackToHomeScreen() {
        mSettingHelper.get().exit();
    }

    @Test
    public void testHotspotName() {
        Log.i(LOG_TAG, "Act: Enable the hotspot");
        if (!mStatusBarHelper.get().isNetworkSwitchEnabled(HOTSPOT)) {
            mStatusBarHelper.get().networkPaletteToggleOnOff(HOTSPOT);
        }
        Log.i(LOG_TAG, "Assert: Hotspot Name and password is displayed");
        assertTrue(
                "Hotspot Name and password is not displayed",
                mStatusBarHelper.get().isHotspotNameDisplayed());
        Log.i(LOG_TAG, "Act: Turn Off the hotspot");
        mStatusBarHelper.get().networkPaletteToggleOnOff(HOTSPOT);
        Log.i(LOG_TAG, "Assert: Hotspot is not enabled");
        assertFalse("Hotspot is enabled", mStatusBarHelper.get().isNetworkSwitchEnabled(HOTSPOT));
    }

    @Test
    public void testWifiName() {
        Log.i(LOG_TAG, "Act: Enable the wifi");
        if (!mSettingHelper.get().isWifiOn()) {
            mStatusBarHelper.get().networkPaletteToggleOnOff(WIFI);
        }
        Log.i(LOG_TAG, "Assert: Wifi is turned ON and Wifi name is displayed");
        assertTrue("Wi-Fi is not enabled", mSettingHelper.get().isWifiOn());
        assertTrue("Wi-Fi Name is not displayed", mStatusBarHelper.get().isWifiNameDisplayed());
        Log.i(LOG_TAG, "Act: Turn Off Wifi");
        mStatusBarHelper.get().networkPaletteToggleOnOff(WIFI);
        Log.i(LOG_TAG, "Assert: Wifi is not enabled");
        assertFalse("Wi-Fi is enabled", mSettingHelper.get().isWifiOn());
        Log.i(LOG_TAG, "Act: Enable the wifi again");
        mStatusBarHelper.get().networkPaletteToggleOnOff(WIFI);
        Log.i(LOG_TAG, "Assert: Wifi is turned ON and Wifi name is displayed");
        assertTrue("Wi-Fi is not enabled", mSettingHelper.get().isWifiOn());
        assertTrue("Wi-Fi Name is not displayed", mStatusBarHelper.get().isWifiNameDisplayed());
        Log.i(LOG_TAG, "Act: Turn Off Wifi");
        mStatusBarHelper.get().networkPaletteToggleOnOff(WIFI);
        Log.i(LOG_TAG, "Assert: Wifi is not enabled");
        assertFalse("Wi-Fi is enabled", mSettingHelper.get().isWifiOn());
    }

    @Test
    public void testWifiAndHotspotEnabledTogether() {
        Log.i(LOG_TAG, "Act: Check and Enable the wifi & hotspot");
        if (!mSettingHelper.get().isWifiOn()) {
            mStatusBarHelper.get().networkPaletteToggleOnOff(WIFI);
        }
        if (!mStatusBarHelper.get().isNetworkSwitchEnabled(HOTSPOT)) {
            mStatusBarHelper.get().networkPaletteToggleOnOff(HOTSPOT);
        }
        Log.i(LOG_TAG, "Assert: Wifi and hotpsot are turned ON");
        assertTrue("Wi-Fi is not enabled", mSettingHelper.get().isWifiOn());
        assertTrue(
                "Hotspot is not enabled", mStatusBarHelper.get().isNetworkSwitchEnabled(HOTSPOT));
        Log.i(LOG_TAG, "Act: Turn Off Wifi and Hotspot");
        mStatusBarHelper.get().networkPaletteToggleOnOff(WIFI);
        mStatusBarHelper.get().networkPaletteToggleOnOff(HOTSPOT);
        Log.i(LOG_TAG, "Assert: Wifi and Hotsport are not enabled");
        assertFalse("Wi-Fi is enabled", mSettingHelper.get().isWifiOn());
        assertFalse("Hotspot is enabled", mStatusBarHelper.get().isNetworkSwitchEnabled(HOTSPOT));
        Log.i(LOG_TAG, "Act: Turn on Wifi and Hotspot");
        mStatusBarHelper.get().networkPaletteToggleOnOff(WIFI);
        mStatusBarHelper.get().networkPaletteToggleOnOff(HOTSPOT);
        Log.i(LOG_TAG, "Assert: Wifi and Hotsport are enabled");
        assertTrue("Wi-Fi is not enabled", mSettingHelper.get().isWifiOn());
        assertTrue(
                "Hotspot is not enabled", mStatusBarHelper.get().isNetworkSwitchEnabled(HOTSPOT));
    }

    @Test
    public void testNetworkAndinternetSettings() {
        Log.i(LOG_TAG, "Act: Open the network settings");
        mSettingHelper.get().openMenuWith("Network & internet settings");
        Log.i(LOG_TAG, "Act: Click on Connected Wifi");
        mStatusBarHelper.get().clickOnConnectedWifi();
        Log.i(LOG_TAG, "Act: Forget the Wifi");
        mStatusBarHelper.get().forgetWifi();
        Log.i(LOG_TAG, "Act: Open the Network Palette");
        openNetworkPalette();
        Log.i(LOG_TAG, "Act: Check and Enable the wifi & hotspot");
        if (!mSettingHelper.get().isWifiOn()) {
            mStatusBarHelper.get().networkPaletteToggleOnOff(WIFI);
        }
        if (!mStatusBarHelper.get().isNetworkSwitchEnabled(HOTSPOT)) {
            mStatusBarHelper.get().networkPaletteToggleOnOff(HOTSPOT);
        }
        Log.i(LOG_TAG, "Assert: Wifi and Hotsport are enabled");
        assertTrue("Wi-Fi is not enabled", mSettingHelper.get().isWifiOn());
        assertTrue(
                "Hotspot is not enabled", mStatusBarHelper.get().isNetworkSwitchEnabled(HOTSPOT));
    }
}

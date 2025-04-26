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
import android.platform.helpers.IAutoStatusBarHelper;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BluetoothPaletteTest {
    private HelperAccessor<IAutoStatusBarHelper> mStatusBarHelper;
    private static final String LOG_TAG = BluetoothPaletteTest.class.getSimpleName();

    public BluetoothPaletteTest() {
        mStatusBarHelper = new HelperAccessor<>(IAutoStatusBarHelper.class);
    }

    @Before
    public void openBluetoothPalette() {
        mStatusBarHelper.get().openBluetoothPalette();
    }

    @Test
    public void testDefaultStatusOfBlueToothPalette() {
        Log.i(LOG_TAG, "Act: Turn on Bluettooth.");
        mStatusBarHelper.get().turnOnOffBluetooth(true);
        Log.i(LOG_TAG, "Assert: Bluetooth toggle button is displayed");
        assertTrue(
                "Bluetooth toggle button is not displayed",
                mStatusBarHelper.get().hasBluetoothSwitch());
        Log.i(LOG_TAG, "Assert: Bluetooth toggle button ON message is displayed");
        assertTrue(
                "Bluetooth toggle button ON message is not displayed",
                mStatusBarHelper.get().hasToggleOnMessage());
    }



    @Test
    public void testBluetoothDisableMessage() {
        Log.i(LOG_TAG, "Act: Turn off Bluettooth.");
        mStatusBarHelper.get().turnOnOffBluetooth(false);
        Log.i(LOG_TAG, "Assert: Bluetooth toggle OFF message is displayed");
        assertTrue(
                "Bluetooth toggle button OFF message is not displayed ",
                mStatusBarHelper.get().hasToggleOffMessage());
        Log.i(LOG_TAG, "Act: Turn ON Bluettooth.");
        mStatusBarHelper.get().turnOnOffBluetooth(true);
    }

    @Test
    public void testOpenBluetoothSettings() {
        Log.i(LOG_TAG, "Act: Open Bluetooth Settings");
        mStatusBarHelper.get().openBluetoothSettings();
        Log.i(LOG_TAG, "Assert: Bluetooth settings page title is displayed");
        assertTrue(
                "Bluetooth settings page title is not displayed",
                mStatusBarHelper.get().hasBluetoothSettingsPageTitle());
    }


    @After
    public void setup() {
        mStatusBarHelper.get().open();
    }
}

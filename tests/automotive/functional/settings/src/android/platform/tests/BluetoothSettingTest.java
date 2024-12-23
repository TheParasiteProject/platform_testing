/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.platform.helpers.IAutoBluetoothSettingsHelper;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.SettingsConstants;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BluetoothSettingTest {
    private HelperAccessor<IAutoBluetoothSettingsHelper> mBluetoothSettingHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;

    public BluetoothSettingTest() throws Exception {
        mBluetoothSettingHelper = new HelperAccessor<>(IAutoBluetoothSettingsHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
    }

    @After
    public void goBackToSettingsScreen() {
        mSettingHelper.get().goBackToSettingsScreen();
    }

    @Test
    public void testBluetoothDefaultState() {
        mSettingHelper.get().openSetting(SettingsConstants.BLUETOOTH_SETTINGS);
        assertTrue(
                "Bluetooth default state is not turned on in the System",
                mSettingHelper.get().isBluetoothOn());
        assertTrue(
                "Use Bluetooth toggle is not turned on in the Settings",
                mBluetoothSettingHelper.get().isUseBluetoothToggleChecked());
    }
}

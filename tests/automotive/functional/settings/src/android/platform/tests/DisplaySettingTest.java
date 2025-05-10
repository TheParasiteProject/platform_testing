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

import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoDisplaySettingsHelper;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.SettingsConstants;
import android.util.Log;

import org.junit.Test;

public class DisplaySettingTest {

    private final HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private HelperAccessor<IAutoDisplaySettingsHelper> mDisplaySettingsHelper;
    private static final String LOG_TAG = DisplaySettingTest.class.getSimpleName();

    public DisplaySettingTest() {
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
        mDisplaySettingsHelper = new HelperAccessor<>(IAutoDisplaySettingsHelper.class);
    }

    @Test
    public void testBrightnessIncrease() {
        Log.i(LOG_TAG, "Act: Open the Display Setting");
        mSettingHelper.get().openSetting(SettingsConstants.DISPLAY_SETTINGS);
        assertTrue(
                "Display Setting did not open",
                mSettingHelper.get().checkMenuExists("Brightness level"));

        int lowBrightness = mSettingHelper.get().setBrightness(0.1f);

        // Increase the screen brightness
        int highBrightness = mSettingHelper.get().setBrightness(0.9f);

        // Verify that the screen brightness has changed.
        assertTrue(
                "Brightness was not increased (from "
                        + lowBrightness
                        + " to "
                        + highBrightness
                        + ")",
                lowBrightness < highBrightness);
    }

    @Test
    public void testAdaptiveBrightnessDefaultValue() {
        Log.i(LOG_TAG, "Act: Open the Display Setting");
        mSettingHelper.get().openSetting(SettingsConstants.DISPLAY_SETTINGS);
        Log.i(LOG_TAG, "Act: Settings did not open");
        assertTrue(
                "Display Setting did not open",
                mSettingHelper.get().checkMenuExists("Adaptive brightness"));

        Log.i(LOG_TAG, "Assert: Adaptive brightness is disabled");
        assertFalse(
                "Adaptive Brightness was enabled, when it should be disabled by default.",
                mDisplaySettingsHelper.get().isAdaptiveBrightnessEnabled());
    }

    @Test
    public void testAdaptiveBrightnessToggle() {
        Log.i(LOG_TAG, "Act: Open the Display Setting");
        mSettingHelper.get().openSetting(SettingsConstants.DISPLAY_SETTINGS);
        Log.i(LOG_TAG, "Assert: Display setting is opened");
        assertTrue(
                "Display Setting did not open",
                mSettingHelper.get().checkMenuExists("Adaptive brightness"));

        // Verify that Adaptive Brightness can be toggled.
        boolean startSetting = mDisplaySettingsHelper.get().isAdaptiveBrightnessEnabled();
        Log.i(LOG_TAG, "Act: Toggle Adaptive brightness");
        mDisplaySettingsHelper.get().toggleAdaptiveBrightness();
        boolean endSetting = mDisplaySettingsHelper.get().isAdaptiveBrightnessEnabled();

        Log.i(LOG_TAG, "Assert: Adaptive Brightness Value is changed after toggle");
        assertFalse(
                String.format(
                        "Adaptive Brightness value did not change after toggle;"
                                + "Value started at %b and ended at %b ",
                        startSetting, endSetting),
                startSetting == endSetting);
    }
}

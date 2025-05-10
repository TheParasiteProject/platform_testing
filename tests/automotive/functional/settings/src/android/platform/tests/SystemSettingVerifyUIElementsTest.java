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
public class SystemSettingVerifyUIElementsTest {
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private HelperAccessor<IAutoUISettingsHelper> mSettingsUIHelper;
    private static final String LOG_TAG = SystemSettingVerifyUIElementsTest.class.getSimpleName();

    public SystemSettingVerifyUIElementsTest() throws Exception {
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
        mSettingsUIHelper = new HelperAccessor<>(IAutoUISettingsHelper.class);
    }

    @Before
    public void openSystemFacet() {
        Log.i(LOG_TAG, "Act: Open the system settings");
        mSettingHelper.get().openSetting(SettingsConstants.SYSTEM_SETTINGS);
        Log.i(LOG_TAG, "Assert: System settings did not open");
        assertTrue(
                "System settings did not open",
                mSettingHelper.get().checkMenuExists("Languages & input"));
    }

    @After
    public void goBackToSettingsScreen() {
        mSettingHelper.get().goBackToSettingsScreen();
    }

    @Test
    public void testLanguagesinputSystemSettings() {
        Log.i(LOG_TAG, "Act: Open Languages & Input");
        mSettingsUIHelper.get().openUIOptions(AutomotiveConfigConstants.LANGUAGES_INPUT_IN_SYSTEM);

        Log.i(LOG_TAG, "Assert: Languages displayed in Languages & input");
        assertTrue(
                "Languages not displayed in Languages & input",
                mSettingsUIHelper.get().hasUIElement(AutomotiveConfigConstants.LANGUAGES_MENU));
        Log.i(LOG_TAG, "Assert: Autofill service displayed in Languages & input");
        assertTrue(
                "Autofill service not displayed in Languages & input",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(
                                AutomotiveConfigConstants
                                        .LANGUAGE_SYSTEM_SETTINGS_AUTOFILL_SERVICE));
        Log.i(LOG_TAG, "Assert: Keyboard service displayed in Languages & input");
        assertTrue(
                "Keyboard service not displayed in Languages & input",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.LANGUAGE_SYSTEM_SETTINGS_KEYBOARD));
        Log.i(LOG_TAG, "Assert: Text to speech output displayed in Languages & input");
        assertTrue(
                "Text to speech output not displayed in Languages & input",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(
                                AutomotiveConfigConstants
                                        .LANGUAGE_SYSTEM_SETTINGS_TEXT_TO_SPEECH_OUTPUT));
    }

    @Test
    public void testUnitSystemSettings() {
        Log.i(LOG_TAG, "Act: Open Units in System Settings");
        mSettingsUIHelper.get().openUIOptions(AutomotiveConfigConstants.SYSTEM_SETTINGS_UNITS);
        Log.i(LOG_TAG, "Assert: Speed is displayed in Units");
        assertTrue(
                "Speed is not displayed in Units",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.UNIT_SYSTEM_SETTINGS_SPEED));
        Log.i(LOG_TAG, "Assert: Distance is displayed in Units");
        assertTrue(
                "Distance is not displayed in Units",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.UNIT_SYSTEM_SETTINGS_DISTANCE));
        Log.i(LOG_TAG, "Assert: Temperature is displayed in Units");
        assertTrue(
                "Temperature is not displayed in Units",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.UNIT_SYSTEM_SETTINGS_TEMPERATURE));
        Log.i(LOG_TAG, "Assert: Pressue is displayed in Units");
        assertTrue(
                "Pressure is not displayed in Units",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.UNIT_SYSTEM_SETTINGS_PRESSURE));
    }
}

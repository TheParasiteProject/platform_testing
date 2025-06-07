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
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.IAutoSettingsLocationHelper;
import android.platform.helpers.SettingsConstants;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.SetFlagsRule;
import android.platform.test.rules.ConditionalIgnore;
import android.platform.test.rules.ConditionalIgnoreRule;
import android.platform.test.rules.IgnoreOnPortrait;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SettingsLocationTest {
    @Rule public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    private HelperAccessor<IAutoSettingsLocationHelper> mSettingLocationHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private static final String LOG_TAG = SettingsLocationTest.class.getSimpleName();

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    public SettingsLocationTest() {
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
        mSettingLocationHelper = new HelperAccessor<>(IAutoSettingsLocationHelper.class);
    }

    @Before
    public void setup() {
        Log.i(LOG_TAG, "Act: Open Location Setting");
        mSettingHelper.get().openSetting(SettingsConstants.LOCATION_SETTINGS);
        Log.i(LOG_TAG, "Assert: Location setting is open");
        assertTrue(
                "Location settings did not open", mSettingHelper.get().checkMenuExists("Location"));
    }

    @Test
    @ConditionalIgnore(condition = IgnoreOnPortrait.class)
    @RequiresFlagsEnabled(
            com.android.car.settings.Flags.FLAG_REQUIRED_INFOTAINMENT_APPS_SETTINGS_PAGE)
    public void testToVerifyToggleLocation() {
        Log.i(LOG_TAG, "Act: Open Location access");
        mSettingLocationHelper.get().locationAccess();
        Log.i(LOG_TAG, "Act: Get location ON status");
        boolean defaultState = mSettingLocationHelper.get().isLocationOn();
        String widgetShownMessage = "Location widget is displayed ";
        String widgetNotShownMessage = "Location widget is not displayed ";
        Log.i(LOG_TAG, "Act: Toggle Off location status");
        mSettingLocationHelper.get().toggleLocation(!defaultState);
        Log.i(LOG_TAG, "Assert: Location status is OFF");
        assertTrue(
                defaultState ? widgetShownMessage : widgetNotShownMessage,
                mSettingLocationHelper.get().hasMapsWidget() != defaultState);
        Log.i(LOG_TAG, "Act: Toggle On location status");
        mSettingLocationHelper.get().toggleLocation(defaultState);
        Log.i(LOG_TAG, "Assert: Location status is ON");
        assertTrue(
                defaultState ? widgetShownMessage : widgetNotShownMessage,
                mSettingLocationHelper.get().hasMapsWidget() == defaultState);
    }

    @Test
    public void testToCheckRecentlyAccessedOption() {
        Log.i(LOG_TAG, "Assert: Recently accessed option is displayed");
        assertTrue(
                "Recently accessed option is not displayed ",
                mSettingLocationHelper.get().hasRecentlyAccessed());
    }
}

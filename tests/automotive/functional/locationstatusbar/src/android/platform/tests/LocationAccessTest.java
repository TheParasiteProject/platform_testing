/*
 * Copyright (C) 2025 The Android Open Source Project
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

import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LocationAccessTest {

    private HelperAccessor<IAutoSettingsLocationHelper> mSettingLocationHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private final String APP_NAME = "Google Maps";

    public LocationAccessTest() {
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
        mSettingLocationHelper = new HelperAccessor<>(IAutoSettingsLocationHelper.class);
    }

    @Before
    public void setup() {
        mSettingHelper.get().openSetting(SettingsConstants.LOCATION_SETTINGS);
        assertTrue(
                "Location settings did not open",
                mSettingHelper.get().checkMenuExists("Location access"));
        mSettingLocationHelper.get().locationAccess();
        boolean defaultState = mSettingLocationHelper.get().isLocationOn();
        mSettingLocationHelper.get().toggleLocation(!defaultState);
        mSettingLocationHelper.get().toggleLocation(defaultState);
        mSettingHelper.get().pressSettingsBackNavIcon();
    }

    @Test
    public void testAppLevelPermission() {
        mSettingLocationHelper.get().openAppLevelPermissions();
        assertTrue(
                "App level permission is not displayed",
                mSettingHelper.get().checkMenuExists("Allowed all the time"));
        assertTrue(
                "Recently accessed app is not displaying",
                mSettingHelper.get().checkMenuExists(APP_NAME));
    }

    @Test
    public void testMapsLocationPermissionPage() {
        mSettingLocationHelper.get().openMapsInRecentlyAccessed();
        assertTrue(
                "Maps location permission page in recently accessed is not displayed",
                mSettingHelper.get().checkMenuExists("Location permission"));
    }

    @Test
    public void testViewAll() {
        mSettingLocationHelper.get().clickViewAll();
        assertTrue(
                "Recently accessed view all page is not launched",
                mSettingHelper.get().checkMenuExists("Recently accessed"));
        assertTrue(
                "Recently accessed app is not displaying",
                mSettingHelper.get().checkMenuExists(APP_NAME));
    }
}

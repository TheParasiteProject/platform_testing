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

import android.platform.helpers.AutomotiveConfigConstants;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoAppInfoSettingsHelper;
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
public class AppInfoVerifyUIElementsTest {
    private HelperAccessor<IAutoAppInfoSettingsHelper> mAppInfoSettingsHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private HelperAccessor<IAutoUISettingsHelper> mSettingsUIHelper;
    private static final String LOG_TAG = AppInfoVerifyUIElementsTest.class.getSimpleName();

    private static final String CALENDAR_APP = "Calendar";

    public AppInfoVerifyUIElementsTest() throws Exception {
        mAppInfoSettingsHelper = new HelperAccessor<>(IAutoAppInfoSettingsHelper.class);
        mSettingsUIHelper = new HelperAccessor<>(IAutoUISettingsHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
    }

    @Before
    public void openAppInfoFacet() {
        Log.i(LOG_TAG, "Assert: Open the application settings");
        mSettingHelper.get().openSetting(SettingsConstants.APPS_SETTINGS);
    }

    @After
    public void goBackToSettingsScreen() {
        mSettingHelper.get().goBackToSettingsScreen();
        mSettingHelper.get().exit();
    }

    @Test
    public void testVerifyAppsPermissionUIElements() {
        Log.i(LOG_TAG, "Assert: App Settings is open");
        assertTrue(
                "Apps setting did not open.",
                mSettingsUIHelper
                                .get()
                                .hasUIElement(AutomotiveConfigConstants.RECENTLY_OPENED_UI_ELEMENT)
                        || mSettingHelper.get().checkMenuExists("Reset app grid to A-Z order"));
        Log.i(LOG_TAG, "Act: Show all apps");
        mAppInfoSettingsHelper.get().showAllApps();
        Log.i(LOG_TAG, "Act: Open the Calendar app");
        mAppInfoSettingsHelper.get().selectApp(CALENDAR_APP);
        Log.i(LOG_TAG, "Assert: Stop app button is open");
        assertTrue(
                "Stop app Button is not displayed",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.STOP_APP_UI_ELEMENT));
        Log.i(LOG_TAG, "Assert: Notification Option is displayed");
        assertTrue(
                "Notification Option is not displayed",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.NOTIFICATIONS_UI_ELEMENT));
        Log.i(LOG_TAG, "Assert: Permissions Option is displayed");
        assertTrue(
                "Permissions Option is not displayed",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.PERMISSIONS_UI_ELEMENT));
        Log.i(LOG_TAG, "Assert: Storage and Cache is displayed");
        assertTrue(
                "Storage and Cache Option is not displayed",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.STORAGE_CACHE_UI_ELEMENT));
    }

    @Test
    public void testVerifyAppsInfoUIElements() {
        Log.i(LOG_TAG, "Assert: Permissions manager Option is displayed");
        assertTrue(
                "Permission manager Option is not displayed",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.PERMISSION_MANAGER_UI_ELEMENT));
        Log.i(LOG_TAG, "Assert: Default apps option is displayed");
        assertTrue(
                "Default apps Option is not displayed",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.DEFAULT_APPS_UI_ELEMENT));
        Log.i(LOG_TAG, "Assert: Unused apps option is displayed");
        assertTrue(
                "Unused apps Option is not displayed",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.UNUSED_APPS_UI_ELEMENT));
        Log.i(LOG_TAG, "Assert: Performance-impacting apps option is displayed");
        assertTrue(
                "Performance-impacting apps Option is not displayed",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(
                                AutomotiveConfigConstants.PERFORMANCE_IMPACTING_APPS_UI_ELEMENT));
        Log.i(LOG_TAG, "Assert: Special apps access option is displayed");
        assertTrue(
                "Special app access Option is not displayed",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.SPECIAL_APPS_UI_ELEMENT));
    }
}


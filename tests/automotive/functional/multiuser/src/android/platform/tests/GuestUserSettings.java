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

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import android.platform.helpers.AutomotiveConfigConstants;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.IAutoUISettingsHelper;
import android.platform.helpers.IAutoUserHelper;
import android.platform.helpers.SettingsConstants;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class GuestUserSettings {

    private static final String GUEST = AutomotiveConfigConstants.HOME_GUEST_BUTTON;
    private static final String DRIVER = AutomotiveConfigConstants.HOME_DRIVER_BUTTON;
    private static final String LOG_TAG = GuestUserSettings.class.getSimpleName();
    private HelperAccessor<IAutoUserHelper> mUsersHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private HelperAccessor<IAutoUISettingsHelper> mSettingsUIHelper;

    public GuestUserSettings() {
        mUsersHelper = new HelperAccessor<>(IAutoUserHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
        mSettingsUIHelper = new HelperAccessor<>(IAutoUISettingsHelper.class);
    }

    @Before
    public void switchToGuestUser() {
        mUsersHelper.get().switchUsingUserIcon(GUEST);
    }

    @After
    public void switchToDriver() {
        mUsersHelper.get().switchUsingUserIcon(DRIVER);
    }

    @Test
    public void testSecuritySettingsNotDisplayed() {
        Log.i(LOG_TAG, "Act: Open settings");
        mSettingHelper.get().openFullSettings();
        Log.i(LOG_TAG, "Assert: Security settings menu is not displayed");
        assertFalse(
                "Security settings is displayed",
                mSettingsUIHelper
                        .get()
                        .hasSettingsMenu(AutomotiveConfigConstants.SECURITY_SETTINGS_TITLE));
    }

    @Test
    public void testAccountForGuestNotDisplayed() {
        Log.i(LOG_TAG, "Act: Open Profile & Account settings");
        mSettingHelper.get().openSetting(SettingsConstants.PROFILE_ACCOUNT_SETTINGS);
        Log.i(LOG_TAG, "Assert: Rename option is displayed");
        assertTrue(
                "Rename option is not displayed",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.USER_SETTINGS_RENAME));
        Log.i(LOG_TAG, "Assert: Adda a profile option is displayed");
        assertTrue(
                "Add a profile option is not displayed",
                mSettingsUIHelper
                        .get()
                        .hasUIElement(AutomotiveConfigConstants.USER_SETTINGS_ADD_PROFILE));
        Log.i(LOG_TAG, "Assert: Accounts for Geust option is not displayed");
        assertFalse(
                "Accounts for Guest is displayed",
                mSettingHelper.get().checkMenuExists("Accounts for Guest"));
    }
}

/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.content.pm.UserInfo;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.IAutoUserHelper;
import android.platform.helpers.MultiUserHelper;
import android.platform.helpers.SettingsConstants;
import android.platform.test.rules.ConditionalIgnore;
import android.platform.test.rules.ConditionalIgnoreRule;
import android.platform.test.rules.IgnoreOnPortrait;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This test will create user through API and delete the same user from UI
 * <p> Set system property to run MU test: adb shell setprop fw.stop_bg_users_on_switch 0
 */
@RunWith(AndroidJUnit4.class)
public class AddUserSettings {
    @Rule public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    private final MultiUserHelper mMultiUserHelper = MultiUserHelper.getInstance();
    private HelperAccessor<IAutoUserHelper> mUsersHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private static final String LOG_TAG = AddUserSettings.class.getSimpleName();

    public AddUserSettings() {
        mUsersHelper = new HelperAccessor<>(IAutoUserHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
    }

    @Before
    public void openAccountsFacet() {
        mSettingHelper.get().openSetting(SettingsConstants.PROFILE_ACCOUNT_SETTINGS);
    }

    @After
    public void goBackToHomeScreen() {
        Log.i(LOG_TAG, "Act: Go back to settings");
        mSettingHelper.get().goBackToSettingsScreen();
    }

    @Test
    @ConditionalIgnore(condition = IgnoreOnPortrait.class)
    public void testAddNonAdminUser() throws Exception {
        // create new user
        Log.i(LOG_TAG, "Act: Get current userinfo");
        UserInfo initialUser = mMultiUserHelper.getCurrentForegroundUserInfo();
        Log.i(LOG_TAG, "Act: Create a non-admin user");
        mUsersHelper.get().addUser();
        // switched to new user
        Log.i(LOG_TAG, "Act: Get current userinfo");
        UserInfo newUser = mMultiUserHelper.getCurrentForegroundUserInfo();
        // switch from new user to initial user
        Log.i(LOG_TAG, "Act: Switch to new user");
        mUsersHelper.get().switchUser(newUser.name, initialUser.name);
        // verify new user is seen in list of users
        Log.i(LOG_TAG, "Assert: Newly created user in user ist");
        assertTrue(mMultiUserHelper.getUserByName(newUser.name) != null);
        // Verify new user is non-Admin
        Log.i(LOG_TAG, "Act: Open Profile & Accounts setting");
        mSettingHelper.get().openSetting(SettingsConstants.PROFILE_ACCOUNT_SETTINGS);
        Log.i(LOG_TAG, "Assert: Newly  created user does not have admin access");
        assertFalse("New user has Admin Access", mUsersHelper.get().isNewUserAnAdmin(newUser.name));
        // remove new user
        Log.i(LOG_TAG, "Act: New user is deleted");
        mMultiUserHelper.removeUser(newUser);
    }
}

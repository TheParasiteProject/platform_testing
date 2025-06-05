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

import android.content.pm.UserInfo;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.IAutoUserHelper;
import android.platform.helpers.MultiUserHelper;
import android.platform.helpers.SettingsConstants;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class EditAdminName {

    public String INITIAL_USERNAME;
    public static final String EDIT_USERNAME = "editedName";
    private final MultiUserHelper mMultiUserHelper = MultiUserHelper.getInstance();
    private HelperAccessor<IAutoUserHelper> mUsersHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;

    private static final String LOG_TAG = EditAdminName.class.getSimpleName();

    public EditAdminName() {
        mUsersHelper = new HelperAccessor<>(IAutoUserHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
    }

    @Before
    public void getUserNameFromSettings() {
        Log.i(LOG_TAG, "Act: Get current userinfo");
        UserInfo initialUser = mMultiUserHelper.getCurrentForegroundUserInfo();
        INITIAL_USERNAME = initialUser.name;
    }

    @After
    public void goBackToHomeScreen() {
        Log.i(LOG_TAG, "Act: Edit initial user's name");
        mUsersHelper.get().editUserName(INITIAL_USERNAME);
        Log.i(LOG_TAG, "Act: Go back to settings");
        mSettingHelper.get().goBackToSettingsScreen();
    }

    @Test
    public void testEditAdminName() {
        Log.i(LOG_TAG, "Act: Open Profile & Accounts setting");
        mSettingHelper.get().openSetting(SettingsConstants.PROFILE_ACCOUNT_SETTINGS);
        Log.i(LOG_TAG, "Act: Edit initial user's name");
        mUsersHelper.get().editUserName(EDIT_USERNAME);
        Log.i(LOG_TAG, "Act: Get current userinfo");
        UserInfo newUser = mMultiUserHelper.getCurrentForegroundUserInfo();
        Log.i(LOG_TAG, "Assert: Current username is changed ");
        assertTrue("Username is not changed", EDIT_USERNAME.equals(newUser.name));
    }
}

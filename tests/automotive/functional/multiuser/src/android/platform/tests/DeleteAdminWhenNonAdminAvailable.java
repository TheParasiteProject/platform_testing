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

import static junit.framework.Assert.assertTrue;

import android.content.pm.UserInfo;
import android.os.SystemClock;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.IAutoUserHelper;
import android.platform.helpers.MultiUserHelper;
import android.platform.helpers.SettingsConstants;
import android.platform.scenario.multiuser.MultiUserConstants;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** This test will create user through API and delete the same user from UI */
@RunWith(AndroidJUnit4.class)
public class DeleteAdminWhenNonAdminAvailable {

    public String INITIAL_USERNAME;
    private static final String USER_NAME = MultiUserConstants.SECONDARY_USER_NAME;

    private static final String GUEST_USER = MultiUserConstants.GUEST_NAME;

    private static final int WAIT_TIME = 10000;
    private final MultiUserHelper mMultiUserHelper = MultiUserHelper.getInstance();
    private HelperAccessor<IAutoUserHelper> mUsersHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private int mTargetUserId;
    private static final String LOG_TAG = DeleteAdminWhenNonAdminAvailable.class.getSimpleName();

    public DeleteAdminWhenNonAdminAvailable() {
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
    public void testRemoveAdminUser() throws Exception {
        // create new user
        Log.i(LOG_TAG, "Act: Create a non admin user");
        mTargetUserId = mMultiUserHelper.createUser(USER_NAME, false);
        SystemClock.sleep(WAIT_TIME);

        // delete current user and make the new user admin
        Log.i(LOG_TAG, "Act: Open Profile & Accounts setting");
        mSettingHelper.get().openSetting(SettingsConstants.PROFILE_ACCOUNT_SETTINGS);

        Log.i(LOG_TAG, "Act: Delete current user and make the new user admin");
        mUsersHelper.get().deleteAdminAndChooseOtherAsAdmin(USER_NAME);

        Log.i(LOG_TAG, "Act: Get login user details");
        UserInfo currentUser = mMultiUserHelper.getCurrentForegroundUserInfo();

        Log.i(LOG_TAG, "Assert: Current userinfo matches Guest userinfo ");
        assertTrue(currentUser.name.equals(GUEST_USER));

        Log.i(LOG_TAG, "Act: Switch to new admin user");
        mMultiUserHelper.switchAndWaitForStable(
                mTargetUserId, MultiUserConstants.WAIT_FOR_IDLE_TIME_MS);

        mUsersHelper.get().skipSetupWizard();

        Log.i(LOG_TAG, "Act: Open Profile & Accounts setting");
        mSettingHelper.get().openSetting(SettingsConstants.PROFILE_ACCOUNT_SETTINGS);

        Log.i(LOG_TAG, "Assert: Current user is admin");
        assertTrue("User is not admin", mSettingHelper.get().checkMenuExists("Signed in as admin"));
    }
}

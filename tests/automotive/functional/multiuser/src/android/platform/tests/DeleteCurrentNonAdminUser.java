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
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This test will create user through API and delete the same user from UI
 * <p> Set system property to run MU test: adb shell setprop fw.stop_bg_users_on_switch 0
 */
@RunWith(AndroidJUnit4.class)
public class DeleteCurrentNonAdminUser {

    private static final String userName = MultiUserConstants.SECONDARY_USER_NAME;
    private static final int WAIT_TIME = 10000;
    private final MultiUserHelper mMultiUserHelper = MultiUserHelper.getInstance();
    private HelperAccessor<IAutoUserHelper> mUsersHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private int mTargetUserId;
    private static final String LOG_TAG = DeleteCurrentNonAdminUser.class.getSimpleName();

    public DeleteCurrentNonAdminUser() {
        mUsersHelper = new HelperAccessor<>(IAutoUserHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
    }

    @After
    public void goBackToHomeScreen() {
        mSettingHelper.get().goBackToSettingsScreen();
    }

    @Test
    public void testRemoveUserSelf() throws Exception {
        Log.i(LOG_TAG, "Act: Get current userinfo");
        UserInfo initialUser = mMultiUserHelper.getCurrentForegroundUserInfo();
        // add new user
        Log.i(LOG_TAG, "Act: Create a non admin user");
        mTargetUserId = mMultiUserHelper.createUser(userName, false);
        SystemClock.sleep(WAIT_TIME);
        // switch to new user
        Log.i(LOG_TAG, "Act: Switch to new user");
        mMultiUserHelper.switchAndWaitForStable(
            mTargetUserId, MultiUserConstants.WAIT_FOR_IDLE_TIME_MS);
        Log.i(LOG_TAG, "Act: Get new userinfo");
        UserInfo newUser = mMultiUserHelper.getCurrentForegroundUserInfo();
        // user deleted self
        Log.i(LOG_TAG, "Act: Open Profile & Accounts setting");
        mSettingHelper.get().openSetting(SettingsConstants.PROFILE_ACCOUNT_SETTINGS);
        Log.i(LOG_TAG, "Act: Delete current user");
        mUsersHelper.get().deleteCurrentUser();
        // goes to guest user, switch back to initial user
        Log.i(LOG_TAG, "Act: Switch back to initial user");
        mMultiUserHelper.switchAndWaitForStable(
            initialUser.id, MultiUserConstants.WAIT_FOR_IDLE_TIME_MS);
        // verify that user is deleted
        Log.i(LOG_TAG, "Assert: New user is deleted");
        assertTrue(mMultiUserHelper.getUserByName(newUser.name) == null);
    }
}

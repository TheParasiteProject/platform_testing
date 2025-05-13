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

import android.content.pm.UserInfo;
import android.os.SystemClock;
import android.platform.helpers.AutomotiveConfigConstants;
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

@RunWith(AndroidJUnit4.class)
public class NonAdminUserSettings {

    private static final String USERNAME = MultiUserConstants.SECONDARY_USER_NAME;
    private static final String DRIVER = AutomotiveConfigConstants.HOME_DRIVER_BUTTON;
    private static final String LOG_TAG = NonAdminUserSettings.class.getSimpleName();
    private static final int WAIT_TIME = 10000;
    private final MultiUserHelper mMultiUserHelper = MultiUserHelper.getInstance();
    private int mTargetUserId;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private HelperAccessor<IAutoUserHelper> mUsersHelper;

    public NonAdminUserSettings() {
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
        mUsersHelper = new HelperAccessor<>(IAutoUserHelper.class);
    }

    @After
    public void removeNonAdminUser() {
        UserInfo newUser = mMultiUserHelper.getCurrentForegroundUserInfo();
        mUsersHelper.get().switchUsingUserIcon(DRIVER);
        mMultiUserHelper.removeUser(newUser);
    }

    @Test
    public void testSeeOtherUsersInSettings() throws Exception {
        Log.i(LOG_TAG, "Act: Create non-admin user");
        mTargetUserId = mMultiUserHelper.createUser(USERNAME, false);
        Log.i(LOG_TAG, "Act: Switch to non-admin user and wait until screen is stable");
        SystemClock.sleep(WAIT_TIME);
        mMultiUserHelper.switchAndWaitForStable(
                mTargetUserId, MultiUserConstants.WAIT_FOR_IDLE_TIME_MS);
        Log.i(LOG_TAG, "Act: Open Profile & Account settings");
        mSettingHelper.get().openSetting(SettingsConstants.PROFILE_ACCOUNT_SETTINGS);
        Log.i(LOG_TAG, "Assert: Not able to see Manage other profiles option");
        assertFalse(
                "Non Admin is able to see other users",
                mSettingHelper.get().checkMenuExists("Manage other profiles"));
    }
}

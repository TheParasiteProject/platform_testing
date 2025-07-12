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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DeleteUserInInfotainmentSystem {
    public String INITIAL_USERNAME;
    private static final String USERNAME = MultiUserConstants.SECONDARY_USER_NAME;
    private static final String GUESTUSERNAME = MultiUserConstants.GUEST_NAME;
    private static final String DRIVER = AutomotiveConfigConstants.HOME_DRIVER_BUTTON;
    private static final String SECONDARY_USER =
            AutomotiveConfigConstants.HOME_SECONDARY_USER_BUTTON;
    private static final int WAIT_TIME = 10000;
    private final MultiUserHelper mMultiUserHelper = MultiUserHelper.getInstance();
    private HelperAccessor<IAutoUserHelper> mUsersHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private int mTargetUserId;

    private static final String LOG_TAG = DeleteUserInInfotainmentSystem.class.getSimpleName();

    public DeleteUserInInfotainmentSystem() {
        mUsersHelper = new HelperAccessor<>(IAutoUserHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
    }

    @Before
    public void createNewAdminUser() throws Exception {
        Log.i(LOG_TAG, "Act: Get current userinfo");
        UserInfo initialUser = mMultiUserHelper.getCurrentForegroundUserInfo();
        INITIAL_USERNAME = initialUser.name;

        Log.i(LOG_TAG, "Act: Create a new user");
        mTargetUserId = mMultiUserHelper.createUser(USERNAME, false);
        SystemClock.sleep(WAIT_TIME);
    }

    @After
    public void goBackToHomeScreen() {
        Log.i(LOG_TAG, "Act: Switch to Driver user");
        mUsersHelper.get().switchUsingUserIcon(SECONDARY_USER);

        Log.i(LOG_TAG, "Act: Skip setup wizard");
        mUsersHelper.get().skipSetupWizard();
        mSettingHelper.get().exit();

        Log.i(LOG_TAG, "Act: Open Profile & Accounts setting");
        mSettingHelper.get().openSetting(SettingsConstants.PROFILE_ACCOUNT_SETTINGS);

        Log.i(LOG_TAG, "Act: Edit initial user's name");
        mUsersHelper.get().editUserName(INITIAL_USERNAME);
        mSettingHelper.get().exit();
    }

    @Test
    public void testDeleteUserInInfoSystem() {
        Log.i(LOG_TAG, "Act: Open privacy settings");
        mSettingHelper.get().openSetting(SettingsConstants.PRIVACY_SETTINGS);

        Log.i(LOG_TAG, "Act: Click on Infotainment system data");
        mSettingHelper.get().openMenuWith("Infotainment system data");

        Log.i(LOG_TAG, "Act: Click on Delete your profile");
        mSettingHelper.get().openMenuWith("Delete your profile");

        Log.i(LOG_TAG, "Act: Choose a new admin before deleting admin user");
        mUsersHelper.get().chooseNewAdmin();

        Log.i(LOG_TAG, "Act: Skip setup wizard");
        mUsersHelper.get().skipSetupWizard();

        UserInfo currentUser = mMultiUserHelper.getCurrentForegroundUserInfo();
        Log.i(LOG_TAG, "Assert: Guest user is launched");
        assertTrue("Guest user is not launched ", currentUser.name.equals(GUESTUSERNAME));
        mSettingHelper.get().exit();
    }
}

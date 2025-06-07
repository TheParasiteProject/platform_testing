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

import android.os.SystemClock;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.IAutoUserHelper;
import android.platform.helpers.MultiUserHelper;
import android.platform.helpers.SettingsConstants;
import android.platform.scenario.multiuser.MultiUserConstants;
import android.platform.test.rules.ConditionalIgnore;
import android.platform.test.rules.ConditionalIgnoreRule;
import android.platform.test.rules.IgnoreOnPortrait;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This test will create user through API and delete the same user from UI
 */
@RunWith(AndroidJUnit4.class)
public class DeleteNonAdminUser {
    @Rule public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    private static final String userName = MultiUserConstants.SECONDARY_USER_NAME;
    private static final int WAIT_TIME = 10000;
    private final MultiUserHelper mMultiUserHelper = MultiUserHelper.getInstance();
    private HelperAccessor<IAutoUserHelper> mUsersHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private int mTargetUserId;

    private static final String LOG_TAG = DeleteNonAdminUser.class.getSimpleName();

    public DeleteNonAdminUser() {
        mUsersHelper = new HelperAccessor<>(IAutoUserHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
    }

    @After
    public void goBackToHomeScreen() {
        Log.i(LOG_TAG, "Act: Go back to settings");
        mSettingHelper.get().goBackToSettingsScreen();
    }

    @Test
    @ConditionalIgnore(condition = IgnoreOnPortrait.class)
    public void testRemoveUser() throws Exception {
        // create new user
        Log.i(LOG_TAG, "Act: Create a non-admin user");
        mTargetUserId = mMultiUserHelper.createUser(userName, false);
        SystemClock.sleep(WAIT_TIME);
        // make the new user admin and delete new user
        Log.i(LOG_TAG, "Act: Open Profile & Accounts setting");
        mSettingHelper.get().openSetting(SettingsConstants.PROFILE_ACCOUNT_SETTINGS);
        Log.i(LOG_TAG, "Act: Deete current user");
        mUsersHelper.get().deleteUser(userName);
        // verify new user was deleted
        Log.i(LOG_TAG, "Assert: New user is deleted");
        assertFalse(mUsersHelper.get().isUserPresent(userName));
    }
}

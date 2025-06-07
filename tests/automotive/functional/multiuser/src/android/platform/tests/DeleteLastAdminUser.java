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

/**
 * This test will create user through API and delete the same user from UI
 *
 * <p>It should be running under user 0, otherwise instrumentation may be killed after user
 * switched.
 */
@RunWith(AndroidJUnit4.class)
public class DeleteLastAdminUser {

    private final MultiUserHelper mMultiUserHelper = MultiUserHelper.getInstance();
    private HelperAccessor<IAutoUserHelper> mUsersHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private static final String LOG_TAG = DeleteLastAdminUser.class.getSimpleName();

    public DeleteLastAdminUser() {
        mUsersHelper = new HelperAccessor<>(IAutoUserHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
    }

    @Before
    public void openAccountsFacet() {
        Log.i(LOG_TAG, "Act: Open  Profile & Accounts setting");
        mSettingHelper.get().openSetting(SettingsConstants.PROFILE_ACCOUNT_SETTINGS);
    }

    @After
    public void goBackToHomeScreen() {
        Log.i(LOG_TAG, "Act: Go back to Settings");
        mSettingHelper.get().goBackToSettingsScreen();
    }

    @Test
    public void testRemoveUserSelf() throws Exception {
        // add new user
        Log.i(LOG_TAG, "Act: Get current userinfo");
        UserInfo initialUser = mMultiUserHelper.getCurrentForegroundUserInfo();
        // user deleted self
        Log.i(LOG_TAG, "Act: Delete current user");
        mUsersHelper.get().deleteCurrentUser();
        Log.i(LOG_TAG, "Act: Get current userinfo");
        UserInfo newUser = mMultiUserHelper.getCurrentForegroundUserInfo();
        // verify that user is deleted
        Log.i(LOG_TAG, "Assert: user is deleted");
        assertTrue((initialUser.id != newUser.id) && (initialUser.name.equals(newUser.name)));
    }

}


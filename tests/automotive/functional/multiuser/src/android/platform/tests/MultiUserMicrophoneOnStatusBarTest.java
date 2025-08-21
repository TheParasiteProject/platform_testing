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

import android.content.pm.UserInfo;
import android.platform.helpers.AutomotiveConfigConstants;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoAppGridHelper;
import android.platform.helpers.IAutoPrivacySettingsHelper;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.IAutoUserHelper;
import android.platform.helpers.MultiUserHelper;
import android.platform.scenario.multiuser.MultiUserConstants;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

/** This test will create user through API and test microphone on status bar */
@RunWith(AndroidJUnit4.class)
public class MultiUserMicrophoneOnStatusBarTest {

    private static final String USER_NAME = MultiUserConstants.SECONDARY_USER_NAME;
    private static final String DRIVER = AutomotiveConfigConstants.HOME_DRIVER_BUTTON;
    private int mTargetUserId;
    private final MultiUserHelper mMultiUserHelper = MultiUserHelper.getInstance();
    private HelperAccessor<IAutoUserHelper> mUsersHelper;
    private HelperAccessor<IAutoPrivacySettingsHelper> mPrivacyHelper;
    private HelperAccessor<IAutoAppGridHelper> mAppGridHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;

    private static final String LOG_TAG = MultiUserMicrophoneOnStatusBarTest.class.getSimpleName();

    public MultiUserMicrophoneOnStatusBarTest() {
        mUsersHelper = new HelperAccessor<>(IAutoUserHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
        mAppGridHelper = new HelperAccessor<>(IAutoAppGridHelper.class);
        mPrivacyHelper = new HelperAccessor<>(IAutoPrivacySettingsHelper.class);
    }

    @After
    public void goBackToHomeScreen() {
        Log.i(LOG_TAG, "Act: Exit settings app");
        mSettingHelper.get().exit();
    }

    @Test
    public void testMicrophoneDriverProfile() {
        Log.i(LOG_TAG, "Act: Open App Grid");
        mAppGridHelper.get().open();
        Log.i(LOG_TAG, "Act: Open Google Assistant app");
        mAppGridHelper.get().openApp("Google Assistant");
        Log.i(LOG_TAG, "Assert: Microphone on status bar is displayed");
        assertTrue(
                "Microphone on status bar is not displayed",
                mPrivacyHelper.get().isMicChipPresentOnStatusBar());
    }

    @Test
    public void testMicrophoneNewProfile() throws Exception {
        // create new user
        Log.i(LOG_TAG, "Act: Create a new non admin user");
        mTargetUserId = mMultiUserHelper.createUser(USER_NAME, false);
        // switched to new user and wait for it to load
        Log.i(LOG_TAG, "Act: Switch to new user");
        mMultiUserHelper.switchAndWaitForStable(
                mTargetUserId, MultiUserConstants.WAIT_FOR_IDLE_TIME_MS);
        Log.i(LOG_TAG, "Act: Skip SetUp Wizard");
        mUsersHelper.get().skipSetupWizard();
        UserInfo newUser = mMultiUserHelper.getCurrentForegroundUserInfo();
        // verify new user is seen in list of users
        Log.i(LOG_TAG, "Act: Get username of the new user in the users list");
        assertTrue(mMultiUserHelper.getUserByName(newUser.name) != null);
        // go to assistance
        Log.i(LOG_TAG, "Act: Open App Grid");
        mAppGridHelper.get().open();
        Log.i(LOG_TAG, "Act: Open Google Assistant app");
        mAppGridHelper.get().openApp("Google Assistant");
        Log.i(LOG_TAG, "Assert: Microphone on status bar is displayed");
        assertTrue(
                "Microphone on status bar is not displayed",
                mPrivacyHelper.get().isMicChipPresentOnStatusBar());
        Log.i(LOG_TAG, "ACT: Switch to Initial User");
        mUsersHelper.get().switchUsingUserIcon(DRIVER);
        // remove new user
        Log.i(LOG_TAG, "Act: Remove created new user");
        mMultiUserHelper.removeUser(newUser);
    }
}

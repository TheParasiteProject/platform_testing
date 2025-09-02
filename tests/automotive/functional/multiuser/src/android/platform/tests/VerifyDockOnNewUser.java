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

import android.content.pm.UserInfo;
import android.platform.helpers.AutomotiveConfigConstants;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoAppGridHelper;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.IAutoUserHelper;
import android.platform.helpers.MultiUserHelper;
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
 * This test will create user through API and delete the same user from API
 *
 * <p>Set system property to run MU test: adb shell setprop fw.stop_bg_users_on_switch 0
 */
@RunWith(AndroidJUnit4.class)
public class VerifyDockOnNewUser {
    @Rule public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    private final MultiUserHelper mMultiUserHelper = MultiUserHelper.getInstance();
    private HelperAccessor<IAutoUserHelper> mUsersHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;

    private HelperAccessor<IAutoAppGridHelper> mAppGridHelper;

    private static final String LOG_TAG = AddUserQuickSettings.class.getSimpleName();

    public VerifyDockOnNewUser() {
        mUsersHelper = new HelperAccessor<>(IAutoUserHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
        mAppGridHelper = new HelperAccessor<>(IAutoAppGridHelper.class);
    }

    @After
    public void goBackToHomeScreen() {
        Log.i(LOG_TAG, "Act: Go back to Home Screen");
        mSettingHelper.get().exit();
    }

    @Test
    @ConditionalIgnore(condition = IgnoreOnPortrait.class)
    public void testDockAndAllAppsOnNewUser() throws Exception {
        Log.i(LOG_TAG, "Act: Create new user");
        UserInfo initialUser = mMultiUserHelper.getCurrentForegroundUserInfo();
        mUsersHelper.get().addUserQuickSettings(initialUser.name);

        Log.i(LOG_TAG, "Act: Switch to new user");
        UserInfo newUser = mMultiUserHelper.getCurrentForegroundUserInfo();

        Log.i(LOG_TAG, "Assert: Google Maps App is Present on DOCK");
        assertTrue(
                "Google Maps App is NOT Present on Dock",
                mAppGridHelper.get().verifyAppOnDock(AutomotiveConfigConstants.MAPS_APP_ON_DOCK));

        Log.i(LOG_TAG, "Assert: Play Store App is Present on DOCK");
        assertTrue(
                "Playstore App is NOT Present on Dock",
                mAppGridHelper
                        .get()
                        .verifyAppOnDock(AutomotiveConfigConstants.PLAY_STORE_APP_ON_DOCK));

        Log.i(LOG_TAG, "Act: Open Appgrid");
        mAppGridHelper.get().open();

        Log.i(LOG_TAG, "Assert: Appgrid is open");
        assertTrue("App Grid is not open.", mAppGridHelper.get().isAppInForeground());

        Log.i(LOG_TAG, "Act: Exit Appgrid");
        mAppGridHelper.get().exit();

        Log.i(LOG_TAG, "Assert: Appgrid is exit");
        assertFalse("App Grid is open even after exit.", mAppGridHelper.get().isAppInForeground());

        Log.i(LOG_TAG, "Act: Remove User");
        mMultiUserHelper.removeUser(newUser);
    }
}

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
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

/** This test will create user through API and delete the same user from UI */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class GrantPermissionsToNonAdminUserTest {
    private static final String USER_NAME = MultiUserConstants.SECONDARY_USER_NAME;
    private final MultiUserHelper mMultiUserHelper = MultiUserHelper.getInstance();
    private HelperAccessor<IAutoUserHelper> mUsersHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private static final String LOG_TAG = GrantPermissionsToNonAdminUserTest.class.getSimpleName();

    public GrantPermissionsToNonAdminUserTest() {
        mUsersHelper = new HelperAccessor<>(IAutoUserHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
    }

    @After
    public void goBackToHomeScreen() {

        Log.i(LOG_TAG, "Act: Go back to Settings screen");
        mSettingHelper.get().goBackToSettingsScreen();
    }

    @Test
    public void testCreateNewUser() throws Exception {
        Log.i(LOG_TAG, "Act: Create a new user");
        // create new user
        mMultiUserHelper.createUser(USER_NAME, false);
    }

    @Test
    public void testOpenPermissionsPageOfNonAdmin() throws Exception {
        Log.i(LOG_TAG, "Act: Open Profile & Accounts Setting");
        mSettingHelper.get().openSetting(SettingsConstants.PROFILE_ACCOUNT_SETTINGS);
        Log.i(LOG_TAG, "Act: Open Permissions screen");
        mUsersHelper.get().openPermissionsPage(USER_NAME);
    }

    @Test
    public void testToggleOffAllPermissionsAndCheck() throws Exception {
        Log.i(LOG_TAG, "Assert: Create New Profile toggle is OFF");
        assertTrue(
                (mUsersHelper.get().isToggleOn(AutomotiveConfigConstants.CREATE_NEW_PROFILE_SWITCH))
                        && (mUsersHelper
                                .get()
                                .toggle(AutomotiveConfigConstants.CREATE_NEW_PROFILE_SWITCH)));
        Log.i(LOG_TAG, "Assert: Make Phone call toggle is OFF");
        assertTrue(
                (mUsersHelper.get().isToggleOn(AutomotiveConfigConstants.MAKE_PHONE_CALLS_SWITCH))
                        && (mUsersHelper
                                .get()
                                .toggle(AutomotiveConfigConstants.MAKE_PHONE_CALLS_SWITCH)));
        Log.i(LOG_TAG, "Assert: Install New Apps toggle is OFF");
        assertTrue(
                (mUsersHelper.get().isToggleOn(AutomotiveConfigConstants.INSTALL_NEW_APPS_SWITCH))
                        && (mUsersHelper
                                .get()
                                .toggle(AutomotiveConfigConstants.INSTALL_NEW_APPS_SWITCH)));
        Log.i(LOG_TAG, "Assert: Uninstall toggle is OFF");
        assertTrue(
                (mUsersHelper.get().isToggleOn(AutomotiveConfigConstants.UNINSTALL_APPS_SWITCH))
                        && (mUsersHelper
                                .get()
                                .toggle(AutomotiveConfigConstants.UNINSTALL_APPS_SWITCH)));
    }

    @Test
    public void testToggleOnAllPermissionsAndCheck() throws Exception {
        Log.i(LOG_TAG, "Assert: Create New Profile toggle is ON");
        assertTrue(
                !(mUsersHelper
                                .get()
                                .isToggleOn(AutomotiveConfigConstants.CREATE_NEW_PROFILE_SWITCH))
                        && (mUsersHelper
                                .get()
                                .toggle(AutomotiveConfigConstants.CREATE_NEW_PROFILE_SWITCH)));
        Log.i(LOG_TAG, "Assert: Make Phone call toggle is ON");
        assertTrue(
                !(mUsersHelper.get().isToggleOn(AutomotiveConfigConstants.MAKE_PHONE_CALLS_SWITCH))
                        && (mUsersHelper
                                .get()
                                .toggle(AutomotiveConfigConstants.MAKE_PHONE_CALLS_SWITCH)));
        Log.i(LOG_TAG, "Assert: Install New Apps toggle is ON");
        assertTrue(
                !(mUsersHelper.get().isToggleOn(AutomotiveConfigConstants.INSTALL_NEW_APPS_SWITCH))
                        && (mUsersHelper
                                .get()
                                .toggle(AutomotiveConfigConstants.INSTALL_NEW_APPS_SWITCH)));
        Log.i(LOG_TAG, "Assert: Uninstall toggle is ON");
        assertTrue(
                !(mUsersHelper.get().isToggleOn(AutomotiveConfigConstants.UNINSTALL_APPS_SWITCH))
                        && (mUsersHelper
                                .get()
                                .toggle(AutomotiveConfigConstants.UNINSTALL_APPS_SWITCH)));
    }

    @Test
    // @ConditionalIgnore(condition = IgnoreOnPortrait.class)
    public void testUnCheckCreateNewProfilesPermissionAndSwitchToNonAdminUser() throws Exception {
        Log.i(LOG_TAG, "Assert: Create New Profile toggle is ON");
        assertTrue(
                (mUsersHelper.get().isToggleOn(AutomotiveConfigConstants.CREATE_NEW_PROFILE_SWITCH))
                        && (mUsersHelper
                                .get()
                                .toggle(AutomotiveConfigConstants.CREATE_NEW_PROFILE_SWITCH)));

        // Switches the user mode to secondary and opens it profile account settings
        Log.i(LOG_TAG, "Act: Switch user mode to secondary");
        UserInfo targetUser = mMultiUserHelper.getUserByName(USER_NAME);
        mMultiUserHelper.switchToUserId(targetUser.id);
        Log.i(LOG_TAG, "Act: Open Profile & Accounts setting");
        mSettingHelper.get().openSetting(SettingsConstants.PROFILE_ACCOUNT_SETTINGS);

        // verifies the current user and the visibility of Add profile
        Log.i(LOG_TAG, "Act: Get user info");
        UserInfo currentUser = mMultiUserHelper.getCurrentForegroundUserInfo();
        Log.i(LOG_TAG, "Assert: Login user is Seconday user");
        assertTrue(currentUser.name.equals(USER_NAME));
        Log.i(LOG_TAG, "Assert: Add Profile is not visible");
        assertFalse(mUsersHelper.get().isVisibleAddProfile());
        Log.i(LOG_TAG, "Assert: Switch user mode to admin user");
        mMultiUserHelper.switchToUserId(mMultiUserHelper.getInitialUser());
        Log.i(LOG_TAG, "Act: Remove user");
        mMultiUserHelper.removeUser(targetUser);
    }
}

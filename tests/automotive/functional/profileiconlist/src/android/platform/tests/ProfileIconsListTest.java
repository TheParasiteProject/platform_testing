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

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoHomeHelper;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.MultiUserHelper;
import android.platform.scenario.multiuser.MultiUserConstants;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ProfileIconsListTest {

    private static final String USER_NAME = MultiUserConstants.SECONDARY_USER_NAME;

    public static final String GUEST_NAME = "Guest";

    private final MultiUserHelper mMultiUserHelper = MultiUserHelper.getInstance();
    private HelperAccessor<IAutoHomeHelper> mHomeHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;

    private static final String LOG_TAG = ProfileIconsListTest.class.getSimpleName();

    public ProfileIconsListTest() {
        mHomeHelper = new HelperAccessor<>(IAutoHomeHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
    }

    /**
     * Setup expectations: Comparing the profiles based on position.
     *
     * <p>This method is used to compare profiles based on position.
     */
    @Test
    public void testListOfProfiles() throws Exception {
        Log.i(LOG_TAG, "Act: Create non-admin user");
        mMultiUserHelper.createUser(USER_NAME, false);
        Log.i(LOG_TAG, "Act: Open status bar profiles");
        mHomeHelper.get().openStatusBarProfiles();
        Log.i(LOG_TAG, "Act: Get list of user profile names");
        List<String> list = mHomeHelper.get().getUserProfileNames();
        Log.i(LOG_TAG, "Assert: New user is at first position");
        assertFalse("newUser at index first position", USER_NAME.equals(list.get(0)));
        int position = list.size() - 1;
        Log.i(LOG_TAG, "Assert: Guest profile is at last position");
        assertTrue("Guest profile not at last position", GUEST_NAME.equals(list.get(position)));
        Log.i(LOG_TAG, "Assert: Add a Profile option is not displayed");
        assertTrue(
                "Add a Profile option is not displayed",
                mSettingHelper.get().checkMenuExists("Add a profile"));
        assertTrue(
                "Profiles and Accounts option is not displayed",
                mSettingHelper.get().checkMenuExists("Profiles & accounts settings"));
        // Currently logged user highlighted in status bar
        // This test step is already covered in the Guest profile test case
    }
}

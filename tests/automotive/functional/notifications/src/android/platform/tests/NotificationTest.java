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
import android.platform.helpers.IAutoNotificationHelper;
import android.platform.helpers.IAutoNotificationMockingHelper;

import android.util.Log;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class NotificationTest {

    private HelperAccessor<IAutoNotificationHelper> mNotificationHelper;
    private HelperAccessor<IAutoNotificationMockingHelper> mNotificationMockingHelper;

    private static final String LOG_TAG = NotificationTest.class.getSimpleName();
    private static String NOTIFICATION_TITLE = "AUTO TEST NOTIFICATION";

    public NotificationTest() {
        mNotificationHelper = new HelperAccessor<>(IAutoNotificationHelper.class);
        mNotificationMockingHelper = new HelperAccessor<>(IAutoNotificationMockingHelper.class);
    }

    @Before
    public void setup() {
        mNotificationMockingHelper.get().clearAllNotification();
    }

    @After
    public void teardown() {
        mNotificationHelper.get().exitNotificationCenter();
    }

    @Test
    public void testOpenCloseNotification() {
        Log.i(LOG_TAG, "Act: Open notification center.");
        mNotificationHelper.get().openNotificationCenter();

        Log.i(LOG_TAG, "Assert: Notification center is open.");
        assertTrue("Notification did not open.", mNotificationHelper.get().isAppInForeground());

        Log.i(LOG_TAG, "Act: Exit notification center.");
        mNotificationHelper.get().exitNotificationCenter();

        Log.i(LOG_TAG, "Assert: Notification center is closed.");
        assertFalse("Notification did not close.", mNotificationHelper.get().isAppInForeground());
    }

    @Test
    public void testPostNotification() {
        Log.i(LOG_TAG, "Act: Post notification.");
        mNotificationMockingHelper.get().postNotifications(1);

        Log.i(LOG_TAG, "Assert: Notification is displayed.");
        assertTrue(
            "Unable to find posted notification.",
            mNotificationHelper.get().isNotificationDisplayedInCenterWithTitle(NOTIFICATION_TITLE)
        );
    }

    @Test
    public void testClearAllNotification() {
        Log.i(LOG_TAG, "Arrange: Post notification.");
        mNotificationMockingHelper.get().postNotifications(1);

        Log.i(LOG_TAG, "Act: Clear all notifications.");
        mNotificationHelper.get().clickClearAllBtn();

        Log.i(LOG_TAG, "Assert: Notification is cleared.");
        assertFalse(
            "Notifications were not cleared.",
            mNotificationHelper.get().isNotificationDisplayedInCenterWithTitle(NOTIFICATION_TITLE)
        );
    }

    @Test
    public void testSwipeAwayNotification() {
        Log.i(LOG_TAG, "Arrange: Post notification.");
        mNotificationMockingHelper.get().postNotifications(1);

        Log.i(LOG_TAG, "Act: Swipe away notification.");
        mNotificationHelper.get().removeNotification(NOTIFICATION_TITLE);

        Log.i(LOG_TAG, "Assert: Notification is swiped away.");
        assertFalse(
            "Notifications were not cleared.",
            mNotificationHelper.get().isNotificationDisplayedInCenterWithTitle(NOTIFICATION_TITLE)
        );
    }

    @Test
    public void testRecentAndOlderNotifications() {
        Log.i(LOG_TAG, "Arrange: Post notification.");
        mNotificationMockingHelper.get().postNotifications(1);

        Log.i(LOG_TAG, "Assert: Notification is present under recent category.");
        assertTrue(
            "Notification are not present under recent category",
            mNotificationHelper.get().isRecentNotification()
        );

        Log.i(LOG_TAG, "Act: Exit and opennotification center.");
        mNotificationHelper.get().exitNotificationCenter();
        mNotificationHelper.get().openNotificationCenter();

        Log.i(LOG_TAG, "Assert: Notification is present under older category.");
        assertTrue(
            "Notification are not present under older category",
            mNotificationHelper.get().isOlderNotification()
        );
    }

    @Test
    public void testManageButton() {
        Log.i(LOG_TAG, "Arrange: Post notification.");
        mNotificationMockingHelper.get().postNotifications(1);

        Log.i(LOG_TAG, "Act: Click on manage button.");
        mNotificationHelper.get().clickManageBtn();

        Log.i(LOG_TAG, "Assert: Notification settings is opened.");
        assertTrue(
            "Notification Settings did not open.",
            mNotificationHelper.get().isNotificationSettingsOpened()
        );
    }

}

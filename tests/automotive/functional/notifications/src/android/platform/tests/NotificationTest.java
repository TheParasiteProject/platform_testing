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

    private static String NOTIFICATION_TITLE = "AUTO TEST NOTIFICATION";
    private static final String LOG_TAG = NotificationTest.class.getSimpleName();

    public NotificationTest() {
        mNotificationHelper = new HelperAccessor<>(IAutoNotificationHelper.class);
        mNotificationMockingHelper = new HelperAccessor<>(IAutoNotificationMockingHelper.class);
    }


    @Before
    public void clearAllNotification() {
        Log.i(LOG_TAG, "Act: Clear all notifications from Notification menu");
        mNotificationMockingHelper.get().clearAllNotification();
    }

    @After
    public void exit() {
        Log.i(LOG_TAG, "Act: Close notification menu");
        mNotificationHelper.get().exit();
    }

    @Test
    public void testOpenCloseNotification() {
        Log.i(LOG_TAG, "Act: Open notification menu");
        mNotificationHelper.get().open();
        Log.i(LOG_TAG, "Assert: Notification menu is open");
        assertTrue("Notification did not open.", mNotificationHelper.get().isAppInForeground());
        Log.i(LOG_TAG, "Act: Close notification menu");
        mNotificationHelper.get().exit();
        Log.i(LOG_TAG, "Assert: Notification menu is closed");
        assertFalse("Notification did not close.", mNotificationHelper.get().isAppInForeground());
    }

    @Test
    public void testClearAllNotification() {
        Log.i(LOG_TAG, "Act: Post a new notification");
        mNotificationMockingHelper.get().postNotifications(1);
        Log.i(LOG_TAG, "Act: Clear all notifications from Notification menu");
        mNotificationHelper.get().tapClearAllBtn();
        Log.i(LOG_TAG, "Act: Close notification menu");
        mNotificationHelper.get().exit();
        Log.i(LOG_TAG, "Assert: Notification is cleared");
        assertFalse(
                "Notifications were not cleared.",
                mNotificationHelper
                        .get()
                        .isNotificationDisplayedInCenterWithTitle(NOTIFICATION_TITLE));
    }

    @Test
    public void testPostNotification() {
        Log.i(LOG_TAG, "Act: Post a new notification");
        mNotificationMockingHelper.get().postNotifications(1);
        Log.i(LOG_TAG, "Assert: Posted notification is displayed in Notifications");
        assertTrue(
                "Unable to find posted notification.",
                mNotificationHelper.get().checkNotificationExists(NOTIFICATION_TITLE));
    }

    @Test
    public void testSwipeAwayNotification() {
        Log.i(LOG_TAG, "Act: Clear all notifications from Notification menu");
        mNotificationHelper.get().tapClearAllBtn();
        Log.i(LOG_TAG, "Act: Post a new notification");
        mNotificationMockingHelper.get().postNotifications(1);
        Log.i(LOG_TAG, "Assert: Posted notification is displayed in Notifications");
        assertTrue(
                "Unable to find posted notification.",
                mNotificationHelper.get().checkNotificationExists(NOTIFICATION_TITLE));
        Log.i(LOG_TAG, "Act: Removed Posted notification from Notification menu");
        mNotificationHelper.get().removeNotification(NOTIFICATION_TITLE);
        Log.i(LOG_TAG, "Act: Posted notification from removed Notification menu");
        assertFalse(
                "Notifications were not cleared.",
                mNotificationHelper
                        .get()
                        .isNotificationDisplayedInCenterWithTitle(NOTIFICATION_TITLE));
    }

    @Test
    public void testManageButton() {
        Log.i(LOG_TAG, "Act: Post a new notification");
        mNotificationMockingHelper.get().postNotifications(1);
        Log.i(LOG_TAG, "Act: Open Notification menu and click on Manage button");
        mNotificationHelper.get().clickManageBtn();
        Log.i(LOG_TAG, "Assert: Notification setting is open");
        assertTrue(
                "Notification Settings did not open.",
                mNotificationHelper.get().isNotificationSettingsOpened());
    }

    @Test
    public void testRecentAndOlderNotifications() {
        Log.i(LOG_TAG, "Act: Clear all notifications from Notification menu");
        mNotificationHelper.get().tapClearAllBtn();
        Log.i(LOG_TAG, "Act: Post a new notification");
        mNotificationMockingHelper.get().postNotifications(1);
        Log.i(LOG_TAG, "Act: Open Notification menu");
        mNotificationHelper.get().open();
        Log.i(LOG_TAG, "Assert: Notification is present under recent category");
        assertTrue(
                "Notification are not present under recent category",
                mNotificationHelper.get().isRecentNotification());
        Log.i(LOG_TAG, "Assert: Close Notification menu");
        mNotificationHelper.get().exit();
        Log.i(LOG_TAG, "Act: Open Notification menu again");
        mNotificationHelper.get().open();
        Log.i(LOG_TAG, "Assert: Notification is present under older category");
        assertTrue(
                "Notification are not present under older category",
                mNotificationHelper.get().isOlderNotification());
    }
}

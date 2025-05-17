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

import android.platform.helpers.AutomotiveConfigConstants;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoAppGridHelper;
import android.platform.helpers.IAutoDialContactDetailsHelper;
import android.platform.helpers.IAutoFacetBarHelper;
import android.platform.helpers.IAutoNotificationHelper;
import android.platform.helpers.IAutoVehicleHardKeysHelper;
import android.platform.helpers.IAutoVehicleHardKeysHelper.DrivingState;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DrivingOptimizedAppsTest {
    private HelperAccessor<IAutoDialContactDetailsHelper> mContactHelper;
    private HelperAccessor<IAutoAppGridHelper> mAppGridHelper;
    private HelperAccessor<IAutoVehicleHardKeysHelper> mHardKeysHelper;
    private HelperAccessor<IAutoFacetBarHelper> mFacetBarHelper;
    private HelperAccessor<IAutoNotificationHelper> mNotificationHelper;

    private static final String NOTIFICATION_TITLE = "Check recent permissions";
    private static final String APP_PERMISSIONS = "App permissions";
    private static final int SPEED_TWENTY = 20;
    private static final String LOG_TAG = DrivingOptimizedAppsTest.class.getSimpleName();

    public DrivingOptimizedAppsTest() throws Exception {
        mAppGridHelper = new HelperAccessor<>(IAutoAppGridHelper.class);
        mContactHelper = new HelperAccessor<>(IAutoDialContactDetailsHelper.class);
        mHardKeysHelper = new HelperAccessor<>(IAutoVehicleHardKeysHelper.class);
        mFacetBarHelper = new HelperAccessor<>(IAutoFacetBarHelper.class);
        mNotificationHelper = new HelperAccessor<>(IAutoNotificationHelper.class);
    }

    @Before
    public void enableDrivingMode() {
        Log.i(LOG_TAG, "Act: Set Driving State to Moving");
        mHardKeysHelper.get().setDrivingState(DrivingState.MOVING);
        Log.i(LOG_TAG, "Act: Set Driving Sped to Twenty");
        mHardKeysHelper.get().setSpeed(SPEED_TWENTY);
    }

    @After
    public void disableDrivingMode() {
        Log.i(LOG_TAG, "Act: Go to Homescreen");
        mAppGridHelper.get().goToHomePage();
        Log.i(LOG_TAG, "Act: Set Driving State to Parking");
        mHardKeysHelper.get().setDrivingState(DrivingState.PARKED);
    }

    @Test
    public void testOpenSettings() {
        Log.i(LOG_TAG, "Act: Open App Grid");
        mAppGridHelper.get().open();
        Log.i(LOG_TAG, "Act: Open Settings");
        mAppGridHelper.get().openApp("Settings");
        Log.i(LOG_TAG, "Assert: Settings is open");
        assertTrue(
                "Settings app is not open",
                mAppGridHelper
                        .get()
                        .checkPackageInForeground(AutomotiveConfigConstants.SETTINGS_PACKAGE));
    }

    @Test
    public void testOpenRadio() {
        Log.i(LOG_TAG, "Act: Open App Grid");
        mAppGridHelper.get().open();
        Log.i(LOG_TAG, "Act: Open Radio App");
        mAppGridHelper.get().openApp("Radio");
        Log.i(LOG_TAG, "Assert: Radio App is open");
        assertTrue(
                "Radio app is not open",
                mAppGridHelper
                        .get()
                        .checkPackageInForeground(AutomotiveConfigConstants.RADIO_PACKAGE));
    }

    @Test
    public void testOpenPhone() {
        Log.i(LOG_TAG, "Act: Open App Grid");
        mAppGridHelper.get().open();
        Log.i(LOG_TAG, "Act: Open Radio App");
        mAppGridHelper.get().openApp("Phone");
        Log.i(LOG_TAG, "Assert: Phone App is open");
        assertTrue(
                "Phone is not open",
                mAppGridHelper
                        .get()
                        .checkPackageInForeground(AutomotiveConfigConstants.DIAL_PACKAGE));
    }

    /**
     * The test below verifies. 1. Contacts can be openned while on Drive mode 2. After the park
     * mode, notifications are sent when a certain permission is granted while driving (Silent
     * post-drive notification)
     */
    @Test
    public void testOpenContactsAndVerifyPostDriveNotification() {
        Log.i(LOG_TAG, "Act: Open App Grid");
        mAppGridHelper.get().open();
        Log.i(LOG_TAG, "Act: Open Contacts App");
        mAppGridHelper.get().openApp("Contacts");
        Log.i(LOG_TAG, "Act: Dismiss pop-up dialog");
        mContactHelper.get().dismissInitialDialogs();
        Log.i(LOG_TAG, "Assert: Contacts is open");
        assertTrue(
                "Contacts is not open",
                mAppGridHelper
                        .get()
                        .checkPackageInForeground(AutomotiveConfigConstants.CONTACTS_PACKAGE));
        Log.i(LOG_TAG, "Act: Disable Driving Mode");
        disableDrivingMode();
        Log.i(LOG_TAG, "Act: Open Notifications");
        mFacetBarHelper.get().clickOnFacetIcon(IAutoFacetBarHelper.FACET_BAR.NOTIFICATION);
        Log.i(LOG_TAG, "Assert: Recent Permission is notified");
        assertTrue(
                "Recent Permission is not Notified",
                mNotificationHelper.get().checkNotificationExists(NOTIFICATION_TITLE));
        Log.i(LOG_TAG, "Act: Open Recent Permission");
        mNotificationHelper.get().clickOnCheckRecentPermissions(NOTIFICATION_TITLE);
        Log.i(LOG_TAG, "Act: Open App Permission is launched");
        assertTrue(
                "App Permissions page is not launched",
                mNotificationHelper.get().checkAppPermissionsExists(APP_PERMISSIONS));
    }
}

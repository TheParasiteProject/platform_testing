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

import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoAppGridHelper;
import android.platform.helpers.IAutoFacetBarHelper;
import android.platform.helpers.IAutoVehicleHardKeysHelper;
import android.platform.helpers.IAutoVehicleHardKeysHelper.DrivingState;
import android.platform.test.rules.ConditionalIgnore;
import android.platform.test.rules.ConditionalIgnoreRule;
import android.platform.test.rules.IgnoreOnPortrait;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UxRestrictionFacetBarTest {
    @Rule public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    private HelperAccessor<IAutoAppGridHelper> mAppGridHelper;
    private HelperAccessor<IAutoFacetBarHelper> mFacetBarHelper;
    private HelperAccessor<IAutoVehicleHardKeysHelper> mHardKeysHelper;
    private static final String LOG_TAG = UxRestrictionFacetBarTest.class.getSimpleName();

    private static final int SPEED_TWENTY = 20;
    private static final int SPEED_ZERO = 0;

    public UxRestrictionFacetBarTest() throws Exception {
        mAppGridHelper = new HelperAccessor<>(IAutoAppGridHelper.class);
        mFacetBarHelper = new HelperAccessor<>(IAutoFacetBarHelper.class);
        mHardKeysHelper = new HelperAccessor<>(IAutoVehicleHardKeysHelper.class);
    }


    @Before
    public void enableDrivingMode() {
        Log.i(LOG_TAG, "Act: Go to Home Screen");
        mFacetBarHelper.get().goToHomeScreen();
        Log.i(LOG_TAG, "Act: Set Driving State to Moving");
        mHardKeysHelper.get().setDrivingState(DrivingState.MOVING);
        Log.i(LOG_TAG, "Act: Set Driving Speed to Twenty");
        mHardKeysHelper.get().setSpeed(SPEED_TWENTY);
    }

    @After
    public void disableDrivingMode() {
        Log.i(LOG_TAG, "Act: Go to Homescreen");
        mFacetBarHelper.get().goToHomeScreen();
        Log.i(LOG_TAG, "Act: Set Driving Speed to zero");
        mHardKeysHelper.get().setSpeed(SPEED_ZERO);
        Log.i(LOG_TAG, "Act: Set Driving State to Parked");
        mHardKeysHelper.get().setDrivingState(DrivingState.PARKED);
    }

    @Test
    @ConditionalIgnore(condition = IgnoreOnPortrait.class)
    public void testRestrictedHomeFacetBar() {
        Log.i(LOG_TAG, "Act: Open App Grid");
        mAppGridHelper.get().open();
        Log.i(LOG_TAG, "Act: Click on Home Facet Icon");
        mFacetBarHelper.get().clickOnFacetIcon(IAutoFacetBarHelper.FACET_BAR.HOME);
        Log.i(LOG_TAG, "Assert: Home screen is open");
        assertTrue(
                "Home screen did not open",
                mFacetBarHelper.get().isAppInForeground(IAutoFacetBarHelper.VERIFY_OPEN_APP.HOME));
    }

    @Test
    @ConditionalIgnore(condition = IgnoreOnPortrait.class)
    public void testRestrictedPhoneFacetBar() {
        Log.i(LOG_TAG, "Act: Open Facetbar Phone App");
        mFacetBarHelper.get().clickOnFacetIcon(IAutoFacetBarHelper.FACET_BAR.PHONE);
        Log.i(LOG_TAG, "Assert: Phone App is open");
        assertTrue(
                "Phone app did not open",
                mFacetBarHelper.get().isAppInForeground(IAutoFacetBarHelper.VERIFY_OPEN_APP.PHONE));
    }

    @Test
    public void testRestrictedHvacFacetBar() {
        Log.i(LOG_TAG, "Act: Open Facetbar Hvac App");
        mFacetBarHelper.get().clickOnFacetIcon(IAutoFacetBarHelper.FACET_BAR.HVAC);
        Log.i(LOG_TAG, "Assert: Havc app is open");
        assertTrue(
                "Hvac did not open",
                mFacetBarHelper.get().isAppInForeground(IAutoFacetBarHelper.VERIFY_OPEN_APP.HVAC));
    }

    @Test
    public void testRestrictedAppGridFacetBar() {
        Log.i(LOG_TAG, "Act: Open Facetbar App Grid");
        mFacetBarHelper.get().clickOnFacetIcon(IAutoFacetBarHelper.FACET_BAR.APP_GRID);
        Log.i(LOG_TAG, "Assert: App Grid is open");
        assertTrue(
                "App grid did not open",
                mFacetBarHelper
                        .get()
                        .isAppInForeground(IAutoFacetBarHelper.VERIFY_OPEN_APP.APP_GRID));
    }

    @Test
    public void testRestrictedNotificationFacetBar() {
        Log.i(LOG_TAG, "Act: Open Facetbar Notification");
        mFacetBarHelper.get().clickOnFacetIcon(IAutoFacetBarHelper.FACET_BAR.NOTIFICATION);
        Log.i(LOG_TAG, "Assert: Notification is open");
        assertTrue(
                "Notification did not open.",
                mFacetBarHelper
                        .get()
                        .isAppInForeground(IAutoFacetBarHelper.VERIFY_OPEN_APP.NOTIFICATION));
    }

    @Test
    public void testRestrictedBluetoothPalette() {
        Log.i(LOG_TAG, "Act: Open Facetbar Bluetooth App");
        mFacetBarHelper.get().clickOnFacetIcon(IAutoFacetBarHelper.FACET_BAR.BLUETOOTH);
        Log.i(LOG_TAG, "Assert: Bluetooth menu is open");
        assertTrue(
                "Bluetooth palette did not open.",
                mFacetBarHelper
                        .get()
                        .isAppInForeground(IAutoFacetBarHelper.VERIFY_OPEN_APP.BLUETOOTH));
    }

    @Test
    public void testRestrictedWifiPalette() {
        Log.i(LOG_TAG, "Act: Open Facetbar Wifi");
        mFacetBarHelper.get().clickOnFacetIcon(IAutoFacetBarHelper.FACET_BAR.WIFI);
        Log.i(LOG_TAG, "Assert: Wifi menu is open");
        assertTrue(
                "Wifi palette did not open.",
                mFacetBarHelper.get().isAppInForeground(IAutoFacetBarHelper.VERIFY_OPEN_APP.WIFI));
    }

    @Test
    public void testRestrictedBrighnessPalette() {
        Log.i(LOG_TAG, "Act: Open Facetbar Brightness");
        mFacetBarHelper.get().clickOnFacetIcon(IAutoFacetBarHelper.FACET_BAR.BRIGHTNESS);
        Log.i(LOG_TAG, "Assert: Brightness menu is open");
        assertTrue(
                "Brightness palette did not open.",
                mFacetBarHelper
                        .get()
                        .isAppInForeground(IAutoFacetBarHelper.VERIFY_OPEN_APP.BRIGHTNESS));
    }

    @Test
    public void testRestrictedSoundPalette() {
        Log.i(LOG_TAG, "Act: Open Facetbar Sound");
        mFacetBarHelper.get().clickOnFacetIcon(IAutoFacetBarHelper.FACET_BAR.SOUND);
        Log.i(LOG_TAG, "Act: Facetbar Sound is open");
        assertTrue(
                "Sound palette did not open.",
                mFacetBarHelper.get().isAppInForeground(IAutoFacetBarHelper.VERIFY_OPEN_APP.SOUND));
    }
}

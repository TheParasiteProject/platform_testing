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

import android.platform.helpers.AutomotiveConfigConstants;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoAppGridHelper;
import android.platform.helpers.IAutoHomeHelper;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class HomeTest {
    private static final String SETTINGS = "Settings";
    private HelperAccessor<IAutoAppGridHelper> mAppGridHelper;
    private HelperAccessor<IAutoHomeHelper> mHomeHelper;

    public HomeTest() {
        mHomeHelper = new HelperAccessor<>(IAutoHomeHelper.class);
        mAppGridHelper = new HelperAccessor<>(IAutoAppGridHelper.class);
    }

    private static final String LOG_TAG = HomeTest.class.getSimpleName();

    @Before
    public void setup() {
        Log.i(LOG_TAG, "Act: Open Home screen");
        mHomeHelper.get().open();
    }

    @Test
    public void testAssistantWidget() {
        Log.i(LOG_TAG, "Assert: Assistant widget is visible");
        assertTrue(mHomeHelper.get().hasAssistantWidget());
    }

    @Test
    public void testMediaWidget() {
        Log.i(LOG_TAG, "Assert: Media widget is visible");
        assertTrue(mHomeHelper.get().hasMediaWidget());
    }

    @Test
    public void testTempetraureWidget() {
        Log.i(LOG_TAG, "Assert: Driver Temperature widget is visible");
        assertTrue("Driver temperature is not displayed", mHomeHelper.get().hasTemperatureWidget());
    }

    @Test
    public void testHvacPanel() {
        Log.i(LOG_TAG, "Act: Open HVAC panel");
        mHomeHelper.get().openHVAC();
        Log.i(LOG_TAG, "Assert: HVAC panel is open");
        assertTrue("HVAC panel is not opened", mHomeHelper.get().isHVACOpen());
    }

    @Test
    public void test1ConstantAppsOnDock() {
        Log.i(LOG_TAG, "Assert: Google Maps App is Present on DOCK");
        assertTrue(
                "Google Maps App is NOT Present on Dock",
                mAppGridHelper.get().verifyAppOnDock(AutomotiveConfigConstants.MAPS_APP_ON_DOCK));

        Log.i(LOG_TAG, "Assert: Google Maps App is Static on DOCK");
        assertTrue(
                "Google Maps is NOT Static App on Dock",
                mAppGridHelper
                        .get()
                        .verifyUnpinOptionForStaticAppOnDock(
                                AutomotiveConfigConstants.MAPS_APP_ON_DOCK));

        Log.i(LOG_TAG, "Act: Go to Home Screen");
        mAppGridHelper.get().goToHomePage();

        Log.i(LOG_TAG, "Assert: Play Store App is Present on DOCK");
        assertTrue(
                "Playstore App is NOT Present on Dock",
                mAppGridHelper
                        .get()
                        .verifyAppOnDock(AutomotiveConfigConstants.PLAY_STORE_APP_ON_DOCK));

        Log.i(LOG_TAG, "Assert: Play Store App is Static on DOCK");
        assertTrue(
                "Playstore is NOT Static App on Dock",
                mAppGridHelper
                        .get()
                        .verifyUnpinOptionForStaticAppOnDock(
                                AutomotiveConfigConstants.PLAY_STORE_APP_ON_DOCK));
    }

    @Test
    public void test2DynamicAppOnDock() {
        Log.i(LOG_TAG, "Act: Open Appgrid");
        mAppGridHelper.get().open();

        Log.i(LOG_TAG, "Act: Open Settings App");
        mAppGridHelper.get().openApp(SETTINGS);

        Log.i(LOG_TAG, "Assert: Settings App is open");
        assertTrue(
                "Settings app is not opened",
                mAppGridHelper
                        .get()
                        .checkPackageInForeground(AutomotiveConfigConstants.SETTINGS_PACKAGE));

        Log.i(LOG_TAG, "Act: Go to Home Screen");
        mAppGridHelper.get().goToHomePage();

        Log.i(LOG_TAG, "Assert: Settings App is Present on DOCK");
        assertTrue(
                "Settings App is Present on Dock",
                mAppGridHelper
                        .get()
                        .verifyAppOnDock(AutomotiveConfigConstants.SETTINGS_APP_ON_DOCK));

        Log.i(LOG_TAG, "Assert: Settings is Dynamic App on Dock");
        assertTrue(
                "Settings App is NOT Dynamic on Dock",
                mAppGridHelper
                        .get()
                        .verifyPinOptionForDynamicAppOnDock(
                                AutomotiveConfigConstants.SETTINGS_APP_ON_DOCK));
    }
}

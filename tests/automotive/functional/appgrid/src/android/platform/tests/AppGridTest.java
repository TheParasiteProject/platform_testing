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

import android.platform.helpers.AutomotiveConfigConstants;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoAppGridHelper;
import android.platform.test.rules.ConditionalIgnore;
import android.platform.test.rules.ConditionalIgnoreRule;
import android.platform.test.rules.IgnoreOnPortrait;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AppGridTest {
    @Rule public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    private static final String SMS_APP = "SMS";
    private static final String BLUETOOTH_APP = "Bluetooth Audio";
    private static final String PHONE_APP = "Phone";
    private static final String NEWS_APP = "News";

    private HelperAccessor<IAutoAppGridHelper> mAppGridHelper;
    private static final String LOG_TAG = AppGridTest.class.getSimpleName();

    public AppGridTest() {
        mAppGridHelper = new HelperAccessor<>(IAutoAppGridHelper.class);
    }


    @Test
    public void testOpen() {
        // Make sure app grid is not open before testing.
        Log.i(LOG_TAG, "Act: Exit Appgrid");
        mAppGridHelper.get().exit();
        Log.i(LOG_TAG, "Assert: Appgrid is exit");
        assertFalse("App Grid is open even after exit.", mAppGridHelper.get().isAppInForeground());
        // Test open.
        Log.i(LOG_TAG, "Act: Open Appgrid");
        mAppGridHelper.get().open();
        Log.i(LOG_TAG, "Assert: Appgrid is open");
        assertTrue("App Grid is not open.", mAppGridHelper.get().isAppInForeground());
    }

    @Test
    public void testExit() {
        // Make sure app grid has been opened before testing.
        Log.i(LOG_TAG, "Act: Open Appgrid");
        mAppGridHelper.get().open();
        Log.i(LOG_TAG, "Assert: Appgrid is open");
        assertTrue("App Grid is not open.", mAppGridHelper.get().isAppInForeground());
        // Test exit.
        Log.i(LOG_TAG, "Act: Exit Appgrid");
        mAppGridHelper.get().exit();
        Log.i(LOG_TAG, "Assert: Appgrid is exit");
        assertFalse("App Grid is open even after exit.", mAppGridHelper.get().isAppInForeground());
    }

    @Test
    public void testScroll() {
        // Re-enter app grid.
        Log.i(LOG_TAG, "Act: Exit Appgrid");
        mAppGridHelper.get().exit();
        Log.i(LOG_TAG, "Act: Open Appgrid");
        mAppGridHelper.get().open();

        Log.i(LOG_TAG, "Act: Scroll to beginning");
        mAppGridHelper.get().scrollToBeginning();
        // Test scroll only when there are more than one page in app grid.
        Log.i(LOG_TAG, "Act: Check Scroll to beginning");
        if (!mAppGridHelper.get().isAtEnd()) {
            Log.i(LOG_TAG, "Act: Scroll forward");
            mAppGridHelper.get().scrollForward();
            Log.i(LOG_TAG, "Assert: Scroll is at end");
            assertFalse("Scrolling did not work.", mAppGridHelper.get().isAtBeginning());
            Log.i(LOG_TAG, "Act: Scroll forward");
            mAppGridHelper.get().scrollBackward();
        }
    }

    @Test
    public void testLaunchBluetoothAudio() {
        // Make sure app grid is not open before testing
        Log.i(LOG_TAG, "Act: Exit Appgrid");
        mAppGridHelper.get().exit();
        Log.i(LOG_TAG, "Act: Open Appgrid");
        mAppGridHelper.get().open();

        Log.i(LOG_TAG, "Act: Open Bluetooth App");
        mAppGridHelper.get().openApp(BLUETOOTH_APP);
        Log.i(LOG_TAG, "Assert: Bluetooth App is open");
        assertTrue(
                "Bluetooth Audio app is not opened",
                mAppGridHelper
                        .get()
                        .checkPackageInForeground(AutomotiveConfigConstants.MEDIA_CENTER_PACKAGE));
    }

    @Test
    public void testLaunchPhone() {
        // Make sure app grid is not open before testing.
        Log.i(LOG_TAG, "Act: Exit Appgrid");
        mAppGridHelper.get().exit();
        Log.i(LOG_TAG, "Act: Open Appgrid");
        mAppGridHelper.get().open();

        Log.i(LOG_TAG, "Act: Open Phone App");
        mAppGridHelper.get().openApp(PHONE_APP);
        Log.i(LOG_TAG, "Assert: Phone App is open");
        assertTrue(
                "Phone app is not opened",
                mAppGridHelper
                        .get()
                        .checkPackageInForeground(AutomotiveConfigConstants.DIAL_PACKAGE));
    }

    @Test
    public void testLaunchSMS() {
        // Make sure app grid is not open before testing.
        Log.i(LOG_TAG, "Act: Exit Appgrid");
        mAppGridHelper.get().exit();
        Log.i(LOG_TAG, "Act: Open Appgrid");
        mAppGridHelper.get().open();

        Log.i(LOG_TAG, "Act: Open SMS App");
        mAppGridHelper.get().openApp(SMS_APP);
        Log.i(LOG_TAG, "Assert: SMS App is Open");
        assertTrue(
                "SMS app is not opened",
                mAppGridHelper
                        .get()
                        .checkPackageInForeground(AutomotiveConfigConstants.SMS_PACKAGE));
    }

    @Test
    public void testLaunchNews() {
        // Make sure app grid is not open before testing.
        Log.i(LOG_TAG, "Act: Exit Appgrid");
        mAppGridHelper.get().exit();
        Log.i(LOG_TAG, "Act: Open Appgrid");
        mAppGridHelper.get().open();

        Log.i(LOG_TAG, "Act: Open News App");
        mAppGridHelper.get().openApp(NEWS_APP);
        Log.i(LOG_TAG, "Assert: News App is Open");
        assertTrue(
                "News app is not opened",
                mAppGridHelper
                        .get()
                        .checkPackageInForeground(AutomotiveConfigConstants.MEDIA_CENTER_PACKAGE));
    }

    @Test
    @ConditionalIgnore(condition = IgnoreOnPortrait.class)
    public void testRecentAppsDisplaying() {
        Log.i(LOG_TAG, "Act: Exit Appgrid");
        mAppGridHelper.get().open();
        Log.i(LOG_TAG, "Act: Open News App");
        mAppGridHelper.get().openApp(PHONE_APP);
        Log.i(LOG_TAG, "Act: Long tap on All Apps");
        mAppGridHelper.get().longTapAllAppsButton();
        Log.i(LOG_TAG, "Assert: Recent App screen is launched");
        assertTrue(
                "Recents Screen is not launched", mAppGridHelper.get().isRecentsScreenLaunched());
        Log.i(LOG_TAG, "Act: Long tap on All Apps");
        mAppGridHelper.get().singleTapAllAppsButton();
        Log.i(LOG_TAG, "Assert: Recent App screen is closed");
        assertFalse("Recents Screen is not closed", mAppGridHelper.get().isRecentsScreenLaunched());
    }
}

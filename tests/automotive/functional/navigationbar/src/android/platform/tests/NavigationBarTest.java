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

import android.app.Instrumentation;
import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoFacetBarHelper;
import android.platform.test.rules.ConditionalIgnore;
import android.platform.test.rules.ConditionalIgnoreRule;
import android.platform.test.rules.IgnoreOnPortrait;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class NavigationBarTest {
    @Rule public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    private Instrumentation mInstrumentation;
    private UiDevice mDevice;
    private HelperAccessor<IAutoFacetBarHelper> mFacetBarHelper;
    private static final String LOG_TAG = NavigationBarTest.class.getSimpleName();

    public NavigationBarTest() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mDevice = UiDevice.getInstance(mInstrumentation);
        mFacetBarHelper = new HelperAccessor<>(IAutoFacetBarHelper.class);
    }

    @After
    public void goBackToHomeScreen() {
        mDevice.pressHome();
    }

    @Test
    @ConditionalIgnore(condition = IgnoreOnPortrait.class)
    public void testHomeButton() {
        Log.i(LOG_TAG, "Act: Click on Home button");
        mFacetBarHelper.get().clickOnFacetIcon(IAutoFacetBarHelper.FACET_BAR.HOME);
        Log.i(LOG_TAG, "Assert: Home screen is open");
        assertTrue(
                "Home screen did not open",
                mFacetBarHelper.get().isAppInForeground(IAutoFacetBarHelper.VERIFY_OPEN_APP.HOME));
    }

    @Test
    @ConditionalIgnore(condition = IgnoreOnPortrait.class)
    public void testDialButton() {
        Log.i(LOG_TAG, "Act: Click on Phone app Button");
        mFacetBarHelper.get().clickOnFacetIcon(IAutoFacetBarHelper.FACET_BAR.PHONE);
        Log.i(LOG_TAG, "Assert: Phone app is open");
        assertTrue(
                "Phone app did not open",
                mFacetBarHelper.get().isAppInForeground(IAutoFacetBarHelper.VERIFY_OPEN_APP.PHONE));
    }

    @Test
    public void testAppGridButton() {
        Log.i(LOG_TAG, "Act: Click on Appgrid Button");
        mFacetBarHelper.get().clickOnFacetIcon(IAutoFacetBarHelper.FACET_BAR.APP_GRID);
        Log.i(LOG_TAG, "Assert: App grid is open");
        assertTrue(
                "App grid did not open",
                mFacetBarHelper
                        .get()
                        .isAppInForeground(IAutoFacetBarHelper.VERIFY_OPEN_APP.APP_GRID));
    }

    @Test
    public void testNotificationButton() {
        Log.i(LOG_TAG, "Act: Click on Notification Button");
        mFacetBarHelper.get().clickOnFacetIcon(IAutoFacetBarHelper.FACET_BAR.NOTIFICATION);
        Log.i(LOG_TAG, "Assert: Notification app is open");
        assertTrue(
                "Notification did not open.",
                mFacetBarHelper
                        .get()
                        .isAppInForeground(IAutoFacetBarHelper.VERIFY_OPEN_APP.NOTIFICATION));
    }

    @Test
    public void testHVACButton() {
        Log.i(LOG_TAG, "Act: Click on Hvac Button");
        mFacetBarHelper.get().clickOnFacetIcon(IAutoFacetBarHelper.FACET_BAR.HVAC);
        Log.i(LOG_TAG, "Assert: Hvac is open");
        assertTrue(
                "Hvac did not open",
                mFacetBarHelper.get().isAppInForeground(IAutoFacetBarHelper.VERIFY_OPEN_APP.HVAC));
    }
}

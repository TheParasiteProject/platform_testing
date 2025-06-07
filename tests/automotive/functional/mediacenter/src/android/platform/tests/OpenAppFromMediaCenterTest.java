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
import android.platform.helpers.IAutoHomeHelper;
import android.platform.helpers.IAutoMediaHelper;
import android.platform.test.option.StringOption;
import android.platform.test.rules.ConditionalIgnore;
import android.platform.test.rules.ConditionalIgnoreRule;
import android.platform.test.rules.IgnoreOnPortrait;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class OpenAppFromMediaCenterTest {
    @Rule public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();
    private static final String BLUETOOTH_APP = "bluetooth-app";
    private static final String MEDIA_APPS = "media-apps";

    private static final String DEFAULT_MEDIA_APP = "Bluetooth Audio";
    private static final String DEFAULT_LIST_OF_MEDIA_APPS = "Bluetooth Audio, Radio";
    private static final String LOG_TAG = OpenAppFromMediaCenterTest.class.getSimpleName();

    @ClassRule
    public static StringOption mMediaDefaultApp =
            new StringOption(BLUETOOTH_APP).setRequired(false);
    @ClassRule
    public static StringOption mMediaApps = new StringOption(MEDIA_APPS).setRequired(false);

    private HelperAccessor<IAutoMediaHelper> mMediaCenterHelper;
    private HelperAccessor<IAutoHomeHelper> mAutoHomeHelper;
    private HelperAccessor<IAutoAppGridHelper> mAutoAppGridHelper;

    public OpenAppFromMediaCenterTest() {
        mMediaCenterHelper = new HelperAccessor<>(IAutoMediaHelper.class);
        mAutoHomeHelper = new HelperAccessor<>(IAutoHomeHelper.class);
        mAutoAppGridHelper = new HelperAccessor<>(IAutoAppGridHelper.class);
    }

    @After
    public void goBackToHomeScreen() {
        Log.i(LOG_TAG, "Act: Exit Media widget");
        mMediaCenterHelper.get().exit();
    }

    @Test
    public void testOpenMediaAppFromMediaWidget() {
        Log.i(LOG_TAG, "Act: Open Media widget");
        mAutoHomeHelper.get().openMediaWidget();
        Log.i(LOG_TAG, "Assert: Radio App is launched");
        assertTrue("Radio app not launched", mMediaCenterHelper.get().isRadioAppLaunched());
    }

    @Test
    public void testOpenMediaAppFromMediaAppAppgrid() {
        Log.i(LOG_TAG, "Act: Open Media widget");
        mAutoHomeHelper.get().openMediaWidget();
        Log.i(LOG_TAG, "Assert: Radio App is launched");
        assertTrue("Radio app not launched", mMediaCenterHelper.get().isRadioAppLaunched());
        Log.i(LOG_TAG, "Act: Open Media App Menu Items");
        mMediaCenterHelper.get().openMediaAppMenuItems();
        Log.i(LOG_TAG, "Act: Open Bluetooth Audio App");
        mMediaCenterHelper.get().openApp(DEFAULT_MEDIA_APP);
        Log.i(LOG_TAG, "Assert: Bluetooth Audio App is open");
        assertTrue(
                "Not a media app",
                mMediaCenterHelper.get().getMediaAppTitle().equals(getDefaultMediaAppName()));
    }

    @Test
    public void testOpenMediaAppFromAppgrid() {
        Log.i(LOG_TAG, "Act: Open App Grid");
        mAutoAppGridHelper.get().open();
        Log.i(LOG_TAG, "Assert: App Grid is open");
        assertTrue("App Grid is not open", mAutoAppGridHelper.get().isAppInForeground());
        Log.i(LOG_TAG, "Act: Open Bluetooth Audio App");
        mMediaCenterHelper.get().openApp(DEFAULT_MEDIA_APP);
        Log.i(LOG_TAG, "Assert: Bluetooth Audio App is open");
        assertTrue(
                "Not a media app",
                mMediaCenterHelper.get().getMediaAppTitle().equals(getDefaultMediaAppName()));
    }

    @Test
    @ConditionalIgnore(condition = IgnoreOnPortrait.class)
    public void testMediaAppPresentInMediaGrid() {
        Log.i(LOG_TAG, "Act: Open App Grid");
        mAutoAppGridHelper.get().open();
        Log.i(LOG_TAG, "Assert: App Grid is open");
        assertTrue("App Grid is not open", mAutoAppGridHelper.get().isAppInForeground());
        Log.i(LOG_TAG, "Act: Open Bluetooth Audio App");
        mMediaCenterHelper.get().openApp(DEFAULT_MEDIA_APP);
        Log.i(LOG_TAG, "Act: Open Media App Menu Items");
        mMediaCenterHelper.get().openMediaAppMenuItems();
        Log.i(LOG_TAG, "Assert: Correct Media App in App Grid");
        assertTrue("Incorrect Media apps in Grid",
                mMediaCenterHelper.get().areMediaAppsPresent(getExpectedMediaAppNames()));
    }

    private String getDefaultMediaAppName() {
        String mediaAppName = DEFAULT_MEDIA_APP;
        if (mMediaDefaultApp != null
                && mMediaDefaultApp.get() != null && !mMediaDefaultApp.get().isEmpty()) {
            Log.i(LOG_TAG, "Act: Get Default Media App");
            mediaAppName = mMediaDefaultApp.get();
        }
        return mediaAppName;
    }

    private List<String> getExpectedMediaAppNames() {
        String mediaAppNames = DEFAULT_LIST_OF_MEDIA_APPS;
        if (mMediaApps != null && mMediaApps.get() != null && !mMediaApps.get().isEmpty()) {
            // Get list of media apps from String options if passed.
            mediaAppNames = mMediaApps.get();
        }
        Log.i(LOG_TAG, "Act: Get list of Default Media Apps");
        String[] mediaAppNamesList = mediaAppNames.trim().split("\\s*,\\s*");
        return Arrays.asList(mediaAppNamesList);
    }
}

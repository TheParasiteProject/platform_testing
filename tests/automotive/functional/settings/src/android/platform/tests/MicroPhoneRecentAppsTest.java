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
import android.platform.helpers.IAutoPrivacySettingsHelper;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.SettingsConstants;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MicroPhoneRecentAppsTest {
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private HelperAccessor<IAutoAppGridHelper> mAppGridHelper;
    private HelperAccessor<IAutoPrivacySettingsHelper> mPrivacySettingsHelper;
    private static final String LOG_TAG = MicroPhoneRecentAppsTest.class.getSimpleName();

    private static final String APP = "Google Assistant";
    private static final String APP_TXT = "Google Assistant is using the mic";

    public MicroPhoneRecentAppsTest() throws Exception {
        mAppGridHelper = new HelperAccessor<>(IAutoAppGridHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
        mPrivacySettingsHelper = new HelperAccessor<>(IAutoPrivacySettingsHelper.class);
    }

    @Before
    public void openPrivacySetting() {
        mAppGridHelper.get().open();
        mAppGridHelper.get().openApp(APP);
    }

    @Before
    public void exit() {
        mPrivacySettingsHelper.get().exit();
    }

    @Test
    public void testRecentlyAccessedApps() {
        Log.i(LOG_TAG, "Act: Open the privacy settings");
        mSettingHelper.get().openSetting(SettingsConstants.PRIVACY_SETTINGS);
        Log.i(LOG_TAG, "Assert: Open MicroPhone");
        mSettingHelper.get().openMenuWith("MicroPhone");
        Log.i(LOG_TAG, "Assert: Recent App time stamp is displayed in microphone settings page");
        assertTrue(
                "Recent App time stamp is not displayed in microphone settings page",
                mPrivacySettingsHelper.get().isRecentAppDisplayedWithStamp(APP));
    }

    @Test
    public void testViewAllLink() {
        Log.i(LOG_TAG, "Act: Open the privacy settings");
        mSettingHelper.get().openSetting(SettingsConstants.PRIVACY_SETTINGS);
        Log.i(LOG_TAG, "Act: Open MicroPhone");
        mSettingHelper.get().openMenuWith("MicroPhone");
        Log.i(LOG_TAG, "Act: Click the View All Link");
        mPrivacySettingsHelper.get().clickViewAllLink();
        Log.i(LOG_TAG, "Assert: Recent App time stamp is displayed in view all page");
        assertTrue(
                "Recent App time stamp is not displayed in view all page",
                mPrivacySettingsHelper.get().isRecentAppDisplayedWithStamp(APP));
    }

    @Test
    public void testMicroPhonePanelUpdatedWithCurrentAppUsage() {
        Log.i(LOG_TAG, "Act: Click MicroPhone Status bar");
        mPrivacySettingsHelper.get().clickUnMutedMicroPhoneStatusBar();
        Log.i(LOG_TAG, "Assert: Current App usage is displayed in the panel");
        assertTrue(
                "Current App usage is not displayed in the panel",
                mPrivacySettingsHelper.get().isMicroPhoneStatusMessageUpdated(APP_TXT));
    }
}

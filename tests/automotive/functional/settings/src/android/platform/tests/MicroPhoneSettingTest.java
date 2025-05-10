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
import android.platform.helpers.IAutoFacetBarHelper;
import android.platform.helpers.IAutoPrivacySettingsHelper;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.SettingsConstants;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled(com.android.car.settings.Flags.FLAG_MICROPHONE_PRIVACY_UPDATES)
public class MicroPhoneSettingTest {
    private static final String USE_MICROPHONE_TXT = "Use microphone";
    private static final String MICROPHONE_OFF_TXT = "Microphone is off.";

    private HelperAccessor<IAutoFacetBarHelper> mFacetBarHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private HelperAccessor<IAutoPrivacySettingsHelper> mPrivacySettingsHelper;
    private static final String LOG_TAG = MicroPhoneSettingTest.class.getSimpleName();

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    public MicroPhoneSettingTest() throws Exception {
        mFacetBarHelper = new HelperAccessor<>(IAutoFacetBarHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
        mPrivacySettingsHelper = new HelperAccessor<>(IAutoPrivacySettingsHelper.class);
    }

    @Before
    public void openPrivacySetting() {
        Log.i(LOG_TAG, "Act: Open the privacy settings");
        mSettingHelper.get().openSetting(SettingsConstants.PRIVACY_SETTINGS);
        Log.i(LOG_TAG, "Assert: Privacy Settings is open");
        assertTrue(
                "Privacy settings did not open",
                mSettingHelper.get().checkMenuExists("Microphone"));
        mSettingHelper.get().openMenuWith("MicroPhone");
        Log.i(LOG_TAG, "Assert: Microphone Settings is open");
        assertTrue(
                "MicroPhone settings did not open",
                mSettingHelper.get().checkMenuExists("Microphone access"));
        mSettingHelper.get().openMenuWith("Microphone access");
        Log.i(LOG_TAG, "Assert: Microphone Access is open");
        assertTrue(
                "MicroPhone access did not open",
                mSettingHelper.get().checkMenuExists("Infotainment apps"));
    }

    @After
    public void goBackToSettingsScreen() {
        mSettingHelper.get().goBackToSettingsScreen();
        mSettingHelper.get().openSetting(SettingsConstants.PRIVACY_SETTINGS);
        mSettingHelper.get().openMenuWith("MicroPhone");
        mSettingHelper.get().openMenuWith("Microphone access");
        // MicroPhone is on by default
        if (!mPrivacySettingsHelper.get().isMicroPhoneOn()) {
            mPrivacySettingsHelper.get().turnOnOffMicroPhone(true);
        }
    }

    @Test
    public void manageMicrophonePermissions() {
        Log.i(LOG_TAG, "Act: Privacy settings is open");
        mSettingHelper.get().openSetting(SettingsConstants.PRIVACY_SETTINGS);
        assertTrue(
                "Privacy settings did not open",
                mSettingHelper.get().checkMenuExists("Microphone"));
        mSettingHelper.get().openMenuWith("MicroPhone");
        mPrivacySettingsHelper.get().clickManageMicroPhonePermissions();
        Log.i(LOG_TAG, "Assert: Microphone Permissions page is displayed");
        assertTrue(
                "Microphone Permissions page is not displayed",
                mPrivacySettingsHelper.get().verifyMicrophoneManagePermissionsPage());
    }

    @Test
    public void testMicroPhoneToggleOff() {
        Log.i(LOG_TAG, "Act: Turn Off the microphone");
        mPrivacySettingsHelper.get().turnOnOffMicroPhone(false);
        Log.i(LOG_TAG, "Assert: Microphone is Off");
        assertFalse("MicroPhone is still on", mPrivacySettingsHelper.get().isMicroPhoneOn());
        Log.i(LOG_TAG, "Act: Go back to settings screen");
        mSettingHelper.get().goBackToSettingsScreen();
        Log.i(LOG_TAG, "Act: Privacy settings is open");
        mSettingHelper.get().openSetting(SettingsConstants.PRIVACY_SETTINGS);
        Log.i(LOG_TAG, "Act: Open Microphone");
        mSettingHelper.get().openMenuWith("MicroPhone");
        Log.i(LOG_TAG, "Assert: Recent apps is displayed");
        assertFalse(
                "Recent apps is displayed",
                mSettingHelper.get().checkMenuExists("Recently accessed"));
        Log.i(LOG_TAG, "Assert: Micro Phone button is diplayed in the Status Bar");
        assertTrue(
                "Micro Phone button is not diplayed in the Status Bar",
                mPrivacySettingsHelper.get().isMutedMicChipPresentOnStatusBar());
    }

    @Test
    public void testMicroPhoneToggleOn() {
        Log.i(LOG_TAG, "Act: Turn Off the microphone");
        mPrivacySettingsHelper.get().turnOnOffMicroPhone(false);
        Log.i(LOG_TAG, "Act: Turn On the microphone");
        mPrivacySettingsHelper.get().turnOnOffMicroPhone(true);
        Log.i(LOG_TAG, "Assert: Microphone is On");
        assertTrue("MicroPhone is still off", mPrivacySettingsHelper.get().isMicroPhoneOn());
        Log.i(LOG_TAG, "Act: Go back to settings screen");
        mSettingHelper.get().goBackToSettingsScreen();
        Log.i(LOG_TAG, "Act: Privacy settings is open");
        mSettingHelper.get().openSetting(SettingsConstants.PRIVACY_SETTINGS);
        Log.i(LOG_TAG, "Act: Open Microphone");
        mSettingHelper.get().openMenuWith("MicroPhone");
        assertTrue(
                "Recently accessed is not present",
                mSettingHelper.get().checkMenuExists("Recently accessed"));
        assertTrue(
                "No Recent apps is not present",
                mPrivacySettingsHelper.get().verifyNoRecentAppsPresent());
        assertFalse(
                "Muted Micro Phone button is still diplayed in the Status Bar",
                mPrivacySettingsHelper.get().isMutedMicChipPresentOnStatusBar());
    }

    @Test
    public void testMicroPhonePanelStatusBar() {
        Log.i(LOG_TAG, "Act: Turn Off the microphone");
        mPrivacySettingsHelper.get().turnOnOffMicroPhone(false);
        Log.i(LOG_TAG, "Act: Open Microphone panel");
        mPrivacySettingsHelper.get().clickMicroPhoneStatusBar();
        Log.i(LOG_TAG, "Assert: MicroPhone status is updated");
        assertTrue(
                "MicroPhone status not updated",
                mPrivacySettingsHelper.get().isMicroPhoneStatusMessageUpdated(MICROPHONE_OFF_TXT));
        Log.i(LOG_TAG, "Assert: MicroPhone settings link is present");
        assertTrue(
                "MicroPhone settings link is not present",
                mPrivacySettingsHelper.get().isMicroPhoneSettingsLinkPresent());
        Log.i(LOG_TAG, "Assert: MicroPhone toggle is present in status bar");
        assertTrue(
                "MicroPhone toggle not present in status bar",
                mPrivacySettingsHelper.get().isMicroPhoneTogglePresent());
    }

    @Test
    public void testMicroPhonePanelStatusBarFromHome() {
        Log.i(LOG_TAG, "Act: Turn Off the microphone");
        mPrivacySettingsHelper.get().turnOnOffMicroPhone(false);
        Log.i(LOG_TAG, "Act: Go to Homescreen");
        mFacetBarHelper.get().goToHomeScreen();
        Log.i(LOG_TAG, "Act: Open Microphone panel");
        mPrivacySettingsHelper.get().clickMicroPhoneStatusBar();
        Log.i(LOG_TAG, "Assert: MicroPhone settings link is present");
        assertTrue(
                "MicroPhone settings link is not present",
                mPrivacySettingsHelper.get().isMicroPhoneSettingsLinkPresent());
        Log.i(LOG_TAG, "Assert: MicroPhone toggle is present in status bar");
        assertTrue(
                "MicroPhone toggle not present in status bar",
                mPrivacySettingsHelper.get().isMicroPhoneTogglePresent());
    }

    @Test
    public void testMicroPhonePanelSettingsLink() {
        Log.i(LOG_TAG, "Act: Turn Off the microphone");
        mPrivacySettingsHelper.get().turnOnOffMicroPhone(false);
        Log.i(LOG_TAG, "Act: Open Microphone panel");
        mPrivacySettingsHelper.get().clickMicroPhoneStatusBar();
        // go to privacy settings
        mPrivacySettingsHelper.get().clickMicroPhoneSettingsLink();
        assertTrue(
                "Privacy settings did not open",
                mSettingHelper.get().checkMenuExists("App permissions"));
    }

    @Test
    public void testMicroPhonePanelToggle() {
        Log.i(LOG_TAG, "Act: Turn Off the microphone");
        mPrivacySettingsHelper.get().turnOnOffMicroPhone(false);
        Log.i(LOG_TAG, "Act: Open Microphone panel");
        mPrivacySettingsHelper.get().clickMicroPhoneStatusBar();
        Log.i(LOG_TAG, "Act: Turn On the microphone");
        mPrivacySettingsHelper.get().clickMicroPhoneToggleStatusBar();
        Log.i(LOG_TAG, "Assert: Microphone is On");
        assertTrue("MicroPhone is still off", mPrivacySettingsHelper.get().isMicroPhoneOn());
        Log.i(LOG_TAG, "Assert: MicroPhone button updated in the status bar");
        assertFalse(
                "MicroPhone button not updated in status bar",
                mPrivacySettingsHelper.get().isMutedMicChipPresentWithMicPanel());
        Log.i(LOG_TAG, "Assert: MicroPhone status updated in the status bar");
        assertTrue(
                "MicroPhone status not updated",
                mPrivacySettingsHelper.get().isMicroPhoneStatusMessageUpdated(USE_MICROPHONE_TXT));
        Log.i(LOG_TAG, "Act: Turn Off the microphone");
        mPrivacySettingsHelper.get().clickMicroPhoneToggleStatusBar();
        Log.i(LOG_TAG, "Act: MicroPhone button is muted");
        assertTrue(
                "MicroPhone button should be muted",
                mPrivacySettingsHelper.get().isMutedMicChipPresentWithMicPanel());
    }

    @Test
    public void testMicroPhoneButtonDismiss() {
        Log.i(LOG_TAG, "Act: Turn Off the microphone");
        mPrivacySettingsHelper.get().turnOnOffMicroPhone(false);
        Log.i(LOG_TAG, "Act: Open Microphone panel");
        mPrivacySettingsHelper.get().clickMicroPhoneStatusBar();
        Log.i(LOG_TAG, "Act: Turn On the microphone");
        mPrivacySettingsHelper.get().clickMicroPhoneToggleStatusBar();
        Log.i(LOG_TAG, "Act: Muted MicroPhone button is displayed in status bar");
        assertFalse(
                "Muted MicroPhone button is displayed in status bar",
                mPrivacySettingsHelper.get().isMutedMicChipPresentWithMicPanel());
        Log.i(LOG_TAG, "Assert: Goto Homescreen");
        mFacetBarHelper.get().goToHomeScreen();
        Log.i(LOG_TAG, "Assert: Muted MicroPhone button is still displayed on status bar");
        assertFalse(
                "MicroPhone button is still displayed on status bar",
                mPrivacySettingsHelper.get().isMicChipPresentOnStatusBar());
    }
}

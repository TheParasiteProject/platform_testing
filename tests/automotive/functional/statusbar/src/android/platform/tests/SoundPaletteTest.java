/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.IAutoStatusBarHelper;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SoundPaletteTest {
    private HelperAccessor<IAutoStatusBarHelper> mStatusBarHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;

    private static final String LOG_TAG = SoundPaletteTest.class.getSimpleName();

    public SoundPaletteTest() {
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
        mStatusBarHelper = new HelperAccessor<>(IAutoStatusBarHelper.class);
    }

    @After
    public void goBackToHomeScreen() {
        mSettingHelper.get().exit();
    }

    @Before
    public void testVerifySoundButtonInHomePage() {
        assertTrue(
                "Sound Button is not displayed on Home Page",
                mStatusBarHelper.get().hasSoundButton());
    }

    @Test
    public void testSoundPaletteOnStatusBar() {
        Log.i(LOG_TAG, "Act: Open Sound Palette On Status Bar.");
        mStatusBarHelper.get().openSoundPaletteOnStatusBar();
        Log.i(LOG_TAG, "Assert: In-call Volume is displayed");
        assertTrue(
                "In-call Volume is not displayed on Sound Palette",
                mStatusBarHelper.get().isUIButtonPresentOnSoundPalette(AutomotiveConfigConstants.SOUND_SETTING_INCALL));
        Log.i(LOG_TAG, "Assert: Media Volume is displayed on Sound Palette");
        assertTrue(
                "Media Volume is not displayed on Sound Palette",
                mStatusBarHelper
                        .get()
                        .isUIButtonPresentOnSoundPalette(
                                AutomotiveConfigConstants.SOUND_PALETTE_MEDIA));
        Log.i(LOG_TAG, "Assert: Navigation Volume is displayed on Sound Palette");
        assertTrue(
                "Navigation Volume is not displayed on Sound Palette",
                mStatusBarHelper
                        .get()
                        .isUIButtonPresentOnSoundPalette(AutomotiveConfigConstants.SOUND_PALETTE_NAVIGATION));
        Log.i(LOG_TAG, "Assert: Sound Settings Volume is displayed on Sound Palette");
        assertTrue(
                "Sound Settings is not displayed on Sound Palette",
                mStatusBarHelper
                        .get()
                        .isUIButtonPresentOnSoundPalette(AutomotiveConfigConstants.SOUND_PALETTE_SOUND_SETTINGS));
    }

    @Test
    public void testSoundSettingsButton() {
        Log.i(LOG_TAG, "Act: Open Sound Palette On Status Bar.");
        mStatusBarHelper.get().openSoundPaletteOnStatusBar();
        Log.i(LOG_TAG, "Act: Open Sound Settings");
        mStatusBarHelper.get().openSoundSettings();
        Log.i(LOG_TAG, "Assert: Sound Settings is opened");
        assertTrue(
                "Sound settings page is not opened",
                mStatusBarHelper.get().hasSoundSettingsPageTitle());
    }
}

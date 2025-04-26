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
import android.platform.helpers.IAutoHomeHelper;
import android.platform.helpers.IAutoStatusBarHelper;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BrightnessPaletteTest {

    private HelperAccessor<IAutoHomeHelper> mHomeHelper;
    private HelperAccessor<IAutoStatusBarHelper> mStatusBarHelper;
    private static final String LOG_TAG = BrightnessPaletteTest.class.getSimpleName();

    public BrightnessPaletteTest() {
        mHomeHelper = new HelperAccessor<>(IAutoHomeHelper.class);
        mStatusBarHelper = new HelperAccessor<>(IAutoStatusBarHelper.class);
    }

    @Before
    public void openBrightnessPalette() {
        Log.i(LOG_TAG, "Act: Brightness palette is open");
        mHomeHelper.get().openBrightnessPalette();
    }

    @Test
    public void testBrightnessPaletteIsDisplayed() {
        Log.i(LOG_TAG, "Assert: Brightness palette is open");
        assertTrue(
                "Brightness palette did not open", mHomeHelper.get().hasDisplayBrightessPalette());
    }

    @Test
    public void testAdaptiveBrightnessSettingIsDisplayed() {
        Log.i(LOG_TAG, "Assert: Adaptive brightness is open");
        assertTrue("Adaptive brightness did not open", mHomeHelper.get().hasAdaptiveBrightness());
    }

    @Test
    public void testAdaptiveBrightnessToggleSwitch() {
        Log.i(LOG_TAG, "Assert: Default Adaptive brightness is OFF");
        assertFalse(
                "Adaptive brightness toggle switch in ON ",
                mStatusBarHelper.get().isAdaptiveBrightnessOn());
        Log.i(LOG_TAG, "Act: Turn On Adaptive Brightness Toggle Switch");
        mStatusBarHelper.get().clickOnAdaptiveBrightnessToggleSwitch();
        Log.i(LOG_TAG, "Assert: Default Adaptive brightness is ON");
        assertTrue(
                "Adaptive brightness toggle switch in OFF",
                mStatusBarHelper.get().isAdaptiveBrightnessOn());
        Log.i(LOG_TAG, "Act: Turn Off Adaptive Brightness Toggle Switch");
        mStatusBarHelper.get().clickOnAdaptiveBrightnessToggleSwitch();
        Log.i(LOG_TAG, "Assert: Default Adaptive brightness is OFF");
        assertFalse(
                "Adaptive brightness toggle switch in ON ",
                mStatusBarHelper.get().isAdaptiveBrightnessOn());
    }

    @After
    public void goToHomeScreen() {
        mHomeHelper.get().open();
    }
}

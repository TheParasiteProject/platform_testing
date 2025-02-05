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

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BrightnessPaletteTest {

    private HelperAccessor<IAutoHomeHelper> mHomeHelper;
    private HelperAccessor<IAutoStatusBarHelper> mStatusBarHelper;

    public BrightnessPaletteTest() {
        mHomeHelper = new HelperAccessor<>(IAutoHomeHelper.class);
        mStatusBarHelper = new HelperAccessor<>(IAutoStatusBarHelper.class);
    }

    @Before
    public void openBrightnessPalette() {
        mHomeHelper.get().openBrightnessPalette();
    }

    @Test
    public void testBrightnessPaletteIsDisplayed() {
        assertTrue(
                "Brightness palette did not open", mHomeHelper.get().hasDisplayBrightessPalette());
    }

    @Test
    public void testAdaptiveBrightnessSettingIsDisplayed() {
        assertTrue("Adaptive brightness did not open", mHomeHelper.get().hasAdaptiveBrightness());
    }

    @Test
    public void testAdaptiveBrightnessToggleSwitch() {
        // Default Adaptive brightness is OFF
        assertFalse(
                "Adaptive brightness toggle switch in ON ",
                mStatusBarHelper.get().isAdaptiveBrightnessOn());
        // Turn On Adaptive Brightness Toggle Switch
        mStatusBarHelper.get().clickOnAdaptiveBrightnessToggleSwitch();
        // Verify the Adaptive brightness Turned ON
        assertTrue(
                "Adaptive brightness toggle switch in OFF",
                mStatusBarHelper.get().isAdaptiveBrightnessOn());
        // return the Adaptive brightness switch to default state "OFF"
        mStatusBarHelper.get().clickOnAdaptiveBrightnessToggleSwitch();
        // Verify Default Adaptive brightness state is "OFF"
        assertFalse(
                "Adaptive brightness toggle switch in ON ",
                mStatusBarHelper.get().isAdaptiveBrightnessOn());
    }

    @After
    public void goToHomeScreen() {
        mHomeHelper.get().open();
    }
}

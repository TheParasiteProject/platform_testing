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

import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoHomeHelper;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class HomeTest {
    private HelperAccessor<IAutoHomeHelper> mHomeHelper;

    private static final String LOG_TAG = HomeTest.class.getSimpleName();

    public HomeTest() {
        mHomeHelper = new HelperAccessor<>(IAutoHomeHelper.class);
    }

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
}

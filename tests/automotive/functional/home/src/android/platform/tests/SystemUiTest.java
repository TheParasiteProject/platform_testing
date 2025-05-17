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
import android.platform.helpers.IAutoFacetBarHelper;
import android.platform.helpers.IAutoHomeHelper;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SystemUiTest {

    private HelperAccessor<IAutoHomeHelper> mHomeHelper;
    private HelperAccessor<IAutoFacetBarHelper> mFacetBarHelper;
    private static final String LOG_TAG = SystemUiTest.class.getSimpleName();

    public SystemUiTest() {
        mHomeHelper = new HelperAccessor<>(IAutoHomeHelper.class);
        mFacetBarHelper = new HelperAccessor<>(IAutoFacetBarHelper.class);
    }

    @Before
    public void setup() {
        Log.i(LOG_TAG, "Act: Open Home screen");
        mHomeHelper.get().open();
    }

    @Test
    public void testSystemUi() {
        Log.i(LOG_TAG, "Act: Open System UI");
        mHomeHelper.get().openSystemUi();
        Log.i(LOG_TAG, "Assert: Maps widget is displayed");
        assertTrue("Maps widget is not displayed", mHomeHelper.get().hasMapsWidget());
        Log.i(LOG_TAG, "Act: Open Car Ui");
        mHomeHelper.get().openCarUi();
        Log.i(LOG_TAG, "Act: Open Facet bar App Grid");
        mFacetBarHelper.get().clickOnFacetIcon(IAutoFacetBarHelper.FACET_BAR.APP_GRID);
        Log.i(LOG_TAG, "Assert: App Grid is Open");
        assertTrue(
                "App grid did not open",
                mFacetBarHelper
                        .get()
                        .isAppInForeground(IAutoFacetBarHelper.VERIFY_OPEN_APP.APP_GRID));
    }
}

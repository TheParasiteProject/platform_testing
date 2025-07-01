/*
 * Copyright (C) 2025 The Android Open Source Project
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
import android.platform.helpers.IAutoAppGridHelper;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DockTest {

    private static final String SETTINGS = "Settings";

    private HelperAccessor<IAutoAppGridHelper> mAppGridHelper;
    private static final String LOG_TAG = DockTest.class.getSimpleName();

    public DockTest() {
        mAppGridHelper = new HelperAccessor<>(IAutoAppGridHelper.class);
    }

    @Test
    public void testUnpinAppOnDock() {
        Log.i(LOG_TAG, "Act: Open Appgrid");
        mAppGridHelper.get().open();
        Log.i(LOG_TAG, "Act: Open Settings App");
        mAppGridHelper.get().openApp(SETTINGS);
        Log.i(LOG_TAG, "Assert: Settings App is open");
        assertTrue(
                "Settings app is not opened",
                mAppGridHelper
                        .get()
                        .checkPackageInForeground(AutomotiveConfigConstants.SETTINGS_PACKAGE));
        Log.i(LOG_TAG, "Act: Go to Home Screen");
        mAppGridHelper.get().goToHomePage();
        Log.i(LOG_TAG, "Assert: Google Playstore App is Present on DOCK");
        assertTrue(
                "Playstore App is NOT Present on Dock",
                mAppGridHelper
                        .get()
                        .verifyAppOnDock(AutomotiveConfigConstants.PLAY_STORE_APP_ON_DOCK));
        Log.i(LOG_TAG, "Act: Unpin the Playstore app on Dock");
        mAppGridHelper.get().unpinAppOnDock(AutomotiveConfigConstants.PLAY_STORE_APP_ON_DOCK);
        Log.i(LOG_TAG, "Assert: Settings App is Present on DOCK");
        assertTrue(
                "Settings App is Present on Dock",
                mAppGridHelper
                        .get()
                        .verifyAppOnDock(AutomotiveConfigConstants.SETTINGS_APP_ON_DOCK));
    }
}

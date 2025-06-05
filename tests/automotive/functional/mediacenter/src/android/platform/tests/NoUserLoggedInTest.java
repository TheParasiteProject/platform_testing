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
import android.platform.helpers.IAutoHomeHelper;
import android.platform.helpers.IAutoMediaHelper;
import android.platform.test.option.StringOption;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class NoUserLoggedInTest {

    @ClassRule
    public static StringOption mMediaApp =
            new StringOption("media-app").setRequired(false).setDefault("YouTube Music");
    private HelperAccessor<IAutoHomeHelper> mAutoHomeHelper;
    private HelperAccessor<IAutoMediaHelper> mMediaCenterHelper;

    private static final String LOG_TAG = NoUserLoggedInTest.class.getSimpleName();

    public NoUserLoggedInTest() throws Exception {
        mMediaCenterHelper = new HelperAccessor<>(IAutoMediaHelper.class);
        mAutoHomeHelper = new HelperAccessor<>(IAutoHomeHelper.class);
    }


    @Test
    public void testNoUserLogInMessage() {
        Log.i(LOG_TAG, "Act: Open Media widget");
        mAutoHomeHelper.get().openMediaWidget();
        Log.i(LOG_TAG, "Act: Open Media App Menu Item");
        mMediaCenterHelper.get().openMediaAppMenuItems();
        Log.i(LOG_TAG, "Act: Open Youtube Music App");
        mMediaCenterHelper.get().openApp(mMediaApp.get());
        Log.i(LOG_TAG, "Assert: Youtube Music App is a Media App");
        assertTrue("Not a media app.",
                mMediaCenterHelper.get().getMediaAppTitle().equals(mMediaApp.get()));
        Log.i(LOG_TAG, "Act: Get Media app error message");
        String noUserLoginMsg = mMediaCenterHelper.get().getMediaAppUserNotLoggedInErrorMessage();
        Log.i(LOG_TAG, "Assert: Sign In error message is correct");
        assertTrue("Incorrect Sign in error message.",
                noUserLoginMsg.equals("Please sign in to YouTube Music."));
    }
}

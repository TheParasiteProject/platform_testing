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

import static org.junit.Assert.assertNotEquals;

import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoHomeHelper;
import android.platform.helpers.IAutoMediaHelper;
import android.platform.helpers.IAutoTestMediaAppHelper;
import android.platform.test.option.StringOption;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MediaTestAppTest {

    private static final String MEDIA_APP = "media-app";
    private static final String TEST_MEDIA_APP = "Test Media App";
    private static final String DEFAULT_SONG_NAME = "A normal 1H song";
    private static final String LOG_TAG = MediaTestAppTest.class.getSimpleName();

    @ClassRule
    public static StringOption mMediaTestApp = new StringOption(MEDIA_APP).setRequired(false);

    public static String mDefaultSongName = new String(DEFAULT_SONG_NAME);

    private static HelperAccessor<IAutoMediaHelper> sMediaCenterHelper =
            new HelperAccessor<>(IAutoMediaHelper.class);
    private static HelperAccessor<IAutoTestMediaAppHelper> sTestMediaAppHelper =
            new HelperAccessor<>(IAutoTestMediaAppHelper.class);
    private static HelperAccessor<IAutoHomeHelper> sAutoHomeHelper =
            new HelperAccessor<>(IAutoHomeHelper.class);

    @BeforeClass
    public static void setup() {
        // Load songs on Test Media App
        Log.i(LOG_TAG, "Act: Open Media widget");
        sAutoHomeHelper.get().openMediaWidget();
        Log.i(LOG_TAG, "Act: Open Media App Menu Items");
        sMediaCenterHelper.get().openMediaAppMenuItems();
        Log.i(LOG_TAG, "Act: Get Test Media App");
        String mediaAppName = TEST_MEDIA_APP;
        if (mMediaTestApp != null
                && mMediaTestApp.get() != null
                && !mMediaTestApp.get().isEmpty()) {
            mediaAppName = mMediaTestApp.get();
        }
        Log.i(LOG_TAG, "Act: Open Test Media App");
        sMediaCenterHelper.get().openApp(mediaAppName);
        Log.i(LOG_TAG, "Act: Open Media App settings page");
        sMediaCenterHelper.get().openMediaAppSettingsPage();
        Log.i(LOG_TAG, "Act: Wait for load Media on Test Media App");
        sTestMediaAppHelper.get().loadMediaInLocalMediaTestApp();
    }

    @After
    public void goMinimizeNowPlaying() {
        Log.i(LOG_TAG, "Act: Minimize playing song");
        sMediaCenterHelper.get().minimizeNowPlaying();
    }

    @Test
    public void testPlayPauseMedia() {
        Log.i(LOG_TAG, "Act: Play media song");
        sMediaCenterHelper.get().playMedia();
        Log.i(LOG_TAG, "Assert: Media song is playing");
        assertTrue("Song not playing.", sMediaCenterHelper.get().isPlaying());
        Log.i(LOG_TAG, "Act: Minimize playing song");
        sMediaCenterHelper.get().minimizeNowPlaying();
        Log.i(LOG_TAG, "Act: Select Normal 1H track song");
        sMediaCenterHelper.get().selectMediaTrack(mDefaultSongName);
        Log.i(LOG_TAG, "Act: Pause media song");
        sMediaCenterHelper.get().pauseMedia();
        Log.i(LOG_TAG, "Assert: Media song is paused");
        assertFalse("Song not paused.", sMediaCenterHelper.get().isPlaying());
    }

    @Test
    public void testNextPreviousTrack() {
        Log.i(LOG_TAG, "Act: Get media track name");
        String currentSong = sMediaCenterHelper.get().getMediaTrackName();
        Log.i(LOG_TAG, "Act: Click on next track");
        sMediaCenterHelper.get().clickNextTrack();
        Log.i(LOG_TAG, "Act: Media Song playing has changed");
        assertNotEquals(
                "Song playing has not been changed",
                currentSong,
                sMediaCenterHelper.get().getMediaTrackName());
        Log.i(LOG_TAG, "Act: Get media track name");
        currentSong = sMediaCenterHelper.get().getMediaTrackName();
        Log.i(LOG_TAG, "Act: Click on previous track");
        sMediaCenterHelper.get().clickPreviousTrack();
        Log.i(LOG_TAG, "Act: Media Song playing has changed");
        assertNotEquals(
                "Song playing has not been changed",
                currentSong,
                sMediaCenterHelper.get().getMediaTrackName());
    }

    @Test
    public void testMediaPlayStateAfterGoingToHomeScreen() {
        Log.i(LOG_TAG, "Act: Play media song");
        sMediaCenterHelper.get().playMedia();
        Log.i(LOG_TAG, "Act: Select Normal 1H track song");
        sMediaCenterHelper.get().selectMediaTrack(mDefaultSongName);
        Log.i(LOG_TAG, "Assert: Media song is playing");
        assertTrue("Song is not playing", sMediaCenterHelper.get().isPlaying());
        Log.i(LOG_TAG, "Act: Exit Media App");
        sMediaCenterHelper.get().exit();
        Log.i(LOG_TAG, "Assert: Media Widget is displayed");
        assertTrue(sAutoHomeHelper.get().hasMediaWidget());
        Log.i(LOG_TAG, "Assert: Media song is playing");
        assertTrue("Song is not playing", sMediaCenterHelper.get().isPlaying());
        Log.i(LOG_TAG, "Act: Open Media widget");
        sAutoHomeHelper.get().openMediaWidget();
        Log.i(LOG_TAG, "Assert: Media song is playing");
        assertTrue("Song is not playing", sMediaCenterHelper.get().isPlaying());
    }

    @Test
    public void testMediaPauseStateAfterGoingToHomeScreen() {
        Log.i(LOG_TAG, "Act: Play media song");
        sMediaCenterHelper.get().playMedia();
        Log.i(LOG_TAG, "Act: Pause media song");
        sMediaCenterHelper.get().pauseMedia();
        Log.i(LOG_TAG, "Assert: Media song is playing");
        assertTrue("Song is playing, it should be paused", sMediaCenterHelper.get().isPaused());
        Log.i(LOG_TAG, "Act: Exit Media App");
        sMediaCenterHelper.get().exit();
        Log.i(LOG_TAG, "Assert: Media Widget is displayed");
        assertTrue(sAutoHomeHelper.get().hasMediaWidget());
        Log.i(LOG_TAG, "Assert: Media song is playing");
        assertTrue("Song is playing, it should be paused", sMediaCenterHelper.get().isPaused());
        Log.i(LOG_TAG, "Act: Open Media widget");
        sAutoHomeHelper.get().openMediaWidget();
        Log.i(LOG_TAG, "Assert: Media song is paused");
        assertTrue("Song is playing, it should be paused", sMediaCenterHelper.get().isPaused());
    }
}

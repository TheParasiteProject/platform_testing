#  Copyright (C) 2023 The Android Open Source Project
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import logging

from bluetooth_test import bluetooth_base_test
from mobly import asserts
from utilities.media_utils import MediaUtils
from utilities.common_utils import CommonUtils
from utilities.main_utils import common_main
from utilities.video_utils_service import VideoRecording


class PlaylistRendering(bluetooth_base_test.BluetoothBaseTest):

    def setup_class(self):
        super().setup_class()
        self.media_utils = MediaUtils(self.target, self.discoverer)
        self.common_utils = CommonUtils(self.target, self.discoverer)

    def setup_test(self):
        self.common_utils.grant_local_mac_address_permission()
        logging.info("\tInitializing video services on Target")
        self.video_utils_service_target = VideoRecording(self.target,self.__class__.__name__)
        logging.info("Enabling video recording for phone Target")
        self.video_utils_service_target.enable_screen_recording()

        self.common_utils.enable_wifi_on_phone_device()
        self.bt_utils.pair_primary_to_secondary()

    def test_playlist_rendering(self):
        """Tests validating is song selectable using playlist on HU"""
        self.media_utils.open_media_app_on_hu()
        self.media_utils.open_youtube_music_app()
        logging.info("Getting song title from phone device: %s", self.media_utils.get_song_title_from_phone())
        self.media_utils.maximize_now_playing()
        asserts.assert_true(self.media_utils.is_playlist_icon_visible(),
                            'Playlist icon should be visible on HU')
        self.media_utils.click_on_playlist_icon()
        self.media_utils.scroll_playlist_to_the_button()
        self.media_utils.select_song_from_playlist()
        self.media_utils.pause_media_on_hu()
        current_phone_song_title = self.media_utils.get_song_title_from_phone()
        current_hu_song_title = self.media_utils.get_song_title_from_hu()
        asserts.assert_true(current_phone_song_title == current_hu_song_title,
                            'Invalid song titles. '
                            'Song title on phone device and HU should be the same')

    def teardown_test(self):
        #  Close YouTube Music app
        self.media_utils.close_youtube_music_app()
        self.call_utils.press_home()
        logging.info("Stopping the screen recording on Target")
        self.video_utils_service_target.stop_screen_recording()
        logging.info("Pull the screen recording from Target")
        self.video_utils_service_target.pull_recording_file(self.log_path)
        logging.info("delete the screen recording from the Target")
        self.video_utils_service_target.delete_screen_recording_from_device()
        super().teardown_test()


if __name__ == '__main__':
    common_main()

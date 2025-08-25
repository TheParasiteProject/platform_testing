#  Copyright (C) 2025 The Android Open Source Project
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

"""Test the bt-media during drive mode
 Steps include:
        1) Precall state check on devices. (OK)
        2) Enable the drive mode in IVI device
        3) Play the Media
        4) Assert that media is playing
        8) Enable the Park Mode in IVI device
"""

import logging
from mobly import asserts
from bluetooth_test import bluetooth_base_test
from utilities import constants
from utilities.main_utils import common_main
from utilities.common_utils import CommonUtils
from utilities.media_utils import MediaUtils


class UxRestrictionBluetoothPlayMediaTest(bluetooth_base_test.BluetoothBaseTest):

    def setup_class(self):
        super().setup_class()
        self.media_utils = MediaUtils(self.target, self.discoverer)
        self.common_utils = CommonUtils(self.target, self.discoverer)

    def setup_test(self):
        logging.info("Pairing phone to car head unit.")
        self.bt_utils.pair_primary_to_secondary()
        super().enable_recording()
        logging.info("Enable the drive mode")
        self.call_utils.enable_driving_mode()

    def test_play_media_during_drive_mode(self):
        # Tests the media during drive mode.
        self.media_utils.open_media_app_on_hu()
        self.call_utils.handle_bluetooth_audio_pop_up()
        self.media_utils.open_youtube_music_app()
        current_phone_song_title = self.media_utils.get_song_title_from_phone()
        current_hu_song_title = self.media_utils.get_song_title_from_hu()
        asserts.assert_true(current_phone_song_title == current_hu_song_title,
                    'Invalid song titles. '
                    'Song title on phone device and HU should be the same')
        self.media_utils.pause_media_on_hu()
        # Next song metadata validation
        self.media_utils.click_next_track_on_hu()
        self.media_utils.pause_media_on_hu()
        logging.info("MetaData Validation after clicking Next Song")
        current_hu_next_song_title = self.media_utils.get_song_title_from_hu()
        asserts.assert_true(current_phone_song_title != current_hu_next_song_title,
                 'Song title on phone device and HU should be different but they are the same')


    def teardown_test(self):
        logging.info("disable the drive mode")
        self.call_utils.disable_driving_mode()
        #  Close YouTube Music app
        self.media_utils.close_youtube_music_app()
        self.call_utils.press_home()
        super().teardown_test()

if __name__ == '__main__':
    common_main()
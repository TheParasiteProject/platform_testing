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

"""Test of MediaCenter and Dialer Continuity
 Steps include:
        1) Precall state check on IVI device and phone devices. (OK)
        2) Launch the media center and play a song
        3) Open the phone app
        4) Place a phone call
        5) End call on IVI device
        6) Open the mediacenter and verify the song is continued to play
"""

from bluetooth_test import bluetooth_base_test
import logging
from mobly import asserts
from utilities import constants
from utilities.main_utils import common_main
from utilities.media_utils import MediaUtils
from utilities.common_utils import CommonUtils


class BTDialerMediaCenter(bluetooth_base_test.BluetoothBaseTest):


    def setup_class(self):
        super().setup_class()
        self.media_utils = MediaUtils(self.target, self.discoverer)
        self.common_utils = CommonUtils(self.target, self.discoverer)

    def setup_test(self):
        self.common_utils.grant_local_mac_address_permission()
        self.common_utils.enable_wifi_on_phone_device()
        self.bt_utils.pair_primary_to_secondary()
        super().enable_recording()

    def test_dialer_media_center(self):
        # Launch the mediacenter and place the song
        self.media_utils.open_media_center_app_on_hu()
        self.call_utils.handle_bluetooth_audio_pop_up()
        self.call_utils.wait_with_log(2)
        if(self.discoverer.mbs.hasUIElementWithText(constants.MEDIA_SONG)):
           logging.info('Test Media App is already opened')
        else:
           logging.info('Opening the Test Media App')
           self.media_utils.open_media_center_app_menu()
           self.call_utils.wait_with_log(5)
           self.media_utils.open_app(constants.TEST_MEDIA_APP)
           asserts.assert_true(self.discoverer.mbs.hasUIElementWithText(constants.MEDIA_SONG),'Unable to open the Test Media App')
        self.media_utils.select_media_track(constants.MEDIA_SONG)
        self.call_utils.wait_with_log(5)
        self.media_utils.minimize_now_playing()
        asserts.assert_true(self.media_utils.is_song_playing_on_hu(),
                                    'Media player should be on PLAY mode')

        # Place the phone call
        dialer_test_phone_number = constants.INFORMATION_THREE_DIGIT_NUMBER
        # Tests the calling three digits number functionality
        logging.info(
            'Calling from %s calling to %s',
            self.target.serial,dialer_test_phone_number
        )
        self.call_utils.wait_with_log(2)
        self.call_utils.dial_a_number(dialer_test_phone_number);
        self.call_utils.make_call()
        self.call_utils.wait_with_log(5)
        self.call_utils.verify_dialing_number(dialer_test_phone_number)
        self.call_utils.end_call()

        # Launch the mediacenter and verify the song is playing
        self.media_utils.open_media_app_on_hu()
        asserts.assert_true(self.media_utils.is_song_playing_on_hu(),
                                            'Media player should be on PLAY mode')
        self.media_utils.pause_media_on_hu()

    def teardown_test(self):
        # End call if test failed
        self.call_utils.end_call_using_adb_command(self.target)
        self.call_utils.press_home()
        super().teardown_test()

if __name__ == '__main__':
    common_main()
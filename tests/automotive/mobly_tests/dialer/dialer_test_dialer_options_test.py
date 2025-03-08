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




from bluetooth_test import bluetooth_base_test
from utilities.main_utils import common_main
from mobly import asserts


class BluetoothDialerOptionsTest(bluetooth_base_test.BluetoothBaseTest):


   def setup_test(self):
       # Pair the devices
       self.bt_utils.pair_primary_to_secondary()
       super().enable_recording()


   def test_dialer_options(self):
       """Test of dialer options
        Steps include:
        1. Launch Dialer app from App Launcher
        2. Verify: Dialer Launches
        3. Record Dialer Categories
        Verify: 'Recents','Contacts','Favorites',Dialpad'  tabs are  showing
        4. Verify: Search Lens, Settings displayed
        5. Tap on Recents, Contacts, Favorites, Dialpad"""

       self.call_utils.open_phone_app()
       asserts.assert_true(self.discoverer.mbs.hasUIElementWithText("Recents"),
                       'Recents menu is not displayed')
       asserts.assert_true(self.discoverer.mbs.hasUIElementWithText("Contacts"),
                           'Contacts menu is not displayed')
       asserts.assert_true(self.discoverer.mbs.hasUIElementWithText("Favorites"),
                           'Favorites menu is not displayed')
       asserts.assert_true(self.discoverer.mbs.hasUIElementWithText("Dialpad"),
                           'Dialpad menu is not displayed')
       self.call_utils.verify_dialer_recents_tab()
       self.call_utils.open_contacts()
       self.call_utils.verify_dialer_contacts_tab()
       self.call_utils.open_favorites()
       self.call_utils.verify_dialer_favorites_tab()
       self.call_utils.open_dialpad()
       self.call_utils.verify_dialer_dialpad_tab()
       self.call_utils.verify_dialer_search_lens()
       self.call_utils.verify_dialer_settings()

   def teardown_test(self):
       self.call_utils.wait_with_log(5)
       self.call_utils.press_home()
       super().teardown_test()

if __name__ == '__main__':
   common_main()

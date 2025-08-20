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


from mobly import asserts, base_test
from mobly.controllers import android_device
from mobly.controllers.android_device_lib.snippet_client_v2 import Config
from utilities.main_utils import common_main, get_test_args


class VhalHvac(base_test.BaseTestClass):
    def setup_class(self):
        self.ads = self.register_controller(android_device)
        self.main_device = android_device.get_device(self.ads, label='auto')

        test_args = get_test_args(shell_escape=True)
        snippet_config = Config(am_instrument_options=test_args)
        self.main_device.load_snippet('mbs', android_device.MBS_PACKAGE, config=snippet_config)

    def setup_test(self):
        pass

    def teardown_test(self):
        pass

    def test_set_driver_temp(self):
        """Set the driver temperature and check the UI for the resulting expected temp."""
        self.main_device.mbs.setDriverHvacTemperature("70")
        asserts.assert_true(self.main_device.mbs.hasUIElementWithText("70"), 'Temperature set')


if __name__ == '__main__':
    common_main()
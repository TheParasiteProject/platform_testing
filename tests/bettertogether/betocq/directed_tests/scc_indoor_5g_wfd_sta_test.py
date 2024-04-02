#  Copyright (C) 2024 The Android Open Source Project
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

"""This Test is to test the Wifi SCC with the indoor channels case.

This is about the feature - using indoor channels for WFD, for details, refer to
https://docs.google.com/presentation/d/18Fl0fY4piq_sfXfo3rCr2Ca55AJHEOvB7rC-rV3SQ9E/edit?usp=sharing
and config_wifiEnableStaIndoorChannelForPeerNetwork -
https://cs.android.com/android/platform/superproject/main/+/main:packages/modules/Wifi/service/ServiceWifiResources/res/values/config.xml;l=1147
In this case, the feature is enabled on both devices; the WFD and STA both are
operating on the same 5G indoor channel.

The device requirements:
  support 5G: true
  using indoor channels for WFD: true
The AP requirements:
  wifi channel: 36 (5180)
"""

import logging
import os
import sys

# Allows local imports to be resolved via relative path, so the test can be run
# without building.
_betocq_dir = os.path.dirname(os.path.dirname(__file__))
if _betocq_dir not in sys.path:
  sys.path.append(_betocq_dir)

from mobly  import base_test
from mobly import test_runner

from betocq import d2d_performance_test_base
from betocq import nc_constants


class SccIndoor5gWfdStaTest(d2d_performance_test_base.D2dPerformanceTestBase):
  """This class is to test the Wifi SCC with the indoor channel case."""

  def _get_country_code(self) -> str:
    return 'JP'

  def setup_class(self):
    super().setup_class()
    self.performance_test_iterations = getattr(
        self.test_scc_indoor_5g_wfd_sta, base_test.ATTR_REPEAT_CNT
    )
    logging.info(
        'performance test iterations: %s', self.performance_test_iterations
    )

  @base_test.repeat(
      count=nc_constants.SCC_PERFORMANCE_TEST_COUNT,
      max_consecutive_error=nc_constants.SCC_PERFORMANCE_TEST_MAX_CONSECUTIVE_ERROR,
  )
  def test_scc_indoor_5g_wfd_sta(self):
    """Test the performance for Wifi SCC with 5G indoor WFD and 5G STA."""
    self._test_connection_medium_performance(
        nc_constants.NearbyMedium.UPGRADE_TO_WIFIDIRECT,
        wifi_ssid=self.test_parameters.wifi_5g_ssid,
        wifi_password=self.test_parameters.wifi_5g_password,
    )

  def _get_file_transfer_failure_tip(self) -> str:
    return (
        'The Wifi Direct connection might be broken, check related logs, '
        f'{self._get_throughput_low_tip()}'
    )

  def _get_throughput_low_tip(self) -> str:
    return (
        'f{self._throughput_low_string}. This is 5G SCC indoor WFD test case.'
        ' In the configuration file,enable_sta_indoor_channel_for_peer_network'
        ' is set to true.Check if the target device does support WFD group'
        ' owner in the STA-associatedindoor channel. Check if'
        ' config_wifiEnableStaIndoorChannelForPeerNetworkhttps://cs.android.com/android/platform/superproject/main/+/main:packages/modules/Wifi/service/ServiceWifiResources/res/values/config.xml;l=1147),is'
        ' set to true and has the correct driver/FW implementation.'
    )

  def _is_wifi_ap_ready(self) -> bool:
    return True if self.test_parameters.wifi_5g_ssid else False

  def _are_devices_capabilities_ok(self) -> bool:
    return (
        self.discoverer.supports_5g
        and self.advertiser.supports_5g
        and self.advertiser.enable_sta_indoor_channel_for_peer_network
    )


if __name__ == '__main__':
  test_runner.main()

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

"""This test suite batches all tests to run in sequence.

This requires 3 APs to be ready and configured in testbed.
2G AP (wifi_2g_ssid): channel 6 (2437)
5G AP (wifi_5g_ssid): channel 36 (5180)
DFS 5G AP(wifi_dfs_5g_ssid): channel 52 (5260)
"""

import os
import sys

# Allows local imports to be resolved via relative path, so the test can be run
# without building.
_betocq_dir = os.path.dirname(os.path.dirname(__file__))
if _betocq_dir not in sys.path:
  sys.path.append(_betocq_dir)

from mobly import suite_runner

from betocq import base_betocq_suite
from betocq import nc_constants
from betocq.compound_tests import mcc_5g_all_wifi_non_dbs_2g_sta_test
from betocq.compound_tests import scc_2g_all_wifi_sta_test
from betocq.compound_tests import scc_5g_all_wifi_dbs_2g_sta_test
from betocq.compound_tests import scc_5g_all_wifi_sta_test
from betocq.directed_tests import bt_performance_test
from betocq.directed_tests import mcc_2g_wfd_indoor_5g_sta_test
from betocq.directed_tests import mcc_5g_hotspot_dfs_5g_sta_test
from betocq.directed_tests import mcc_5g_wfd_dfs_5g_sta_test
from betocq.directed_tests import mcc_5g_wfd_non_dbs_2g_sta_test
from betocq.directed_tests import scc_2g_wfd_sta_test
from betocq.directed_tests import scc_2g_wlan_sta_test
from betocq.directed_tests import scc_5g_wfd_dbs_2g_sta_test
from betocq.directed_tests import scc_5g_wfd_sta_test
from betocq.directed_tests import scc_5g_wlan_sta_test
from betocq.directed_tests import scc_dfs_5g_hotspot_sta_test
from betocq.directed_tests import scc_dfs_5g_wfd_sta_test
from betocq.directed_tests import scc_indoor_5g_wfd_sta_test
from betocq.function_tests import beto_cq_function_group_test


class BetoCqPerformanceTestSuite(base_betocq_suite.BaseBetocqSuite):
  """Add all BetoCQ tests to run in sequence."""

  def setup_suite(self, config):
    """Add all BetoCQ tests to the suite."""
    test_parameters = nc_constants.TestParameters.from_user_params(
        config.user_params
    )

    if test_parameters.target_cuj_name is nc_constants.TARGET_CUJ_ESIM:
      self.add_test_class(bt_performance_test.BtPerformanceTest)
      return

    # add function tests if required
    if (
        test_parameters.run_function_tests_with_performance_tests
        or test_parameters.use_auto_controlled_wifi_ap
    ):
      self.add_test_class(
          beto_cq_function_group_test.BetoCqFunctionGroupTest
      )

    # add bt and ble test
    self.add_test_class(bt_performance_test.BtPerformanceTest)

    # TODO(kaishi): enable BLE test when it is ready

    # add directed/cuj tests which requires 2G wlan AP - channel 6
    if (
        test_parameters.wifi_2g_ssid
        or test_parameters.use_auto_controlled_wifi_ap
    ):
      config = self._config.copy()
      config.user_params['wifi_channel'] = 6

      self.add_test_class(
          clazz=mcc_5g_wfd_non_dbs_2g_sta_test.Mcc5gWfdNonDbs2gStaTest,
          config=config,
      )
      self.add_test_class(
          clazz=scc_2g_wfd_sta_test.Scc2gWfdStaTest,
          config=config,
      )
      self.add_test_class(
          clazz=scc_2g_wlan_sta_test.Scc2gWlanStaTest,
          config=config,
      )
      self.add_test_class(
          clazz=scc_5g_wfd_dbs_2g_sta_test.Scc5gWfdDbs2gStaTest,
          config=config,
      )
      self.add_test_class(
          clazz=mcc_5g_all_wifi_non_dbs_2g_sta_test.Mcc5gAllWifiNonDbs2gStaTest,
          config=config,
      )
      self.add_test_class(
          clazz=scc_2g_all_wifi_sta_test.Scc2gAllWifiStaTest,
          config=config,
      )
      self.add_test_class(
          clazz=scc_5g_all_wifi_dbs_2g_sta_test.Scc5gAllWifiDbs2gStaTest,
          config=config,
      )

    # add directed tests which requires 5G wlan AP - channel 36
    if (
        test_parameters.wifi_5g_ssid
        or test_parameters.use_auto_controlled_wifi_ap
    ):
      config = self._config.copy()
      config.user_params['wifi_channel'] = 36

      self.add_test_class(
          clazz=mcc_2g_wfd_indoor_5g_sta_test.Mcc2gWfdIndoor5gStaTest,
          config=config,
      )
      self.add_test_class(
          clazz=scc_5g_wfd_sta_test.Scc5gWfdStaTest,
          config=config,
      )
      self.add_test_class(
          clazz=scc_5g_wlan_sta_test.Scc5gWifiLanStaTest,
          config=config,
      )
      self.add_test_class(
          clazz=scc_indoor_5g_wfd_sta_test.SccIndoor5gWfdStaTest,
          config=config,
      )

    # add directed/cuj tests which requires DFS 5G wlan AP - channel 52
    if (
        test_parameters.wifi_dfs_5g_ssid
        or test_parameters.use_auto_controlled_wifi_ap
    ):
      config = self._config.copy()
      config.user_params['wifi_channel'] = 52

      self.add_test_class(
          clazz=mcc_5g_hotspot_dfs_5g_sta_test.Mcc5gHotspotDfs5gStaTest,
          config=config,
      )
      self.add_test_class(
          clazz=mcc_5g_wfd_dfs_5g_sta_test.Mcc5gWfdDfs5gStaTest,
          config=config,
      )
      self.add_test_class(
          clazz=scc_dfs_5g_hotspot_sta_test.SccDfs5gHotspotStaTest,
          config=config,
      )
      self.add_test_class(
          clazz=scc_dfs_5g_wfd_sta_test.SccDfs5gWfdStaTest,
          config=config,
      )
      self.add_test_class(
          clazz=scc_5g_all_wifi_sta_test.Scc5gAllWifiStaTest,
          config=config,
      )


if __name__ == '__main__':
  # Use suite_runner's `main`.
  suite_runner.run_suite_class()
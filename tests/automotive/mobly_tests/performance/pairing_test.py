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
import statsd_collector
import statistics
import time

from bluetooth_test import bluetooth_base_test
from utilities.main_utils import common_main
from utilities.crystalball_metrics_utils import export_to_crystalball

ITERATIONS_PARAM_NAME = 'iterations'
DEFAULT_ITERATIONS = 10
DEFAULT_ITERATION_DELAY_S = 2

class BTPerformancePairingTest(bluetooth_base_test.BluetoothBaseTest):
    """Test Class for Bluetooth Pairing Test."""

    def setup_class(self):
        super().setup_class()
        self.iterations = DEFAULT_ITERATIONS
        self.iteration_delay = DEFAULT_ITERATION_DELAY_S
        if ITERATIONS_PARAM_NAME in self.user_params:
            self.iterations = self.user_params[ITERATIONS_PARAM_NAME]
        else:
            logging.info(f'{ITERATIONS_PARAM_NAME} is not in testbed config. Using default value')
        logging.info(f'Setup {self.__class__.__name__} with {ITERATIONS_PARAM_NAME} = {self.iterations} and iteration delay = {self.iteration_delay}')
        self.discoverer.services.register('statsd', statsd_collector.StatsdCollector)
        self.discoverer.bt_connection_time = self.discoverer.services.statsd.add_config('/data/local/tmp/bt_connection_time.pb')
        self.discoverer.bluetooth_event_metrics = self.discoverer.services.statsd.add_config('/data/local/tmp/bluetooth_event_metrics.pb')

    def setup_test(self):
        super().setup_test()
        super().enable_recording()

    def test_pairing(self):
        """Test for pairing/unpairing a HU with a bluetooth device"""
        pairing_success_count = 0
        metrics = {}
        for i in range(1, self.iterations + 1):
            logging.info(f'Pairing iteration {i}')
            try:
                self.bt_utils.pair_primary_to_secondary()
                pairing_success_count += 1
                self.process_per_iteration_metrics(metrics)
            except:
                logging.error(f'Failed to pair devices on iteration {i}')
            self.bt_utils.unpair()
            time.sleep(self.iteration_delay)
        self.process_per_test_metrics(metrics, pairing_success_count)
        export_to_crystalball(metrics, self.log_path, self.current_test_info.name)

    def process_per_iteration_metrics(self, metrics):
        self.process_bt_connection_time_metrics(metrics)
        self.process_bluetooth_event_metrics_metrics(metrics)

    def process_bt_connection_time_metrics(self, metrics):
        report = self.discoverer.services.statsd.get_metrics(self.discoverer.bt_connection_time, True)
        try:
            metrics_for_iteration = self.discoverer.services.statsd.process_bt_connection_time_stats_report(report.data)
        except Exception as e:
            logging.error(f'An error occurred: {e} during parsing of {str(report.data)}')
            raise
        logging.info(str(metrics_for_iteration))
        for key in metrics_for_iteration:
            metrics_key = "{headunit}:" + key + "-median"
            if metrics_key in metrics:
                metrics[metrics_key].append(metrics_for_iteration[key])
            else:
                metrics[metrics_key] = [metrics_for_iteration[key]]

    def process_bluetooth_event_metrics_metrics(self, metrics):
        report = self.discoverer.services.statsd.get_metrics(self.discoverer.bluetooth_event_metrics, True)
        try:
            metrics_for_iteration = self.discoverer.services.statsd.process_bluetooth_event_metrics_stats_report(report.data)
        except Exception as e:
            logging.error(f'An error occurred: {e} during parsing of {str(report.data)}')
            raise
        logging.info(str(metrics_for_iteration))
        for key in metrics_for_iteration:
            if key in metrics:
                metrics[key] += metrics_for_iteration[key]
            else:
                metrics[key] = metrics_for_iteration[key]

    def process_per_test_metrics(self, metrics, pairing_success_count):
        profile_success_rate = {}
        for metrics_key in metrics:
            if "-median" in metrics_key:
                metrics[metrics_key] = round(statistics.median(metrics[metrics_key]), 2)
            elif "connection_state_changed" in metrics_key:
                num_of_state_connecting = metrics[metrics_key].count(1)
                num_of_state_connected = metrics[metrics_key].count(2)
                if num_of_state_connecting == 0:
                    logging.error(f'Metric {metrics_key} does not have connecting state reported. Skipping calculating success rate')
                    continue
                profile_success_rate["{headunit}:" + metrics_key + "-success-rate"] = num_of_state_connected / num_of_state_connecting
        metrics.update(profile_success_rate)
        success_rate = pairing_success_count / self.iterations
        metrics['pairing_success_rate'] = success_rate

    def teardown_class(self):
        super().teardown_class()
        self.discoverer.services.unregister_all()

if __name__ == '__main__':
    common_main()

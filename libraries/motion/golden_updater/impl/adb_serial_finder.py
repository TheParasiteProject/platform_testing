# Copyright 2025, The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import subprocess

class ADBSerialFinder:

    def __init__(self):
        self.model_serial_map = {}
        self.update_model_serial_map()

    def update_model_serial_map(self):
        devices_response = subprocess.run(
            ["adb", "devices", "-l"], check=True, capture_output=True
        ).stdout.decode("utf-8")
        lines = [s for s in devices_response.splitlines() if s.strip()]

        '''
        Example output of 'adb devices -l':

        List of devices attached
        127.0.0.1:42093        device product:cf_x86_64_phone model:Cuttlefish_GMS_x86_64 device:vsoc_x86_64 transport_id:5
        localhost:35725        device product:panther model:Pixel_7 device:panther transport_id:4
        '''

        if len(lines) == 1:
            print("no adb devices found")
            return None

        self.model_serial_map = self.__create_model_serial_map(lines[1:])

    def __create_model_serial_map(self, devices):
        model_serial_map = {}
        for device in devices:
            device_info = [info for info in device.split(" ") if info != ""]
            model_serial_map[device_info[3].split(":")[1]]=device_info[0]
        return model_serial_map
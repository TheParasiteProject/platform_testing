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
        self.model_serial_map = {} # stores {model_serialno: <localhost:port>}
        self.update_model_serial_map()

    def update_model_serial_map(self):
        self.model_serial_map = {}
        try:
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
            self.__update_model_serial_map_with_device_info(lines[1:])
        except Exception as e:
            # Failing quietly as not having adb devices is not a blocker for running the server.
            print(f'Exception occurred while updating adb model serial map: {e}')

    def __update_model_serial_map_with_device_info(self, devices):
        '''
        Example value post update:
        {'Pixel_7_2A121FDH200F40': 'localhost:35725'}
        '''

        for device in devices:
            device_info = [info for info in device.split(" ") if info != ""]
            serial_number = self.__find_serial_number(device_info[0])
            model_key = f'{device_info[3].split(":")[1]}_{serial_number}'
            self.model_serial_map[model_key] = device_info[0]

    def __find_serial_number(self, socket_info):
        ''' Find the serial number of the device connected in the socket: <localhost:port> '''
        '''
            Example output of 'adb -s localhost:35725 shell getprop ro.serialno'

            2A121FDH200F40
        '''
        if not socket_info:
            return ""
        try:
            serial_number = subprocess.run(
                ["adb", "-s", socket_info, "shell", "getprop", "ro.serialno"],
                check=True, capture_output=True
            ).stdout.decode("utf-8")
            return serial_number.strip()
        except Exception as e:
            # Failing quietly as not having serial number is not a blocker for running the server.
            print(f'Exception occurred while fetching adb model serial number: {e}')
            return ""

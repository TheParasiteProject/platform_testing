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

from test_assets.test_assets import returnNoneIfOverFlow

class FakeADBClient:
    def __init__(self):
        self.run_adb_command_calls = []
        self.run_as_root_calls = 0
        self.wait_for_device_calls = 0
        self.run_adb_command_result = []
        self.run_as_root_result = []

    def run_as_root(self):
        self.run_as_root_calls += 1
        return returnNoneIfOverFlow(self.run_as_root_result,self.run_as_root_calls-1)

    def wait_for_device(self):
        self.wait_for_device_calls += 1

    def run_adb_command(self, args):
        self.run_adb_command_calls.append(args)
        return returnNoneIfOverFlow(self.run_adb_command_result, len(self.run_adb_command_calls) -1)
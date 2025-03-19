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

class AdbClient:
    def __init__(self, adb_serial, subprocess_run_func = subprocess.run):
        self.adb_serial = adb_serial
        self.subprocess_run = subprocess_run_func

    def run_as_root(self):
        root_result = self.run_adb_command(["root"])
        if "restarting adbd as root" in root_result:
            self.wait_for_device()
            return True
        if "adbd is already running as root" in root_result:
            return True

        print(f"run_as_root returned [{root_result}]")

        return False

    def wait_for_device(self):
        self.run_adb_command(["wait-for-device"])

    def run_adb_command(self, args):
        command = ["adb"]
        command += ["-s", self.adb_serial]
        command += args
        return self.subprocess_run(command, check=True, capture_output=True).stdout.decode(
            "utf-8"
        )
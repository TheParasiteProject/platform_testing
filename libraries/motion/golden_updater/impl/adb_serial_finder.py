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

    @staticmethod
    def get_serial():
        devices_response = subprocess.run(
            ["adb", "devices"], check=True, capture_output=True
        ).stdout.decode("utf-8")
        lines = [s for s in devices_response.splitlines() if s.strip()]

        if len(lines) == 1:
            print("no adb devices found")
            return None

        if len(lines) > 2:
            print("multiple adb devices found, specify --serial")
            return None

        return lines[1].split("\t")[0]
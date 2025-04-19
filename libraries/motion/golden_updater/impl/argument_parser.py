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

import argparse
import os
from impl.port_finder import PortFinder

class ArgumentParser:

    @staticmethod
    def get_args():
        parser = argparse.ArgumentParser(
            "Watches a connected device for golden file updates."
        )

        parser.add_argument(
            "--port",
            default = PortFinder.find_free_port(),
            type=int,
            help="Port to run test at watcher web UI on.",
        )

        parser.add_argument(
            "--atest",
            default=False,
            help="Watches atest output",
        )

        parser.add_argument(
            "--robolectricTest",
            default=False,
            help="Watch for artifacs generated when a deviceless test is run via SysUi Studio",
        )

        parser.add_argument(
            "--serial",
            default=os.environ.get("ANDROID_SERIAL"),
            help="The ADB device serial to pull goldens from.",
        )

        parser.add_argument(
            "--android_build_top",
            default=os.environ.get("ANDROID_BUILD_TOP"),
            help="The root directory of the android checkout.",
        )

        parser.add_argument(
            "--client_url",
            default="http://motion.teams.x20web.corp.google.com/",
            help="The URL where the client app is deployed.",
        )

        return parser.parse_args()
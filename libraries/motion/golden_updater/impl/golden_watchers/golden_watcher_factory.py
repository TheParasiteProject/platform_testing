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

import os
from impl.golden_watchers.golden_watcher_types import GoldenWatcherTypes
from impl.golden_watchers.atest_golden_watcher import AtestGoldenWatcher
from impl.golden_watchers.robolectric_golden_watcher import RobolectricGoldenWatcher
from impl.golden_watchers.golden_file_watcher import GoldenFileWatcher

class GoldenWatcherFactory:

    @staticmethod
    def create_watcher(type: GoldenWatcherTypes, tmpdir, adb_client = None):

        match type:
            case GoldenWatcherTypes.ATEST:
                user = os.environ.get("USER")
                return AtestGoldenWatcher(
                    tmpdir, f"/tmp/atest_result_{user}/LATEST/"
                )

            case GoldenWatcherTypes.ROBOLECTRIC:
                return RobolectricGoldenWatcher(
                    tmpdir, f"/tmp/motion/"
                )

            case GoldenWatcherTypes.FILE:
                if not adb_client:
                    raise ValueError("adb client not found")

                return GoldenFileWatcher(tmpdir, adb_client)

            case _:
                print("No such Golden Watcher exists.")
                raise ValueError("Imporper Golden Watcher Type.")




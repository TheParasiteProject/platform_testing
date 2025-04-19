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

import glob
import os
import hashlib
import datetime
import shutil
from impl.cached_golden import CachedGolden
from impl.golden_watchers.golden_watcher import GoldenWatcher

class RobolectricGoldenWatcher(GoldenWatcher):

    def __init__(self, temp_dir, latest_dir, cached_golden_service=CachedGolden):
        self.temp_dir = temp_dir
        self.latest_dir = latest_dir
        self.cached_goldens = {}
        self.cached_golden_service = cached_golden_service
        self.refresh_golden_files()

    def refresh_golden_files(self):
        for filename in glob.iglob(
            f"{self.latest_dir}//**/*.actual.json", recursive=True
        ):
            baseName = os.path.basename(filename)
            baseFilename, ext = os.path.splitext(baseName)
            timeHash = hashlib.md5(datetime.datetime.now().isoformat().encode("utf-8")).hexdigest()
            local_file = os.path.join(self.temp_dir, f'copy_{baseFilename}_{timeHash}{ext}')
            self.copy_file(filename, local_file)
            golden = self.cached_golden_service(filename, local_file)
            self.cached_goldens[filename] = golden
            if golden.video_location:
                filepath = os.path.join(self.latest_dir,
                                        f"{golden.test_class_name}/{baseFilename}.screenshots.zip")
                if os.path.isfile(filepath):
                    local_video_file = os.path.join(
                        self.temp_dir, golden.video_location
                    )
                    self.copy_file(
                        filepath, local_video_file
                    )

    def copy_file(self, source, target):
        os.makedirs(os.path.dirname(target), exist_ok=True)
        shutil.copyfile(source, target)

    def clean(self):
        self.cached_goldens = {}
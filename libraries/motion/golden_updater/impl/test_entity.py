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

'''
    A test mode can be cached with a combination of three values:
        1. golden watcher, 2. download client, 3.goldens_list.
    Not all are required all the time.
        Presubmit -> 1 and 2.
        Gerrit -> 3.
        Rest all -> 1, 3(optional).
    TestEntity encapsulates these attributes and helps in caching the test mode data.
'''
class TestEntity:
    def __init__(self, golden_watcher=None, download_client=None, goldens_list=None):
        self.golden_watcher = golden_watcher
        self.goldens_list = goldens_list
        self.download_client = download_client

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
from urllib.parse import quote

GOLDEN_ACCESS_TOKEN_HEADER = "Golden-Access-Token"
GOLDEN_ACCESS_TOKEN_LOCATION = os.path.expanduser("~/.config/motion-golden/.token")
ANDROID_BUILD_SCOPE = "https://www.googleapis.com/auth/androidbuild.internal"

def create_presubmit_artifacts_list_url(invocation_id, max_results):
    return (f'https://androidbuildinternal.googleapis.com/android/internal/build/v3/testArtifacts?'
           f'invocationId={invocation_id}&maxResults={max_results}&fields=nextPageToken%2Ctest_artifacts.name')

def create_get_download_url_for_presubmit_artifact(resource_id, invocation_id):
    return (f'https://androidbuildinternal.googleapis.com/android/internal/build/v3/testArtifacts/'
            f'{quote(resource_id, safe='')}/url?invocationId={invocation_id}')
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

import json
from requests.exceptions import RequestException
from test_assets.test_assets import returnNoneIfOverFlow
class FakeResponse:
    def __init__(self, status_code, url, error_message, data):
        self.status_code = status_code
        self.url = url
        self.error_message = error_message
        self.content = data

    def raise_for_status(self):
        if 400 <= self.status_code < 600:
            raise RequestException(self.error_message, response=self)

    def iter_content(self, block_size):
        return [self.content[i:i+block_size]
                for i in range(0, len(self.content), block_size)]

    def json(self):
        if(type(self.content) != dict):
            raise json.JSONDecodeError("Failure to parse into json object", "", 0)
        return self.content

class FakePresubmitArtifactAPIClient():
    def __init__(self, status_code=[], error_message=[],
                 data=[], raise_exception=None):
        self.status_code = status_code
        self.error_message = error_message
        self.data = data
        self.raise_exception = raise_exception
        self.calls = []

    def get(self, url, **argument_list):
        argument_list["url"] = url
        self.calls.append(argument_list)
        if self.raise_exception:
            raise self.raise_exception
        else:
            index = len(self.calls) - 1
            return FakeResponse(
                returnNoneIfOverFlow(self.status_code, index),
                url,
                returnNoneIfOverFlow(self.error_message,index),
                returnNoneIfOverFlow(self.data,index)
            )



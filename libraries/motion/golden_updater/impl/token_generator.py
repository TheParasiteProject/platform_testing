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

import secrets
import os
from impl.constants import GOLDEN_ACCESS_TOKEN_LOCATION

class TokenGenerator:

    def get_token() -> str:
        try:
            with open(GOLDEN_ACCESS_TOKEN_LOCATION, "r") as token_file:
                token = token_file.readline()
                return token
        except IOError:
            token = secrets.token_hex(32)
            os.makedirs(os.path.dirname(GOLDEN_ACCESS_TOKEN_LOCATION), exist_ok=True)
            try:
                with open(GOLDEN_ACCESS_TOKEN_LOCATION, "w") as token_file:
                    token_file.write(token)
                os.chmod(GOLDEN_ACCESS_TOKEN_LOCATION, 0o600)
            except IOError:
                print(
                    "Unable to save persistent token {} to {}".format(
                        token, GOLDEN_ACCESS_TOKEN_LOCATION
                    )
                )
            return token
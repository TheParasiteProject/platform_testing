#  Copyright (C) 2024 The Android Open Source Project
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

"""DesktopTest decorator."""

from typing import List


class DesktopTest(object):
  """Marks the type of test with purpose of asserting Desktop requirements and cujs.

  Args:
      requirements: the list of Desktop requirements.
      cujs: the list of Desktop cujs.

  Example:
      @DesktopTest(requirements=['D-0-1', 'D-0-2'], cujs=['cuj-1', 'cuj-2'])
  """

  def __init__(self, requirements: List[str] = [], cujs: List[str] = []):
    self._requirements = requirements
    self._cujs = cujs

  def __call__(self, func):
    return func

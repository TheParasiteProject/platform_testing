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

"""ApiTest decorator."""

from typing import List


class ApiTest(object):
  """Marks the type of CTS test with purpose of asserting API functionalities and behaviors.

  Args:
      apis: the list of APIs being tested.

  Example:
      @ApiTest(apis=['android.example.ClassA#MethodA', 'android.example.ClassB#MethodB'])
  """

  def __init__(self, apis: List[str] = []):
    self._apis = apis

  def __call__(self, func):
    return func

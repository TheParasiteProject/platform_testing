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

import unittest
import sys
import os

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
from impl.adb_client import AdbClient

class FakeSubprocessRun:
    def __init__(self, return_stdout=None, raise_exception=None):
        self.return_stdout = return_stdout
        self.raise_exception = raise_exception
        self.calls = []

    def __call__(self, command, check=True, capture_output=True):
        self.calls.append(command)
        if self.raise_exception:
            raise self.raise_exception
        else:
            class FakeProcess:
                stdout = self.return_stdout

            return FakeProcess()

fake_subprocess_run = FakeSubprocessRun(return_stdout=b"test output")
adb_client = AdbClient("adb_serial", fake_subprocess_run)
command_prefix = ["adb","-s","adb_serial"]

class AdbClientTest(unittest.TestCase):

  def test_init(self):
    self.assertEqual(adb_client.adb_serial, "adb_serial")

  def test_run_as_root_restarting_adb(self):
    fake_subprocess_run.return_stdout = b"restarting adbd as root"
    fake_subprocess_run.calls = []

    self.assertTrue(adb_client.run_as_root())

    self.assertEqual(
       fake_subprocess_run.calls,
        [
          (command_prefix + ["root"]),
          (command_prefix + ["wait-for-device"]),
        ]
    )

  def test_run_as_root_adb_already_running(self):
    fake_subprocess_run.return_stdout = b"adbd is already running as root"
    fake_subprocess_run.calls = []

    self.assertTrue(adb_client.run_as_root())
    self.assertEqual(fake_subprocess_run.calls, [(command_prefix + ["root"])])

  def test_run_as_root_failed(self):
    fake_subprocess_run.return_stdout = b"permission denied"
    fake_subprocess_run.calls = []

    self.assertFalse(adb_client.run_as_root())
    self.assertEqual(fake_subprocess_run.calls, [(command_prefix + ["root"])])

  def test_run_adb_command(self):
    fake_subprocess_run.return_stdout = b"results returned"
    fake_subprocess_run.calls = []

    result = adb_client.run_adb_command(["wait-for-device"])

    self.assertEqual(fake_subprocess_run.calls, [(command_prefix + ["wait-for-device"])])
    self.assertEqual(result, "results returned")

if __name__ == '__main__':
  unittest.main()
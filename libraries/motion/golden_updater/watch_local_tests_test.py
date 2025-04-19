#!/usr/bin/env python3

#
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

import subprocess
import os
import glob
import sys

base_path = os.path.dirname(__file__)

def main():
  for filename in glob.iglob(
      f"{base_path}/tests/**/*test.py", recursive=True
  ):
    result = run_script(filename)
    if result:
        print(result.stdout)
        print(result.stderr)

def run_script(script_path, *args):
    """Runs a Python script and returns its output."""
    try:
        result = subprocess.run(
            ["python", script_path, *args],
            capture_output=True,
            text=True,
            check=True
        )
        return result
    except subprocess.CalledProcessError as e:
        print(f"Error running script: {e}")
        print(f"Stderr: {e.stderr}")
        sys.exit(1)

if __name__ == '__main__':
  main()
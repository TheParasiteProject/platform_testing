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
from unittest.mock import patch, mock_open, call
import sys
import os

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
from test_assets.test_assets import MOCK_TOKEN_LOCATION
from impl.token_generator import TokenGenerator

class TokenGeneratorTest(unittest.TestCase):

  @patch("builtins.open", new_callable=mock_open, read_data="found_token")
  @patch('impl.token_generator.GOLDEN_ACCESS_TOKEN_LOCATION', new=MOCK_TOKEN_LOCATION)
  def test_get_token(self, mock_open_func):
    self.assertEqual(TokenGenerator.get_token(), "found_token")
    mock_open_func.assert_called_once_with(MOCK_TOKEN_LOCATION, "r")

  @patch("builtins.open")
  @patch('secrets.token_hex')
  @patch('os.chmod')
  @patch('impl.token_generator.GOLDEN_ACCESS_TOKEN_LOCATION', new=MOCK_TOKEN_LOCATION)
  def test_get_token_throws_error_on_reading(self, mock_chmod, mock_secret, mock_open_func):
    mock_write_file = mock_open()

    mock_open_func.side_effect = [FileNotFoundError, mock_write_file.return_value]
    mock_secret.return_value = "super_secret_key"

    token = TokenGenerator.get_token()
    self.assertTrue(os.path.isdir(os.path.dirname(MOCK_TOKEN_LOCATION)))
    mock_open_func.assert_has_calls(
      [
        call(MOCK_TOKEN_LOCATION, "r"),
        call(MOCK_TOKEN_LOCATION, "w")
      ]
    )
    mock_chmod.assert_called_once_with(MOCK_TOKEN_LOCATION, 0o600)
    self.assertEqual(token, "super_secret_key")


  @patch("builtins.open")
  @patch('secrets.token_hex')
  @patch('impl.token_generator.GOLDEN_ACCESS_TOKEN_LOCATION', new=MOCK_TOKEN_LOCATION)
  def test_get_token_throws_error_on_reading_and_writing(self, mock_secret, mock_open_func):
    mock_open_func.side_effect = [FileNotFoundError, PermissionError]
    mock_secret.return_value = "super_secret_key"

    self.assertEqual(TokenGenerator.get_token(), "super_secret_key")
    mock_open_func.assert_has_calls(
      [
        call(MOCK_TOKEN_LOCATION, "r"),
        call(MOCK_TOKEN_LOCATION, "w")
      ]
    )

if __name__ == '__main__':
  unittest.main()
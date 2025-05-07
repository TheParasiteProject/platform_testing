#  Copyright (C) 2025 The Android Open Source Project
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

import os
import shutil
import subprocess
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest import mock
from unittest.mock import patch, mock_open
from zipfile import ZipFile

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))
from impl.zip_to_video_converter import ZipToVideoConverter


class TestZipToVideoConverter(unittest.TestCase):

    def setUp(self):
        self.converter = ZipToVideoConverter()
        self.temp_dir = Path(tempfile.mkdtemp())
        self.zip_file_path = self.temp_dir / "test.zip"
        self.final_video_path = self.temp_dir / "final_test.mp4"
        self.video_file_path = self.temp_dir / "test.mp4"

    def tearDown(self):
        shutil.rmtree(self.temp_dir)

    def create_test_zip(self, png_files, with_dotdots=False, with_absolute_paths=False):
        with zipfile.ZipFile(self.zip_file_path, 'w') as zipf:
            for filename in png_files:
                file_content = b"Test file"
                if with_dotdots:
                    filename = f"../{filename}"
                if with_absolute_paths:
                    filename = f"/{filename}"
                zipf.writestr(filename, file_content)

    def test_extract_pngs_valid_zip(self):
        self.create_test_zip(["image_0001.png", "image_0002.png", "image_0003.png"])
        extracted_count = self.converter.extract_pngs(self.zip_file_path, self.temp_dir, self.zip_file_path)
        self.assertEqual(extracted_count, 3)
        for i in range(1,4):
          self.assertTrue((self.temp_dir / f"image_{i:04d}.png").exists())

    def test_extract_pngs_no_pngs(self):
        self.create_test_zip([])
        extracted_count = self.converter.extract_pngs(self.zip_file_path, self.temp_dir, self.zip_file_path)
        self.assertEqual(extracted_count, 0)

    def test_extract_pngs_invalid_zip(self):
        with open(self.zip_file_path, 'w') as f:
            f.write("This is not a valid zip file")
        extracted_count = self.converter.extract_pngs(self.zip_file_path, self.temp_dir, self.zip_file_path)
        self.assertEqual(extracted_count, -1)

    def test_extract_pngs_ignore_dotdots(self):
        self.create_test_zip(["image_0001.png", "image_0002.png"], with_dotdots=True)
        self.create_test_zip(["image_0003.png"], with_dotdots=False)
        extracted_count = self.converter.extract_pngs(self.zip_file_path, self.temp_dir, self.zip_file_path)
        self.assertEqual(extracted_count, 1)
        self.assertTrue((self.temp_dir / "image_0003.png").exists())
        self.assertFalse((self.temp_dir / "../image_0001.png").exists())
        self.assertFalse((self.temp_dir / "../image_0002.png").exists())

    def test_extract_pngs_ignore_absolute_paths(self):
        self.create_test_zip(["image_0001.png", "image_0002.png"], with_absolute_paths=True)
        self.create_test_zip(["image_0003.png"], with_absolute_paths=False)
        extracted_count = self.converter.extract_pngs(self.zip_file_path, self.temp_dir, self.zip_file_path)
        self.assertEqual(extracted_count, 1)
        self.assertTrue((self.temp_dir / "image_0003.png").exists())
        self.assertFalse((self.temp_dir / "/image_0001.png").exists())
        self.assertFalse((self.temp_dir / "/image_0002.png").exists())

    @patch('subprocess.run')
    def test_run_ffmpeg_success(self, mock_run):
        mock_run.return_value = subprocess.CompletedProcess(args=[], returncode=0)
        with open(self.temp_dir / "test.mp4", "w") as f:
            f.write("test video")
        result = self.converter.run_ffmpeg(self.temp_dir, self.temp_dir / "test.mp4", self.zip_file_path)
        self.assertTrue(result)

    def create_dummy_png(self, filename):
        # Create a simple 1x1 grayscale PNG using raw bytes
        png_bytes = b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x00\x00\x00\x00:~\x9bU\x00\x00\x00\nIDATx\x9cc\xfc\xff\x03\x00\x00\x02\x00\x01\xd4\x00\x0c\x00\x00\x00\x00IEND\xaeB`\x82"
        with open(self.temp_dir / filename, "wb") as f:
            f.write(png_bytes)

    @patch('subprocess.run')
    def test_run_ffmpeg_success(self, mock_run):
        mock_run.return_value = subprocess.CompletedProcess(args=[], returncode=0)
        with open(self.temp_dir / "test.mp4", "w") as f:
            f.write("test video")
        result = self.converter.run_ffmpeg(self.temp_dir, self.temp_dir / "test.mp4", self.zip_file_path)
        self.assertTrue(result)

    def test_run_ffmpeg_success(self):
        self.create_test_zip(["image_0001.png", "image_0002.png", "image_0003.png"])
        for i in range(1,4):
            self.create_dummy_png(f"image_{i:04d}.png")

        try:
            subprocess.run([ZipToVideoConverter.FFMPEG_PATH, "-version"], check=True, capture_output=True)
        except (FileNotFoundError, subprocess.CalledProcessError):
            self.skipTest(f"ffmpeg executable not found or not working: '{ZipToVideoConverter.FFMPEG_PATH}'. Skipping integration test.")
            return

        # Run ffmpeg and assert that it returns True
        result = self.converter.run_ffmpeg(self.temp_dir, self.temp_dir / "test.mp4", self.zip_file_path)
        self.assertTrue(result)
        self.assertTrue((self.temp_dir / "test.mp4").exists())

    def test_run_ffmpeg_failure(self):
        self.create_test_zip(["image_0001.png", "image_0002.png", "image_0003.png"])

        for i in range(1, 4):
            self.create_dummy_png(f"image_{i:04d}.png")

        try:
            subprocess.run([ZipToVideoConverter.FFMPEG_PATH, "-version"], check=True, capture_output=True)
        except (FileNotFoundError, subprocess.CalledProcessError):
            self.skipTest(f"ffmpeg executable not found or not working: '{ZipToVideoConverter.FFMPEG_PATH}'. Skipping integration test.")
            return

        # Run ffmpeg with an invalid frame rate to force a failure
        # Invalid frame rate will cause ffmpeg to fail
        ZipToVideoConverter.FRAME_RATE = "invalid"

        result = self.converter.run_ffmpeg(self.temp_dir, self.temp_dir / "test.mp4", self.zip_file_path)
        self.assertFalse(result)
        self.assertFalse((self.temp_dir / "test.mp4").exists())
        # Reset the frame rate to original value.
        ZipToVideoConverter.FRAME_RATE = "62.5"

    def test_run_ffmpeg_no_ffmpeg(self):
        # Simulate a situation where ffmpeg is not found
        with patch('subprocess.run', side_effect=FileNotFoundError):
            result = self.converter.run_ffmpeg(self.temp_dir, self.temp_dir / "test.mp4", self.zip_file_path)
            self.assertFalse(result)

    def test_run_ffmpeg_no_output(self):
        self.create_test_zip(["image_0001.png", "image_0002.png", "image_0003.png"])

        for i in range(1, 4):
            self.create_dummy_png(f"image_{i:04d}.png")

        # Check if ffmpeg is available. If not, skip the test.
        try:
            subprocess.run([ZipToVideoConverter.FFMPEG_PATH, "-version"], check=True, capture_output=True)
        except (FileNotFoundError, subprocess.CalledProcessError):
            self.skipTest(f"ffmpeg executable not found or not working: '{ZipToVideoConverter.FFMPEG_PATH}'. Skipping integration test.")
            return

        # Mock subprocess.run to simulate a successful ffmpeg run, but without creating an output file
        with patch('subprocess.run') as mock_run:
            mock_run.return_value = subprocess.CompletedProcess(args=[], returncode=0)
            result = self.converter.run_ffmpeg(self.temp_dir, self.temp_dir / "test.mp4", self.zip_file_path)
        # Assert that run_ffmpeg returns False, because the output file is not present
            self.assertFalse(result)

    def test_move_video_success(self):
        with open(self.video_file_path, "w") as f:
            f.write("test video")
        result = self.converter.move_video(self.video_file_path, self.final_video_path, self.zip_file_path)
        self.assertTrue(result)
        self.assertTrue(self.final_video_path.exists())
        self.assertFalse((self.video_file_path).exists())

    def test_move_video_no_file(self):
        result = self.converter.move_video(self.video_file_path, self.final_video_path, self.zip_file_path)
        self.assertFalse(result)

    def test_move_video_destination_exists(self):
        with open(self.video_file_path, "w") as f:
            f.write("temp video")
        final_video_path = self.final_video_path
        with open(final_video_path, "w") as f:
            f.write("existing video")

        result = self.converter.move_video(self.video_file_path, final_video_path, self.zip_file_path)
        self.assertTrue(result)
        self.assertTrue(final_video_path.exists())
        with open(final_video_path, "r") as f:
          self.assertEqual(f.read(), "temp video")

    @patch.object(ZipToVideoConverter, 'extract_pngs', return_value=3)
    @patch.object(ZipToVideoConverter, 'run_ffmpeg', return_value=True)
    @patch.object(ZipToVideoConverter, 'move_video', return_value=True)
    def test_process_single_zip_success(self, mock_move_video, mock_run_ffmpeg, mock_extract_pngs):
        self.create_test_zip(["image_0001.png", "image_0002.png", "image_0003.png"])
        result = self.converter.process_single_zip(self.zip_file_path)
        self.assertTrue(result)
        mock_extract_pngs.assert_called_once()
        mock_run_ffmpeg.assert_called_once()
        mock_move_video.assert_called_once()

    @patch.object(ZipToVideoConverter, 'extract_pngs', return_value=-1)
    def test_process_single_zip_extract_fails(self, mock_extract_pngs):
        self.create_test_zip(["image_0001.png", "image_0002.png", "image_0003.png"])
        result = self.converter.process_single_zip(self.zip_file_path)
        self.assertFalse(result)
        mock_extract_pngs.assert_called_once()

    @patch.object(ZipToVideoConverter, 'extract_pngs', return_value=3)
    @patch.object(ZipToVideoConverter, 'run_ffmpeg', return_value=False)
    def test_process_single_zip_ffmpeg_fails(self, mock_run_ffmpeg, mock_extract_pngs):
        self.create_test_zip(["image_0001.png", "image_0002.png", "image_0003.png"])
        result = self.converter.process_single_zip(self.zip_file_path)
        self.assertFalse(result)
        mock_extract_pngs.assert_called_once()
        mock_run_ffmpeg.assert_called_once()

    @patch.object(ZipToVideoConverter, 'extract_pngs', return_value=3)
    @patch.object(ZipToVideoConverter, 'run_ffmpeg', return_value=True)
    @patch.object(ZipToVideoConverter, 'move_video', return_value=False)
    def test_process_single_zip_move_fails(self, mock_move_video, mock_run_ffmpeg, mock_extract_pngs):
        self.create_test_zip(["image_0001.png", "image_0002.png", "image_0003.png"])
        result = self.converter.process_single_zip(self.zip_file_path)
        self.assertFalse(result)
        mock_extract_pngs.assert_called_once()
        mock_run_ffmpeg.assert_called_once()
        mock_move_video.assert_called_once()

    @patch.object(ZipToVideoConverter, 'extract_pngs', return_value=0)
    def test_process_single_zip_no_pngs(self, mock_extract_pngs):
        self.create_test_zip([])
        result = self.converter.process_single_zip(self.zip_file_path)
        self.assertTrue(result)
        mock_extract_pngs.assert_called_once()

    @patch.object(ZipToVideoConverter, 'extract_pngs', side_effect=Exception("Test Exception"))
    def test_process_single_zip_exception(self, mock_extract_pngs):
        self.create_test_zip(["image_0001.png", "image_0002.png", "image_0003.png"])
        result = self.converter.process_single_zip(self.zip_file_path)
        self.assertFalse(result)
        mock_extract_pngs.assert_called_once()

if __name__ == '__main__':
    unittest.main()
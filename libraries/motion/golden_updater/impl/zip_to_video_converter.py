import zipfile
import os
import subprocess
import sys
import tempfile
import shutil
import pathlib

class ZipToVideoConverter:

    FFMPEG_PATH = "ffmpeg"
    FRAME_RATE = "62.5"
    PNG_PATTERN = "image_%04d.png"

    @staticmethod
    def get_relative_path(path: pathlib.Path) -> pathlib.Path:
        try:
            return path.relative_to(pathlib.Path.cwd())
        except ValueError:
            return path

    @staticmethod
    def extract_pngs(zip_path: pathlib.Path, temp_dir: pathlib.Path, relative_zip_path: pathlib.Path) -> int:
        """
        Extracts PNG files from the ZIP to the temp directory.
        Returns: Extracted count, 0 if no PNGs/0 extracted, -1 on fatal error.
        """
        extracted_png_count = 0
        try:
            with zipfile.ZipFile(zip_path, 'r') as zip_ref:
                png_files_in_zip = [
                    item for item in zip_ref.infolist()
                    if item.filename.lower().endswith('.png')
                ]

                if not png_files_in_zip:
                    return 0

                for item in png_files_in_zip:
                    if ".." in item.filename or os.path.isabs(item.filename):
                        continue
                    try:
                        zip_ref.extract(item, temp_dir)
                        extracted_png_count += 1
                    except Exception:
                        pass

                if extracted_png_count == 0:
                    print(f"Info: Extracted 0 usable PNG files from {relative_zip_path}. Skipping video creation.", file=sys.stderr)
                    return 0

            return extracted_png_count

        except zipfile.BadZipFile:
            print(f"Error: Invalid ZIP file '{relative_zip_path}'", file=sys.stderr)
            return -1
        except Exception as e:
            print(f"Error during extraction for '{relative_zip_path}': {e}", file=sys.stderr)
            return -1

    @staticmethod
    def run_ffmpeg(temp_dir: pathlib.Path, temp_video_output_path: pathlib.Path, relative_zip_path: pathlib.Path) -> bool:
        """
        Runs FFmpeg in the temp directory.
        Returns: True on success, False on failure.
        """
        original_cwd = os.getcwd()
        try:
            os.chdir(temp_dir)

            ffmpeg_command = [
                ZipToVideoConverter.FFMPEG_PATH,
                "-r", ZipToVideoConverter.FRAME_RATE,
                "-i", ZipToVideoConverter.PNG_PATTERN,
                str(temp_video_output_path)
            ]

            result = subprocess.run(ffmpeg_command, capture_output=True, text=True, check=False)

            if result.returncode != 0:
                print(f"Error: FFmpeg failed for '{relative_zip_path}' (exit code {result.returncode}).", file=sys.stderr)
                return False
            else:
                 if not temp_video_output_path.exists():
                     print(f"Error: FFmpeg success, but output video not found for '{relative_zip_path}'!", file=sys.stderr)
                     return False
                 return True

        except FileNotFoundError:
             print(f"Error: FFmpeg executable not found: '{ZipToVideoConverter.FFMPEG_PATH}'. Check path or install FFmpeg.", file=sys.stderr)
             return False
        except Exception as e:
            print(f"Error during FFmpeg processing for '{relative_zip_path}': {e}", file=sys.stderr)
            return False
        finally:
            os.chdir(original_cwd)


    @staticmethod
    def move_video(temp_video_output_path: pathlib.Path, final_video_path: pathlib.Path, relative_zip_path: pathlib.Path) -> bool:
        """
        Moves the video file from temp to final destination.
        Returns: True on success, False on failure.
        """
        if temp_video_output_path.exists():
            try:
                final_video_path.parent.mkdir(parents=True, exist_ok=True)
                shutil.move(str(temp_video_output_path), str(final_video_path))
                print(f"Success: Created {ZipToVideoConverter.get_relative_path(final_video_path)}", file=sys.stderr)
                return True
            except Exception as move_err:
                print(f"Error: Failed to move video for '{relative_zip_path}': {move_err}", file=sys.stderr)
                return False
        else:
            print(f"Error: Cannot move video for '{relative_zip_path}', temp file does not exist!", file=sys.stderr)
            return False


    @staticmethod
    def process_single_zip(zip_path: pathlib.Path) -> bool:
        """
        Processes a single ZIP file by extracting PNGs, creating a video using ffmpeg,
        and cleaning up temporary files. Saves the video in the same directory as the input zip.

        Args:
            zip_path: Path object pointing to the input ZIP file.

        Returns:
            True if processing was successful (or skipped safely), False otherwise.
        """
        output_dir = zip_path.parent
        base_name = zip_path.stem
        video_name = base_name + ".mp4"
        final_video_path = output_dir / video_name
        relative_zip_path = ZipToVideoConverter.get_relative_path(zip_path)

        try:
            with tempfile.TemporaryDirectory(prefix=f"{base_name}_ffmpeg_") as temp_dir_str:
                temp_dir = pathlib.Path(temp_dir_str)

                extracted_count = ZipToVideoConverter.extract_pngs(zip_path, temp_dir, relative_zip_path)
                if extracted_count < 0:
                    return False
                if extracted_count == 0:
                    return True

                temp_video_output_path = temp_dir / video_name

                if not ZipToVideoConverter.run_ffmpeg(temp_dir, temp_video_output_path, relative_zip_path):
                    return False

                if not ZipToVideoConverter.move_video(temp_video_output_path, final_video_path, relative_zip_path):
                    return False

                return True

        except Exception as e:
            print(f"Error processing '{relative_zip_path}': {e}", file=sys.stderr)
            return False
        finally:
             print(f"--- Finished: {relative_zip_path} ---", file=sys.stderr)

#!/usr/bin/env python3

import argparse
import os
import shutil


def parse_arguments():
    """Parses command-line arguments and returns the parsed arguments object."""
    parser = argparse.ArgumentParser(description="Validate directories and golden files.")
    parser.add_argument("--source-directory", required=True, help="Path to the source directory.")
    parser.add_argument("--android-build-top", required=True,
                        help="Path to the Android build directory.")
    return parser.parse_args()


def validate_directories(args):
    """Validates the provided source and Android directory arguments and returns their paths if valid."""
    if not args.source_directory or not args.android_build_top:
        print("Error: Both --source-directory and --android-build-top arguments are required.")
        return None

    source_dir = args.source_directory
    android_dir = args.android_build_top

    is_source_dir_valid = os.path.isdir(source_dir)
    is_android_dir_valid = os.path.isdir(android_dir)

    if not is_source_dir_valid:
        print(f"Error: Source directory does not exist: {source_dir}")

    if not is_android_dir_valid:
        print(f"Error: Android build directory does not exist: {android_dir}")

    if not is_source_dir_valid or not is_android_dir_valid:
        return None

    return [source_dir, android_dir]


def find_golden_files(source_dir):
    """Finds golden files within the source directory and returns their filenames or handles errors."""
    golden_files = []
    for root, _, files in os.walk(source_dir):
        for file in files:
            if "_goldResult_" in file and file.endswith(".textproto"):
                golden_files.append(os.path.join(root, file))

    if not golden_files:
        print("Error: No golden files found in the source directory.")
        return None

    return golden_files


def validate_protos(proto_files):
    """
    Validates proto files, determines status (new or update), and returns image location data.
    This function does not print on success.
    """
    all_image_locations = []
    for filepath in proto_files:
        filename = os.path.basename(filepath)
        found_image_locations = {}

        with open(filepath, 'r') as file:
            for line in file:
                parts = line.strip().split(": ", 1)
                if len(parts) == 2 and "image_location_" in parts[0]:
                    key = parts[0]
                    if key in found_image_locations:
                        print(f"Error: Duplicate '{key}' entry found in {filename}.")
                        return None
                    found_image_locations[key] = parts[1].strip().strip('"')

        found_keys = set(found_image_locations.keys())
        update_keys = {"image_location_diff", "image_location_golden", "image_location_reference", "image_location_test"}
        new_image_keys = {"image_location_golden", "image_location_test"}

        if found_keys == update_keys:
            found_image_locations['status'] = 'Updated'
            all_image_locations.append(found_image_locations)
        elif found_keys == new_image_keys:
            found_image_locations['status'] = 'Added'
            all_image_locations.append(found_image_locations)
        else:
            if not new_image_keys.issubset(found_keys):
                missing = new_image_keys - found_keys
                print(f"Error: {filename} is missing essential key(s): {', '.join(missing)}")
            else:
                unexpected = found_keys - update_keys
                print(f"Error: {filename} has an invalid key combination. Unexpected: {', '.join(unexpected)}")
            return None

    return all_image_locations


def validate_test_images(source_dir, all_image_locations):
    """
    Validates if PNG files exist in the source directory. Does not print on success.
    """
    for image_locations in all_image_locations:
        for location_key, path_value in image_locations.items():
            if location_key not in ["image_location_golden", "status"]:
                base_name = os.path.splitext(os.path.basename(path_value))[0]
                found_png = False
                for root, _, files in os.walk(source_dir):
                    for file in files:
                        if file.startswith(base_name) and file.endswith(".png"):
                            image_locations[location_key] = os.path.join(root, file)
                            found_png = True
                            break
                    if found_png:
                        break
                if not found_png:
                    print(f"Error: No PNG file found for '{path_value}' in {source_dir}")
                    return None
    return all_image_locations


def update_goldens(android_dir, updated_image_locations_list):
    """
    Copies test images to their golden paths and prints the final status for each image.
    """
    for image_locations in updated_image_locations_list:
        golden_path_relative = image_locations["image_location_golden"]
        golden_image_name = os.path.basename(golden_path_relative)
        test_image_path = image_locations["image_location_test"]
        golden_image_path_full = os.path.join(android_dir, golden_path_relative)
        status = image_locations.get('status', 'Failed')

        try:
            os.makedirs(os.path.dirname(golden_image_path_full), exist_ok=True)
            shutil.copy2(test_image_path, golden_image_path_full)
            print(f"{status}: {golden_image_name}")
        except IOError as e:
            print(f"Fail: {golden_image_name} - {e}")
        except Exception as e:
            print(f"Fail: {golden_image_name} - An unexpected error occurred: {e}")


def main():
    args = parse_arguments()

    directories = validate_directories(args)
    if directories is None:
        return

    source_dir, android_dir = directories

    proto_files = find_golden_files(source_dir)
    if proto_files is None:
        return

    all_image_locations = validate_protos(proto_files)
    if all_image_locations is None:
        return

    updated_image_locations_list = validate_test_images(source_dir, all_image_locations)
    if updated_image_locations_list is None:
        return

    update_goldens(android_dir, updated_image_locations_list)


if __name__ == "__main__":
    main()
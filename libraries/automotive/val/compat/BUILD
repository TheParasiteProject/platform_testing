load("//tools/build_defs/android:rules.bzl", "android_library")

package(
    default_visibility = [
        "//java/com/google/android/libraries/automotive/val:__subpackages__",
        "//javatests/com/google/android/libraries/automotive/val:__subpackages__",
    ],
)

licenses(["unencumbered"])  # Google owned

# A file set for gradle library module assembly in CCA Reference App.
Fileset(
    name = "gradle_source_fileset",
    entries = [
        FilesetEntry(
            files = glob(["*.java"]),
            destdir = "src/main/java/com/google/android/libraries/automotive/val/compat",
        ),
    ],
    visibility = ["//java/com/google/cloud/ml/api/cca/sample:__subpackages__"],
)

android_library(
    name = "compat",
    srcs = [
        "CarAreaConfigCompat.java",
        "CarPropertyConfigCompat.java",
        "CarPropertyManagerCompat.java",
        "CarPropertyValueCompat.java",
        "CompatUtils.java",
    ],
    manifest = "AndroidManifest.xml",
    deps = [
        "//java/com/google/android/libraries/automotive/val/api:error_or",
        "//java/com/google/common/annotations",
        "//third_party/java/android_libs/car_lib",
        "//third_party/java/android_libs/guava_jdk5:base",
        "//third_party/java/android_libs/guava_jdk5:collect",
        "//third_party/java/androidx/core",
        "//third_party/java/auto:auto_value",
    ],
)

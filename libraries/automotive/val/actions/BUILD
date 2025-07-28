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
            destdir = "src/main/java/com/google/android/libraries/automotive/val/actions",
        ),
    ],
    visibility = ["//java/com/google/cloud/ml/api/cca/sample:__subpackages__"],
)

android_library(
    name = "actions",
    srcs = [
        "Action.java",
        "ActionCategory.java",
        "GetAction.java",
        "GetActionResult.java",
        "HvacPowerDependentGetAction.java",
        "HvacPowerDependentOffsetAction.java",
        "HvacPowerDependentSetAction.java",
        "HvacTargetTemperatureGetAction.java",
        "HvacTargetTemperatureOffsetAction.java",
        "HvacTargetTemperatureSetAction.java",
        "OffsetAction.java",
        "OffsetActionResult.java",
        "OptionalActionParameters.java",
        "SetAction.java",
        "SetActionResult.java",
    ],
    deps = [
        "//java/com/google/android/libraries/automotive/val/api:error_or",
        "//java/com/google/android/libraries/automotive/val/api:offset_request",
        "//java/com/google/android/libraries/automotive/val/api:temperature",
        "//java/com/google/android/libraries/automotive/val/api:update_target_temperature_request",
        "//java/com/google/android/libraries/automotive/val/api:value_range",
        "//java/com/google/android/libraries/automotive/val/compat",
        "//java/com/google/android/libraries/automotive/val/utils",
        "//third_party/java/android_libs/car_lib",
        "//third_party/java/android_libs/guava_jdk5:base",
        "//third_party/java/android_libs/guava_jdk5:collect",
        "//third_party/java/android_libs/guava_jdk5:concurrent",
        "//third_party/java/android_libs/guava_jdk5:listenablefuture",
        "//third_party/java/auto:auto_value",
    ],
)

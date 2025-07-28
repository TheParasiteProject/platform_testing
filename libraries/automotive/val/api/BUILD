load("//tools/build_defs/android:rules.bzl", "android_library")

licenses(["unencumbered"])  # Google owned

# A file set for gradle library module assembly in CCA Reference App.
Fileset(
    name = "gradle_source_fileset",
    entries = [
        FilesetEntry(
            files = glob([
                "*.java",
                "*.kt",
            ]),
            destdir = "src/main/java/com/google/android/libraries/automotive/val/api",
        ),
    ],
    visibility = ["//java/com/google/cloud/ml/api/cca/sample:__subpackages__"],
)

android_library(
    name = "api",
    visibility = [
        "//java/com/google/android/libraries/assistant/auto/tng/carautomation:__subpackages__",
        "//java/com/google/android/libraries/automotive/val/api/tools:__subpackages__",
        "//java/com/google/android/libraries/automotive/val/app:__subpackages__",
        "//java/com/google/cloud/ml/api/cca/sample:__subpackages__",
        "//java/com/google/research/air/automation:internal",
        "//javatests/com/google/android/libraries/assistant/auto/tng/carautomation:__subpackages__",
        "//javatests/com/google/android/libraries/automotive/val/api/tools:__subpackages__",
        "//javatests/com/google/cloud/ml/api/cca/sample:__subpackages__",
    ],
    exports = [
        ":api_internal",
        ":error_or",
        ":offset_request",
        ":temperature",
        ":update_target_temperature_request",
        ":value_range",
    ],
)

android_library(
    name = "api_internal",
    srcs = [
        "ApiActionCategory.java",
        "DoorActions.java",
        "ExternalLightActions.java",
        "FunctionCall.kt",
        "FunctionDeclaration.kt",
        "FunctionResponse.kt",
        "GetResult.java",
        "GlobalGetResult.java",
        "GlobalSetResult.java",
        "OffsetResult.java",
        "OffsetTargetTemperatureRequest.kt",
        "Schema.kt",
        "SeatActions.java",
        "SetResult.java",
        "SteeringWheelActions.java",
        "VehicleActions.java",
        "VehicleAgents.java",
        "WindowActions.java",
    ],
    deps = [
        ":error_or",
        ":offset_request",
        ":parameter_name",
        ":temperature",
        ":update_target_temperature_request",
        ":value_range",
        "//java/com/google/android/libraries/automotive/val/actions",
        "//java/com/google/android/libraries/automotive/val/compat",
        "//java/com/google/android/libraries/automotive/val/utils",
        "//java/com/google/common/annotations",
        "//third_party/java/android_libs/car_lib",
        "//third_party/java/android_libs/guava_jdk5:base",
        "//third_party/java/android_libs/guava_jdk5:collect",
        "//third_party/java/android_libs/guava_jdk5:concurrent",
        "//third_party/java/android_libs/guava_jdk5:listenablefuture",
        "//third_party/java/android_libs/guava_jdk5:reflect",
        "//third_party/java/androidx/annotation",
        "//third_party/java/auto:auto_value",
    ],
)

android_library(
    name = "parameter_name",
    srcs = ["ParameterName.java"],
    visibility = ["//visibility:private"],
)

android_library(
    name = "temperature",
    srcs = ["Temperature.kt"],
    visibility = [
        "//java/com/google/android/libraries/automotive/val:__subpackages__",
        "//javatests/com/google/android/libraries/automotive/val:__subpackages__",
    ],
)

android_library(
    name = "update_target_temperature_request",
    srcs = ["UpdateTargetTemperatureRequest.kt"],
    visibility = [
        "//java/com/google/android/libraries/automotive/val:__subpackages__",
        "//javatests/com/google/android/libraries/automotive/val:__subpackages__",
    ],
    deps = [
        ":temperature",
    ],
)

android_library(
    name = "offset_request",
    srcs = ["OffsetRequest.kt"],
    visibility = [
        "//java/com/google/android/libraries/automotive/val:__subpackages__",
        "//javatests/com/google/android/libraries/automotive/val:__subpackages__",
    ],
    deps = [
        ":temperature",
    ],
)

android_library(
    name = "error_or",
    srcs = [
        "ErrorOr.java",
    ],
    visibility = [
        "//java/com/google/android/libraries/assistant/auto/tng/carautomation/apiexecution:__subpackages__",
        "//java/com/google/android/libraries/automotive/val:__subpackages__",
        "//java/com/google/cloud/ml/api/cca/sample/fulfillment:__subpackages__",
        "//javatests/com/google/android/libraries/assistant/auto/tng/carautomation/apiexecution:__subpackages__",
        "//javatests/com/google/android/libraries/automotive/val:__subpackages__",
        "//javatests/com/google/cloud/ml/api/cca/sample/fulfillment:__subpackages__",
    ],
    deps = [
        "//third_party/java/android_libs/guava_jdk5:base",
        "//third_party/java/androidx/core",
        "//third_party/java/auto:auto_value",
    ],
)

android_library(
    name = "value_range",
    srcs = ["ValueRange.java"],
    visibility = [
        "//java/com/google/android/libraries/automotive/val:__subpackages__",
        "//javatests/com/google/android/libraries/automotive/val:__subpackages__",
    ],
    deps = [
        "//java/com/google/android/libraries/automotive/val/utils",
        "//third_party/java/android_libs/guava_jdk5:base",
        "//third_party/java/android_libs/guava_jdk5:collect",
        "//third_party/java/auto:auto_value",
    ],
)

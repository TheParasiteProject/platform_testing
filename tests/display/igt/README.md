# IGT GPU Tools for Android Display Testing

## Overview

This directory contains test wrappers for the
[IGT GPU Tools](https://gitlab.freedesktop.org/drm/igt-gpu-tools) test suite.
The original IGT tests are located in `//external/igt-gpu-tools`. These wrappers
facilitate running the IGT test binaries on-device, selecting specific subtests,
and processing the results in a way that is compatible with Android's test
infrastructure (like Tradefed) for automated lab deployment.

## How it Works

IGT tests are composed of multiple subtests. Our wrappers allow us to hand-pick
the subtests relevant for our validation needs. Anyone can extend the wrappers
to include more subtests as required.

For example, the IGT test `kms_color.c` (from `//external/igt-gpu-tools`)
contains the low-level test logic. We create a C++ wrapper, `src/kms_color.cpp`,
where we specify which subtests from `kms_color` we want to execute.

The `Android.bp` file defines a `cc_test` module for each wrapper, such as
`IgtKmsColorTestCases`. To run this test on a device, you can use `atest`:

```sh
atest IgtKmsColorTestCases
```

## Why Run IGT on Android?

From the official IGT website:

> IGT GPU Tools is a collection of tools for development and testing of the DRM
> drivers. There are many macro-level test suites that get used against the
> drivers, including xtest, rendercheck, piglit, and oglconform, but failures
> from those can be difficult to track down to kernel changes, and many require
> complicated build procedures or specific testing environments to get useful
> results. Therefore, IGT GPU Tools includes low-level tools and tests
> specifically for development and testing of the DRM Drivers.

For Android device development, this is crucial. While Android has many
high-level graphics and display tests, failures can be difficult to trace back
to the kernel's DRM (Direct Rendering Manager) driver. IGT provides low-level
tests that directly target the DRM driver, allowing you to:

*   Verify the correctness of your display kernel driver implementation.
*   Pinpoint regressions caused by kernel changes with high precision.
*   Test specific hardware features like color management, atomic modesetting,
    plane scaling, and more, in isolation.

By running IGT tests, you can ensure the stability and correctness of the
foundational display stack on your device.

## Automatic VKMS Setup on AVD

The test configuration template (`igt_config_template.xml`) includes a special
feature for Android Virtual Devices (AVDs). When these tests run on an AVD, they
automatically:

1.  Enable the VKMS (Virtual Kernel Mode Setting) driver.
2.  Configure the virtual hardware with an eDP, a DisplayPort, and an HDMI
    connector.

This significantly increases test coverage on virtual devices without any manual
setup.

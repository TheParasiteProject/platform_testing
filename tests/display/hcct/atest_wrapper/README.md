# HCCT Atest Wrapper for VKMS

## Overview

This directory contains a novel way of using VKMS (Virtual Kernel Mode Setting)
to enhance testing. It allows any existing test in the Android tree to run on an
AVD with multiple simulated displays, without requiring any code changes to the
original test. The wrappers provide all the necessary building blocks to wrap a
test module. The wrapper takes care of creating the simulated connected displays
before the test runs, making the test believe it is running on a device with
multiple physical displays, thus expanding test coverage.

For instance, an AVD can be configured with 5 virtual displays before running
the VTS HWC test, ensuring the test validates HWC behavior on a multi-display
setup.

## How to Wrap Your Test

To demonstrate how to wrap an existing test, we will use
`VtsHalGraphicsComposer3_TargetTest` as an example. The steps to wrap your own
test will be very similar.

### 1. Prerequisite: Ensure your test can run with `atest`

```sh
atest VtsHalGraphicsComposer3_TargetTest
```

### 2. Create a directory for the wrapper

Create a new directory to house the `Android.bp` and `AndroidTest.xml` for your
wrapped test. For our example, we created:
`platform_testing/tests/display/hcct/atest_wrapper/Vkms5WithConfig_VtsHalGraphicsComposer3/`

### 3. Create an `Android.bp` file

This file defines your new test target.

```bp
// In platform_testing/tests/display/hcct/atest_wrapper/Vkms5WithConfig_VtsHalGraphicsComposer3/Android.bp

cc_test {
    name: "Vkms5WithConfig_VtsHalGraphicsComposer3",
    defaults: ["atester_wrapper_defaults"],
    data_bins: [
        "//hardware/interfaces/graphics/composer/aidl/vts:VtsHalGraphicsComposer3_TargetTest",
    ],
    // ...
}
```

Key parts:
* `name`: Give your test a unique, descriptive name. Here, it
indicates that `VtsHalGraphicsComposer3` is run with a 5-display VKMS
configuration.
* `defaults: ["atester_wrapper_defaults"]`: This default is
crucial. It pulls in the `setup_vkms_connectors_for_atest` and `teardown_vkms`
binaries needed to configure the hardware.
* `data_bins`: This property packages
the original test binary (`VtsHalGraphicsComposer3_TargetTest` in this case)
with your wrapper test.

### 4. Create an `AndroidTest.xml` file

This Tradefed configuration file orchestrates the test execution: setting up the
environment, running the test, and tearing it down. Below is a breakdown of the
important sections from the example `AndroidTest.xml`.

#### Pushing required files

The `FilePusher` pushes the test binary and our VKMS helper binaries to the
device.

```xml
<target_preparer class="com.android.compatibility.common.tradefed.targetprep.FilePusher">
    <!-- Avoids needing to add '64' suffix for 64-bit binaries -->
    <option name="append-bitness" value="false"/>
    <!-- The actual test we want to run -->
    <option name="push-file" key="VtsHalGraphicsComposer3_TargetTest" value="/data/local/tmp/VtsHalGraphicsComposer3_TargetTest"/>
    <!-- The binary that sets up the HW configuration using VKMS -->
    <option name="push-file" key="setup_vkms_connectors_for_atest" value="/data/local/tmp/setup_vkms_connectors_for_atest"/>
    <!-- The binary that cleans up and resets the HW configuration -->
    <option name="push-file" key="teardown_vkms" value="/data/local/tmp/teardown_vkms"/>
</target_preparer>
```

#### Setting up and tearing down the environment

The `RunCommandTargetPreparer` executes shell commands on the device to prepare
the test environment and clean up afterwards.

```xml
<target_preparer class="com.android.tradefed.targetprep.RunCommandTargetPreparer">
    <!-- Stop framework and HWC service to allow VKMS to take over -->
    <option name="run-command" value="stop"/>
    <option name="run-command" value="stop vendor.hwcomposer-3"/>

    <!-- Configure the hardware with 5 specific displays using VKMS. -->
    <!-- See "setup_vkms_connectors_for_atest Usage" section below for more options. -->
    <option name="run-command" value="/data/local/tmp/setup_vkms_connectors_for_atest --config eDP,0,REDRIX DP,1,HP_Spectre32_4K_DP HDMIA,2,ACI_9155_ASUS_VH238_HDMI HDMIA,3,HWP_12447_HP_Z24i_HDMI DP,4,DEL_61463_DELL_U2410_DP"/>

    <!-- After the test, reset the environment and restart services. -->
    <option name="teardown-command" value="/data/local/tmp/teardown_vkms"/>
    <option name="teardown-command" value="start vendor.hwcomposer-3"/>
    <option name="teardown-command" value="start"/>
</target_preparer>
```

#### Running the test

The `GTest` test runner executes the original test binary.

```xml
<test class="com.android.tradefed.testtype.GTest" >
    <!-- Must match the directory used in FilePusher -->
    <option name="native-test-device-path" value="/data/local/tmp" />
    <!-- The name of the original test module -->
    <option name="module-name" value="VtsHalGraphicsComposer3_TargetTest" />
    <!-- It's good practice to use the same timeout as the original test -->
    <option name="native-test-timeout" value="900000"/>
</test>
```

## `setup_vkms_connectors_for_atest` Usage

The `setup_vkms_connectors_for_atest` binary provides a flexible way to
configure the virtual displays.

**Simple mode:** Create N generic connectors.
```sh
# Creates 3 generic virtual connectors
/data/local/tmp/setup_vkms_connectors_for_atest 3
```

**Advanced mode:** Create connectors with specific types, plane counts, and EDID
profiles.

`/data/local/tmp/setup_vkms_connectors_for_atest --config
TYPE,NUM_PLANES[,EDID_NAME] ...`
* `TYPE`: Connector type (e.g., `DP`, `HDMIA`,
`eDP`).
* `NUM_PLANES`: Number of additional overlay planes (integer).
* `EDID_NAME`: (Optional) An EDID profile name from `edid_helper.h`.

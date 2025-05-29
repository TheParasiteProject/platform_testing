/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tests;

import com.android.tradefed.config.Option;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.log.LogUtil;
import com.android.tradefed.metrics.proto.MetricMeasurement.Directionality;
import com.android.tradefed.metrics.proto.MetricMeasurement.Measurements;
import com.android.tradefed.metrics.proto.MetricMeasurement.Metric;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner.TestMetrics;
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Test to reboot device for 10 times and check the shutdown time in pstore files. */
@RunWith(DeviceJUnit4ClassRunner.class)
public class ShutdownPerfTest extends BaseHostJUnit4Test {
    private ITestDevice mDevice;

    /** Metrics to collect. */
    private Long[] mMilliSecondsShutdownTime;

    /** Number of reboots. */
    @Option(name = "iterations", description = "Number of times to reboot")
    private int mIterations = 10;

    @Rule public TestMetrics metrics = new TestMetrics();

    @Before
    public void setUp() throws Exception {
        mDevice = getDevice();
        mMilliSecondsShutdownTime = new Long[mIterations];
    }

    // possible kernel console output paths to check
    private static final List<String> sPstorePaths =
            Arrays.asList("/sys/fs/pstore/console-ramoops-0", "/sys/fs/pstore/console-ramoops");

    @Test
    public void shutdownPerf() throws Exception {
        LogUtil.CLog.i("Number of iterations: " + mIterations);

        if (!sysfsPstoreConsoleExists()) {
            LogUtil.CLog.w("Skip test: pstore console log is not available");
            return;
        }

        // Reboot mIteration times to get the shutdown time in pstore files.
        for (int i = 0; i < mIterations; i++) {
            mDevice.reboot("Reboot to check logs in pstore");
            mDevice.enableAdbRoot();
            mMilliSecondsShutdownTime[i] = tryGetShutdownTime();
        }

        // Report the shutdown time that has value.
        for (int i = 0; i < mIterations; i++) {
            if (mMilliSecondsShutdownTime[i] != null) {
                long iShutdownTime = mMilliSecondsShutdownTime[i].longValue();
                metrics.addTestMetric(
                        "reboot_shutdown_time" + "_" + i, createMetric(iShutdownTime));
            }
        }
    }

    private boolean sysfsPstoreConsoleExists() throws DeviceNotAvailableException {
        for (String pathString : sPstorePaths) {
            // Check if the current file exists
            if (mDevice.doesFileExist(pathString)) {
                return true;
            }
        }
        return false;
    }

    private Metric createMetric(long value) {
        return Metric.newBuilder()
                .setMeasurements(Measurements.newBuilder().setSingleInt(value))
                .setDirection(Directionality.DOWN_BETTER)
                .setUnit("ms")
                .build();
    }

    private static Long extractShutdownTimeFromFile(File logFile) {
        Long shutdownTime = null;
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            Pattern pattern = Pattern.compile("powerctl_shutdown_time_ms:(\\d+)");
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String shutdownTimeStr = matcher.group(1);
                    try {
                        shutdownTime = Long.parseLong(shutdownTimeStr);
                        break; // Stop once shutdown time found.
                    } catch (NumberFormatException e) {
                        LogUtil.CLog.e("Error parsing shutdown time: " + e.getMessage());
                        return null;
                    }
                }
            }
        } catch (IOException e) {
            LogUtil.CLog.w("Error reading log file: " + e.getMessage());
            return null;
        }

        return shutdownTime;
    }

    // Try to find shutdown time log in pstore files.
    // Return the shutdown time(ms). If not found, return null.
    private Long tryGetShutdownTime() throws Exception {
        for (String pathString : sPstorePaths) {
            // Check if the current file exists
            if (mDevice.doesFileExist(pathString)) {
                File pstoreFile = mDevice.pullFile(pathString);
                if (pstoreFile != null) {
                    return extractShutdownTimeFromFile(pstoreFile);
                }
            }
        }
        return null;
    }
}

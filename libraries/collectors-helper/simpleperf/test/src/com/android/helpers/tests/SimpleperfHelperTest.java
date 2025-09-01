/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.helpers.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;

import com.android.helpers.SimpleperfHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Android Unit tests for {@link SimpleperfHelper}.
 *
 * <p>atest CollectorsHelperTest:com.android.helpers.tests.SimpleperfHelperTest
 */
@RunWith(AndroidJUnit4.class)
public class SimpleperfHelperTest {

    private static final String REMOVE_CMD = "rm %s";
    private static final String FILE_SIZE_IN_BYTES = "wc -c %s";
    private static final String DEFAULT_SUBCOMMAND = "simpleperf record";
    private static final String DEFAULT_ARGUMENTS = "-g --post-unwind=yes -f 500 -a --exclude-perf";
    private static final String PERF_DATA_PATH = "/data/local/tmp/simpleperf_profiling.data";
    private static final String PERF_REPORT_PATH = "/data/local/tmp/simpleperf_report.data";
    private static final long WAIT_TIME_MS = 1000;

    private SimpleperfHelper simpleperfHelper;

    @Before
    public void setUp() {
        simpleperfHelper = new SimpleperfHelper();
    }

    @After
    public void teardown() throws IOException {
        simpleperfHelper.stopCollecting(PERF_DATA_PATH);
        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        uiDevice.executeShellCommand(String.format(REMOVE_CMD, PERF_DATA_PATH));
        uiDevice.executeShellCommand(String.format(REMOVE_CMD, PERF_REPORT_PATH));
    }

    /** Test simpleperf collection starts collecting properly. */
    @Test
    public void testSimpleperfStartSuccess() throws Exception {
        List<String> commandArgsList = new ArrayList<>();
        commandArgsList.add(DEFAULT_SUBCOMMAND);
        commandArgsList.add("-o");
        commandArgsList.add(PERF_DATA_PATH);
        commandArgsList.add(DEFAULT_ARGUMENTS);
        assertTrue(simpleperfHelper.startCollecting(commandArgsList));
    }

    /** Test if the path name is prefixed with /. */
    @Test
    public void testSimpleperfValidOutputPath() throws Exception {
        List<String> commandArgsList = new ArrayList<>();
        commandArgsList.add(DEFAULT_SUBCOMMAND);
        commandArgsList.add("-o");
        commandArgsList.add(PERF_DATA_PATH);
        commandArgsList.add(DEFAULT_ARGUMENTS);
        assertTrue(simpleperfHelper.startCollecting(commandArgsList));
        assertTrue(simpleperfHelper.stopCollecting(PERF_DATA_PATH));
    }

    /** Test the invalid output path. */
    @Test
    public void testSimpleperfInvalidOutputPath() throws Exception {
        List<String> commandArgsList = new ArrayList<>();
        commandArgsList.add(DEFAULT_SUBCOMMAND);
        commandArgsList.add("-o");
        commandArgsList.add(PERF_DATA_PATH);
        commandArgsList.add(DEFAULT_ARGUMENTS);
        assertTrue(simpleperfHelper.startCollecting(commandArgsList));
        // Don't have permission to create new folder under /data
        assertFalse(simpleperfHelper.stopCollecting("/data/dummy/xyz/perf.data"));
    }

    /** Test simpleperf collection returns true and output file size greater than zero */
    @Test
    public void testSimpleperfSuccess() throws Exception {
        List<String> commandArgsList = new ArrayList<>();
        commandArgsList.add(DEFAULT_SUBCOMMAND);
        commandArgsList.add("-o");
        commandArgsList.add(PERF_DATA_PATH);
        commandArgsList.add(DEFAULT_ARGUMENTS);
        assertTrue(simpleperfHelper.startCollecting(commandArgsList));
        Thread.sleep(WAIT_TIME_MS);
        assertTrue(simpleperfHelper.stopCollecting(PERF_DATA_PATH));
        Thread.sleep(WAIT_TIME_MS);
        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        String[] fileStats =
                uiDevice.executeShellCommand(String.format(FILE_SIZE_IN_BYTES, PERF_DATA_PATH))
                        .split(" ");
        int fileSize = Integer.parseInt(fileStats[0].trim());
        assertTrue(fileSize > 0);
    }

    /** Test simpleperf report returns true and output file size greater than zero */
    @Test
    public void testSimpleperfReportSuccess() throws Exception {
        List<String> commandArgsList = new ArrayList<>();
        commandArgsList.add(DEFAULT_SUBCOMMAND);
        commandArgsList.add("-o");
        commandArgsList.add(PERF_DATA_PATH);
        commandArgsList.add(DEFAULT_ARGUMENTS);
        assertTrue(simpleperfHelper.startCollecting(commandArgsList));
        Thread.sleep(WAIT_TIME_MS);
        assertTrue(simpleperfHelper.stopCollecting(PERF_DATA_PATH));
        Thread.sleep(WAIT_TIME_MS);
        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        String[] fileStats =
                uiDevice.executeShellCommand(String.format(FILE_SIZE_IN_BYTES, PERF_DATA_PATH))
                        .split(" ");
        int fileSize = Integer.parseInt(fileStats[0].trim());
        assertTrue(fileSize > 0);

        assertTrue(
                simpleperfHelper.getSimpleperfReport(
                        PERF_DATA_PATH, PERF_REPORT_PATH, new HashMap<>()));
        String[] reportStats =
                uiDevice.executeShellCommand(String.format(FILE_SIZE_IN_BYTES, PERF_REPORT_PATH))
                        .split(" ");
        int reportFileSize = Integer.parseInt(reportStats[0].trim());
        assertTrue(reportFileSize > 0);
    }

    /** Test getMetrics with a invalid report file. */
    @Test
    public void testSimpleperfMetricsFailure() throws Exception {
        // Prepare arguments for getMetrics
        Set<String> processSet = new HashSet<>();
        processSet.add("system_server"); // Example process
        Map<String, String> symbols = new HashMap<>();
        symbols.put("art", "art"); // Example symbol
        int testIterations = 1;

        Map<String, String> metrics = new HashMap<>();
        for (String process : processSet) {
            metrics.putAll(simpleperfHelper.getMetrics(process, symbols, testIterations));
        }
        assertEquals(metrics, new HashMap<>());
    }

    /** Test getMetrics with a valid report file. */
    @Test
    public void testSimpleperfMetricsSuccess() throws Exception {
        List<String> commandArgsList = new ArrayList<>();
        commandArgsList.add(DEFAULT_SUBCOMMAND);
        commandArgsList.add("-o");
        commandArgsList.add(PERF_DATA_PATH);
        commandArgsList.add(DEFAULT_ARGUMENTS);
        assertTrue(simpleperfHelper.startCollecting(commandArgsList));
        Thread.sleep(WAIT_TIME_MS);
        assertTrue(simpleperfHelper.stopCollecting(PERF_DATA_PATH));
        Thread.sleep(WAIT_TIME_MS);
        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        String[] fileStats =
                uiDevice.executeShellCommand(String.format(FILE_SIZE_IN_BYTES, PERF_DATA_PATH))
                        .split(" ");
        int fileSize = Integer.parseInt(fileStats[0].trim());
        assertTrue(fileSize > 0);

        assertTrue(
                simpleperfHelper.getSimpleperfReport(
                        PERF_DATA_PATH, PERF_REPORT_PATH, new HashMap<>()));
        String[] reportStats =
                uiDevice.executeShellCommand(String.format(FILE_SIZE_IN_BYTES, PERF_REPORT_PATH))
                        .split(" ");
        int reportFileSize = Integer.parseInt(reportStats[0].trim());
        assertTrue(reportFileSize > 0);

        // Prepare arguments for getMetrics
        Set<String> processSet = new HashSet<>();
        processSet.add("system_server"); // Example process
        Map<String, String> symbols = new HashMap<>();
        symbols.put("art", "art"); // Example symbol
        int testIterations = 1;

        Map<String, String> metrics = new HashMap<>();
        for (String process : processSet) {
            metrics.putAll(simpleperfHelper.getMetrics(process, symbols, testIterations));
        }
        assertNotNull(metrics);
    }
}

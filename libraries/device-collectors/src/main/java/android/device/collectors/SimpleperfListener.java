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
package android.device.collectors;

import android.device.collectors.annotations.OptionClass;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.helpers.SimpleperfHelper;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A {@link SimpleperfListener} that captures simpleperf samples for a test run or per test method
 * run and saves the results under
 * <root_folder>/<test_display_name>/SimpleperfListener/<test_display_name>-<invocation_count>.perf
 */
@OptionClass(alias = "simpleperf-collector")
public class SimpleperfListener extends BaseMetricListener {

    // Default output folder to store the simpleperf sample files.
    private static final String DEFAULT_OUTPUT_ROOT = "/sdcard/test_results";
    // Default arguments passed to simpleperf command
    private static final String DEFAULT_ARGUMENTS =
            "-g --post-unwind=yes -f 500 --exclude-perf --log-to-android-buffer";
    // Destination directory to save the trace results.
    private static final String TEST_OUTPUT_ROOT = "test_output_root";
    // Simpleperf profiling file path key from simpleperf record.
    private static final String SIMPLEPERF_PROFILING_FILE_PATH_KEY =
            "simpleperf_profiling_file_path";
    // Simpleperf report file path key from simpleperf report.
    private static final String SIMPLEPERF_REPORT_FILE_PATH_KEY = "simpleperf_report_file_path";
    // Argument determining whether we collect for the entire run, or per test.
    public static final String COLLECT_PER_RUN = "per_run";
    public static final String SIMPLEPERF_PREFIX = "simpleperf_";
    // Skip failure metrics collection if set to true.
    public static final String SKIP_TEST_FAILURE_METRICS = "skip_test_failure_metrics";
    // Arguments to pass to simpleperf on start.
    public static final String ADDITIONAL_ARGUMENTS = "additional_arguments";
    // Arguments to record specific processes separated by commas and spaces are ignored.
    // Ex. "surfaceflinger,system_server"
    public static final String PROCESSES = "processes_to_record";
    // Arguments to record specific events separated by commas and spaces are ignored.
    // Ex. "instructions,cpu-cycles"
    public static final String EVENTS = "events_to_record";
    // Argument to determine if report is generated after recording.
    public static final String REPORT = "generate_simpleperf_report";
    // Argument to determine if report is generated after recording.
    public static final String EXTRACT_METRICS = "extract_simpleperf_metrics";
    // Report events per symbol. Symbols are separated by the key to identify the symbol and the
    // substring used to search for the symbol.
    // Ex. "writeInt32;android::Parcel::writeInt32(;commit;android::SurfaceFlinger::commit("
    // symbols matching the substring "android::Parcel::writeInt32(" will be reported as
    // "writeInt32" and
    // symbols matching "android::SurfaceFlinger::commit(" will be reported as "commit"
    public static final String REPORT_SYMBOLS = "symbols_to_report";
    // Test iterations used to divide any reported event counts.
    public static final String TEST_ITERATIONS = "test_iterations";
    // Simpleperf samples collected during the test will be saved under this root folder.

    private String mTestOutputRoot;
    // Store the method name and invocation count to create a unique filename for each trace.
    private Map<String, Integer> mTestIdInvocationCount = new HashMap<>();
    private Map<String, String> mSymbolToMetricKey = new HashMap<>();
    private Map<String, String> mProcessToPid = new HashMap<>();
    private boolean mIsTestFailed = false;
    private boolean mSimpleperfStartSuccess = false;
    private boolean mSkipTestFailureMetrics;
    private boolean mIsCollectPerRun;
    private boolean mExtractMetrics;
    private boolean mReport;
    private String mArguments;
    private String mEvents;
    private String mProcessesToRecord;
    private int mTestIterations;

    private SimpleperfHelper mSimpleperfHelper = new SimpleperfHelper();

    public SimpleperfListener() {
        super();
    }

    /**
     * Constructor to simulate receiving the instrumentation arguments. Should not be used except
     * for testing.
     */
    @VisibleForTesting
    SimpleperfListener(Bundle args, SimpleperfHelper helper, Map invocationMap) {
        super(args);
        mSimpleperfHelper = helper;
        mTestIdInvocationCount = invocationMap;
    }

    /** Process the test arguments */
    @Override
    public void setupAdditionalArgs() {
        Bundle args = getArgsBundle();
        // Whether to collect for the entire run, or per test. By default, collect per test.
        mIsCollectPerRun =
                Boolean.parseBoolean(args.getString(COLLECT_PER_RUN, String.valueOf(false)));
        // Destination folder in the device to save all simpleperf sample files.
        // Defaulted to /sdcard/test_results if test_output_root is not passed.
        mTestOutputRoot = args.getString(TEST_OUTPUT_ROOT, DEFAULT_OUTPUT_ROOT);
        // By default this flag is set to false to collect metrics on test failure.
        mSkipTestFailureMetrics =
                Boolean.parseBoolean(
                        args.getString(SKIP_TEST_FAILURE_METRICS, String.valueOf(false)));
        // Whether to generate report after recording or not, by default set to true.
        mReport = Boolean.parseBoolean(args.getString(REPORT, String.valueOf(true)));
        // Whether to extract metrics from the report after recording or not, by default set to
        // false.
        mExtractMetrics =
                Boolean.parseBoolean(args.getString(EXTRACT_METRICS, String.valueOf(false)));
        // Command arguments passed to simpleperf.
        mArguments = args.getString(ADDITIONAL_ARGUMENTS, DEFAULT_ARGUMENTS);
        mTestIterations = Integer.parseInt(args.getString(TEST_ITERATIONS, "1"));
        // Symbols to look for when reporting events for processes.
        String[] symbolAndMetricKey = args.getString(REPORT_SYMBOLS, "").trim().split("\\s*;\\s*");
        for (int i = 0; i < symbolAndMetricKey.length - 1; i += 2) {
            mSymbolToMetricKey.put(symbolAndMetricKey[i + 1], symbolAndMetricKey[i]);
        }
        mEvents = args.getString(EVENTS, "");
        mProcessesToRecord = args.getString(PROCESSES, "");
    }

    @Override
    public void onTestRunStart(DataRecord runData, Description description) {
        if (mIsCollectPerRun) {
            Log.i(getTag(), "onTestRunStart");
            startSimpleperf(getRecordCommandArgsList());
        }
    }

    @Override
    public void onTestStart(DataRecord testData, Description description) {
        if (mIsCollectPerRun) {
            return;
        }
        Log.i(getTag(), "onTestStart");
        mTestIdInvocationCount.compute(
                getTestFileName(description), (key, value) -> (value == null) ? 1 : value + 1);
        startSimpleperf(getRecordCommandArgsList());
    }

    @Override
    public void onTestFail(DataRecord testData, Description description, Failure failure) {
        Log.i(getTag(), "onTestFail");
        mIsTestFailed = true;
    }

    @Override
    public void onTestEnd(DataRecord testData, Description description) {
        if (mIsCollectPerRun) {
            return;
        }
        Log.i(getTag(), "onTestEnd");
        if (!mSimpleperfStartSuccess) {
            Log.i(
                    getTag(),
                    "Skipping simpleperf stop attempt onTestEnd because simpleperf did not start"
                            + " successfully");
            return;
        }
        if (mSkipTestFailureMetrics && mIsTestFailed) {
            // Stop the existing simpleperf session and don't log the file path.
            Log.i(getTag(), "Skipping metric collection due to test failure");
            stopSimpleperf(null);
            return;
        }

        Log.i(getTag(), "Stopping simpleperf after test ended.");
        // Construct simpleperf record output directory in the below format
        // <root_folder>/<test_name>/SimpleperfListener/simplerperf_profiling_<test_name>-<count>.data
        Path profilingPath =
                Paths.get(
                        mTestOutputRoot,
                        getTestFileName(description),
                        this.getClass().getSimpleName(),
                        String.format(
                                "%s%s%s-%d.data",
                                SIMPLEPERF_PREFIX,
                                "profiling_",
                                getTestFileName(description),
                                mTestIdInvocationCount.get(getTestFileName(description))));
        if (stopSimpleperf(profilingPath)) {
            logSimpleperfFilePath(SIMPLEPERF_PROFILING_FILE_PATH_KEY, profilingPath, testData);
        }
        if (mReport) {
            // Construct simpleperf report output directory in the below format
            // <root_folder>/<test_name>/SimpleperfListener/simplerperf_report_<test_name>-<count>.txt
            Path reportPath =
                    Paths.get(
                            mTestOutputRoot,
                            getTestFileName(description),
                            this.getClass().getSimpleName(),
                            String.format(
                                    "%s%s%s-%d.txt",
                                    SIMPLEPERF_PREFIX,
                                    "report_",
                                    getTestFileName(description),
                                    mTestIdInvocationCount.get(getTestFileName(description))));
            if (getSimpleperfReport(profilingPath, reportPath)) {
                logSimpleperfFilePath(SIMPLEPERF_REPORT_FILE_PATH_KEY, reportPath, testData);
                if (mExtractMetrics) {
                    Map<String, String> metrics = extractReportToMetrics();
                    for (Map.Entry<String /*event-process-symbol*/, String /*eventCount*/> metric :
                            metrics.entrySet()) {
                        testData.addStringMetric(metric.getKey(), metric.getValue());
                    }
                }
            }
        }
    }

    @Override
    public void onTestRunEnd(DataRecord runData, Result result) {
        if (!mIsCollectPerRun) {
            return;
        }
        if (!mSimpleperfStartSuccess) {
            Log.i(getTag(), "Skipping simpleperf stop attempt as simpleperf failed to start.");
            return;
        }

        Log.i(getTag(), "onTestRunEnd");
        String uniqueId = Integer.toString(UUID.randomUUID().hashCode());
        Path profilingPath =
                Paths.get(
                        mTestOutputRoot,
                        this.getClass().getSimpleName(),
                        String.format("%s%s%s.data", SIMPLEPERF_PREFIX, "profiling_", uniqueId));
        if (stopSimpleperf(profilingPath)) {
            logSimpleperfFilePath(SIMPLEPERF_PROFILING_FILE_PATH_KEY, profilingPath, runData);
        }
        if (mReport) {
            Path reportPath =
                    Paths.get(
                            mTestOutputRoot,
                            this.getClass().getSimpleName(),
                            String.format("%s%s%s.txt", SIMPLEPERF_PREFIX, "report_", uniqueId));
            if (getSimpleperfReport(profilingPath, reportPath)) {
                logSimpleperfFilePath(SIMPLEPERF_REPORT_FILE_PATH_KEY, reportPath, runData);
                if (mExtractMetrics) {
                    Map<String, String> metrics = extractReportToMetrics();
                    for (Map.Entry<String /*event-process-symbol*/, String /*eventCount*/> metric :
                            metrics.entrySet()) {
                        runData.addStringMetric(metric.getKey(), metric.getValue());
                    }
                }
            }
        }
    }

    /** Start simpleperf sampling. */
    public void startSimpleperf(List<String> commandArgsList) {
        Log.i(getTag(), "startSimpleperf");
        mSimpleperfStartSuccess = mSimpleperfHelper.startCollecting(commandArgsList);
        if (!mSimpleperfStartSuccess) {
            Log.e(getTag(), "Simpleperf did not start successfully.");
        }
    }

    /** Stop simpleperf sampling. */
    public boolean stopSimpleperf(Path profilingPath) {
        Log.i(getTag(), "stopSimpleperf");
        return mSimpleperfHelper.stopCollecting(
                profilingPath == null ? "" : profilingPath.toString());
    }

    /** Dump the collected file into the given path. */
    private void logSimpleperfFilePath(
            String hostPathKey, Path devicePathValue, DataRecord record) {
        record.addStringMetric(hostPathKey, devicePathValue.toString());
    }

    /**
     * Generate simpleperf report from an existing record file then add parsed metrics to
     * DataRecord.
     *
     * @param profilingPath Path to read binary record from.
     * @param reportPath Path to write report to.
     * @param data DataRecord to store metrics parsed from report
     * @return String containing the simpleperf report file name.
     */
    private boolean getSimpleperfReport(Path profilingPath, Path reportPath) {
        if (profilingPath == null || reportPath == null) {
            Log.e(
                    getTag(),
                    "Invalid source or destination file path: " + profilingPath + " " + reportPath);
            return false;
        }
        return mSimpleperfHelper.getSimpleperfReport(
                profilingPath.toString(), reportPath.toString(), mProcessToPid);
    }

    private Map<String, String> extractReportToMetrics() {
        Map<String, String> metrics = new HashMap<>();
        for (String process : mProcessToPid.keySet()) {
            metrics.putAll(
                    mSimpleperfHelper.getMetrics(process, mSymbolToMetricKey, mTestIterations));
        }
        return metrics;
    }

    /**
     * Returns the packagename.classname_methodname which has no special characters and is used to
     * create file names.
     */
    public String getTestFileName(Description description) {
        return String.format("%s_%s", description.getClassName(), description.getMethodName());
    }

    public List<String> getRecordCommandArgsList() {
        List<String> commandArgsList = new ArrayList<String>();
        commandArgsList.add("simpleperf record");
        commandArgsList.add("-o");
        commandArgsList.add(mSimpleperfHelper.SIMPLEPERF_PROFILING_TMP_FILE_PATH);
        commandArgsList.add(mArguments);

        // Appending recording argument for recording specified events if given.
        String[] individualEvents = mEvents.trim().split("\\s*,\\s*");
        if (!mEvents.isEmpty()) {
            commandArgsList.add("-e");
            commandArgsList.add(String.join(",", individualEvents));
        }

        // Processes passed into recording arguments for simpleperf.
        // Appending recording argument for recording specified processes if given.
        String[] individualProcesses = mProcessesToRecord.trim().split("\\s*,\\s*");
        if (mProcessesToRecord.trim().isEmpty()) {
            // Record system wide.
            commandArgsList.add("-a");
        } else {
            commandArgsList.add("-p");
            if (individualProcesses.length == 1) {
                commandArgsList.add(individualProcesses[0]);
            } else {
                for (String process : individualProcesses) {
                    String pid = mSimpleperfHelper.getPID(process);
                    if (pid != null && !pid.isEmpty()) {
                        mProcessToPid.put(process, pid);
                    }
                }
                if (!mProcessToPid.isEmpty()) {
                    commandArgsList.add(String.join(",", mProcessToPid.values()));
                }
            }
        }
        return commandArgsList;
    }
}

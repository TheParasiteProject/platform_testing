/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.app.Instrumentation;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.helpers.MetricUtility;
import com.android.helpers.PerfettoHelper;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A base {@link PerfettoTracingStrategy} that allows capturing traces during testing and save the
 * perfetto trace files under <root>/<test_name>/PerfettoTracingStrategy/
 */
public abstract class PerfettoTracingStrategy {
    // Option to pass the folder name which contains the perfetto trace config file.
    private static final String PERFETTO_CONFIG_ROOT_DIR_ARG = "perfetto_config_root_dir";
    // Default folder name to look for the perfetto config file.
    // Argument to indicate the perfetto output file prefix
    public static final String PERFETTO_CONFIG_OUTPUT_FILE_PREFIX =
            "perfetto_config_output_file_prefix";
    public static final String PERFETTO_PID_TRACK_ROOT = "perfetto_pid_track_root";
    // Enable to persist the pid of perfetto process during test execution and use it
    // for cleanup during instrumentation crash instances.
    private static final String PERFETTO_PERSIST_PID_TRACK = "perfetto_persist_pid_track";
    private static final String DEFAULT_PERFETTO_PID_TRACK_ROOT = "sdcard/";
    private static final String DEFAULT_PERFETTO_CONFIG_ROOT_DIR = "/data/misc/perfetto-traces/";
    // Collect per run if it is set to true otherwise collect per test.
    // Default perfetto config file name.
    private static final String DEFAULT_CONFIG_FILE = "trace_config.pb";
    // Default perfetto config file name when text proto config is used.
    private static final String DEFAULT_TEXT_CONFIG_FILE = "trace_config.textproto";
    // Argument to get custom config file name for collecting the trace.
    private static final String PERFETTO_CONFIG_FILE_ARG = "perfetto_config_file";
    // Skip failure metrics collection if this flag is set to true.
    public static final String SKIP_TEST_FAILURE_METRICS = "skip_test_failure_metrics";
    // Skip success metrics collection if this flag is set to true (i.e. collect only failures).
    public static final String SKIP_TEST_SUCCESS_METRICS = "skip_test_success_metrics";
    // Skip the empty metrics storing if this flag is set to true (i.e. if the perfetto trace file
    // is empty, then do not store the empty file).
    public static final String SKIP_EMPTY_METRICS = "skip_empty_metrics";
    // List of test iterations to capture, capture all if empty
    public static final String ARGUMENT_ALLOW_ITERATIONS = "allow_iterations";
    // Perfetto file path key argument name
    public static final String ARGUMENT_FILE_PATH_KEY_PREFIX = "perfetto_file_path_key_prefix";
    // Perfetto file path key prefix
    protected static final String DEFAULT_FILE_PATH_KEY_PREFIX = "perfetto_file_path";
    // Argument to get custom time in millisecs to wait before dumping the trace.
    // This has to be at least the dump interval time set in the trace config file
    // or greater than that. Otherwise, we will miss trace information from the test.
    private static final String PERFETTO_WAIT_TIME_ARG = "perfetto_wait_time_ms";
    // Argument to indicate the perfetto config file is text proto file.
    public static final String PERFETTO_CONFIG_TEXT_PROTO = "perfetto_config_text_proto";
    // Argument to indicate the perfetto config content in a textual format
    public static final String PERFETTO_CONFIG_TEXT_CONTENT = "perfetto_config_text_content";
    // Destination directory to save the trace results.
    private static final String TEST_OUTPUT_ROOT = "test_output_root";
    // Default output folder to store the perfetto output traces.
    private static final String DEFAULT_OUTPUT_ROOT = "/data/local/tmp/perfetto-traces";
    // Default wait time before stopping the perfetto trace.
    private static final String DEFAULT_WAIT_TIME_MSECS = "0";
    // Argument to get custom time in millisecs to wait before starting the
    // perfetto trace.
    public static final String PERFETTO_START_WAIT_TIME_ARG = "perfetto_start_wait_time_ms";
    // Default prefix for the output file
    public static final String DEFAULT_PERFETTO_PREFIX = "perfetto_";
    // Default wait time before starting the perfetto trace.
    public static final String DEFAULT_START_WAIT_TIME_MSECS = "0";
    // Regular expression pattern to identify multiple spaces.
    public static final String SPACES_PATTERN = "\\s+";
    // Space replacement value
    public static final String REPLACEMENT_CHAR = "#";
    // Separator character used to specify custom arguments for the strategy
    public static final String STRATEGY_ARGUMENT_NAMESPACE_SEPARATOR = "#";
    // For USB disconnected cases you may want this option to be true. This option makes sure
    // the device does not go to sleep while collecting.
    public static final String PERFETTO_START_BG_WAIT = "perfetto_start_bg_wait";

    @VisibleForTesting
    static final String HOLD_WAKELOCK_WHILE_COLLECTING = "hold_wakelock_while_collecting";

    private boolean mHoldWakelockWhileCollecting;

    private final WakeLockContext mWakeLockContext;
    private final Supplier<PowerManager.WakeLock> mWakelockSupplier;
    private final WakeLockAcquirer mWakeLockAcquirer;
    private final WakeLockReleaser mWakeLockReleaser;
    private final Instrumentation mInstr;
    private final String mIdentifier;
    private final PerfettoHelper mPerfettoHelper;
    // Wait time can be customized based on the dump interval set in the trace config.
    private long mWaitTimeInMs;
    // Wait time can be customized based on how much time to wait before starting the
    // trace.
    private long mWaitStartTimeInMs;
    // Trace config file name to use while collecting the trace which is defaulted to
    // trace_config.pb. It can be changed via the perfetto_config_file arg.
    private String mConfigFileName;
    // Perfetto traces collected during the test will be saved under this root folder.
    private String mTestOutputRoot;
    private boolean mIsConfigTextProto = false;
    private String mConfigContent;
    protected boolean mSkipTestFailureMetrics;
    private boolean mSkipTestSuccessMetrics;
    private boolean mSkipEmptyMetrics;
    protected boolean mIsTestFailed = false;
    // Store the method name and invocation count to create unique file name for each trace.
    private boolean mPerfettoStartSuccess = false;
    private String mFilePathKeyPrefix = DEFAULT_FILE_PATH_KEY_PREFIX;
    private String mOutputFilePrefix;
    private String mTrackPerfettoProcIdRootDir;
    private final Set<Integer> mAllowedIterations = new HashSet<>();

    /**
     * @param instr Android instrumentation object
     * @param identifier Unique strategy identifier, used for strategy specific configuration
     *                   parameters and to track multiple parallel strategies running at the
     *                   same time. For example, to pass Perfetto configuration to a specific
     *                   strategy we could pass "[strategy_identifier]:perfetto_config_file"
     *                   argument instead of "perfetto_config_file".
     */
    PerfettoTracingStrategy(Instrumentation instr, String identifier) {
        super();
        mInstr = instr;
        mIdentifier = identifier;
        mPerfettoHelper = new PerfettoHelper(identifier);
        mWakeLockContext = this::runWithWakeLock;
        mWakelockSupplier = this::getWakeLock;
        mWakeLockAcquirer = this::acquireWakelock;
        mWakeLockReleaser = this::releaseWakelock;
    }

    /**
     * Constructor to simulate receiving the instrumentation arguments. Should not be used except
     * for testing.
     */
    @VisibleForTesting
    PerfettoTracingStrategy(PerfettoHelper helper, Instrumentation instr, String identifier) {
        super();
        mPerfettoHelper = helper;
        mInstr = instr;
        mIdentifier = identifier;
        mWakeLockContext = this::runWithWakeLock;
        mWakelockSupplier = this::getWakeLock;
        mWakeLockAcquirer = this::acquireWakelock;
        mWakeLockReleaser = this::releaseWakelock;
    }
    /**
     * Constructor to simulate receiving the instrumentation arguments. Should not be used except
     * for testing.
     */
    @VisibleForTesting
    PerfettoTracingStrategy(
            PerfettoHelper helper,
            Instrumentation instr,
            String identifier,
            WakeLockContext wakeLockContext,
            Supplier<PowerManager.WakeLock> wakelockSupplier,
            WakeLockAcquirer wakeLockAcquirer,
            WakeLockReleaser wakeLockReleaser) {
        super();
        mPerfettoHelper = helper;
        mIdentifier = identifier;
        mInstr = instr;
        mWakeLockContext = wakeLockContext;
        mWakeLockAcquirer = wakeLockAcquirer;
        mWakeLockReleaser = wakeLockReleaser;
        mWakelockSupplier = wakelockSupplier;
    }

    PerfettoHelper getPerfettoHelper() {
        return mPerfettoHelper;
    }

    void testFail(DataRecord testData, Description description, Failure failure) {
        mIsTestFailed = true;
    }

    void testRunStart(DataRecord runData, Description description) {
        // Clean up any perfetto process from previous test runs.
        if (!mPerfettoHelper.getPerfettoPids().isEmpty()) {
            try {
                if (mPerfettoHelper.stopPerfettoProcesses(mPerfettoHelper.getPerfettoPids())) {
                    Log.i(
                            getTag(),
                            "Stopped the already running perfetto tracing before the new test run"
                                    + " start.");
                }
            } catch (IOException e) {
                Log.e(getTag(), "Failed to stop the perfetto.", e);
            }
        } else {
            Log.i(getTag(), "No perfetto process running before the test run starts.");
        }

        // Clean up any perfetto process from previous runs tracked via perfetto pid files.
        if (mPerfettoHelper.getTrackPerfettoPidFlag()) {
            cleanupPerfettoSessionsFromPreviousRuns();
        }
    }

    private void cleanupPerfettoSessionsFromPreviousRuns() {
        File rootFolder = new File(mPerfettoHelper.getTrackPerfettoRootDir());
        File[] perfettoPidFiles =
                rootFolder.listFiles(
                        (d, name) -> name.startsWith(mPerfettoHelper.getPerfettoFilePrefix()));
        Set<Integer> pids = new HashSet<>();
        for (File perfettoPidFile : perfettoPidFiles) {
            try {
                String pid = MetricUtility.readStringFromFile(perfettoPidFile);
                pids.add(Integer.parseInt(pid.trim()));
                Log.i(getTag(), "Adding perfetto process for cleanup - ." + pid);
            } catch (FileNotFoundException fnf) {
                Log.e(getTag(), "Unable to access the perfetto process id file.", fnf);
            } catch (IOException ioe) {
                Log.e(getTag(), "Failed to retrieve the perfetto process id.", ioe);
            }
            if (perfettoPidFile.exists()) {
                Log.i(
                        getTag(),
                        String.format(
                                "Deleting perfetto process id file %s .",
                                perfettoPidFile.toString()));
                perfettoPidFile.delete();
            }
        }

        try {
            if (mPerfettoHelper.stopPerfettoProcesses(pids)) {
                Log.i(
                        getTag(),
                        "Stopped the already running perfetto tracing before the new test run"
                                + " start.");
            }
        } catch (IOException e) {
            Log.e(getTag(), "Failed to stop the perfetto.", e);
        }
    }

    void testStart(DataRecord testData, Description description, int iteration) {
        mIsTestFailed = false;
    }

    void testEnd(DataRecord testData, Description description, int iteration) {
        // No-op
    }

    void testRunEnd(DataRecord runData, Result result) {
        // No-op
    }

    /** Start perfetto tracing using the given config file. */
    protected void startPerfettoTracing() {
        boolean perfettoStartSuccess;
        SystemClock.sleep(mWaitStartTimeInMs);

        perfettoStartSuccess =
                mPerfettoHelper
                        .setTextProtoConfig(mConfigContent)
                        .setConfigFileName(mConfigFileName)
                        .setIsTextProtoConfig(mIsConfigTextProto)
                        .startCollecting();
        if (!perfettoStartSuccess) {
            Log.e(getTag(), "Perfetto did not start successfully.");
        }

        setPerfettoStartSuccess(perfettoStartSuccess);
    }

    /**
     * Stop perfetto tracing and dumping the collected trace file in given path and updating the
     * record with the path to the trace file.
     */
    protected boolean stopPerfettoTracing(Path path) {
        boolean success = mPerfettoHelper.stopCollecting(mWaitTimeInMs, path.toString());
        if (!success) {
            Log.e(getTag(), "Failed to collect the perfetto output.");
        }
        setPerfettoStartSuccess(false);
        return success;
    }

    /**
     * Stop perfetto tracing and dumping the collected trace file in given path and updating the
     * record with the path to the trace file.
     */
    protected void stopPerfettoTracingAndReportMetric(Path path, DataRecord record) {
        stopPerfettoTracingAndReportMetric(path, record, mFilePathKeyPrefix);
    }

    protected void stopPerfettoTracingAndReportMetric(Path path, DataRecord record,
            String metricName) {
        if (stopPerfettoTracing(path)) {
            if (mIsTestFailed) {
                record.addStringMetric(metricName + "_FAILED", path.toString());
            } else {
                record.addStringMetric(metricName, path.toString());
            }
        }
    }

    protected void stopPerfettoTracingWithoutMetric() {
        // Stop the existing perfetto trace collection.
        try {
            if (!mPerfettoHelper.stopPerfetto(mPerfettoHelper.getPerfettoPid())) {
                Log.e(getTag(), "Failed to stop the perfetto process.");
            }
            setPerfettoStartSuccess(false);
        } catch (IOException e) {
            Log.e(getTag(), "Failed to stop the perfetto.", e);
        }
    }

    protected void setPerfettoStartSuccess(boolean success) {
        mPerfettoStartSuccess = success;
    }

    protected boolean isPerfettoStartSuccess() {
        return mPerfettoStartSuccess;
    }

    protected String getOutputFilePrefix() {
        return mOutputFilePrefix;
    }

    protected String getFilePathKeyPrefix() {
        return mFilePathKeyPrefix;
    }

    protected String getTestOutputRoot() {
        return mTestOutputRoot;
    }

    protected boolean skipMetric() {
        return skipMetric(/* iteration= */ null);
    }

    protected boolean skipMetric(Integer iteration) {
        if (iteration != null && !mAllowedIterations.isEmpty()) {
            if (iteration.equals(0)) {
                throw new IllegalStateException("Skip metric check was executed before "
                        + "the test has started");
            }

            if (!mAllowedIterations.contains(iteration)) {
                return true;
            }
        }

        return (mSkipTestFailureMetrics && mIsTestFailed)
                || (mSkipTestSuccessMetrics && !mIsTestFailed);
    }

    protected void runTask(Runnable task, String message) {
        if (mHoldWakelockWhileCollecting) {
            if (!message.isEmpty()) {
                Log.d(getTag(), message);
            }
            mWakeLockContext.run(task);
        } else {
            task.run();
        }
    }

    @VisibleForTesting
    void runWithWakeLock(Runnable runnable) {
        PowerManager.WakeLock wakelock = null;
        try {
            wakelock = mWakelockSupplier.get();
            mWakeLockAcquirer.acquire(wakelock);
            runnable.run();
        } finally {
            mWakeLockReleaser.release(wakelock);
        }
    }

    @VisibleForTesting
    public void acquireWakelock(PowerManager.WakeLock wakelock) {
        if (wakelock != null) {
            Log.d(getTag(), "wakelock.isHeld: " + wakelock.isHeld());
            Log.d(getTag(), "acquiring wakelock.");
            Log.d(getTag(), "wakelock acquired.");
            Log.d(getTag(), "wakelock.isHeld: " + wakelock.isHeld());
        }
    }

    @VisibleForTesting
    public void releaseWakelock(PowerManager.WakeLock wakelock) {
        if (wakelock != null) {
            Log.d(getTag(), "wakelock.isHeld: " + wakelock.isHeld());
            Log.d(getTag(), "releasing wakelock.");
            wakelock.release();
            Log.d(getTag(), "wakelock released.");
            Log.d(getTag(), "wakelock.isHeld: " + wakelock.isHeld());
        }
    }

    private PowerManager.WakeLock getWakeLock() {
        PowerManager pm =
                (PowerManager) mInstr.getContext().getSystemService(Context.POWER_SERVICE);

        return pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, PerfettoTracingStrategy.class.getName());
    }

    String getTag() {
        return this.getClass().getName();
    }

    interface WakeLockContext {
        void run(Runnable runnable);
    }

    interface WakeLockAcquirer {
        void acquire(PowerManager.WakeLock wakelock);
    }

    interface WakeLockReleaser {
        void release(PowerManager.WakeLock wakelock);
    }

    void setup(Bundle args) {
        boolean perfettoStartBgWait = Boolean.parseBoolean(getArgumentValue(args,
                PERFETTO_START_BG_WAIT, String.valueOf(true)));
        mPerfettoHelper.setPerfettoStartBgWait(perfettoStartBgWait);

        // Root directory path containing the perfetto config file.
        String configRootDir =
                args.getString(PERFETTO_CONFIG_ROOT_DIR_ARG, DEFAULT_PERFETTO_CONFIG_ROOT_DIR);
        if (!configRootDir.endsWith("/")) {
            configRootDir = configRootDir.concat("/");
        }
        mPerfettoHelper.setPerfettoConfigRootDir(configRootDir);

        // Whether the config is text proto or not. By default set to false.
        mIsConfigTextProto = Boolean.parseBoolean(getArgumentValue(args, PERFETTO_CONFIG_TEXT_PROTO,
                String.valueOf(false)));

        // Perfetto config file has to be under /data/misc/perfetto-traces/
        // defaulted to DEFAULT_TEXT_CONFIG_FILE or DEFAULT_CONFIG_FILE if perfetto_config_file is
        // not passed.
        mConfigFileName = getArgumentValue(args, PERFETTO_CONFIG_FILE_ARG,
                mIsConfigTextProto ? DEFAULT_TEXT_CONFIG_FILE : DEFAULT_CONFIG_FILE);

        mConfigContent = getArgumentValue(args, PERFETTO_CONFIG_TEXT_CONTENT, "");

        mOutputFilePrefix = getArgumentValue(args, PERFETTO_CONFIG_OUTPUT_FILE_PREFIX,
                DEFAULT_PERFETTO_PREFIX);

        mPerfettoHelper.setTrackPerfettoPidFlag(Boolean.parseBoolean(getArgumentValue(args,
                PERFETTO_PERSIST_PID_TRACK, String.valueOf(true))));
        if (mPerfettoHelper.getTrackPerfettoPidFlag()) {
            mPerfettoHelper.setTrackPerfettoRootDir(getArgumentValue(args, PERFETTO_PID_TRACK_ROOT,
                    DEFAULT_PERFETTO_PID_TRACK_ROOT));
        }

        // Whether to hold wakelocks on all Prefetto tracing functions. You may want to enable
        // this if your device is not USB connected. This option prevents the device from
        // going into suspend mode while this listener is running intensive tasks.
        mHoldWakelockWhileCollecting = Boolean.parseBoolean(getArgumentValue(args,
                HOLD_WAKELOCK_WHILE_COLLECTING, String.valueOf(false)));

        // Wait time before stopping the perfetto trace collection after the test
        // is completed. Defaulted to 0 msecs.
        mWaitTimeInMs = Long.parseLong(getArgumentValue(args, PERFETTO_WAIT_TIME_ARG,
                DEFAULT_WAIT_TIME_MSECS));

        // Wait time before the perfetto trace is started.
        mWaitStartTimeInMs = Long.parseLong(getArgumentValue(args, PERFETTO_START_WAIT_TIME_ARG,
                DEFAULT_START_WAIT_TIME_MSECS));

        // Destination folder in the device to save all the trace files.
        // Defaulted to /sdcard/test_results if test_output_root is not passed.
        mTestOutputRoot = getArgumentValue(args, TEST_OUTPUT_ROOT, DEFAULT_OUTPUT_ROOT);

        // By default, this flag is set to false to collect the metrics on test failure.
        mSkipTestFailureMetrics =
                Boolean.parseBoolean(
                        getArgumentValue(args, SKIP_TEST_FAILURE_METRICS, String.valueOf(false)));
        mSkipTestSuccessMetrics = Boolean.parseBoolean(getArgumentValue(args,
                SKIP_TEST_SUCCESS_METRICS, String.valueOf(false)));
        mSkipEmptyMetrics =
                Boolean.parseBoolean(
                        getArgumentValue(args, SKIP_EMPTY_METRICS, String.valueOf(false)));
        mPerfettoHelper.setCheckEmptyMetrics(mSkipEmptyMetrics);

        mFilePathKeyPrefix = getArgumentValue(args, ARGUMENT_FILE_PATH_KEY_PREFIX,
                DEFAULT_FILE_PATH_KEY_PREFIX);

        final String iterations = getArgumentValue(args, ARGUMENT_ALLOW_ITERATIONS, "");
        for (String iteration : iterations.split(",")) {
            if (!iteration.isEmpty()) {
                mAllowedIterations.add(Integer.parseInt(iteration));
            }
        }
    }

    /**
     * Gets argument value, strategy-specific argument has a priority over global argument
     */
    private String getArgumentValue(Bundle args, String key, String defaultValue) {
        String strategySpecificValue = args.getString(mIdentifier +
                STRATEGY_ARGUMENT_NAMESPACE_SEPARATOR + key);
        if (strategySpecificValue != null) return strategySpecificValue;
        return args.getString(key, defaultValue);
    }

    /**
     * Returns the packagename.classname_methodname which has no spaces and used to create file
     * names.
     */
    public static String getTestFileName(Description description) {
        return String.format(
                "%s_%s",
                sanitizeString(description.getClassName()),
                sanitizeString(description.getMethodName()));
    }

    protected static String sanitizeString(String value) {
        return value.replaceAll(SPACES_PATTERN, REPLACEMENT_CHAR).trim();
    }
}

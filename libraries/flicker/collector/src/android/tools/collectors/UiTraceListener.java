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

package android.tools.collectors;

import static perfetto.protos.PerfettoConfig.SurfaceFlingerLayersConfig.TraceFlag.TRACE_FLAG_BUFFERS;
import static perfetto.protos.PerfettoConfig.SurfaceFlingerLayersConfig.TraceFlag.TRACE_FLAG_INPUT;
import static perfetto.protos.PerfettoConfig.SurfaceFlingerLayersConfig.TraceFlag.TRACE_FLAG_VIRTUAL_DISPLAYS;

import android.device.collectors.DataRecord;
import android.device.collectors.PerfettoListener;
import android.device.collectors.PerfettoTracingStrategy;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import org.junit.runner.Result;

import perfetto.protos.PerfettoConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * A {@link PerfettoListener} that captures the perfetto trace for UI traces during each test method
 * and save the perfetto trace files under
 * {root}/{test_name}/PerfettoListener/{test_name}-{invocation_count}.perfetto-trace
 */
public class UiTraceListener extends PerfettoListener {
    private static final String LOG_TAG = "UITraceListener";

    public static final String TRACE_FTRACE_KEY = "trace_ftrace";
    public static final String TRACE_LAYERS_KEY = "trace_layers";
    public static final String TRACE_SHELL_TRANSITIONS_KEY = "trace_shell_transitions";
    public static final String TRACE_INPUT_KEY = "trace_input";
    public static final String PROTOLOG_GROUPS_KEY = "protolog_groups";

    private File mTempConfigFile;

    public UiTraceListener() {
        super();
    }

    /**
     * Constructor to simulate receiving the instrumentation arguments. Should not be used except
     * for testing.
     */
    @VisibleForTesting
    UiTraceListener(Bundle args, List<PerfettoTracingStrategy> strategies) {
        super(args, strategies);
        Log.d(LOG_TAG, "Initialized the UiTraceListener#" + this.hashCode());
    }

    @Override
    public void setupAdditionalArgs() {
        super.setupAdditionalArgs();

        Bundle args = getArgsBundle();
        PerfettoConfig.TraceConfig protoConfig = buildConfig(args);

        try {
            File configDir = getInstrumentation().getContext().getExternalCacheDir();
            Log.d(LOG_TAG, "Creating temp config file in: " + configDir.getAbsolutePath());
            mTempConfigFile = File.createTempFile("trace_config", ".pb", configDir);

            // Make readable by Perfetto.
            var permissionsChanged = mTempConfigFile.setReadable(true, false);
            if (!permissionsChanged) {
                throw new RuntimeException("Unable to set permissions for temp config file");
            }
            Log.d(LOG_TAG, "Temp config file created: " + mTempConfigFile.getAbsolutePath());
            try (FileOutputStream out = new FileOutputStream(mTempConfigFile)) {
                out.write(protoConfig.toByteArray());
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Unable to create or write temp config file", e);
            throw new RuntimeException("Unable to create or write temp config file", e);
        }

        args.putString(
                PerfettoTracingStrategy.PERFETTO_CONFIG_ROOT_DIR_ARG, mTempConfigFile.getParent());
        args.putString(PerfettoTracingStrategy.PERFETTO_CONFIG_FILE_ARG, mTempConfigFile.getName());
        args.putString(PerfettoTracingStrategy.PERFETTO_CONFIG_TEXT_PROTO, "false");
        args.putString(PerfettoTracingStrategy.PERFETTO_STREAM_CONFIG_FROM_FILE, "true");
    }

    @Override
    public void onTestRunEnd(DataRecord runData, Result result) {
        super.onTestRunEnd(runData, result);
        // mTempConfigFile is no longer created, so we don't need to delete it.
        if (mTempConfigFile != null && mTempConfigFile.exists()) {
            Log.d(LOG_TAG, "Deleting temp config file: " + mTempConfigFile.getAbsolutePath());
            mTempConfigFile.delete();
        }
    }

    @VisibleForTesting
    PerfettoConfig.TraceConfig buildConfig(Bundle args) {
        PerfettoConfig.TraceConfig.Builder config = PerfettoConfig.TraceConfig.newBuilder();

        // Enable periodic flushing of the trace buffer into the output file.
        config.setWriteIntoFile(true);
        // Writes the userspace buffer into the file every 1s.
        config.setFileWritePeriodMs(1000);
        // See b/126487238 - we need to guarantee ordering of events.
        config.setFlushPeriodMs(30000);

        /*
         * The trace buffers needs to be big enough to hold |file_write_period_ms| of
         * trace data. The trace buffer sizing depends on the number of trace categories
         * enabled and the device activity.
         */
        config.addBuffers(
                PerfettoConfig.TraceConfig.BufferConfig.newBuilder()
                        .setSizeKb(63488)
                        .setFillPolicy(
                                PerfettoConfig.TraceConfig.BufferConfig.FillPolicy.RING_BUFFER));

        // Ftrace data source
        if (traceFtrace(args)) {
            Log.d(LOG_TAG, "Enabling Ftrace datasource");
            config.addDataSources(
                    PerfettoConfig.TraceConfig.DataSource.newBuilder()
                            .setConfig(
                                    PerfettoConfig.DataSourceConfig.newBuilder()
                                            .setName("linux.ftrace")
                                            .setFtraceConfig(
                                                    PerfettoConfig.FtraceConfig.newBuilder()
                                                            .addFtraceEvents("ftrace/print")
                                                            .addFtraceEvents("task/task_newtask")
                                                            .addFtraceEvents("task/task_rename")
                                                            .addAtraceCategories("wm")
                                                            .addAtraceCategories("ss"))));
        }

        // SurfaceFlinger traces
        if (traceLayers(args)) {
            Log.d(LOG_TAG, "Enabling surfaceflinger datasource");
            var sfConfig =
                    PerfettoConfig.SurfaceFlingerLayersConfig.newBuilder()
                            .setMode(PerfettoConfig.SurfaceFlingerLayersConfig.Mode.MODE_ACTIVE)
                            .addTraceFlags(TRACE_FLAG_INPUT)
                            .addTraceFlags(TRACE_FLAG_BUFFERS)
                            .addTraceFlags(TRACE_FLAG_VIRTUAL_DISPLAYS);
            config.addDataSources(
                    PerfettoConfig.TraceConfig.DataSource.newBuilder()
                            .setConfig(
                                    PerfettoConfig.DataSourceConfig.newBuilder()
                                            .setName("android.surfaceflinger.layers")
                                            .setSurfaceflingerLayersConfig(sfConfig)));

            var transactionsConfig =
                    PerfettoConfig.SurfaceFlingerTransactionsConfig.newBuilder()
                            .setMode(
                                    PerfettoConfig.SurfaceFlingerTransactionsConfig.Mode
                                            .MODE_ACTIVE);
            config.addDataSources(
                    PerfettoConfig.TraceConfig.DataSource.newBuilder()
                            .setConfig(
                                    PerfettoConfig.DataSourceConfig.newBuilder()
                                            .setName("android.surfaceflinger.transactions")
                                            .setSurfaceflingerTransactionsConfig(
                                                    transactionsConfig)));
        }

        // Shell Transitions
        if (traceShellTransitions(args)) {
            Log.d(LOG_TAG, "Enabling shell transitions datasource");
            config.addDataSources(
                    PerfettoConfig.TraceConfig.DataSource.newBuilder()
                            .setConfig(
                                    PerfettoConfig.DataSourceConfig.newBuilder()
                                            .setName("com.android.wm.shell.transition")));
        }

        // Input
        if (traceInput(args)) {
            Log.d(LOG_TAG, "Enabling input datasource");
            var inputConfig =
                    PerfettoConfig.AndroidInputEventConfig.newBuilder()
                            .setMode(
                                    PerfettoConfig.AndroidInputEventConfig.TraceMode
                                            .TRACE_MODE_TRACE_ALL);
            config.addDataSources(
                    PerfettoConfig.TraceConfig.DataSource.newBuilder()
                            .setConfig(
                                    PerfettoConfig.DataSourceConfig.newBuilder()
                                            .setName("android.input.inputevent")
                                            .setAndroidInputEventConfig(inputConfig)));
        }

        // Protolog
        String protologGroups = args.getString(PROTOLOG_GROUPS_KEY);
        if (protologGroups != null && !protologGroups.isEmpty()) {
            Log.d(LOG_TAG, "Enabling protolog datasource");
            PerfettoConfig.ProtoLogConfig.Builder protologConfig =
                    PerfettoConfig.ProtoLogConfig.newBuilder();

            for (String group : protologGroups.split(",\\s*")) {
                String trimmedGroup = group.trim();
                if (!trimmedGroup.isEmpty()) {
                    protologConfig.addGroupOverrides(
                            PerfettoConfig.ProtoLogGroup.newBuilder()
                                    .setGroupName(trimmedGroup)
                                    .setLogFrom(
                                            PerfettoConfig.ProtoLogLevel.PROTOLOG_LEVEL_VERBOSE));
                }
            }
            config.addDataSources(
                    PerfettoConfig.TraceConfig.DataSource.newBuilder()
                            .setConfig(
                                    PerfettoConfig.DataSourceConfig.newBuilder()
                                            .setName("android.protolog")
                                            .setProtologConfig(protologConfig)));
        }

        return config.build();
    }

    protected boolean traceFtrace(Bundle args) {
        return Boolean.parseBoolean(args.getString(TRACE_FTRACE_KEY, "false"));
    }

    protected boolean traceLayers(Bundle args) {
        return Boolean.parseBoolean(args.getString(TRACE_LAYERS_KEY, "false"));
    }

    protected boolean traceShellTransitions(Bundle args) {
        return Boolean.parseBoolean(args.getString(TRACE_SHELL_TRANSITIONS_KEY, "false"));
    }

    protected boolean traceInput(Bundle args) {
        return Boolean.parseBoolean(args.getString(TRACE_INPUT_KEY, "false"));
    }
}

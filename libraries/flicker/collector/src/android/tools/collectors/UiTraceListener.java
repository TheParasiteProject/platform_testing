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

import android.device.collectors.PerfettoListener;
import android.device.collectors.PerfettoTracingStrategy;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import perfetto.protos.PerfettoConfig;

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
    public static final String TRACE_WM_KEY = "trace_windows";
    public static final String PROTOLOG_GROUPS_KEY = "protolog_groups";
    public static final String PROTOLOG_DEFAULT_LOG_FROM_LEVEL_KEY = "protolog_log_from_level";

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
        Bundle args = getArgsBundle();
        PerfettoConfig.TraceConfig protoConfig = buildConfig(args);
        var protoBinary = protoConfig.toByteArray();
        String protoBase64 = Base64.encodeToString(protoBinary, Base64.NO_WRAP);

        args.putString(PerfettoTracingStrategy.PERFETTO_CONFIG_TEXT_PROTO, "false");
        args.putString(PerfettoTracingStrategy.PERFETTO_CONFIG_CONTENT, protoBase64);
        args.putString(PerfettoTracingStrategy.PERFETTO_CONFIG_OUTPUT_FILE_PREFIX, "uiTrace_");

        // The super call must be at the end to ensure the tracing strategies are
        // initialized with the custom perfetto config arguments added in this method.
        super.setupAdditionalArgs();
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

        // Window Manager
        if (traceWindows(args)) {
            Log.d(LOG_TAG, "Enabling window manager datasource");
            var wmConfig =
                    PerfettoConfig.WindowManagerConfig.newBuilder()
                            .setLogLevel(
                                    PerfettoConfig.WindowManagerConfig.LogLevel.LOG_LEVEL_VERBOSE)
                            .setLogFrequency(
                                    PerfettoConfig.WindowManagerConfig.LogFrequency
                                            .LOG_FREQUENCY_FRAME);
            config.addDataSources(
                    PerfettoConfig.TraceConfig.DataSource.newBuilder()
                            .setConfig(
                                    PerfettoConfig.DataSourceConfig.newBuilder()
                                            .setName("android.windowmanager")
                                            .setWindowmanagerConfig(wmConfig)));
        }

        // Protolog
        String protologDefaultLogFromLevel = getDefaultProtoLogFromLevel(args);
        String protologGroups = args.getString(PROTOLOG_GROUPS_KEY);

        var hasDefaultLogFromLevel =
                protologDefaultLogFromLevel != null && !protologDefaultLogFromLevel.isBlank();
        var hasProtoLogGroups = protologGroups != null && !protologGroups.isBlank();
        var enableProtoLogging = hasDefaultLogFromLevel || hasProtoLogGroups;

        PerfettoConfig.ProtoLogConfig.Builder protologConfig =
                PerfettoConfig.ProtoLogConfig.newBuilder();

        if (hasDefaultLogFromLevel) {
            PerfettoConfig.ProtoLogLevel protoLogLevel =
                    switch (protologDefaultLogFromLevel.toUpperCase()) {
                        case "VERBOSE" -> PerfettoConfig.ProtoLogLevel.PROTOLOG_LEVEL_VERBOSE;
                        case "DEBUG" -> PerfettoConfig.ProtoLogLevel.PROTOLOG_LEVEL_DEBUG;
                        case "INFO" -> PerfettoConfig.ProtoLogLevel.PROTOLOG_LEVEL_INFO;
                        case "WARN" -> PerfettoConfig.ProtoLogLevel.PROTOLOG_LEVEL_WARN;
                        case "ERROR" -> PerfettoConfig.ProtoLogLevel.PROTOLOG_LEVEL_ERROR;
                        case "WTF" -> PerfettoConfig.ProtoLogLevel.PROTOLOG_LEVEL_WTF;
                        default ->
                                throw new IllegalArgumentException(
                                        "Invalid protolog log level argument provided: "
                                                + protologDefaultLogFromLevel);
                    };

            protologConfig.setDefaultLogFromLevel(protoLogLevel);
        }

        if (hasProtoLogGroups) {
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
        }

        if (enableProtoLogging) {
            Log.d(LOG_TAG, "Enabling protolog datasource");
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

    protected boolean traceWindows(Bundle args) {
        return Boolean.parseBoolean(args.getString(TRACE_WM_KEY, "false"));
    }

    @Nullable
    protected String getDefaultProtoLogFromLevel(Bundle args) {
        return args.getString(PROTOLOG_DEFAULT_LOG_FROM_LEVEL_KEY);
    }
}

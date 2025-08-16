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

package android.tools.collectors;


import android.device.collectors.PerfettoListener;
import android.device.collectors.PerfettoTracingStrategy;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.junit.runner.notification.RunListener;

import java.util.List;

/**
 * A {@link PerfettoListener} that captures the perfetto trace for UI traces during each test method
 * and save the perfetto trace files under
 * <root>/<test_name>/PerfettoListener/<test_name>-<invocation_count>.perfetto-trace
 */
@RunListener.ThreadSafe
public class DefaultUITraceListener extends UiTraceListener {

    @SuppressWarnings("unused")
    public DefaultUITraceListener() {
        super();
    }

    /**
     * Constructor to simulate receiving the instrumentation arguments. Should not be used except
     * for testing.
     */
    @VisibleForTesting
    DefaultUITraceListener(Bundle args, List<PerfettoTracingStrategy> strategies) {
        super(args, strategies);
    }

    @Override
    protected boolean traceFtrace(Bundle args) {
        return Boolean.parseBoolean(args.getString(TRACE_FTRACE_KEY, "true"));
    }

    @Override
    protected boolean traceLayers(Bundle args) {
        return Boolean.parseBoolean(args.getString(TRACE_LAYERS_KEY, "true"));
    }

    @Override
    protected boolean traceShellTransitions(Bundle args) {
        return Boolean.parseBoolean(args.getString(TRACE_SHELL_TRANSITIONS_KEY, "true"));
    }

    @Override
    protected boolean traceInput(Bundle args) {
        return Boolean.parseBoolean(args.getString(TRACE_INPUT_KEY, "true"));
    }

    @Nullable
    @Override
    protected String getDefaultProtoLogFromLevel(Bundle args) {
        return args.getString(PROTOLOG_DEFAULT_LOG_FROM_LEVEL_KEY, "DEBUG");
    }
}

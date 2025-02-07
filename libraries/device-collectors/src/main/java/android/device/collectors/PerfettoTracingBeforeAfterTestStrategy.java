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

package android.device.collectors;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.helpers.PerfettoHelper;

import org.junit.runner.Description;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

/**
 * A strategy that allows capturing one-shot traces before and after each test
 */
public class PerfettoTracingBeforeAfterTestStrategy extends PerfettoTracingStrategy {
    private static final String STRATEGY_IDENTIFIER = "before_after";

    PerfettoTracingBeforeAfterTestStrategy(Instrumentation instr) {
        super(instr, STRATEGY_IDENTIFIER);
    }

    @VisibleForTesting
    public PerfettoTracingBeforeAfterTestStrategy(
            PerfettoHelper helper,
            Instrumentation instr) {
        super(helper, instr, STRATEGY_IDENTIFIER);
    }

    /**
     * Constructor to simulate receiving the instrumentation arguments. Should not be used except
     * for testing.
     */
    @VisibleForTesting
    PerfettoTracingBeforeAfterTestStrategy(
            PerfettoHelper helper,
            Instrumentation instr,
            WakeLockContext wakeLockContext,
            Supplier<PowerManager.WakeLock> wakelockSupplier,
            WakeLockAcquirer wakeLockAcquirer,
            WakeLockReleaser wakeLockReleaser) {
        super(helper, instr, STRATEGY_IDENTIFIER, wakeLockContext, wakelockSupplier,
                wakeLockAcquirer, wakeLockReleaser);
    }

    @Override
    void testStart(DataRecord testData, Description description, int iteration) {
        super.testStart(testData, description, iteration);
        captureTrace("before", testData, description, iteration);
    }

    @Override
    void testEnd(DataRecord testData, Description description, int iteration) {
        super.testEnd(testData, description, iteration);
        captureTrace("after", testData, description, iteration);
    }

    private void captureTrace(String tag, DataRecord testData, Description description,
            int iteration) {
        if (skipMetric(iteration)) {
            return;
        }

        final String metricName = getFilePathKeyPrefix() + "_" + tag;

        Log.i(getTag(), "Capturing trace '" + tag + "', for " +
                getTestFileName(description));

        Runnable task =
                () -> {
                    Log.i(getTag(), "Starting perfetto tracing.");
                    startPerfettoTracing();

                    if (!isPerfettoStartSuccess()) {
                        return;
                    }

                    Log.i(getTag(), "Stopping perfetto tracing.");
                    Path path = getOutputPathModeTest(tag, description, iteration);
                    stopPerfettoTracingAndReportMetric(path, testData, metricName);
                };

        runTask(task, "Holding a wakelock at captureTrace");
    }

    @SuppressLint("DefaultLocale")
    private Path getOutputPathModeTest(String tag, Description description, int iteration) {
        return Paths.get(
                getTestOutputRoot(),
                getTestFileName(description),
                this.getClass().getSimpleName(),
                String.format(
                        "%s%s-%s-%d.perfetto-trace",
                        getOutputFilePrefix(),
                        getTestFileName(description),
                        tag,
                        iteration));
    }
}

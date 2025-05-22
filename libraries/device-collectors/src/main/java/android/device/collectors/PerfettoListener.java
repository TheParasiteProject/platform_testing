/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.os.Trace;

import androidx.annotation.VisibleForTesting;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link PerfettoListener} that captures the perfetto trace during each test method and save the
 * perfetto trace files under
 * <root>/<test_name>/PerfettoTracingStrategy/<test_name>-<invocation_count>.perfetto-trace
 */
@OptionClass(alias = "perfetto-collector")
public class PerfettoListener extends BaseMetricListener {
    public static final String COLLECT_PER_RUN = "per_run";
    public static final String COLLECT_PER_CLASS = "per_class";
    public static final String COLLECT_BEFORE_AFTER = "perfetto_before_after_test";

    private List<PerfettoTracingStrategy> mTracingStrategies;

    public PerfettoListener() {
        super();
    }

    /**
     * Constructor to simulate receiving the instrumentation arguments. Should not be used except
     * for testing.
     */
    @VisibleForTesting
    public PerfettoListener(Bundle args, List<PerfettoTracingStrategy> strategies) {
        super(args);
        mTracingStrategies = strategies;
    }

    @Override
    public void onTestRunStart(DataRecord runData, Description description) {
        mTracingStrategies.forEach(strategy -> strategy.testRunStart(runData, description));
    }

    @Override
    public void onTestStart(DataRecord testData, Description description) {
        final int iteration = getIteration(description);
        String sectionName = "test:" + description;
        if (iteration > 0) {
            sectionName += " iteration:" + iteration;
        }
        Trace.beginSection(sectionName);
        mTracingStrategies.forEach(strategy -> strategy.testStart(testData, description,
                iteration));
    }

    @Override
    public void onTestFail(DataRecord testData, Description description, Failure failure) {
        mTracingStrategies.forEach(strategy -> strategy.testFail(testData, description, failure));
    }

    @Override
    public void onTestEnd(DataRecord testData, Description description) {
        final int iteration = getIteration(description);
        mTracingStrategies.forEach(strategy -> strategy.testEnd(testData, description, iteration));
        Trace.endSection();
    }

    @Override
    public void onTestRunEnd(DataRecord runData, Result result) {
        mTracingStrategies.forEach(strategy -> strategy.testRunEnd(runData, result));
    }

    @Override
    public void setupAdditionalArgs() {
        Bundle args = getArgsBundle();

        if (mTracingStrategies == null) {
            initTracingStrategies(args);
        }

        mTracingStrategies.forEach(strategy -> strategy.setup(args));
    }

    private void initTracingStrategies(Bundle args) {
        mTracingStrategies = new ArrayList<>();

        // Whether to collect the for the entire test run, per test, or per class.
        if (Boolean.parseBoolean(args.getString(COLLECT_PER_RUN))) {
            mTracingStrategies.add(new PerfettoTracingPerRunStrategy(getInstrumentation()));
        } else if (Boolean.parseBoolean(args.getString(COLLECT_PER_CLASS))) {
            mTracingStrategies.add(new PerfettoTracingPerClassStrategy(getInstrumentation()));
        } else {
            mTracingStrategies.add(new PerfettoTracingPerTestStrategy(getInstrumentation()));
        }

        if (Boolean.parseBoolean(args.getString(COLLECT_BEFORE_AFTER))) {
            mTracingStrategies.add(
                    new PerfettoTracingBeforeAfterTestStrategy(getInstrumentation()));
        }
    }

    @VisibleForTesting
    String getTestFileName(Description description) {
        return PerfettoTracingStrategy.getTestFileName(description);
    }

    @VisibleForTesting
    void runWithWakeLock(Runnable runnable) {
        mTracingStrategies.forEach(strategy -> strategy.runWithWakeLock(runnable));
    }
}

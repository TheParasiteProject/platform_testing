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

import static android.device.collectors.PerfettoTracingStrategy.ARGUMENT_ALLOW_ITERATIONS;
import static android.device.collectors.PerfettoTracingStrategy.ARGUMENT_FILE_PATH_KEY_PREFIX;
import static android.device.collectors.PerfettoTracingStrategy.STRATEGY_ARGUMENT_NAMESPACE_SEPARATOR;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.os.Bundle;
import android.os.PowerManager;

import androidx.test.runner.AndroidJUnit4;

import com.android.helpers.PerfettoHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.function.Supplier;

/**
 * Android Unit tests for {@link PerfettoTracingStrategy}.
 *
 * <p>To run: atest
 * CollectorDeviceLibTest:android.device.collectors.PerfettoTracingStrategyTest
 */
@RunWith(AndroidJUnit4.class)
public class PerfettoTracingStrategyTest {

    private static final String TEST_STRATEGY_IDENTIFIER = "test_strategy";

    @Spy
    private PerfettoHelper mPerfettoHelper;
    @Mock
    private Instrumentation mInstrumentation;

    @Mock
    private PerfettoTracingStrategy.WakeLockContext mWakeLockContext;
    @Mock
    private PerfettoTracingStrategy.WakeLockAcquirer mWakelLockAcquirer;
    @Mock
    private PerfettoTracingStrategy.WakeLockReleaser mWakeLockReleaser;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doAnswer(
                invocation -> {
                    Runnable runnable = invocation.getArgument(0);
                    runnable.run();
                    return null;
                })
                .when(mWakeLockContext)
                .run(any());
    }

    @SuppressLint("VisibleForTests")
    private TestPerfettoTracingStrategy initStrategy(Bundle b) {
        TestPerfettoTracingStrategy strategy =
                new TestPerfettoTracingStrategy(mPerfettoHelper,
                        mInstrumentation,
                        mWakeLockContext,
                        () -> null,
                        mWakelLockAcquirer,
                        mWakeLockReleaser
                );
        strategy.setup(b);
        return strategy;
    }

    @Test
    public void testGetParameter_noArgumentPassed_returnsDefaultValue() {
        Bundle b = new Bundle();
        TestPerfettoTracingStrategy strategy = initStrategy(b);

        assertEquals(strategy.getDefaultFilePathKeyPrefix(), strategy.getFilePathKeyPrefix());
    }

    @Test
    public void testGetParameter_globalArgumentPassed_returnsProvidedValue() {
        Bundle b = new Bundle();
        b.putString(ARGUMENT_FILE_PATH_KEY_PREFIX, "overridden_file_path_key");
        TestPerfettoTracingStrategy strategy = initStrategy(b);

        assertEquals("overridden_file_path_key", strategy.getFilePathKeyPrefix());
    }

    @Test
    public void testGetParameter_strategySpecificArgumentPassed_returnsProvidedValue() {
        Bundle b = new Bundle();
        b.putString(TEST_STRATEGY_IDENTIFIER + STRATEGY_ARGUMENT_NAMESPACE_SEPARATOR
                + ARGUMENT_FILE_PATH_KEY_PREFIX, "overridden_file_path_key");
        TestPerfettoTracingStrategy strategy = initStrategy(b);

        assertEquals("overridden_file_path_key", strategy.getFilePathKeyPrefix());
    }

    @Test
    public void testGetParameter_otherStrategySpecificArgumentPassed_returnsDefaultValue() {
        Bundle b = new Bundle();
        b.putString("some_other_strategy_identifier" + STRATEGY_ARGUMENT_NAMESPACE_SEPARATOR
                + ARGUMENT_FILE_PATH_KEY_PREFIX, "overridden_file_path_key");
        TestPerfettoTracingStrategy strategy = initStrategy(b);

        assertEquals(strategy.getDefaultFilePathKeyPrefix(), strategy.getFilePathKeyPrefix());
    }

    @Test
    public void testNoIterationArgumentPassed_doesNotSkipMetrics() {
        Bundle b = new Bundle();
        TestPerfettoTracingStrategy strategy = initStrategy(b);

        assertFalse(strategy.skipMetric(/* iteration= */ 1));
        assertFalse(strategy.skipMetric(/* iteration= */ 2));
        assertFalse(strategy.skipMetric(/* iteration= */ 100));
    }

    @Test
    public void testAllowIterationArgumentPassedWithOneIteration_skipMetricForOtherIterations() {
        Bundle b = new Bundle();
        b.putString(ARGUMENT_ALLOW_ITERATIONS, "42");
        TestPerfettoTracingStrategy strategy = initStrategy(b);

        assertFalse(strategy.skipMetric(/* iteration= */ 42));
        assertTrue(strategy.skipMetric(/* iteration= */ 1));
        assertTrue(strategy.skipMetric(/* iteration= */ 2));
        assertTrue(strategy.skipMetric(/* iteration= */ 100));
    }

    @Test
    public void testAllowIterationArgumentPassedWithMultipleIterations_skipMetricForOthers() {
        Bundle b = new Bundle();
        b.putString(ARGUMENT_ALLOW_ITERATIONS, "1,2,3");
        TestPerfettoTracingStrategy strategy = initStrategy(b);

        assertFalse(strategy.skipMetric(/* iteration= */ 1));
        assertFalse(strategy.skipMetric(/* iteration= */ 2));
        assertFalse(strategy.skipMetric(/* iteration= */ 3));
        assertTrue(strategy.skipMetric(/* iteration= */ 4));
    }

    @Test
    public void testCallSkipMetricForNotExecutedTest_throwException() {
        Bundle b = new Bundle();
        b.putString(ARGUMENT_ALLOW_ITERATIONS, "1");
        TestPerfettoTracingStrategy strategy = initStrategy(b);

        assertThrows(IllegalStateException.class, () -> strategy.skipMetric(/* iteration= */ 0));
    }

    private static class TestPerfettoTracingStrategy extends PerfettoTracingStrategy {

        @SuppressLint("VisibleForTests")
        TestPerfettoTracingStrategy(PerfettoHelper helper, Instrumentation instr,
                WakeLockContext wakeLockContext,
                Supplier<PowerManager.WakeLock> wakelockSupplier,
                WakeLockAcquirer wakeLockAcquirer, WakeLockReleaser wakeLockReleaser) {
            super(helper, instr, TEST_STRATEGY_IDENTIFIER, wakeLockContext, wakelockSupplier,
                    wakeLockAcquirer,
                    wakeLockReleaser);
        }

        @Override
        public String getFilePathKeyPrefix() {
            return super.getFilePathKeyPrefix();
        }

        @Override
        public boolean skipMetric(Integer iteration) {
            return super.skipMetric(iteration);
        }

        public String getDefaultFilePathKeyPrefix() {
            return DEFAULT_FILE_PATH_KEY_PREFIX;
        }
    }
}

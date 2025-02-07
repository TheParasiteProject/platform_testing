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

import static android.device.collectors.PerfettoTracingStrategy.ARGUMENT_ALLOW_ITERATIONS;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.os.Bundle;

import androidx.test.runner.AndroidJUnit4;

import com.android.helpers.PerfettoHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

/**
 * Android Unit tests for {@link PerfettoTracingBeforeAfterTestStrategy}.
 *
 * <p>To run: atest
 * CollectorDeviceLibTest:android.device.collectors.PerfettoTracingBeforeAfterStrategyTest.
 */
@RunWith(AndroidJUnit4.class)
public class PerfettoTracingBeforeAfterStrategyTest {
    private Description mRunDesc;
    private Description mTest1Desc;
    private Description mTest2Desc;
    private DataRecord mDataRecord;

    @Spy private PerfettoHelper mPerfettoHelper;
    @Mock private Instrumentation mInstrumentation;

    @Mock private PerfettoTracingStrategy.WakeLockContext mWakeLockContext;
    @Mock private PerfettoTracingStrategy.WakeLockAcquirer mWakelLockAcquirer;
    @Mock private PerfettoTracingStrategy.WakeLockReleaser mWakeLockReleaser;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mRunDesc = Description.createSuiteDescription("run");
        mTest1Desc = Description.createTestDescription("run", "test1");
        mTest2Desc = Description.createTestDescription("run", "test2");
        mDataRecord = new DataRecord();

        doReturn(true).when(mPerfettoHelper).startCollecting();
        doReturn(true).when(mPerfettoHelper).stopCollecting(anyLong(), anyString());

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
    private PerfettoTracingBeforeAfterTestStrategy initStrategy(Bundle b) {
        PerfettoTracingBeforeAfterTestStrategy strategy =
                spy(
                        new PerfettoTracingBeforeAfterTestStrategy(
                                mPerfettoHelper,
                                mInstrumentation,
                                mWakeLockContext,
                                () -> null,
                                mWakelLockAcquirer,
                                mWakeLockReleaser));

        strategy.setup(b);
        return strategy;
    }

    @Test
    public void testPerfettoTraceStartAndEndOnTestStart() {
        Bundle b = new Bundle();
        PerfettoTracingStrategy strategy = initStrategy(b);
        strategy.testRunStart(mDataRecord, mRunDesc);
        verify(mPerfettoHelper, never()).startCollecting();

        strategy.testStart(mDataRecord, mTest1Desc, /* iteration= */ 1);

        verify(mPerfettoHelper).startCollecting();
        verify(mPerfettoHelper).stopCollecting(anyLong(), anyString());
    }

    @Test
    public void testPerfettoTraceStartAndEndOnTestEnd() {
        Bundle b = new Bundle();
        PerfettoTracingStrategy strategy = initStrategy(b);
        strategy.testRunStart(mDataRecord, mRunDesc);
        verify(mPerfettoHelper, never()).startCollecting();
        strategy.testStart(mDataRecord, mTest1Desc, /* iteration= */ 1);
        clearInvocations(mPerfettoHelper);

        strategy.testEnd(mDataRecord, mTest1Desc, /* iteration= */ 1);

        verify(mPerfettoHelper).startCollecting();
        verify(mPerfettoHelper).stopCollecting(anyLong(), anyString());
    }

    @Test
    public void testTwoTests_startsAndEndsPerfettoCollectionForAllExecutions() {
        Bundle b = new Bundle();
        PerfettoTracingStrategy strategy = initStrategy(b);
        strategy.testRunStart(mDataRecord, mRunDesc);
        verify(mPerfettoHelper, never()).startCollecting();

        strategy.testStart(mDataRecord, mTest1Desc, /* iteration= */ 1);
        strategy.testEnd(mDataRecord, mTest1Desc, /* iteration= */ 1);
        strategy.testStart(mDataRecord, mTest2Desc, /* iteration= */ 1);
        strategy.testEnd(mDataRecord, mTest2Desc, /* iteration= */ 1);

        verify(mPerfettoHelper, times(4)).startCollecting();
        verify(mPerfettoHelper, times(4)).stopCollecting(anyLong(), anyString());
    }

    @Test
    public void testTwoIterations_onlySecondIsAllowed_collectsTraceOnlyForTheSecond() {
        Bundle b = new Bundle();
        b.putString(ARGUMENT_ALLOW_ITERATIONS, "2");
        PerfettoTracingStrategy strategy = initStrategy(b);
        strategy.testRunStart(mDataRecord, mRunDesc);
        verify(mPerfettoHelper, never()).startCollecting();

        strategy.testStart(mDataRecord, mTest1Desc, /* iteration= */ 1);
        strategy.testEnd(mDataRecord, mTest1Desc, /* iteration= */ 1);
        // Should not collect traces for the first iteration
        verify(mPerfettoHelper, never()).startCollecting();

        strategy.testStart(mDataRecord, mTest1Desc, /* iteration= */ 2);

        verify(mPerfettoHelper).startCollecting();
        verify(mPerfettoHelper).stopCollecting(anyLong(), anyString());
    }
}

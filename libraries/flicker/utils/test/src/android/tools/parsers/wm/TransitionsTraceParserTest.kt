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

package android.tools.parsers.wm

import android.tools.Cache
import android.tools.Timestamp
import android.tools.Timestamps
import android.tools.testutils.CleanFlickerEnvironmentRule
import android.tools.testutils.readAsset
import android.tools.traces.parsers.perfetto.Row
import android.tools.traces.parsers.perfetto.TraceProcessorSession
import android.tools.traces.parsers.perfetto.TransitionsTraceParser
import android.tools.traces.wm.ShellTransitionData
import android.tools.traces.wm.Transition
import android.tools.traces.wm.WmTransitionData
import com.google.common.truth.Truth
import org.junit.Assume
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test

/** Tests for [TransitionsTraceParser] */
class TransitionsTraceParserTest {
    @Before
    fun before() {
        Assume.assumeTrue(android.tracing.Flags.perfettoTransitionTracing())
        Cache.clear()
    }

    @Test
    fun canParse() {
        TraceProcessorSession.loadPerfettoTrace(readAsset("transitions.perfetto-trace")) { session
            ->
            val trace = TransitionsTraceParser().parse(session)
            Truth.assertWithMessage("Unable to parse dump").that(trace.entries).hasSize(4)
        }
    }

    @Test
    fun canParseTimestamps() {
        TraceProcessorSession.loadPerfettoTrace(readAsset("transitions.perfetto-trace")) { session
            ->
            val trace = TransitionsTraceParser().parse(session)
            Truth.assertWithMessage("Has all timestamps")
                .that(trace.entries.first().timestamp.hasAllTimestamps)
                .isTrue()
            Truth.assertWithMessage("Has unix timestamps")
                .that(trace.entries.first().timestamp.unixNanos)
                .isEqualTo(1700573425428925648L)
            Truth.assertWithMessage("Has elapsed timestamps")
                .that(trace.entries.first().timestamp.elapsedNanos)
                .isEqualTo(479583450794L)
            Truth.assertWithMessage("Has uptime timestamps")
                .that(trace.entries.first().timestamp.systemUptimeNanos)
                .isEqualTo(479583450997L)
        }
    }

    @Test
    fun filtersTransitionWithDispatchTimeBeforeFrom() {
        val from = Timestamps.from(elapsedNanos = 100)
        val to = Timestamps.from(elapsedNanos = 200)
        val transition =
            createMockTransition(
                id = 1,
                dispatchTime = Timestamps.from(elapsedNanos = 99), // Before 'from'
                finishTime = Timestamps.from(elapsedNanos = 150),
            )
        val parser = MockableTransitionsTraceParser(listOf(transition))
        val trace = parser.parse(MockTraceProcessorSession(), from, to)
        Truth.assertThat(trace.entries).isEmpty()
    }

    @Test
    fun filtersTransitionWithSendTimeBeforeFrom() {
        val from = Timestamps.from(elapsedNanos = 100)
        val to = Timestamps.from(elapsedNanos = 200)
        val transition =
            createMockTransition(
                id = 1,
                sendTime = Timestamps.from(elapsedNanos = 99), // Before 'from'
                finishTime = Timestamps.from(elapsedNanos = 150),
            )
        val parser = MockableTransitionsTraceParser(listOf(transition))
        val trace = parser.parse(MockTraceProcessorSession(), from, to)
        Truth.assertThat(trace.entries).isEmpty()
    }

    @Test
    fun filtersTransitionWithFinishTimeAfterTo() {
        val from = Timestamps.from(elapsedNanos = 100)
        val to = Timestamps.from(elapsedNanos = 200)
        val transition =
            createMockTransition(
                id = 1,
                dispatchTime = Timestamps.from(elapsedNanos = 110),
                finishTime = Timestamps.from(elapsedNanos = 201), // After 'to'
            )
        val parser = MockableTransitionsTraceParser(listOf(transition))
        val trace = parser.parse(MockTraceProcessorSession(), from, to)
        Truth.assertThat(trace.entries).isEmpty()
    }

    @Test
    fun filtersTransitionWithWmAbortTimeAfterTo() {
        val from = Timestamps.from(elapsedNanos = 100)
        val to = Timestamps.from(elapsedNanos = 200)
        val transition =
            createMockTransition(
                id = 1,
                dispatchTime = Timestamps.from(elapsedNanos = 110),
                wmAbortTime = Timestamps.from(elapsedNanos = 201), // After 'to'
            )
        val parser = MockableTransitionsTraceParser(listOf(transition))
        val trace = parser.parse(MockTraceProcessorSession(), from, to)
        Truth.assertThat(trace.entries).isEmpty()
    }

    @Test
    fun filtersTransitionWithShellAbortTimeAfterTo() {
        val from = Timestamps.from(elapsedNanos = 100)
        val to = Timestamps.from(elapsedNanos = 200)
        val transition =
            createMockTransition(
                id = 1,
                dispatchTime = Timestamps.from(elapsedNanos = 110),
                shellAbortTime = Timestamps.from(elapsedNanos = 201), // After 'to'
            )
        val parser = MockableTransitionsTraceParser(listOf(transition))
        val trace = parser.parse(MockTraceProcessorSession(), from, to)
        Truth.assertThat(trace.entries).isEmpty()
    }

    @Test
    fun filtersTransitionWithMergeTimeAfterTo() {
        val from = Timestamps.from(elapsedNanos = 100)
        val to = Timestamps.from(elapsedNanos = 200)
        val transition =
            createMockTransition(
                id = 1,
                dispatchTime = Timestamps.from(elapsedNanos = 110),
                mergeTime = Timestamps.from(elapsedNanos = 201), // After 'to'
            )
        val parser = MockableTransitionsTraceParser(listOf(transition))
        val trace = parser.parse(MockTraceProcessorSession(), from, to)
        Truth.assertThat(trace.entries).isEmpty()
    }

    @Test
    fun includesValidTransition() {
        val from = Timestamps.from(elapsedNanos = 100)
        val to = Timestamps.from(elapsedNanos = 200)
        val transition = createMockTransition(id = 1, dispatchTime = from, finishTime = to)
        val parser = MockableTransitionsTraceParser(listOf(transition))
        val trace = parser.parse(MockTraceProcessorSession(), from, to)
        Truth.assertThat(trace.entries).containsExactly(transition)
    }

    private class MockTraceProcessorSession : TraceProcessorSession {
        override fun <T> query(sql: String, predicate: (List<Row>) -> T): T {
            // Not used in tests
            error("Not implemented")
        }
    }

    // Simple mock implementation for testing purposes
    private class MockableTransitionsTraceParser(private val mockEntries: List<Transition>) :
        TransitionsTraceParser() {
        // Override getEntries to return the mocked list directly
        override fun getEntries(input: TraceProcessorSession): List<Transition> {
            return mockEntries
        }
    }

    private fun createMockTransition(
        id: Int,
        sendTime: Timestamp? = null,
        dispatchTime: Timestamp? = null,
        finishTime: Timestamp? = null,
        wmAbortTime: Timestamp? = null,
        shellAbortTime: Timestamp? = null,
        mergeTime: Timestamp? = null,
    ): Transition {
        return Transition(
            id = id,
            wmData =
                WmTransitionData(
                    sendTime = sendTime,
                    finishTime = finishTime,
                    abortTime = wmAbortTime,
                ),
            shellData =
                ShellTransitionData(
                    dispatchTime = dispatchTime,
                    abortTime = shellAbortTime,
                    mergeTime = mergeTime,
                ),
        )
    }

    companion object {
        @ClassRule @JvmField val ENV_CLEANUP = CleanFlickerEnvironmentRule()
    }
}

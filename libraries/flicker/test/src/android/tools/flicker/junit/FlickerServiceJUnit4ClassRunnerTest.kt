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

package android.tools.flicker.junit

import android.app.Instrumentation
import android.os.Bundle
import android.tools.Scenario
import android.tools.Timestamps
import android.tools.device.apphelpers.BrowserAppHelper
import android.tools.flicker.AssertionInvocationGroup
import android.tools.flicker.FlickerConfig
import android.tools.flicker.ScenarioInstance
import android.tools.flicker.annotation.Debug
import android.tools.flicker.annotation.ExpectedScenarios
import android.tools.flicker.annotation.FlickerConfigProvider
import android.tools.flicker.assertions.FlickerTest
import android.tools.flicker.assertors.AssertionTemplate
import android.tools.flicker.config.FlickerConfig
import android.tools.flicker.config.FlickerConfigEntry
import android.tools.flicker.config.ScenarioId
import android.tools.flicker.extractors.ScenarioExtractor
import android.tools.flicker.extractors.TraceSlice
import android.tools.io.Reader
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import java.time.Instant
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runner.notification.RunNotifier
import org.junit.runners.model.Statement
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

class FlickerServiceJUnit4ClassRunnerTest {
    @Test
    fun runsTheTestRules() {
        testRuleRunCount = 0
        val runner = FlickerServiceJUnit4ClassRunner(SimpleTest::class.java)
        runner.run(RunNotifier())

        Truth.assertThat(testRuleRunCount).isEqualTo(1)
    }

    @Test
    fun runsTheTestRulesAtTheRightTime() {
        var testRuleStartTs: Instant? = null
        var testRuleEndTs: Instant? = null
        var testStateTs: Instant? = null
        var testRuleExecutionCount = 0
        onTestRuleStart = Runnable {
            testRuleExecutionCount++
            testRuleStartTs = Instant.now()
        }
        onTestRuleEnd = Runnable { testRuleEndTs = Instant.now() }
        onTestStart = Runnable { testStateTs = Instant.now() }

        val runner = FlickerServiceJUnit4ClassRunner(SimpleTest::class.java)
        runner.run(RunNotifier())

        Truth.assertWithMessage("Test rule start should run before test block")
            .that(testRuleStartTs)
            .isLessThan(testStateTs)
        Truth.assertWithMessage("Test rule end should run after test block")
            .that(testStateTs)
            .isLessThan(testRuleEndTs)

        Truth.assertWithMessage("Test rule ran the wrong number of times")
            .that(testRuleExecutionCount)
            .isEqualTo(1)
    }

    @Test
    fun skipsNonBlockingTestsIfRequested() {
        val arguments = Bundle()
        arguments.putString(Scenario.FAAS_BLOCKING, "true")
        val runner = FlickerServiceJUnit4ClassRunner(SimpleTest::class.java, arguments = arguments)
        val notifier = Mockito.mock(RunNotifier::class.java)
        runner.run(notifier)

        Truth.assertThat(runner.testCount()).isAtLeast(2)
        Mockito.verify(notifier, Mockito.atLeast(1))
            .fireTestStarted(ArgumentMatchers.argThat { it.methodName.contains("FaaS") })
        Mockito.verify(notifier, Mockito.atLeast(1))
            .fireTestAssumptionFailed(
                ArgumentMatchers.argThat { it.description.methodName.contains("FaaS") }
            )
        Mockito.verify(notifier, Mockito.atLeast(1))
            .fireTestFinished(ArgumentMatchers.argThat { it.methodName.contains("FaaS") })
    }

    @Test
    fun runBlockingTestsIfRequested() {
        val arguments = Bundle()
        arguments.putString(Scenario.FAAS_BLOCKING, "false")
        val runner = FlickerServiceJUnit4ClassRunner(SimpleTest::class.java, arguments = arguments)
        val notifier = Mockito.mock(RunNotifier::class.java)
        runner.run(notifier)

        Truth.assertThat(runner.testCount()).isAtLeast(2)
        Mockito.verify(notifier, Mockito.atLeast(2))
            .fireTestStarted(ArgumentMatchers.argThat { it.methodName.contains("FaaS") })
        Mockito.verify(notifier, Mockito.never())
            .fireTestAssumptionFailed(
                ArgumentMatchers.argThat { it.description.methodName.contains("FaaS") }
            )
        Mockito.verify(notifier, Mockito.atLeast(2))
            .fireTestFinished(ArgumentMatchers.argThat { it.methodName.contains("FaaS") })
    }

    @Test
    fun reportsTestRuleFailureInTest() {
        var testRan = false
        onTestRuleStart = Runnable { Assert.fail("Test rule failed") }
        onTestStart = Runnable { testRan = true }

        val runner = FlickerServiceJUnit4ClassRunner(SimpleTest::class.java)
        val notifier = Mockito.mock(RunNotifier::class.java)
        runner.run(notifier)

        Truth.assertThat(testRan).isFalse()

        Truth.assertThat(runner.testCount()).isEqualTo(1)
        Mockito.verify(notifier)
            .fireTestFailure(
                ArgumentMatchers.argThat {
                    it.description.methodName.equals("test") &&
                        it.message.contains("Test rule failed")
                }
            )
    }

    @Test
    fun usesLocalTraceWithDebugAnnotation() {
        val runner = FlickerServiceJUnit4ClassRunner(DebugTest::class.java)
        val notifier = Mockito.mock(RunNotifier::class.java)
        runner.run(notifier)

        Truth.assertThat(runner.testCount()).isEqualTo(3)
        Mockito.verify(notifier, Mockito.never()).fireTestFailure(ArgumentMatchers.any())

        for (expectedMethod in
            listOf(
                "test",
                "MY_CUSTOM_SCENARIO::myBlockingAssertion",
                "DetectedExpectedScenarios",
            )) {
            Mockito.verify(notifier, Mockito.times(1))
                .fireTestStarted(
                    ArgumentMatchers.argThat { it.methodName.contains(expectedMethod) }
                )
        }
    }

    /** Below are all the mock test classes uses for testing purposes */
    @RunWith(FlickerServiceJUnit4ClassRunner::class)
    open class SimpleTest {
        val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
        private val testApp: BrowserAppHelper = BrowserAppHelper(instrumentation)

        @get:Rule
        val myRule = TestRule { base, _ ->
            object : Statement() {
                @Throws(Throwable::class)
                override fun evaluate() {
                    onTestRuleStart?.run()
                    testRuleRunCount++
                    base.evaluate()
                    onTestRuleEnd?.run()
                }
            }
        }

        @Test
        @ExpectedScenarios(["MY_CUSTOM_SCENARIO"])
        fun test() {
            onTestStart?.run()
            testApp.open()
        }

        companion object {
            @FlickerConfigProvider
            @JvmStatic
            fun flickerConfigProvider(): FlickerConfig {
                return FlickerConfig()
                    .use(
                        FlickerConfigEntry(
                            scenarioId = ScenarioId("MY_CUSTOM_SCENARIO"),
                            extractor =
                                object : ScenarioExtractor {
                                    override fun extract(reader: Reader): List<TraceSlice> {
                                        return listOf(
                                            TraceSlice(Timestamps.min(), Timestamps.max())
                                        )
                                    }
                                },
                            assertions =
                                mapOf(
                                    object : AssertionTemplate("myBlockingAssertion") {
                                        override fun doEvaluate(
                                            scenarioInstance: ScenarioInstance,
                                            flicker: FlickerTest,
                                        ) {
                                            flicker.assertWm {
                                                // Random test
                                                visibleWindowsShownMoreThanOneConsecutiveEntry()
                                            }
                                        }
                                    } to AssertionInvocationGroup.BLOCKING,
                                    object : AssertionTemplate("myNonBlockingAssertion") {
                                        override fun doEvaluate(
                                            scenarioInstance: ScenarioInstance,
                                            flicker: FlickerTest,
                                        ) {
                                            flicker.assertWm {
                                                // Random test
                                                visibleWindowsShownMoreThanOneConsecutiveEntry()
                                            }
                                        }
                                    } to AssertionInvocationGroup.NON_BLOCKING,
                                ),
                            enabled = true,
                        )
                    )
            }
        }
    }

    /** Below are all the mock test classes uses for testing purposes */
    @RunWith(FlickerServiceJUnit4ClassRunner::class)
    open class DebugTest {
        val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
        private val testApp: BrowserAppHelper = BrowserAppHelper(instrumentation)

        @Test
        @ExpectedScenarios(["MY_CUSTOM_SCENARIO"])
        @Debug("testdata/quickswitch.winscope.zip")
        fun test() {
            onTestStart?.run()
            testApp.open()
        }

        companion object {
            @FlickerConfigProvider
            @JvmStatic
            fun flickerConfigProvider(): FlickerConfig {
                return FlickerConfig()
                    .use(
                        FlickerConfigEntry(
                            scenarioId = ScenarioId("MY_CUSTOM_SCENARIO"),
                            extractor =
                                object : ScenarioExtractor {
                                    override fun extract(reader: Reader): List<TraceSlice> {
                                        return listOf(
                                            TraceSlice(Timestamps.min(), Timestamps.max())
                                        )
                                    }
                                },
                            assertions =
                                mapOf(
                                    object : AssertionTemplate("myBlockingAssertion") {
                                        override fun doEvaluate(
                                            scenarioInstance: ScenarioInstance,
                                            flicker: FlickerTest,
                                        ) {
                                            // Check to make sure we are running this assertion on
                                            // the debug trace
                                            flicker.assertLayersStart {
                                                Truth.assertThat(this.timestamp.unixNanos)
                                                    .isEqualTo(1743439123983024119)
                                            }
                                        }
                                    } to AssertionInvocationGroup.BLOCKING
                                ),
                            enabled = true,
                        )
                    )
            }
        }
    }

    companion object {
        var testRuleRunCount = 0

        var onTestRuleStart: Runnable? = null
        var onTestStart: Runnable? = null
        var onTestRuleEnd: Runnable? = null
    }
}

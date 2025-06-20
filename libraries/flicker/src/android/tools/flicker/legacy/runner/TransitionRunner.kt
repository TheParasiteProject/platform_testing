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

package android.tools.flicker.legacy.runner

import android.tools.flicker.legacy.FlickerTestData
import android.tools.flicker.rules.ArtifactSaverRule
import android.tools.traces.io.IResultData
import android.tools.traces.io.ResultWriter
import android.tools.withTracing
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description

/**
 * Transition runner that executes a default device setup (based on [testIdentifier]) as well as the
 * flicker setup/transition/teardown
 */
class TransitionRunner(
    private val testIdentifier: String,
    private val setupRules: List<TestRule>,
    private val resultWriter: ResultWriter,
) {
    /** Executes [flicker] transition and returns the result */
    fun execute(flicker: FlickerTestData, description: Description?): IResultData {
        return withTracing("TransitionRunner:execute") {
            resultWriter.withName(testIdentifier).withOutputDir(flicker.outputDir)

            val ruleChain = buildTestRuleChain(flicker)
            try {
                ruleChain.apply(null, description).evaluate()
                resultWriter.setRunComplete()
            } catch (e: Throwable) {
                resultWriter.setRunFailed(e)
            }
            resultWriter.write()
        }
    }

    /**
     * Create the default flicker test setup rules. In order:
     * - unlock device
     * - change orientation
     * - change navigation mode
     * - launch an app
     * - remove all apps
     * - go home
     *
     * (b/186740751) An app should be launched because, after changing the navigation mode, the
     * first app launch is handled as a screen size change (similar to a rotation), this causes
     * different problems during testing (e.g. IME now shown on app launch)
     */
    private fun buildTestRuleChain(flicker: FlickerTestData): RuleChain {
        val errorRule = ArtifactSaverRule()

        val rules =
            setupRules +
                listOf(
                    TraceMonitorRule(
                        flicker.traceMonitors,
                        testIdentifier,
                        flicker.wmHelper,
                        resultWriter,
                    ),
                    *flicker.rules.toTypedArray(),
                    SetupTeardownRule(flicker, resultWriter, testIdentifier),
                    TransitionExecutionRule(flicker, resultWriter, testIdentifier),
                )

        return rules.foldIndexed(RuleChain.outerRule(errorRule)) { index, chain, rule ->
            chain.around(rule).let {
                if (index != rules.lastIndex) {
                    it.around(errorRule)
                } else {
                    it
                }
            }
        }
    }
}

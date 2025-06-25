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

package android.tools.flicker

import android.app.Instrumentation
import android.device.collectors.BaseMetricListener
import android.device.collectors.DataRecord
import android.tools.FLICKER_TAG
import android.tools.flicker.assertions.AssertionResult
import android.tools.flicker.config.FlickerServiceConfig
import android.tools.flicker.config.ScenarioId
import android.tools.io.Reader
import android.tools.io.ResultArtifactDescriptor
import android.tools.io.RunStatus
import android.tools.io.TraceType
import android.tools.traces.getDefaultFlickerOutputDir
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.android.internal.annotations.VisibleForTesting
import java.io.File
import org.junit.runner.Description
import org.junit.runner.Result
import org.junit.runner.notification.Failure

/**
 * Collects all the Flicker Service's metrics which are then uploaded for analysis and monitoring to
 * the CrystalBall database.
 */
class FlickerServiceResultsCollector
@JvmOverloads
constructor(
    private val tracesCollector: TracesCollector,
    private val flickerService: FlickerService =
        FlickerService(FlickerConfig().use(FlickerServiceConfig.DEFAULT)),
    instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation(),
    private val collectMetricsPerTest: Boolean = true,
    private val reportOnlyForPassingTests: Boolean = true,
) : BaseMetricListener(), IFlickerServiceResultsCollector {
    private var hasFailedTest = false
    private var testSkipped = false

    private val _executionErrors = mutableListOf<Throwable>()
    override val executionErrors
        get() = _executionErrors

    @VisibleForTesting val assertionResults = mutableListOf<AssertionResult>()

    @VisibleForTesting
    val assertionResultsByTest = mutableMapOf<Description, Collection<AssertionResult>>()

    @VisibleForTesting
    val detectedScenariosByTest = mutableMapOf<Description, Collection<ScenarioId>>()

    private var testRunIdentifier: String = ""
    private var testIdentifier: String = ""

    init {
        setInstrumentation(instrumentation)
    }

    override fun onTestRunStart(runData: DataRecord, description: Description) {
        errorReportingBlock {
            tracesCollector.cleanup() // Cleanup any trace archives from previous runs

            Log.i(LOG_TAG, "onTestRunStart :: collectMetricsPerTest = $collectMetricsPerTest")
            val key = description.toString()
            if (!collectMetricsPerTest) {
                hasFailedTest = false
                testRunIdentifier = key
                tracesCollector.start(key)
            }
        }
    }

    override fun onTestStart(testData: DataRecord, description: Description) {
        errorReportingBlock {
            Log.i(LOG_TAG, "onTestStart :: collectMetricsPerTest = $collectMetricsPerTest")
            if (collectMetricsPerTest) {
                hasFailedTest = false
                val key = "${description.testClass?.canonicalName}#${description.methodName}"
                testIdentifier = key
                tracesCollector.start(key)
            }
            testSkipped = false
        }
    }

    override fun onTestFail(testData: DataRecord, description: Description, failure: Failure) {
        errorReportingBlock {
            Log.i(LOG_TAG, "onTestFail")
            hasFailedTest = true
        }
    }

    override fun testAssumptionFailure(failure: Failure?) {
        errorReportingBlock {
            Log.i(LOG_TAG, "testAssumptionFailure")
            testSkipped = true
        }
    }

    override fun testSkipped(description: Description) {
        errorReportingBlock {
            Log.i(LOG_TAG, "testSkipped")
            testSkipped = true
        }
    }

    override fun onTestEnd(testData: DataRecord, description: Description) {
        Log.i(LOG_TAG, "onTestEnd :: collectMetricsPerTest = $collectMetricsPerTest")
        if (collectMetricsPerTest) {
            val results = errorReportingBlock {
                Log.i(LOG_TAG, "Stopping trace collection")
                val reader = tracesCollector.stop()
                Log.i(LOG_TAG, "Stopped trace collection")

                if (reportOnlyForPassingTests && hasFailedTest) {
                    return@errorReportingBlock null
                }

                if (testSkipped) {
                    return@errorReportingBlock null
                }

                return@errorReportingBlock collectFlickerMetrics(testData, reader, description)
            }

            require(testIdentifier.isNotEmpty()) { "testIdentifier should not be empty" }
            reportFlickerServiceStatus(testData, results, testIdentifier, testData)
        }
    }

    override fun onTestRunEnd(runData: DataRecord, result: Result) {
        Log.i(LOG_TAG, "onTestRunEnd :: collectMetricsPerTest = $collectMetricsPerTest")
        if (!collectMetricsPerTest) {
            val results = errorReportingBlock {
                Log.i(LOG_TAG, "Stopping trace collection")
                val reader = tracesCollector.stop()
                Log.i(LOG_TAG, "Stopped trace collection")

                if (reportOnlyForPassingTests && hasFailedTest) {
                    return@errorReportingBlock null
                }

                return@errorReportingBlock collectFlickerMetrics(runData, reader)
            }

            require(testRunIdentifier.isNotEmpty()) { "testRunIdentifier should not be empty" }
            reportFlickerServiceStatus(runData, results, testRunIdentifier, runData)
        }
    }

    private fun collectFlickerMetrics(
        dataRecord: DataRecord,
        reader: Reader,
        description: Description? = null,
    ): Collection<AssertionResult>? {
        return errorReportingBlock {
            return@errorReportingBlock try {
                Log.i(LOG_TAG, "Processing traces")
                val scenarios = flickerService.detectScenarios(reader)
                val results = scenarios.flatMap { it.generateAssertions() }.map { it.execute() }
                reader.artifacts.forEach { it.updateStatus(RunStatus.RUN_EXECUTED) }
                Log.i(LOG_TAG, "Got ${results.size} results")
                assertionResults.addAll(results)
                if (description != null) {
                    require(assertionResultsByTest[description] == null) {
                        "Test description ($description) already contains flicker assertion results"
                    }
                    require(detectedScenariosByTest[description] == null) {
                        "Test description ($description) already contains detected scenarios"
                    }
                    assertionResultsByTest[description] = results
                    detectedScenariosByTest[description] = scenarios.map { it.type }.distinct()
                }
                if (results.any { it.status == AssertionResult.Status.FAIL }) {
                    reader.updateStatus(RunStatus.ASSERTION_FAILED)
                } else {
                    reader.updateStatus(RunStatus.ASSERTION_SUCCESS)
                }

                Log.v(LOG_TAG, "Adding metric $FLICKER_ASSERTIONS_COUNT_KEY = ${results.size}")
                dataRecord.addStringMetric(FLICKER_ASSERTIONS_COUNT_KEY, "${results.size}")

                val aggregatedResults = processFlickerResults(results)
                collectMetrics(dataRecord, aggregatedResults)

                results
            } finally {
                // We want to report the Perfetto trace which contains all the relevant data
                // CB will pull the file specified by the WINSCOPE_FILE_PATH_KEY metric
                val perfettoDescriptor = ResultArtifactDescriptor(TraceType.PERFETTO)
                require(reader.artifacts.count { it.hasTrace(perfettoDescriptor) } <= 1) {
                    "Expected at most a single artifact with a Perfetto trace..."
                }
                val targetArtifact =
                    reader.artifacts.firstOrNull { it.hasTrace(perfettoDescriptor) }
                        // Fallback to the first artifact if no Perfetto trace is found
                        ?: reader.artifacts.first()

                Log.v(
                    LOG_TAG,
                    "Adding metric $WINSCOPE_FILE_PATH_KEY = ${targetArtifact.absolutePath}",
                )
                dataRecord.addStringMetric(WINSCOPE_FILE_PATH_KEY, targetArtifact.absolutePath)
            }
        }
    }

    private fun processFlickerResults(
        results: Collection<AssertionResult>
    ): Map<String, AggregatedFlickerResult> {
        val aggregatedResults = mutableMapOf<String, AggregatedFlickerResult>()
        for (result in results) {
            val key = getKeyForAssertionResult(result)
            if (!aggregatedResults.containsKey(key)) {
                aggregatedResults[key] = AggregatedFlickerResult()
            }
            aggregatedResults[key]!!.addResult(result)
        }
        return aggregatedResults
    }

    private fun collectMetrics(
        data: DataRecord,
        aggregatedResults: Map<String, AggregatedFlickerResult>,
    ) {
        val it = aggregatedResults.entries.iterator()

        while (it.hasNext()) {
            val (key, aggregatedResult) = it.next()
            aggregatedResult.results.forEachIndexed { index, result ->
                if (result.status == AssertionResult.Status.ASSUMPTION_VIOLATION) {
                    // skip
                    return@forEachIndexed
                }

                val resultStatus = if (result.status == AssertionResult.Status.PASS) 0 else 1
                Log.v(LOG_TAG, "Adding metric ${key}_$index = $resultStatus")
                data.addStringMetric("${key}_$index", "$resultStatus")
            }
        }
    }

    private fun <T> errorReportingBlock(function: () -> T): T? {
        return try {
            function()
        } catch (e: Throwable) {
            Log.e(FLICKER_TAG, "Error executing in FlickerServiceResultsCollector", e)
            _executionErrors.add(e)
            null
        }
    }

    override fun resultsForTest(description: Description): Collection<AssertionResult> {
        val resultsForTest = assertionResultsByTest[description]
        requireNotNull(resultsForTest) { "No results set for test $description" }
        return resultsForTest
    }

    override fun detectedScenariosForTest(description: Description): Collection<ScenarioId> {
        val scenariosForTest = detectedScenariosByTest[description]
        requireNotNull(scenariosForTest) { "No detected scenarios set for test $description" }
        return scenariosForTest
    }

    private fun reportFlickerServiceStatus(
        record: DataRecord,
        results: Collection<AssertionResult>?,
        testIdentifier: String,
        dataRecord: DataRecord,
    ) {
        val status = if (executionErrors.isEmpty()) OK_STATUS_CODE else EXECUTION_ERROR_STATUS_CODE
        record.addStringMetric(FAAS_STATUS_KEY, status.toString())

        val maxLineLength = 120
        val statusFile = createFlickerServiceStatusFile(testIdentifier)
        val flickerResultString = buildString {
            appendLine(
                "FAAS_STATUS: ${if (executionErrors.isEmpty()) "OK" else "EXECUTION_ERROR"}\n"
            )

            appendLine("EXECUTION ERRORS:\n")
            if (executionErrors.isEmpty()) {
                appendLine("None".prependIndent())
            } else {
                appendLine(
                    executionErrors
                        .joinToString("\n\n${"-".repeat(maxLineLength / 2)}\n\n") {
                            it.stackTraceToString()
                        }
                        .prependIndent()
                )
            }

            appendLine()
            appendLine("FLICKER RESULTS:\n")
            val executionErrorsString =
                buildString {
                        results?.forEach {
                            append("${it.name} (${it.stabilityGroup}) :: ")
                            append("${it.status}\n")
                            appendLine(
                                it.assertionErrors
                                    .joinToString("\n${"-".repeat(maxLineLength / 2)}\n\n") { error
                                        ->
                                        error.message
                                    }
                                    .prependIndent()
                            )
                        }
                    }
                    .prependIndent()
            appendLine(executionErrorsString)
        }

        statusFile.writeText(flickerResultString.replace(Regex("(.{$maxLineLength})"), "$1\n"))

        Log.v(LOG_TAG, "Adding metric $FAAS_RESULTS_FILE_PATH_KEY = ${statusFile.absolutePath}")
        dataRecord.addStringMetric(FAAS_RESULTS_FILE_PATH_KEY, statusFile.absolutePath)
    }

    private fun createFlickerServiceStatusFile(testIdentifier: String): File {
        val fileName = "FAAS_RESULTS_$testIdentifier"

        val outputDir = getDefaultFlickerOutputDir()
        // Ensure output directory exists
        outputDir.mkdirs()
        return outputDir.resolve(fileName)
    }

    companion object {
        // Unique prefix to add to all FaaS metrics to identify them
        const val FAAS_METRICS_PREFIX = "FAAS"
        private const val LOG_TAG = "$FLICKER_TAG-Collector"
        const val FAAS_STATUS_KEY = "${FAAS_METRICS_PREFIX}_STATUS"
        const val WINSCOPE_FILE_PATH_KEY = "winscope_file_path"
        const val FAAS_RESULTS_FILE_PATH_KEY = "faas_results_file_path"
        const val FLICKER_ASSERTIONS_COUNT_KEY = "flicker_assertions_count"
        const val OK_STATUS_CODE = 0
        const val EXECUTION_ERROR_STATUS_CODE = 1

        fun getKeyForAssertionResult(result: AssertionResult): String {
            return "$FAAS_METRICS_PREFIX::${result.name}"
        }

        class AggregatedFlickerResult {
            val results = mutableListOf<AssertionResult>()
            var failures = 0
            var passes = 0
            var assumptionViolations = 0
            val errors = mutableListOf<String>()
            var invocationGroup: AssertionInvocationGroup? = null

            fun addResult(result: AssertionResult) {
                results.add(result)

                when (result.status) {
                    AssertionResult.Status.PASS -> passes++
                    AssertionResult.Status.FAIL -> {
                        failures++
                        result.assertionErrors.forEach { errors.add(it.message) }
                    }
                    AssertionResult.Status.ASSUMPTION_VIOLATION -> assumptionViolations++
                }

                if (invocationGroup == null) {
                    invocationGroup = result.stabilityGroup
                }

                if (invocationGroup != result.stabilityGroup) {
                    error("Unexpected assertion group mismatch")
                }
            }
        }
    }
}

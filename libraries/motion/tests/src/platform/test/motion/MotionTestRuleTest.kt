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

package platform.test.motion

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.regex.Pattern
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import platform.test.motion.golden.SupplementalFrameId
import platform.test.motion.golden.TimeSeries
import platform.test.motion.golden.TimestampFrameId
import platform.test.motion.testing.JsonSubject.Companion.assertThat
import platform.test.motion.testing.createGoldenPathManager

@RunWith(AndroidJUnit4::class)
class MotionTestRuleTest {

    private val goldenPathManager = createGoldenPathManager("assets")

    private val subject = MotionTestRule(Unit, goldenPathManager)

    val goldenExportDirectory =
        if (MotionTestRule.isRobolectricRuntime()) File("/tmp/motion")
        else File(goldenPathManager.deviceLocalPath)

    private val emptyRecordedMotion =
        RecordedMotion(
            "FooClass",
            "bar_test",
            TimeSeries(listOf(), emptyList()),
            screenshots = null,
        )

    @Test
    fun readGoldenTimeSeries_withExistingGolden_returnsParsedJson() {
        assertThat(subject.readGoldenTimeSeries("empty_timeseries", emptyMap()))
            .isEqualTo(TimeSeries(listOf(), emptyList()))
    }

    @Test
    fun readGoldenTimeSeries_withUnavailableGolden_throwsGoldenNotFoundException() {
        val exception =
            assertThrows(GoldenNotFoundException::class.java) {
                subject.readGoldenTimeSeries("no_golden", emptyMap())
            }
        assertThat(exception.missingGoldenFile).endsWith("no_golden.json")
    }

    @Test
    fun readGoldenTimeSeries_withInvalidJsonFile_throwsJSONException() {
        assertThrows(JSONException::class.java) {
            subject.readGoldenTimeSeries("invalid_json_data", emptyMap())
        }
    }

    @Test
    fun writeGeneratedTimeSeries_createsFile() {
        subject.writeGeneratedTimeSeries(
            "updated_golden",
            emptyRecordedMotion,
            TimeSeriesVerificationResult.PASSED,
        )
        val expectedFile = goldenExportDirectory.resolve("FooClass/updated_golden.actual.json")
        assertThat(expectedFile.exists()).isTrue()
    }

    @Test
    fun writeGeneratedTimeSeries_writesTimeSeries() {
        subject.writeGeneratedTimeSeries(
            "updated_golden",
            emptyRecordedMotion,
            TimeSeriesVerificationResult.PASSED,
        )
        val expectedFile = goldenExportDirectory.resolve("FooClass/updated_golden.actual.json")

        val fileContentsWithoutMetadata =
            JSONObject(expectedFile.readText()).apply { remove("//metadata") }

        assertThat(fileContentsWithoutMetadata)
            .isEqualTo(
                JSONObject().apply {
                    put("frame_ids", JSONArray())
                    put("features", JSONArray())
                }
            )
    }

    @Test
    fun writeGeneratedTimeSeries_includesMetadata() {
        subject.writeGeneratedTimeSeries(
            "updated_golden",
            emptyRecordedMotion,
            TimeSeriesVerificationResult.MISSING_REFERENCE,
        )
        val expectedFile = goldenExportDirectory.resolve("FooClass/updated_golden.actual.json")
        val fileContents = JSONObject(expectedFile.readText())
        val metadataJson = fileContents.get("//metadata") as JSONObject

        assertThat(metadataJson.has("deviceLocalPath")).isTrue()
        val deviceLocalPath = metadataJson.remove("deviceLocalPath") as String
        if (MotionTestRule.isRobolectricRuntime()) {
            assertThat(deviceLocalPath).matches("/tmp/motion")
        } else {
            assertThat(deviceLocalPath).matches(DeviceLocalPathPattern)
        }

        assertThat(metadataJson)
            .isEqualTo(
                JSONObject().apply {
                    put("goldenRepoPath", "assets/updated_golden.json")
                    put("goldenIdentifier", "updated_golden")
                    put("testClassName", "FooClass")
                    put("testMethodName", "bar_test")
                    put("result", "MISSING_REFERENCE")
                }
            )
    }

    @Test
    fun writeGeneratedTimeSeries_withScreenshots_writesVideoAndIncludesMetadata() {
        val w = 100
        val h = 200
        val recordedMotion =
            RecordedMotion(
                "FooClass",
                "bar_test",
                TimeSeries(
                    listOf(TimestampFrameId(0), TimestampFrameId(16), SupplementalFrameId("after")),
                    listOf(),
                ),
                listOf(
                    mockScreenshot(Color.RED, w, h),
                    mockScreenshot(Color.GREEN, w, h),
                    mockScreenshot(Color.BLUE, w, h),
                ),
            )
        subject.writeGeneratedTimeSeries(
            "updated_golden",
            recordedMotion,
            TimeSeriesVerificationResult.MISSING_REFERENCE,
        )
        if (recordedMotion.videoRenderer != null) {
            val expectedVideo =
                File(goldenPathManager.deviceLocalPath)
                    .resolve("FooClass/updated_golden.actual.mp4")
            assertThat(expectedVideo.exists()).isTrue()
        }

        val expectedFile = goldenExportDirectory.resolve("FooClass/updated_golden.actual.json")
        val fileContents = JSONObject(expectedFile.readText())
        val metadataJson = fileContents.get("//metadata") as JSONObject

        assertThat(metadataJson.has("deviceLocalPath")).isTrue()
        val deviceLocalPath = metadataJson.remove("deviceLocalPath") as String
        if (!MotionTestRule.isRobolectricRuntime()) {
            assertThat(deviceLocalPath).matches(DeviceLocalPathPattern)
        } else {
            assertThat(deviceLocalPath).matches("/tmp/motion")
        }
        assertThat(metadataJson)
            .isEqualTo(
                JSONObject().apply {
                    put("goldenRepoPath", "assets/updated_golden.json")
                    put("goldenIdentifier", "updated_golden")
                    put("testClassName", "FooClass")
                    put("testMethodName", "bar_test")
                    put("result", "MISSING_REFERENCE")
                    if (recordedMotion.videoRenderer != null) {
                        put("videoLocation", "FooClass/updated_golden.actual.mp4")
                    }
                }
            )
    }

    @Test
    fun writeGeneratedTimeSeries_withInvalidIdentifier_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            subject.writeGeneratedTimeSeries(
                "invalid identifier!",
                emptyRecordedMotion,
                TimeSeriesVerificationResult.PASSED,
            )
        }
    }

    private fun mockScreenshot(
        color: Int,
        width: Int = 400,
        height: Int = 200,
        bitmapConfig: Bitmap.Config = Bitmap.Config.ARGB_8888,
    ) = Bitmap.createBitmap(width, height, bitmapConfig).also { Canvas(it).drawColor(color) }

    companion object {
        // The canonical path is `/data/user/0/platform.test.motion.tests/files/goldens`, however:
        // - Running from gradle, the package name ends with `.test` (as opposed `.tests`)
        // - Running from HSUM, the user might be different than 0 (10 usually)
        val DeviceLocalPathPattern: Pattern =
            Pattern.compile(
                "/data/user/(?<userId>\\d+)/" +
                    "platform\\.test\\.motion\\.test(?<gradlePackageNameFix>s?)" +
                    "/files/goldens"
            )
    }
}

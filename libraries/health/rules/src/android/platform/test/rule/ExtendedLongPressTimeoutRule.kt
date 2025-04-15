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

package android.platform.test.rule

import android.Manifest
import android.provider.Settings
import android.util.Log
import android.view.ViewConfiguration
import androidx.test.platform.app.InstrumentationRegistry
import com.android.launcher3.testing.BuildConfig
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Increases the device's long-press timeout to reduce flakiness in slow-running devices.
 *
 * <p>
 * When running TAPL tests remotely in pre- and post-submit tests, the test devices (eg. cf builds)
 * lack the resources for responsive test execution. This can cause a swipe gesture to be
 * interpreted as a long press, which leads to flakiness.
 *
 * <p>
 * Since this is not as much of an issue when running tests locally in studio, the long press
 * timeout doesn't need to be overwritten. Additionally, if the test is cancelled manually
 * mid-execution, the clean up will not run leaving the long press timeout overwritten.
 */
class ExtendedLongPressTimeoutRule : TestRule {

    private val TAG = "ExtendedLongPressTimeoutRule"

    private val temporaryLongPressTimeoutMs = 5000

    override fun apply(base: Statement, description: Description): Statement {
        // No-op when running in studio to prevent accidental overwrite
        return if (BuildConfig.IS_STUDIO_BUILD) base
        else
            object : Statement() {
                @Throws(Throwable::class)
                override fun evaluate() {
                    val contentResolver =
                        InstrumentationRegistry.getInstrumentation().context.contentResolver
                    val originalLongPressTimeout =
                        Settings.Secure.getInt(
                            contentResolver,
                            Settings.Secure.LONG_PRESS_TIMEOUT,
                            ViewConfiguration.getLongPressTimeout(),
                        )

                    try {
                        Log.d(
                            TAG,
                            "In try-block: Setting long press timeout from " +
                                "${originalLongPressTimeout}ms to " +
                                "${temporaryLongPressTimeoutMs}ms",
                        )
                        grantWriteSecurePermission()
                        Settings.Secure.putInt(
                            contentResolver,
                            Settings.Secure.LONG_PRESS_TIMEOUT,
                            temporaryLongPressTimeoutMs,
                        )

                        base.evaluate()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error", e)
                        throw e
                    } finally {
                        Log.d(
                            TAG,
                            "In finally-block: resetting long press timeout to " +
                                "${originalLongPressTimeout}ms",
                        )
                        grantWriteSecurePermission()
                        Settings.Secure.putInt(
                            contentResolver,
                            Settings.Secure.LONG_PRESS_TIMEOUT,
                            originalLongPressTimeout,
                        )
                    }
                }
            }
    }

    private fun grantWriteSecurePermission() {
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .adoptShellPermissionIdentity(Manifest.permission.WRITE_SECURE_SETTINGS)
    }
}

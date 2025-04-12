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

package platform.test.motion.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.TestMonotonicFrameClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

/**
 * This method creates a [CoroutineScope] that can be used in animations created in a composable
 * function.
 *
 * The [TestCoroutineScheduler] is passed to provide the functionality to wait for idle.
 *
 * Note: Please refer to the documentation for [runTest], as this feature utilizes it. This will
 * provide a comprehensive understanding of all its behaviors.
 */
@OptIn(ExperimentalTestApi::class)
fun runMonotonicClockTest(block: suspend MonotonicClockTestScope.() -> Unit) = runTest {
    val testScope: TestScope = this

    withContext(TestMonotonicFrameClock(coroutineScope = testScope)) {
        val testScopeWithMonotonicFrameClock: CoroutineScope = this

        val scope =
            MonotonicClockTestScope(
                testScope = testScopeWithMonotonicFrameClock,
                testScheduler = testScope.testScheduler,
                backgroundScope = backgroundScope,
            )

        // Run the test
        scope.block()
    }
}

/**
 * A coroutine scope that for launching test coroutines for Compose.
 *
 * @param testScheduler The delay-skipping scheduler used by the test dispatchers running the code
 *   in this scope (see [TestScope.testScheduler]).
 * @param backgroundScope A scope for background work (see [TestScope.backgroundScope]).
 */
class MonotonicClockTestScope(
    testScope: CoroutineScope,
    val testScheduler: TestCoroutineScheduler,
    val backgroundScope: CoroutineScope,
) : CoroutineScope by testScope

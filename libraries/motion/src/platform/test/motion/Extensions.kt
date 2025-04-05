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

package platform.test.motion

import kotlin.math.abs
import kotlin.math.max

/**
 * Checks if this nullable Float is within the tolerance of another nullable Float value,
 *
 * @receiver The nullable Float value (`this`) being compared.
 * @param expected The nullable Float value to compare against.
 * @param acceptedDeviation The tolerance value. Defaults to `0.01f`
 * @return `true` if the Floats are considered approximately equal
 */
fun Float?.isApproximatelyEqualTo(expected: Float?, acceptedDeviation: Float = 0.01f): Boolean {

    if (this == null && expected == null) return true
    if (this == null || expected == null) return false

    if(this == expected) return true

    if (this == 0f || expected == 0f) {
        //Check to see if absolute difference is within threshold for near zero values
        //This handles cases like (0, tiny) and (tiny, 0)
        return abs(this - expected) <= acceptedDeviation
    }

    val absDiff = abs(this - expected)
    val larger = max(abs(this), abs(expected))
    val relativeDiff = absDiff / larger
    return relativeDiff <= acceptedDeviation
}

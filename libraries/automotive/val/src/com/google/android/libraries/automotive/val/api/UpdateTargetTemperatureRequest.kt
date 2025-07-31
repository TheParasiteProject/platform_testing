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

package com.google.android.libraries.automotive.`val`.api

/** Data class that represents a client request to offset the current target temperature. */
data class UpdateTargetTemperatureRequest
@JvmOverloads
constructor(
  /** The seats to apply the offset to. Must be non-empty. */
  val seats: Set<String>,
  /** The temperature to set. */
  val temperature: Temperature,
  /**
   * Whether to round the target temperature to the nearest supported value if needed.
   *
   * <p>If {@code true}, the target temperature will be rounded to the nearest known supported value
   * before setting if needed. If {@code false}, the exact temperature will try to be set, but it
   * may return an error if the value is not supported for the action.
   */
  val roundToNearestSupportedValue: Boolean = true,
) {
  init {
    require(seats.isNotEmpty()) { "Seats must be non-empty" }
  }
}

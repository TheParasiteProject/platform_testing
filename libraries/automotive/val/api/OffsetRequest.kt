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

/**
 * Data class that represents a client offset action request. Offsets are used for increase and
 * decrease actions. This applies to all types of offsets including temperature, fan speed, door
 * position, etc.
 */
sealed class OffsetRequest<out T : Any> {
  /**
   * An object that represents an offset amount for an action. This depends on the action type eg
   * for temperature, this is a [Temperature] object or for fan speed, this is an [Int] object.
   */
  abstract val offset: T
  /** The elements to apply the offset to. Must be non-empty. */
  abstract val elements: Set<String>

  /**
   * Creates a copy of the current request with a new set of elements.
   *
   * @param newElements The new set of elements to apply the offset to.
   * @return A new OffsetRequest instance with the updated elements.
   */
  abstract fun copyWithNewElements(newElements: Set<String>): OffsetRequest<T>

  data class IntOffset(override val offset: Int, override val elements: Set<String>) :
    OffsetRequest<Int>() {

    init {
      require(elements.isNotEmpty()) { "Elements must be non-empty" }
    }

    override fun copyWithNewElements(newElements: Set<String>): OffsetRequest<Int> =
      this.copy(elements = newElements)
  }

  data class FloatOffset(override val offset: Float, override val elements: Set<String>) :
    OffsetRequest<Float>() {

    init {
      require(elements.isNotEmpty()) { "Elements must be non-empty" }
    }

    override fun copyWithNewElements(newElements: Set<String>): OffsetRequest<Float> =
      this.copy(elements = newElements)
  }

  data class TemperatureOffset
  @JvmOverloads
  constructor(
    override val offset: Temperature,
    /** The seats to apply the offset to. Must be non-empty. */
    val seats: Set<String>,
    /**
     * Whether to round the offsetted temperature to the nearest supported value if needed.
     *
     * <p>If {@code true}, the offsetted temperature will be rounded to the nearest known supported
     * value before setting if needed. If {@code false}, the exact temperature will try to be set,
     * but it may return error if the value is not supported for the action.
     */
    val roundToNearestSupportedValue: Boolean = true,
  ) : OffsetRequest<Temperature>() {
    override val elements: Set<String> = seats

    init {
      require(seats.isNotEmpty()) { "Seats must be non-empty" }
    }

    override fun copyWithNewElements(newElements: Set<String>): OffsetRequest<Temperature> =
      this.copy(seats = newElements)
  }
}

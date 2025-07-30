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

package com.google.android.libraries.automotive.val.actions;

import com.google.android.libraries.automotive.val.api.ErrorOr;
import com.google.android.libraries.automotive.val.api.ErrorOr.ErrorCode;
import com.google.android.libraries.automotive.val.api.ValueRange;
import com.google.android.libraries.automotive.val.compat.CarPropertyManagerCompat;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.jspecify.annotations.Nullable;

/** Class that contains optional parameters for {@link Action} and child classes. */
@AutoValue
public abstract class OptionalActionParameters<T> {
  /**
   * Interface for allowing OptionalActionParameters to take custom logic calculating value range.
   */
  public interface CustomValueRangeGenerator<N> {
    /**
     * Returns the value range for the given min and max values.
     *
     * <p>If the min and max values are not supported, returns an error code.
     */
    public default ErrorOr<ValueRange<N>> getValueRange(
        N minValue, N maxValue, ImmutableList<Integer> configArray) {
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }

    /**
     * Returns the custom value range using the {@code carPropertyManagerCompat} and {@code area}.
     */
    public default ErrorOr<ValueRange<N>> getValueRange(
        CarPropertyManagerCompat carPropertyManagerCompat, int area) {
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }
  }

  public abstract boolean enableHvacPowerIfDependent();

  public abstract boolean isMinMaxProperty();

  public abstract ImmutableSet<T> requiredSupportedValues();

  public abstract @Nullable CustomValueRangeGenerator<T> customValueRangeGenerator();

  public static <U> Builder<U> builder() {
    return new AutoValue_OptionalActionParameters.Builder<U>()
        .setEnableHvacPowerIfDependent(false)
        .setIsMinMaxProperty(false)
        .setRequiredSupportedValues(ImmutableSet.of())
        .setCustomValueRangeGenerator(null);
  }

  /** Builder for {@link OptionalActionParameters}. */
  @AutoValue.Builder
  public abstract static class Builder<T> {
    abstract Builder<T> setEnableHvacPowerIfDependent(boolean enableHvacPowerIfDependent);

    public Builder<T> setEnableHvacPowerIfDependent() {
      return setEnableHvacPowerIfDependent(true);
    }

    abstract Builder<T> setIsMinMaxProperty(boolean isMinMaxProperty);

    public Builder<T> setIsMinMaxProperty() {
      return setIsMinMaxProperty(true);
    }

    abstract Builder<T> setRequiredSupportedValues(ImmutableSet<T> requiredSupportedValues);

    public Builder<T> setRequiredSupportedValues(T... requiredSupportedValues) {
      return setRequiredSupportedValues(ImmutableSet.copyOf(requiredSupportedValues));
    }

    public abstract Builder<T> setCustomValueRangeGenerator(
        CustomValueRangeGenerator<T> customValueRangeGenerator);

    public abstract OptionalActionParameters<T> build();
  }
}

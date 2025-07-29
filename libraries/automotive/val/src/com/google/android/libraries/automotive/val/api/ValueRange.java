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

package com.google.android.libraries.automotive.val.api;

import com.google.android.libraries.automotive.val.utils.ActionUtils;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * Class that represents the values supported by an Action. This can be the range of values between
 * the min and max and/or a list of supported values.
 */
@AutoValue
public abstract class ValueRange<N> {

  public abstract Class<N> clazz();

  public abstract ImmutableList<N> supportedValues();

  public N min() {
    return supportedValues().get(0);
  }

  public N max() {
    return Iterables.getLast(supportedValues());
  }

  /** Returns true if the value is within the supported values list. */
  public boolean isValueSupported(N value) {
    if (value == null) {
      return false;
    }
    if (clazz().equals(Float.class)) {
      for (N supportedValue : supportedValues()) {
        if (ActionUtils.floatEquals((Float) value, (Float) supportedValue)) {
          return true;
        }
      }
      return false;
    }
    return supportedValues().contains(value);
  }

  public static ValueRange<Float> create(float min, float max) {
    Preconditions.checkArgument(min < max);
    ImmutableList.Builder<Float> supportedValuesBuilder = ImmutableList.builder();
    for (float i = min; i <= max; i++) {
      supportedValuesBuilder.add(i);
    }
    return create(Float.class, supportedValuesBuilder.build());
  }

  public static ValueRange<Integer> create(int min, int max) {
    Preconditions.checkArgument(min < max);
    ImmutableList.Builder<Integer> supportedValuesBuilder = ImmutableList.builder();
    for (int i = min; i <= max; i++) {
      supportedValuesBuilder.add(i);
    }
    return create(Integer.class, supportedValuesBuilder.build());
  }

  public static <N> ValueRange<N> create(Class<N> clazz, ImmutableList<N> supportedValues) {
    Preconditions.checkNotNull(clazz);
    Preconditions.checkNotNull(supportedValues);
    Preconditions.checkArgument(!supportedValues.isEmpty());
    return new AutoValue_ValueRange<N>(clazz, supportedValues);
  }
}

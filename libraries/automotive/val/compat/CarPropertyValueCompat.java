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

package com.google.android.libraries.automotive.val.compat;

import android.car.hardware.CarPropertyValue;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;

/** Compatibility version of the {@link CarPropertyValue}. */
@AutoValue
public abstract class CarPropertyValueCompat<T> {

  /** Creates an instance. */
  public static <U> CarPropertyValueCompat<U> create(CarPropertyValue<U> carPropertyValue) {
    Preconditions.checkNotNull(carPropertyValue);
    Preconditions.checkNotNull(carPropertyValue.getValue());
    Preconditions.checkArgument(carPropertyValue.getStatus() == CarPropertyValue.STATUS_AVAILABLE);
    return new AutoValue_CarPropertyValueCompat<U>(
        carPropertyValue.getPropertyId(),
        carPropertyValue.getAreaId(),
        carPropertyValue.getTimestamp(),
        carPropertyValue.getValue());
  }

  /** Returns the property ID. */
  public abstract int propertyId();

  /** Returns the property ID. */
  public abstract int areaId();

  /** Returns the elapsed realtime nanos timestamp for this value. */
  public abstract long elapsedRealtimeNanos();

  /** Returns value. */
  public abstract T value();
}

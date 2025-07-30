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

import android.car.hardware.CarPropertyConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/** Utility class for converting AAOS property classes to compat classes. */
final class CompatUtils {
  private static final ImmutableSet<Integer> READ_ACCESSES =
      ImmutableSet.of(
          CarPropertyConfig.VEHICLE_PROPERTY_ACCESS_READ,
          CarPropertyConfig.VEHICLE_PROPERTY_ACCESS_READ_WRITE);
  private static final ImmutableSet<Integer> WRITE_ACCESSES =
      ImmutableSet.of(
          CarPropertyConfig.VEHICLE_PROPERTY_ACCESS_WRITE,
          CarPropertyConfig.VEHICLE_PROPERTY_ACCESS_READ_WRITE);

  static <T> CarPropertyConfigCompat<T> toCompat(CarPropertyConfig<T> carPropertyConfig) {
    ImmutableSet.Builder<CarAreaConfigCompat<T>> carAreaConfigsBuilder = ImmutableSet.builder();
    for (int areaId : carPropertyConfig.getAreaIds()) {
      carAreaConfigsBuilder.add(
          CarAreaConfigCompat.<T>builder(areaId)
              .setIsReadable(READ_ACCESSES.contains(carPropertyConfig.getAccess()))
              .setIsWritable(WRITE_ACCESSES.contains(carPropertyConfig.getAccess()))
              .setMinSupportedValue(carPropertyConfig.getMinValue(areaId))
              .setMaxSupportedValue(carPropertyConfig.getMaxValue(areaId))
              .build());
    }

    return CarPropertyConfigCompat.builder(
            carPropertyConfig.getPropertyId(),
            carPropertyConfig.getPropertyType(),
            carAreaConfigsBuilder.build())
        .setConfigArray(ImmutableList.copyOf(carPropertyConfig.getConfigArray()))
        .build();
  }

  private CompatUtils() {}
}

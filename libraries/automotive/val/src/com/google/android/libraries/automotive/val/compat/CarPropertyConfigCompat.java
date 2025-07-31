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

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import android.util.Log;
import com.google.android.libraries.automotive.val.api.ErrorOr;
import com.google.android.libraries.automotive.val.api.ErrorOr.ErrorCode;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.function.Function;

/** Compatibility version of the AAOS CarPropertyConfig. */
@AutoValue
public abstract class CarPropertyConfigCompat<T> {
  private static final String TAG = "CarPropertyConfigCompat";
  private static final int GLOBAL_AREA_ID = 0;
  private static final ErrorOr<Integer> GLOBAL_AREA_ID_VALUE = ErrorOr.createValue(GLOBAL_AREA_ID);
  private static final ErrorOr<Integer> AREA_NOT_SUPPORTED_ERROR =
      ErrorOr.createError(ErrorCode.ERROR_CODE_AREA_NOT_SUPPORTED);

  public abstract int propertyId();

  public abstract Class<T> propertyClazz();

  public abstract ImmutableList<Integer> configArray();

  public abstract ImmutableMap<Integer, CarAreaConfigCompat<T>> areaIdToCarAreaConfig();

  public static <U> Builder<U> builder(
      int propertyId, Class<U> propertyClazz, ImmutableSet<CarAreaConfigCompat<U>> carAreaConfigs) {
    Preconditions.checkNotNull(propertyClazz);
    Preconditions.checkNotNull(carAreaConfigs);
    Preconditions.checkArgument(!carAreaConfigs.isEmpty());
    Preconditions.checkArgument(
        (int) carAreaConfigs.stream().map(CarAreaConfigCompat::areaId).distinct().count()
            == carAreaConfigs.size());
    return new AutoValue_CarPropertyConfigCompat.Builder<U>()
        .setPropertyId(propertyId)
        .setPropertyClazz(propertyClazz)
        .setConfigArray(ImmutableList.of())
        .setAreaIdToCarAreaConfig(
            carAreaConfigs.stream()
                .collect(toImmutableMap(CarAreaConfigCompat::areaId, Function.identity())));
  }

  /** Gets the area ID for the given {@code area}. */
  public ErrorOr<Integer> getAreaId(int area) {
    if (area == GLOBAL_AREA_ID) {
      if (!areaIdToCarAreaConfig().keySet().equals(ImmutableSet.of(GLOBAL_AREA_ID))) {
        Log.e(
            TAG,
            "getAreaId() - area is 0 but area ID is not 0 - areaIdToCarPropertyConfig(): "
                + areaIdToCarAreaConfig());
        return AREA_NOT_SUPPORTED_ERROR;
      }
      return GLOBAL_AREA_ID_VALUE;
    }
    for (Integer areaId : areaIdToCarAreaConfig().keySet()) {
      if ((areaId & area) == area) {
        return ErrorOr.createValue(areaId);
      }
    }
    Log.d(
        TAG,
        "getAreaId() - no area ID found for "
            + " area: "
            + area
            + " - areaIdToCarPropertyConfig(): "
            + areaIdToCarAreaConfig());
    return AREA_NOT_SUPPORTED_ERROR;
  }

  public boolean areAllAreasReadable() {
    return areaIdToCarAreaConfig().values().stream().allMatch(CarAreaConfigCompat::isReadable);
  }

  public boolean areAllAreasWritable() {
    return areaIdToCarAreaConfig().values().stream().allMatch(CarAreaConfigCompat::isWritable);
  }

  /** Builder for {@link CarPropertyConfigCompat}. */
  @AutoValue.Builder
  public abstract static class Builder<T> {
    abstract Builder<T> setPropertyId(int propertyId);

    abstract Builder<T> setPropertyClazz(Class<T> propertyClazz);

    public abstract Builder<T> setConfigArray(ImmutableList<Integer> configArray);

    abstract Builder<T> setAreaIdToCarAreaConfig(
        ImmutableMap<Integer, CarAreaConfigCompat<T>> areaIdToCarAreaConfig);

    public abstract CarPropertyConfigCompat<T> build();
  }
}

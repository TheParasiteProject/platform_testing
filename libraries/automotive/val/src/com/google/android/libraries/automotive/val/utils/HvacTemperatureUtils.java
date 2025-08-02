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

package com.google.android.libraries.automotive.val.utils;

import android.car.VehiclePropertyIds;
import android.car.VehicleUnit;
import android.util.Log;
import com.google.android.libraries.automotive.val.api.ErrorOr;
import com.google.android.libraries.automotive.val.api.ErrorOr.ErrorCode;
import com.google.android.libraries.automotive.val.api.Temperature;
import com.google.android.libraries.automotive.val.api.Temperature.TemperatureUnit;
import com.google.android.libraries.automotive.val.compat.CarPropertyManagerCompat;
import com.google.android.libraries.automotive.val.compat.CarPropertyValueCompat;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.List;

/** A utility class for handling HVAC temperature related operations. */
public final class HvacTemperatureUtils {
  private static final String TAG = HvacTemperatureUtils.class.getSimpleName();
  private static final ImmutableMap<Integer, TemperatureUnit>
      HVAC_TEMPERATURE_DISPLAY_UNITS_TO_TEMPERATURE_UNIT =
          ImmutableMap.of(
              VehicleUnit.CELSIUS, TemperatureUnit.CELSIUS,
              VehicleUnit.FAHRENHEIT, TemperatureUnit.FAHRENHEIT);

  public static int findIndexOfClosestCelsiusValue(float targetValue, List<Float> supportedValues) {
    return findIndexOfClosestSupportedValue(
        targetValue,
        supportedValues,
        getCelsiusValuesStartIndex(supportedValues),
        getCelsiusValuesEndIndex(supportedValues));
  }

  public static int findIndexOfClosestFahrenheitValue(
      float targetValue, List<Float> supportedValues) {
    return findIndexOfClosestSupportedValue(
        targetValue,
        supportedValues,
        getFahrenheitValuesStartIndex(supportedValues),
        getFahrenheitValuesEndIndex(supportedValues));
  }

  private static int findIndexOfClosestSupportedValue(
      float targetValue, List<Float> supportedValues, int startIndex, int endIndex) {
    List<Float> supportedValuesInRange = supportedValues.subList(startIndex, endIndex + 1);
    int index = Collections.binarySearch(supportedValuesInRange, targetValue);
    if (index >= 0) {
      return index + startIndex;
    }

    // If the target value is not found, then {@link Collections#binarySearch(List, Object)} returns
    // a negative number that can be used to find the insertion point.
    int insertionPoint = -index - 1;
    if (insertionPoint == 0) {
      return startIndex;
    }
    if (insertionPoint == supportedValuesInRange.size()) {
      return endIndex;
    }

    Float lower = supportedValuesInRange.get(insertionPoint - 1);
    Float upper = supportedValuesInRange.get(insertionPoint);
    // In the case of a tie, return the value that rounds to the same value as the target.
    // For example, if the target is 70.5, and lower is 70 and upper is 71, we want to return 71
    // because 70.5 rounds up to 71. If the target is 60.4, and lower is 60.2 and upper is 60.6, we
    // want to return 60.2 because 60.4 rounds down to 60.
    if (ActionUtils.floatEquals(Math.abs(lower - targetValue), Math.abs(upper - targetValue))) {
      if (ActionUtils.floatEquals(Math.round(targetValue), Math.round(lower))) {
        return insertionPoint - 1 + startIndex;
      }
      return insertionPoint + startIndex;
    } else if (Math.abs(lower - targetValue) < Math.abs(upper - targetValue)) {
      return insertionPoint - 1 + startIndex;
    }
    return insertionPoint + startIndex;
  }

  public static int getCelsiusValuesStartIndex(List<Float> supportedValues) {
    return 0;
  }

  public static int getCelsiusValuesEndIndex(List<Float> supportedValues) {
    return supportedValues.size() / 2 - 1;
  }

  public static int getFahrenheitValuesStartIndex(List<Float> supportedValues) {
    return supportedValues.size() / 2;
  }

  public static int getFahrenheitValuesEndIndex(List<Float> supportedValues) {
    return supportedValues.size() - 1;
  }

  public static ErrorOr<TemperatureUnit> getHvacTemperatureDisplayUnits(
      CarPropertyManagerCompat carPropertyManagerCompat) {
    ErrorOr<ImmutableMap<Integer, ErrorOr<CarPropertyValueCompat<Integer>>>> result =
        carPropertyManagerCompat.<Integer>getValues(
            VehiclePropertyIds.HVAC_TEMPERATURE_DISPLAY_UNITS, ImmutableSet.of(0));
    if (result.isError()) {
      Log.w(
          TAG,
          "getHvacTemperatureDisplayUnits() - getValues returned error: " + result.errorCode());
      return ErrorOr.createError(result.errorCode());
    }
    ErrorOr<CarPropertyValueCompat<Integer>> areaIdResult = result.value().get(0);
    if (areaIdResult.isError()) {
      Log.w(
          TAG,
          "getHvacTemperatureDisplayUnits() - areaIdResult is error: " + areaIdResult.errorCode());
      return ErrorOr.createError(areaIdResult.errorCode());
    }
    TemperatureUnit temperatureUnit =
        HVAC_TEMPERATURE_DISPLAY_UNITS_TO_TEMPERATURE_UNIT.get(areaIdResult.value().value());
    if (temperatureUnit == null) {
      Log.w(
          TAG,
          "getHvacTemperatureDisplayUnits() - unsupported temperatureUnit: "
              + areaIdResult.value().value());
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
    }
    return ErrorOr.createValue(temperatureUnit);
  }

  /**
   * Helper method to process a temperature value based on rounding preference and supported values.
   */
  public static ErrorOr<Integer> getProcessedTemperatureIndex(
      Temperature updatedTemp,
      boolean roundToNearestSupportedValue,
      ImmutableList<Float> supportedValues,
      int startIndex,
      int endIndex) {
    float value = updatedTemp.getValue();
    if (roundToNearestSupportedValue) {
      int index =
          updatedTemp.getUnit().equals(TemperatureUnit.FAHRENHEIT)
              ? findIndexOfClosestFahrenheitValue(value, supportedValues)
              : findIndexOfClosestCelsiusValue(value, supportedValues);
      return ErrorOr.createValue(index);
    }

    int updatedValueIndex = supportedValues.indexOf(value);
    if (updatedValueIndex == -1) {
      ErrorCode errorCode;
      if (value < supportedValues.get(startIndex)) {
        errorCode = ErrorCode.ERROR_CODE_VALUE_BELOW_MINIMUM;
      } else if (value > supportedValues.get(endIndex)) {
        errorCode = ErrorCode.ERROR_CODE_VALUE_ABOVE_MAXIMUM;
      } else {
        errorCode = ErrorCode.ERROR_CODE_VALUE_NOT_SUPPORTED;
      }
      return ErrorOr.createError(errorCode);
    }
    return ErrorOr.createValue(updatedValueIndex);
  }

  private HvacTemperatureUtils() {}
}

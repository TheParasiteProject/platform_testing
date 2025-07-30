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

import android.car.VehiclePropertyIds;
import android.util.Log;
import android.util.Pair;
import com.google.android.libraries.automotive.val.api.ErrorOr;
import com.google.android.libraries.automotive.val.api.ErrorOr.ErrorCode;
import com.google.android.libraries.automotive.val.api.OffsetRequest;
import com.google.android.libraries.automotive.val.api.Temperature;
import com.google.android.libraries.automotive.val.api.Temperature.TemperatureUnit;
import com.google.android.libraries.automotive.val.compat.CarPropertyManagerCompat;
import com.google.android.libraries.automotive.val.utils.HvacTemperatureUtils;
import com.google.android.libraries.automotive.val.utils.PermissionUtility;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import java.util.Optional;

/** A {@link OffsetAction} that sets the HVAC target temperature according to the display units. */
public class HvacTargetTemperatureOffsetAction extends HvacPowerDependentOffsetAction<Float> {
  private static final String TAG = HvacTargetTemperatureOffsetAction.class.getSimpleName();

  public HvacTargetTemperatureOffsetAction(
      CarPropertyManagerCompat carPropertyManagerCompat,
      PermissionUtility permissionUtility,
      String actionName,
      String requiredPermission,
      ImmutableBiMap<String, Integer> seatToArea,
      OptionalActionParameters<Float> optionalActionParameters) {
    super(
        carPropertyManagerCompat,
        permissionUtility,
        actionName,
        VehiclePropertyIds.HVAC_TEMPERATURE_SET,
        Float.class,
        requiredPermission,
        seatToArea,
        optionalActionParameters);
  }

  @Override
  protected <U> ErrorOr<Pair<Float, U>> calculateSetAndOutputValue(
      Float currentValue,
      OffsetRequest<U> request,
      String element,
      ImmutableList<Float> supportedValues) {
    OffsetRequest.TemperatureOffset temperatureOffset = (OffsetRequest.TemperatureOffset) request;
    ErrorOr<TemperatureUnit> displayUnits =
        HvacTemperatureUtils.getHvacTemperatureDisplayUnits(carPropertyManagerCompat);
    if (displayUnits.isError()) {
      return ErrorOr.createError(displayUnits.errorCode());
    }

    int startIndex;
    int endIndex;
    Temperature updatedTemperature;
    if (displayUnits.value().equals(TemperatureUnit.FAHRENHEIT)) {
      int indexOfClosestCelsiusValue =
          HvacTemperatureUtils.findIndexOfClosestCelsiusValue(currentValue, supportedValues);
      int fahrenheitStartIndex =
          HvacTemperatureUtils.getFahrenheitValuesStartIndex(supportedValues);
      float currValueF = supportedValues.get(indexOfClosestCelsiusValue + fahrenheitStartIndex);
      float updatedValueF = currValueF + getOffsetInFahrenheit(temperatureOffset.getOffset());

      startIndex = HvacTemperatureUtils.getFahrenheitValuesStartIndex(supportedValues);
      endIndex = HvacTemperatureUtils.getFahrenheitValuesEndIndex(supportedValues);
      updatedTemperature = new Temperature(updatedValueF, TemperatureUnit.FAHRENHEIT);
    } else {
      float updatedValueC = currentValue + getOffsetInCelsius(temperatureOffset.getOffset());
      startIndex = HvacTemperatureUtils.getCelsiusValuesStartIndex(supportedValues);
      endIndex = HvacTemperatureUtils.getCelsiusValuesEndIndex(supportedValues);
      updatedTemperature = new Temperature(updatedValueC, TemperatureUnit.CELSIUS);
    }

    ErrorOr<Integer> processedTemperatureIndex =
        HvacTemperatureUtils.getProcessedTemperatureIndex(
            updatedTemperature,
            temperatureOffset.getRoundToNearestSupportedValue(),
            supportedValues,
            startIndex,
            endIndex);
    if (processedTemperatureIndex.isError()) {
      Log.e(
          TAG,
          "calculateSetAndOutputValue() - new temperature value not found in supportedValues: "
              + " - action: "
              + actionName
              + " - element: "
              + element
              + " - updatedTemperature: "
              + updatedTemperature);
      return ErrorOr.createError(processedTemperatureIndex.errorCode());
    }

    float newHvacTemperatureSetValueC =
        supportedValues.get(processedTemperatureIndex.value() - startIndex);
    Temperature newDisplayTemperature =
        new Temperature(
            supportedValues.get(processedTemperatureIndex.value()), displayUnits.value());

    @SuppressWarnings("unchecked") // Safe cast since offset must be a temperature.
    U outputValue = (U) newDisplayTemperature;
    return ErrorOr.createValue(Pair.create(newHvacTemperatureSetValueC, outputValue));
  }

  @Override
  protected <U> Optional<ErrorCode> validateOffsetRequest(OffsetRequest<U> request) {
    if (request instanceof OffsetRequest.TemperatureOffset) {
      return Optional.empty();
    }
    return Optional.of(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
  }

  private static float getOffsetInCelsius(Temperature temperature) {
    if (temperature.getUnit().equals(TemperatureUnit.CELSIUS)) {
      return temperature.getValue();
    }
    return temperature.getValue() * 5f / 9f;
  }

  private static float getOffsetInFahrenheit(Temperature temperature) {
    if (temperature.getUnit().equals(TemperatureUnit.FAHRENHEIT)) {
      return temperature.getValue();
    }
    return temperature.getValue() * 9f / 5f;
  }
}

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
import com.google.android.libraries.automotive.val.api.ErrorOr;
import com.google.android.libraries.automotive.val.api.Temperature;
import com.google.android.libraries.automotive.val.api.Temperature.TemperatureUnit;
import com.google.android.libraries.automotive.val.api.ValueRange;
import com.google.android.libraries.automotive.val.compat.CarPropertyManagerCompat;
import com.google.android.libraries.automotive.val.utils.HvacTemperatureUtils;
import com.google.android.libraries.automotive.val.utils.PermissionUtility;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Set;

/** A {@link GetAction} that gets the HVAC target temperature according to the display units. */
public class HvacTargetTemperatureGetAction extends HvacPowerDependentGetAction<Float> {
  private static final String TAG = HvacTargetTemperatureGetAction.class.getSimpleName();

  public HvacTargetTemperatureGetAction(
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
  public ErrorOr<GetActionResult<Temperature>> getTemperature(Set<String> elements) {
    ErrorOr<TemperatureUnit> displayUnits =
        HvacTemperatureUtils.getHvacTemperatureDisplayUnits(carPropertyManagerCompat);
    if (displayUnits.isError()) {
      return ErrorOr.createError(displayUnits.errorCode());
    }
    return getTemperature(elements, displayUnits.value());
  }

  @Override
  public ErrorOr<GetActionResult<Temperature>> getTemperature(
      Set<String> elements, TemperatureUnit unit) {
    ErrorOr<GetActionResult<Float>> getActionResult = super.get(elements);
    if (getActionResult.isError()) {
      return ErrorOr.createError(getActionResult.errorCode());
    }

    ErrorOr<ImmutableMap<String, ValueRange<Float>>> seatToValueRange = getElementToValueRangeMap();
    ImmutableMap.Builder<String, ErrorOr<Temperature>> seatToTemperatureBuilder =
        ImmutableMap.builder();
    for (Map.Entry<String, ErrorOr<Float>> entry :
        getActionResult.value().elementToValue().entrySet()) {
      String seat = entry.getKey();
      if (entry.getValue().isError()) {
        seatToTemperatureBuilder.put(seat, ErrorOr.createError(entry.getValue().errorCode()));
        continue;
      }

      float currentTemp = entry.getValue().value();
      if (unit.equals(TemperatureUnit.FAHRENHEIT)) {
        ImmutableList<Float> supportedValues = seatToValueRange.value().get(seat).supportedValues();
        currentTemp = convertCelsiusToFahrenheit(currentTemp, supportedValues);
      }
      seatToTemperatureBuilder.put(seat, ErrorOr.createValue(new Temperature(currentTemp, unit)));
    }

    return ErrorOr.createValue(
        GetActionResult.create(actionName, seatToTemperatureBuilder.buildOrThrow()));
  }

  /** Converts a temperature in Celsius to Fahrenheit using the supported values mapping. */
  private float convertCelsiusToFahrenheit(float celsius, ImmutableList<Float> supportedValues) {
    int indexOfClosestCelsiusValue =
        HvacTemperatureUtils.findIndexOfClosestCelsiusValue(celsius, supportedValues);
    int fahrenheitStartIndex = HvacTemperatureUtils.getFahrenheitValuesStartIndex(supportedValues);
    return supportedValues.get(indexOfClosestCelsiusValue + fahrenheitStartIndex);
  }
}

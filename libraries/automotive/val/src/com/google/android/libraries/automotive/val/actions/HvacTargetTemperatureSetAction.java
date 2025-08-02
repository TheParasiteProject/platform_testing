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
import com.google.android.libraries.automotive.val.api.ErrorOr;
import com.google.android.libraries.automotive.val.api.ErrorOr.ErrorCode;
import com.google.android.libraries.automotive.val.api.Temperature;
import com.google.android.libraries.automotive.val.api.Temperature.TemperatureUnit;
import com.google.android.libraries.automotive.val.api.UpdateTargetTemperatureRequest;
import com.google.android.libraries.automotive.val.api.ValueRange;
import com.google.android.libraries.automotive.val.compat.CarPropertyManagerCompat;
import com.google.android.libraries.automotive.val.utils.HvacTemperatureUtils;
import com.google.android.libraries.automotive.val.utils.PermissionUtility;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.util.Optional;
import java.util.Set;

/** A {@link SetAction} that sets the HVAC target temperature according to the display units. */
public class HvacTargetTemperatureSetAction extends HvacPowerDependentSetAction<Float> {
  private static final String TAG = HvacTargetTemperatureSetAction.class.getSimpleName();

  public HvacTargetTemperatureSetAction(
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
  public ErrorOr<SetActionResult> set(UpdateTargetTemperatureRequest request) {
    Set<String> seats = request.getSeats();
    Optional<ErrorCode> seatsSupportedErrorCode = checkAreElementsSupported(seats);
    if (seatsSupportedErrorCode.isPresent()) {
      Log.e(
          TAG,
          "set() - checkAreElementsSupported returned error: "
              + seatsSupportedErrorCode
              + " - action: "
              + getActionName()
              + " - seats: "
              + seats);
      return ErrorOr.createError(seatsSupportedErrorCode.get());
    }

    Temperature temperature = request.getTemperature();
    ErrorOr<TemperatureUnit> displayUnits =
        HvacTemperatureUtils.getHvacTemperatureDisplayUnits(carPropertyManagerCompat);
    if (displayUnits.isError()) {
      return ErrorOr.createError(displayUnits.errorCode());
    }

    ErrorOr<ImmutableMap<String, ValueRange<Float>>> seatToValueRange = getElementToValueRangeMap();
    ImmutableMap.Builder<String, ErrorCode> seatToErrorCodeBuilder = ImmutableMap.builder();
    ImmutableSetMultimap.Builder<Float, String> setValueToSeatBuilder =
        ImmutableSetMultimap.builder();
    for (String seat : seats) {
      ImmutableList<Float> supportedValues = seatToValueRange.value().get(seat).supportedValues();
      int startIndex;
      int endIndex;
      Temperature updatedTemperature;
      if (displayUnits.value().equals(TemperatureUnit.FAHRENHEIT)) {
        startIndex = HvacTemperatureUtils.getFahrenheitValuesStartIndex(supportedValues);
        endIndex = HvacTemperatureUtils.getFahrenheitValuesEndIndex(supportedValues);
        updatedTemperature = getTemperatureInFahrenheit(temperature);
      } else {
        startIndex = HvacTemperatureUtils.getCelsiusValuesStartIndex(supportedValues);
        endIndex = HvacTemperatureUtils.getCelsiusValuesEndIndex(supportedValues);
        updatedTemperature = getTemperatureInCelsius(temperature);
      }

      ErrorOr<Integer> processedTemperatureIndex =
          HvacTemperatureUtils.getProcessedTemperatureIndex(
              updatedTemperature,
              request.getRoundToNearestSupportedValue(),
              supportedValues,
              startIndex,
              endIndex);
      if (processedTemperatureIndex.isError()) {
        Log.e(
            TAG,
            "set() - new temperature value not found in supportedValues: "
                + " - action: "
                + actionName
                + " - seat: "
                + seat
                + " - temperature: "
                + temperature);
        seatToErrorCodeBuilder.put(seat, processedTemperatureIndex.errorCode());
        continue;
      }

      setValueToSeatBuilder.put(
          supportedValues.get(processedTemperatureIndex.value() - startIndex), seat);
    }

    ImmutableSetMultimap<Float, String> setValueToSeat = setValueToSeatBuilder.build();
    for (Float setValue : setValueToSeat.keySet()) {
      ImmutableSet<String> seatsToUpdate = setValueToSeat.get(setValue);
      ErrorOr<SetActionResult> result = super.set(seatsToUpdate, setValue);
      if (result.isError()) {
        Log.e(
            TAG,
            "set() - super.set returned error: "
                + result.errorCode()
                + " - action: "
                + actionName
                + " - seatsToUpdate: "
                + seatsToUpdate
                + " - setValue: "
                + setValue);
        return result;
      }
      seatToErrorCodeBuilder.putAll(result.value().elementToErrorCode());
    }

    return ErrorOr.createValue(
        SetActionResult.create(actionName, seatToErrorCodeBuilder.buildKeepingLast()));
  }

  private static Temperature getTemperatureInCelsius(Temperature temperature) {
    if (temperature.getUnit().equals(TemperatureUnit.CELSIUS)) {
      return temperature;
    }
    return new Temperature((temperature.getValue() - 32f) * 5f / 9f, TemperatureUnit.CELSIUS);
  }

  private static Temperature getTemperatureInFahrenheit(Temperature temperature) {
    if (temperature.getUnit().equals(TemperatureUnit.FAHRENHEIT)) {
      return temperature;
    }
    return new Temperature((temperature.getValue() * 9f / 5f) + 32f, TemperatureUnit.FAHRENHEIT);
  }
}

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
import android.util.Log;
import com.google.android.libraries.automotive.val.api.ErrorOr;
import com.google.android.libraries.automotive.val.api.ErrorOr.ErrorCode;
import com.google.android.libraries.automotive.val.compat.CarPropertyConfigCompat;
import com.google.android.libraries.automotive.val.compat.CarPropertyManagerCompat;
import com.google.android.libraries.automotive.val.compat.CarPropertyValueCompat;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;

/** A utility class for handling possibly HVAC power dependent actions. */
public final class HvacPowerUtils {
  private static final String TAG = HvacPowerUtils.class.getSimpleName();
  private final AtomicReference<ErrorOr<Boolean>> atomicCachedIsHvacPowerDependentResult =
      new AtomicReference<>();

  private final CarPropertyManagerCompat carPropertyManagerCompat;
  private final int propertyId;
  private final ImmutableBiMap<String, Integer> seatToArea;
  private final boolean enableHvacPowerIfDependent;

  public HvacPowerUtils(
      CarPropertyManagerCompat carPropertyManagerCompat,
      int propertyId,
      ImmutableBiMap<String, Integer> seatToArea,
      boolean enableHvacPowerIfDependent) {
    Preconditions.checkNotNull(carPropertyManagerCompat);
    Preconditions.checkArgument(propertyId != VehiclePropertyIds.HVAC_POWER_ON);
    Preconditions.checkNotNull(seatToArea);
    Preconditions.checkArgument(!seatToArea.isEmpty());

    this.carPropertyManagerCompat = carPropertyManagerCompat;
    this.propertyId = propertyId;
    this.seatToArea = seatToArea;
    this.enableHvacPowerIfDependent = enableHvacPowerIfDependent;
  }

  /**
   * Returns {@code true} if the property is dependent on HVAC power.
   *
   * <p>This method will cache the result and return the cached result if called again.
   */
  public ErrorOr<Boolean> isHvacPowerDependent() {
    if (atomicCachedIsHvacPowerDependentResult.get() == null) {
      atomicCachedIsHvacPowerDependentResult.set(generateIsHvacPowerDependent());
    }
    return atomicCachedIsHvacPowerDependentResult.get();
  }

  /**
   * Returns the HvacPowerState including the set of seats that initially had HVAC power disabled
   * and were enabled after this method was called, the seats that can be updated, and the seats
   * that had an error.
   *
   * <p>If this a HVAC power dependent property, this method will enable the HVAC power for the
   * seats that have the HVAC power disabled if {@code enableHvacPowerIfDependent} is {@code true}.
   * Otherwise, the seats that have the HVAC power disabled will be returned with an error code.
   *
   * @param seats the set of seats to check
   * @return the {@link HvacPowerState}
   */
  public HvacPowerState computeHvacPowerState(Set<String> seats) {
    ErrorOr<Boolean> isHvacPowerDependent = isHvacPowerDependent();
    if (isHvacPowerDependent.isError()) {
      Log.e(
          TAG,
          "getSeatsThatEnabledHvacPower() - isHvacPowerDependent returned error: "
              + isHvacPowerDependent.errorCode()
              + " - seats: "
              + seats);
      return HvacPowerState.builder()
          .setErrorCode(Optional.of(isHvacPowerDependent.errorCode()))
          .build();
    }

    if (!isHvacPowerDependent.value()) {
      return HvacPowerState.builder().setSeatsToUpdate(ImmutableSet.copyOf(seats)).build();
    }

    ImmutableSet.Builder<String> seatsToUpdateBuilder = ImmutableSet.builder();
    ImmutableMap.Builder<String, ErrorCode> seatToErrorCodeBuilder = ImmutableMap.builder();

    ErrorOr<ImmutableSetMultimap<Boolean, String>> hvacPowerStateToSeats =
        getHvacPowerStateToSeats(seats, seatToErrorCodeBuilder);
    if (hvacPowerStateToSeats.isError()) {
      Log.e(
          TAG,
          "getSeatsThatEnabledHvacPower() - getHvacPowerStateToSeats returned an errorCode: "
              + hvacPowerStateToSeats.errorCode()
              + " for seats: "
              + seats
              + " property ID: "
              + propertyId);
      return HvacPowerState.builder()
          .setErrorCode(Optional.of(hvacPowerStateToSeats.errorCode()))
          .build();
    }

    ImmutableSet<String> seatsWithHvacPowerEnabled = hvacPowerStateToSeats.value().get(true);
    seatsToUpdateBuilder.addAll(seatsWithHvacPowerEnabled);

    ImmutableSet<String> seatsWithHvacPowerDisabled = hvacPowerStateToSeats.value().get(false);
    if (!enableHvacPowerIfDependent) {
      for (String seat : seatsWithHvacPowerDisabled) {
        seatToErrorCodeBuilder.put(seat, ErrorCode.ERROR_CODE_HVAC_POWER_IS_DISABLED);
      }
      // No seats to enable HVAC power for
      return HvacPowerState.builder()
          .setSeatsToUpdate(seatsToUpdateBuilder.build())
          .setSeatToErrorCode(seatToErrorCodeBuilder.buildOrThrow())
          .build();
    }

    return enableHvacPower(
        seatsWithHvacPowerDisabled, seatsToUpdateBuilder, seatToErrorCodeBuilder);
  }

  /**
   * Returns the filtered seat to error code map.
   *
   * <p>If the seat has an error code of {@link ErrorCode#ERROR_CODE_VALUE_ALREADY_SET} and the
   * seat's power was enabled by this utility class, the error code will be filtered out.
   *
   * @param seatsThatEnabledHvacPower the set of seats that initially had HVAC power disabled and
   *     were enabled afterwards.
   * @param seatToErrorCode the map of seat to error code
   * @return the filtered seat to error code map
   */
  public static ImmutableMap<String, ErrorCode> getFilteredSeatToErrorCode(
      ImmutableSet<String> seatsThatEnabledHvacPower,
      ImmutableMap<String, ErrorCode> seatToErrorCode) {
    ImmutableMap.Builder<String, ErrorCode> filteredSeatToErrorCodeBuilder = ImmutableMap.builder();
    for (Map.Entry<String, ErrorCode> entry : seatToErrorCode.entrySet()) {
      if (filterOutValueAlreadySetErrors(
          entry.getValue(), seatsThatEnabledHvacPower, entry.getKey())) {
        Log.d(
            TAG,
            "getFilteredSeatToErrorCode() - updating action to success because HVAC power was"
                + " enabled: "
                + entry.getKey());
        continue;
      }
      filteredSeatToErrorCodeBuilder.put(entry);
    }
    return filteredSeatToErrorCodeBuilder.buildOrThrow();
  }

  /**
   * Returns the filtered seat to new value map.
   *
   * <p>If the seat has an error code of {@link ErrorCode#ERROR_CODE_VALUE_ALREADY_SET} and the
   * seat's power was enabled by this utility class, the error code will be filtered out.
   *
   * @param seatsThatEnabledHvacPower the set of seats that initially had HVAC power disabled and
   *     were enabled afterwards.
   * @param seatToNewValue the map of seat to new value
   * @return the filtered seat to new value map
   */
  public static <U> ImmutableMap<String, ErrorOr<U>> getFilteredSeatToNewValue(
      ImmutableSet<String> seatsThatEnabledHvacPower,
      ImmutableMap<String, ErrorOr<U>> seatToNewValue) {
    ImmutableMap.Builder<String, ErrorOr<U>> filteredSeatToNewValueBuilder = ImmutableMap.builder();
    for (Map.Entry<String, ErrorOr<U>> entry : seatToNewValue.entrySet()) {
      if (filterOutValueAlreadySetErrors(
          entry.getValue().errorCode(), seatsThatEnabledHvacPower, entry.getKey())) {
        Log.d(
            TAG,
            "getFilteredSeatToNewValue() - updating action to success because HVAC power was"
                + " enabled: "
                + entry.getKey());
        continue;
      }
      filteredSeatToNewValueBuilder.put(entry);
    }
    return filteredSeatToNewValueBuilder.buildOrThrow();
  }

  private static boolean filterOutValueAlreadySetErrors(
      @Nullable ErrorCode errorCode, ImmutableSet<String> seatsThatEnabledHvacPower, String seat) {
    return Objects.equals(errorCode, ErrorCode.ERROR_CODE_VALUE_ALREADY_SET)
        && seatsThatEnabledHvacPower.contains(seat);
  }

  private ErrorOr<Boolean> generateIsHvacPowerDependent() {
    ErrorOr<Boolean> isHvacPowerSupported =
        carPropertyManagerCompat.isPropertySupported(VehiclePropertyIds.HVAC_POWER_ON);
    if (isHvacPowerSupported.isError()) {
      Log.e(
          TAG,
          "generateIsHvacPowerDependent() - isPropertySupported returns error for HVAC_POWER_ON -"
              + " error code: "
              + isHvacPowerSupported.errorCode());
      return ErrorOr.createError(isHvacPowerSupported.errorCode());
    }

    if (!isHvacPowerSupported.value()) {
      return ErrorOr.createValue(false);
    }

    ErrorOr<CarPropertyConfigCompat<Boolean>> hvacPowerConfigCompat =
        carPropertyManagerCompat.getConfig(VehiclePropertyIds.HVAC_POWER_ON);
    if (hvacPowerConfigCompat.isError()) {
      Log.e(
          TAG,
          "generateIsHvacPowerDependent() - getConfig() returned error for HVAC_POWER_ON even"
              + " though it is supported - error code: "
              + hvacPowerConfigCompat.errorCode()
              + " property ID: "
              + VehiclePropertyIds.toString(propertyId));
      return ErrorOr.createError(hvacPowerConfigCompat.errorCode());
    }

    if (!hvacPowerConfigCompat.value().configArray().contains(propertyId)) {
      return ErrorOr.createValue(false);
    }

    // getConfig is assumed to return a valid value since we check it in isActionSupported().
    ErrorOr<CarPropertyConfigCompat<Object>> propertyConfigCompat =
        carPropertyManagerCompat.getConfig(propertyId);
    for (int propertyAreaId : propertyConfigCompat.value().areaIdToCarAreaConfig().keySet()) {
      ErrorOr<Integer> hvacPowerAreaId = hvacPowerConfigCompat.value().getAreaId(propertyAreaId);
      if (hvacPowerAreaId.isError()) {
        Log.e(
            TAG,
            "generateIsHvacPowerDependent() - propertyAreaId: "
                + propertyAreaId
                + " does not match any of the hvacPowerAreaIds: "
                + hvacPowerConfigCompat.value().areaIdToCarAreaConfig().keySet());
        return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
      }
    }

    return ErrorOr.createValue(true);
  }

  private ErrorOr<ImmutableSetMultimap<Boolean, String>> getHvacPowerStateToSeats(
      Set<String> seats, ImmutableMap.Builder<String, ErrorCode> seatToErrorCodeBuilder) {
    if (seats.isEmpty()) {
      return ErrorOr.createValue(ImmutableSetMultimap.of());
    }

    ErrorOr<ImmutableSet<Integer>> areas = ActionUtils.getAreas(seats, seatToArea);
    if (areas.isError()) {
      Log.e(
          TAG,
          "getHvacPowerStateToSeats() - getAreas() returned error: "
              + areas.errorCode()
              + " - seats: "
              + seats);
      return ErrorOr.createError(areas.errorCode());
    }

    ErrorOr<ImmutableMap<Integer, ErrorOr<CarPropertyValueCompat<Boolean>>>> areaToValue =
        carPropertyManagerCompat.<Boolean>getValues(
            VehiclePropertyIds.HVAC_POWER_ON, areas.value());
    if (areaToValue.isError()) {
      Log.e(
          TAG,
          "getHvacPowerStateToSeats() - getValues returned error: "
              + areaToValue.errorCode()
              + " - areas: "
              + areas.value());
      return ErrorOr.createError(areaToValue.errorCode());
    }

    ImmutableSetMultimap.Builder<Boolean, String> hvacPowerStateToSeatsBuilder =
        ImmutableSetMultimap.builder();
    for (Map.Entry<Integer, ErrorOr<CarPropertyValueCompat<Boolean>>> entry :
        areaToValue.value().entrySet()) {
      String seat = seatToArea.inverse().get(entry.getKey());
      ErrorOr<CarPropertyValueCompat<Boolean>> isEnabled = entry.getValue();
      if (isEnabled.isError()) {
        Log.e(
            TAG,
            "getHvacPowerStateToSeats() - getValues returned error: "
                + isEnabled.errorCode()
                + " - area: "
                + entry.getKey()
                + " - areas: "
                + areas.value());
        seatToErrorCodeBuilder.put(seat, isEnabled.errorCode());
        continue;
      }
      hvacPowerStateToSeatsBuilder.put(isEnabled.value().value(), seat);
    }

    return ErrorOr.createValue(hvacPowerStateToSeatsBuilder.build());
  }

  private HvacPowerState enableHvacPower(
      ImmutableSet<String> seatsToEnableHvacPower,
      ImmutableSet.Builder<String> seatsToUpdateBuilder,
      ImmutableMap.Builder<String, ErrorCode> seatToErrorCodeBuilder) {
    if (seatsToEnableHvacPower.isEmpty()) {
      return HvacPowerState.builder()
          .setSeatsToUpdate(seatsToUpdateBuilder.build())
          .setSeatToErrorCode(seatToErrorCodeBuilder.buildOrThrow())
          .build();
    }

    // getAreas is assumed to return a valid value since we check it in getHvacPowerStateToSeats().
    ErrorOr<ImmutableSet<Integer>> areasToEnableHvacPower =
        ActionUtils.getAreas(seatsToEnableHvacPower, seatToArea);

    ErrorOr<ImmutableMap<Integer, ErrorOr<CarPropertyValueCompat<Boolean>>>> areasToUpdatedValue =
        carPropertyManagerCompat.<Boolean>setValueWithConfirmation(
            VehiclePropertyIds.HVAC_POWER_ON, areasToEnableHvacPower.value(), true);
    if (areasToUpdatedValue.isError()) {
      Log.e(
          TAG,
          "enableHvacPower() - setValueWithConfirmation returned error: "
              + areasToUpdatedValue.errorCode()
              + " - areasToEnableHvacPower: "
              + areasToEnableHvacPower
              + " - seatsToEnableHvacPower: "
              + seatsToEnableHvacPower);
      return HvacPowerState.builder()
          .setErrorCode(Optional.of(areasToUpdatedValue.errorCode()))
          .build();
    }

    ImmutableSet.Builder<String> seatsThatEnabledHvacPowerBuilder = ImmutableSet.builder();
    for (Map.Entry<Integer, ErrorOr<CarPropertyValueCompat<Boolean>>> entry :
        areasToUpdatedValue.value().entrySet()) {
      String seat = seatToArea.inverse().get(entry.getKey());
      ErrorOr<CarPropertyValueCompat<Boolean>> updatedValue = entry.getValue();
      if (updatedValue.isError()) {
        Log.e(
            TAG,
            "enableHvacPower() - setValueWithConfirmation returned error: "
                + updatedValue.errorCode()
                + " - seatsToEnableHvacPower: "
                + seatsToEnableHvacPower
                + " - seat: "
                + seat);
        seatToErrorCodeBuilder.put(seat, updatedValue.errorCode());
        continue;
      }
      if (!updatedValue.value().value()) {
        Log.e(
            TAG,
            "enableHvacPower() - setValueWithConfirmation returned false: "
                + updatedValue.errorCode()
                + " - seatsToEnableHvacPower: "
                + seatsToEnableHvacPower
                + " - seat: "
                + seat);
        return HvacPowerState.builder()
            .setErrorCode(Optional.of(ErrorCode.ERROR_CODE_BAD_VAL_IMPL))
            .build();
      }
      seatsToUpdateBuilder.add(seat);
      seatsThatEnabledHvacPowerBuilder.add(seat);
    }

    return HvacPowerState.builder()
        .setSeatsThatEnabledHvacPower(seatsThatEnabledHvacPowerBuilder.build())
        .setSeatsToUpdate(seatsToUpdateBuilder.build())
        .setSeatToErrorCode(seatToErrorCodeBuilder.buildOrThrow())
        .build();
  }
}

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

import android.util.Log;
import com.google.android.libraries.automotive.val.api.ErrorOr;
import com.google.android.libraries.automotive.val.api.ErrorOr.ErrorCode;
import com.google.android.libraries.automotive.val.compat.CarPropertyManagerCompat;
import com.google.android.libraries.automotive.val.utils.HvacPowerState;
import com.google.android.libraries.automotive.val.utils.HvacPowerUtils;
import com.google.android.libraries.automotive.val.utils.PermissionUtility;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.Set;

/**
 * A {@link SetAction} that will check the HVAC power state before setting the value of the
 * property.
 */
public class HvacPowerDependentSetAction<T> extends SetAction<T> {
  private static final String TAG = HvacPowerDependentSetAction.class.getSimpleName();
  private final HvacPowerUtils hvacPowerUtils;

  public HvacPowerDependentSetAction(
      CarPropertyManagerCompat carPropertyManagerCompat,
      PermissionUtility permissionUtility,
      String actionName,
      int propertyId,
      Class<T> propertyTypeClazz,
      String requiredPermission,
      ImmutableBiMap<String, Integer> seatToArea) {
    this(
        carPropertyManagerCompat,
        permissionUtility,
        actionName,
        propertyId,
        propertyTypeClazz,
        requiredPermission,
        seatToArea,
        OptionalActionParameters.<T>builder().build());
  }

  public HvacPowerDependentSetAction(
      CarPropertyManagerCompat carPropertyManagerCompat,
      PermissionUtility permissionUtility,
      String actionName,
      int propertyId,
      Class<T> propertyTypeClazz,
      String requiredPermission,
      ImmutableBiMap<String, Integer> seatToArea,
      OptionalActionParameters<T> optionalActionParameters) {
    super(
        carPropertyManagerCompat,
        permissionUtility,
        actionName,
        propertyId,
        propertyTypeClazz,
        requiredPermission,
        seatToArea,
        optionalActionParameters);
    hvacPowerUtils =
        new HvacPowerUtils(
            carPropertyManagerCompat,
            propertyId,
            seatToArea,
            optionalActionParameters.enableHvacPowerIfDependent());
  }

  @Override
  protected ErrorOr<Boolean> determineIsActionSupportedFromDependencies() {
    ErrorOr<Boolean> isActionSupportedFromDependencies =
        super.determineIsActionSupportedFromDependencies();
    if (isActionSupportedFromDependencies.isError()) {
      return isActionSupportedFromDependencies;
    }

    ErrorOr<Boolean> isHvacPowerDependent = hvacPowerUtils.isHvacPowerDependent();
    if (isHvacPowerDependent.isError()) {
      return isHvacPowerDependent;
    }

    return isActionSupportedFromDependencies;
  }

  @Override
  public ErrorOr<SetActionResult> set(Set<String> seats, T valueToSet) {
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

    HvacPowerState hvacPowerState = hvacPowerUtils.computeHvacPowerState(seats);
    if (hvacPowerState.errorCode().isPresent()) {
      Log.e(
          TAG,
          "executeSetAction() - computeHvacPowerEnabledSeats returned error: "
              + hvacPowerState.errorCode().get()
              + " - seats: "
              + seats);
      return ErrorOr.createError(hvacPowerState.errorCode().get());
    }

    ImmutableMap.Builder<String, ErrorCode> seatToErrorCodeBuilder =
        ImmutableMap.<String, ErrorCode>builder().putAll(hvacPowerState.seatToErrorCode());
    if (hvacPowerState.seatsToUpdate().isEmpty()) {
      return ErrorOr.createValue(
          SetActionResult.create(actionName, seatToErrorCodeBuilder.buildOrThrow()));
    }

    ErrorOr<ImmutableMap<String, ErrorCode>> seatToErrorCodeSetResult =
        setInternal(hvacPowerState.seatsToUpdate(), valueToSet);
    if (seatToErrorCodeSetResult.isError()) {
      Log.e(
          TAG,
          "executeSetAction() - setInternal returned error: "
              + seatToErrorCodeSetResult.errorCode()
              + " - action: "
              + getActionName()
              + " - seats: "
              + seats
              + " - valueToSet: "
              + valueToSet);
      return ErrorOr.createError(seatToErrorCodeSetResult.errorCode());
    }

    return ErrorOr.createValue(
        SetActionResult.create(
            actionName,
            seatToErrorCodeBuilder
                .putAll(
                    HvacPowerUtils.getFilteredSeatToErrorCode(
                        hvacPowerState.seatsThatEnabledHvacPower(),
                        seatToErrorCodeSetResult.value()))
                .buildOrThrow()));
  }
}

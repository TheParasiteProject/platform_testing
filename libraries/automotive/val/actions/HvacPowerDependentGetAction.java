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
import com.google.common.collect.Maps;
import java.util.Set;

/**
 * A {@link GetAction} that will check the HVAC power state before getting the value of the
 * property.
 */
public class HvacPowerDependentGetAction<T> extends GetAction<T> {
  private static final String TAG = HvacPowerDependentGetAction.class.getSimpleName();
  private final HvacPowerUtils hvacPowerUtils;

  public HvacPowerDependentGetAction(
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

  public HvacPowerDependentGetAction(
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
  public ErrorOr<GetActionResult<T>> get(Set<String> seats) {
    ErrorCode seatsSupportedErrorCode = checkAreElementsSupported(seats).orElse(null);
    if (seatsSupportedErrorCode != null) {
      Log.e(
          TAG,
          "get() - checkAreElementsSupported returned error: "
              + seatsSupportedErrorCode
              + " - action: "
              + getActionName()
              + " - seats: "
              + seats);
      return ErrorOr.createError(seatsSupportedErrorCode);
    }

    HvacPowerState hvacPowerState = hvacPowerUtils.computeHvacPowerState(seats);
    ErrorCode hvacPowerStateErrorCode = hvacPowerState.errorCode().orElse(null);
    if (hvacPowerStateErrorCode != null) {
      Log.e(
          TAG,
          "get() - computeHvacPowerEnabledSeats returned error: "
              + hvacPowerStateErrorCode
              + " - action: "
              + getActionName()
              + " - seats: "
              + seats);
      return ErrorOr.createError(hvacPowerStateErrorCode);
    }

    ImmutableMap.Builder<String, ErrorOr<T>> seatToValueBuilder =
        ImmutableMap.<String, ErrorOr<T>>builder()
            .putAll(Maps.transformValues(hvacPowerState.seatToErrorCode(), ErrorOr::createError));
    if (hvacPowerState.seatsToUpdate().isEmpty()) {
      return ErrorOr.createValue(
          GetActionResult.create(actionName, seatToValueBuilder.buildOrThrow()));
    }

    ErrorOr<ImmutableMap<String, ErrorOr<T>>> getInternalResult =
        getInternal(hvacPowerState.seatsToUpdate());
    if (getInternalResult.isError()) {
      Log.e(
          TAG,
          "get() - getInternal returned error: "
              + getInternalResult.errorCode()
              + " - action: "
              + getActionName()
              + " - seats: "
              + seats);
      return ErrorOr.createError(getInternalResult.errorCode());
    }

    return ErrorOr.createValue(
        GetActionResult.create(
            actionName, seatToValueBuilder.putAll(getInternalResult.value()).buildOrThrow()));
  }
}

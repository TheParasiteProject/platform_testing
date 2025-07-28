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
import com.google.android.libraries.automotive.val.api.OffsetRequest;
import com.google.android.libraries.automotive.val.compat.CarPropertyManagerCompat;
import com.google.android.libraries.automotive.val.utils.HvacPowerState;
import com.google.android.libraries.automotive.val.utils.HvacPowerUtils;
import com.google.android.libraries.automotive.val.utils.PermissionUtility;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Optional;
import java.util.Set;

/** A {@link OffsetAction} that will check the HVAC power state before applying the offset. */
public class HvacPowerDependentOffsetAction<T extends Number> extends OffsetAction<T> {
  private static final String TAG = HvacPowerDependentOffsetAction.class.getSimpleName();
  private final HvacPowerUtils hvacPowerUtils;

  public HvacPowerDependentOffsetAction(
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

  public HvacPowerDependentOffsetAction(
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

  /**
   * Executes "Offset" action for the set of {@code seats} with {@code offset}. This will check the
   * HVAC power state before applying the offset. If it fails it will return an error code.
   *
   * <p>Caller should check that {@link #isActionSupported()} is {@code true} before using this
   * function. {@code seats} must not be empty.
   */
  @Override
  public <U> ErrorOr<OffsetActionResult<U>> applyOffset(OffsetRequest<U> request) {
    Set<String> seats = request.getElements();
    Optional<ErrorCode> seatsSupportedErrorCode = checkAreElementsSupported(seats);
    if (seatsSupportedErrorCode.isPresent()) {
      Log.e(
          TAG,
          "applyOffset() - checkAreElementsSupported returned error: "
              + seatsSupportedErrorCode.get()
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
          "applyOffset() - computeHvacPowerEnabledSeats returned error: "
              + hvacPowerState.errorCode().get()
              + " - seats: "
              + seats);
      return ErrorOr.createError(hvacPowerState.errorCode().get());
    }

    ImmutableMap.Builder<String, ErrorOr<U>> seatToNewValueBuilder =
        ImmutableMap.<String, ErrorOr<U>>builder()
            .putAll(Maps.transformValues(hvacPowerState.seatToErrorCode(), ErrorOr::createError));
    if (hvacPowerState.seatsToUpdate().isEmpty()) {
      return ErrorOr.createValue(
          OffsetActionResult.create(actionName, seatToNewValueBuilder.buildOrThrow()));
    }

    ErrorOr<OffsetActionResult<U>> offsetActionResult =
        super.applyOffset(request.copyWithNewElements(hvacPowerState.seatsToUpdate()));
    if (offsetActionResult.isError()) {
      Log.e(
          TAG,
          "applyOffset() - applyOffset returned error: "
              + offsetActionResult.errorCode()
              + " - request: "
              + request);
      return ErrorOr.createError(offsetActionResult.errorCode());
    }

    return ErrorOr.createValue(
        OffsetActionResult.create(
            actionName,
            seatToNewValueBuilder
                .putAll(
                    HvacPowerUtils.getFilteredSeatToNewValue(
                        hvacPowerState.seatsThatEnabledHvacPower(),
                        offsetActionResult.value().elementToNewValue()))
                .buildOrThrow()));
  }
}

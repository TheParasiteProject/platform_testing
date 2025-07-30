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
import com.google.android.libraries.automotive.val.api.Temperature;
import com.google.android.libraries.automotive.val.api.Temperature.TemperatureUnit;
import com.google.android.libraries.automotive.val.compat.CarPropertyManagerCompat;
import com.google.android.libraries.automotive.val.utils.PermissionUtility;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.Set;

/** Class encapsulates a single "get" action for the VAL. */
public class GetAction<T> extends Action<T> {
  private static final String TAG = GetAction.class.getSimpleName();

  public GetAction(
      CarPropertyManagerCompat carPropertyManagerCompat,
      PermissionUtility permissionUtility,
      String actionName,
      int propertyId,
      Class<T> propertyTypeClazz,
      String requiredPermission,
      ImmutableBiMap<String, Integer> elementToArea) {
    super(
        carPropertyManagerCompat,
        permissionUtility,
        actionName,
        propertyId,
        propertyTypeClazz,
        requiredPermission,
        elementToArea);
  }

  public GetAction(
      CarPropertyManagerCompat carPropertyManagerCompat,
      PermissionUtility permissionUtility,
      String actionName,
      int propertyId,
      Class<T> propertyTypeClazz,
      String requiredPermission,
      ImmutableBiMap<String, Integer> elementToArea,
      OptionalActionParameters<T> optionalActionParameters) {
    super(
        carPropertyManagerCompat,
        permissionUtility,
        actionName,
        propertyId,
        propertyTypeClazz,
        requiredPermission,
        elementToArea,
        optionalActionParameters);
  }

  /**
   * Returns {@code true} if {@link #actionName} is supported on vehicle. Otherwise {@code false} or
   * an error code.
   */
  @Override
  public ErrorOr<Boolean> isActionSupported() {
    return super.isActionSupported(/* requiresWriteAccess= */ false);
  }

  /**
   * Executes get temperature action for the passed {@code elements}. If it fails it will return an
   * error code.
   *
   * <p>See {@link #get(Set<String>)}.
   */
  public ErrorOr<GetActionResult<Temperature>> getTemperature(Set<String> elements) {
    return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
  }

  /**
   * Executes get temperature action for the passed {@code elements} and {@code unit}. If it fails
   * it will return an error code.
   *
   * <p>See {@link #get(Set<String>)}.
   */
  public ErrorOr<GetActionResult<Temperature>> getTemperature(
      Set<String> elements, TemperatureUnit unit) {
    return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
  }

  /**
   * Executes "Get" action for the set of {@code elements}. If it fails it will return an error
   * code.
   *
   * <p>Caller should check that {@link #isActionSupported()} is {@code true} before using this
   * function.
   */
  public ErrorOr<GetActionResult<T>> get(Set<String> elements) {
    Optional<ErrorCode> elementsSupportedErrorCode = checkAreElementsSupported(elements);
    if (elementsSupportedErrorCode.isPresent()) {
      Log.e(
          TAG,
          "get() - checkAreElementsSupported returned error: "
              + elementsSupportedErrorCode.get()
              + " - action: "
              + actionName
              + " - elements: "
              + elements);
      return ErrorOr.createError(elementsSupportedErrorCode.get());
    }

    ErrorOr<ImmutableMap<String, ErrorOr<T>>> elementToValue = getInternal(elements);
    if (elementToValue.isError()) {
      Log.e(
          TAG,
          "get() - getInternal returned error: "
              + elementToValue.errorCode()
              + " - action: "
              + actionName
              + " - elements: "
              + elements);
      return ErrorOr.createError(elementToValue.errorCode());
    }
    return ErrorOr.createValue(GetActionResult.create(actionName, elementToValue.value()));
  }
}

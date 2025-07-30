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
import com.google.android.libraries.automotive.val.api.UpdateTargetTemperatureRequest;
import com.google.android.libraries.automotive.val.compat.CarPropertyManagerCompat;
import com.google.android.libraries.automotive.val.utils.PermissionUtility;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.Set;

/** Class encapsulates a single "set" action for the VAL. */
public class SetAction<T> extends Action<T> {
  private static final String TAG = SetAction.class.getSimpleName();

  public SetAction(
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

  public SetAction(
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
    return super.isActionSupported(/* requiresWriteAccess= */ true);
  }

  /**
   * Executes "Set" action for the passed {@code request}. If fails it will return an error code.
   *
   * <p>See {@link #set(Set<String>, T)}.
   */
  public ErrorOr<SetActionResult> set(UpdateTargetTemperatureRequest request) {
    return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
  }

  /**
   * Executes "Set" action for the passed {@code request}. If fails it will return an error code.
   *
   * <p>Caller should check that {@link #isActionSupported()} is {@code true} before using this
   * function.
   */
  public ErrorOr<SetActionResult> set(Set<String> elements, T valueToSet) {
    Optional<ErrorCode> elementsSupportedErrorCode = checkAreElementsSupported(elements);
    if (elementsSupportedErrorCode.isPresent()) {
      Log.e(
          TAG,
          "set() - checkAreElementsSupported returned error: "
              + elementsSupportedErrorCode.get()
              + " - action: "
              + actionName
              + " - elements: "
              + elements);
      return ErrorOr.createError(elementsSupportedErrorCode.get());
    }

    ErrorOr<ImmutableMap<String, ErrorCode>> elementToErrorCode = setInternal(elements, valueToSet);
    if (elementToErrorCode.isError()) {
      Log.e(
          TAG,
          "set() - setInternal returned error: "
              + elementToErrorCode.errorCode()
              + " - action: "
              + actionName
              + " - elements: "
              + elements
              + " - valueToSet: "
              + valueToSet);
      return ErrorOr.createError(elementToErrorCode.errorCode());
    }
    return ErrorOr.createValue(SetActionResult.create(actionName, elementToErrorCode.value()));
  }
}

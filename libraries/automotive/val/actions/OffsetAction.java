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
import android.util.Pair;
import com.google.android.libraries.automotive.val.api.ErrorOr;
import com.google.android.libraries.automotive.val.api.ErrorOr.ErrorCode;
import com.google.android.libraries.automotive.val.api.OffsetRequest;
import com.google.android.libraries.automotive.val.api.ValueRange;
import com.google.android.libraries.automotive.val.compat.CarPropertyManagerCompat;
import com.google.android.libraries.automotive.val.utils.PermissionUtility;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Class encapsulates a single "offset" action for the VAL.
 *
 * <p>The offset action will get the current value of the property, add the offset to it, check if
 * the new value is supported by the property, and set the property. The only supported property
 * type is {@code Integer}.
 */
public class OffsetAction<T extends Number> extends Action<T> {
  private static final String TAG = OffsetAction.class.getSimpleName();

  public OffsetAction(
      CarPropertyManagerCompat carPropertyManagerCompat,
      PermissionUtility permissionUtility,
      String actionName,
      int propertyId,
      Class<T> propertyTypeClazz,
      String requiredPermission,
      ImmutableBiMap<String, Integer> elementToArea) {
    this(
        carPropertyManagerCompat,
        permissionUtility,
        actionName,
        propertyId,
        propertyTypeClazz,
        requiredPermission,
        elementToArea,
        OptionalActionParameters.<T>builder().build());
  }

  public OffsetAction(
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
    Preconditions.checkArgument(
        propertyTypeClazz.equals(Integer.class) || propertyTypeClazz.equals(Float.class));
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
   * Calculates the value to set for the property and the return value for the action.
   *
   * @param currentValue the current value of the property
   * @param request the offset request
   * @param element the element to calculate the updated value for
   * @param supportedValues the supported values for the element
   * @return an error code if the value to set is not supported by the property, otherwise a pair of
   *     the set and output values
   */
  protected <U> ErrorOr<Pair<T, U>> calculateSetAndOutputValue(
      T currentValue, OffsetRequest<U> request, String element, ImmutableList<T> supportedValues) {
    // Default implementation for offset type and property type being the same.
    T offset = propertyTypeClazz.cast(request.getOffset());
    T setValue = getUpdatedValue(currentValue, offset);
    @SuppressWarnings("unchecked") // Safe cast since the output value is the same as the set value.
    U outputValue = (U) setValue;
    ErrorOr<Boolean> doesElementSupportValue = doesElementSupportValue(element, setValue);
    if (!doesElementSupportValue.value()) {
      Log.e(
          TAG,
          "calculateSetAndOutputValue() - doesElementSupportValue returned false: "
              + " - action: "
              + actionName
              + " - element: "
              + element
              + " - setValue: "
              + setValue);
      ErrorCode errorCode;
      if (setValue.floatValue() < supportedValues.get(0).floatValue()) {
        errorCode = ErrorCode.ERROR_CODE_VALUE_BELOW_MINIMUM;
      } else if (setValue.floatValue() > Iterables.getLast(supportedValues).floatValue()) {
        errorCode = ErrorCode.ERROR_CODE_VALUE_ABOVE_MAXIMUM;
      } else {
        errorCode = ErrorCode.ERROR_CODE_VALUE_NOT_SUPPORTED;
      }
      return ErrorOr.createError(errorCode);
    }
    return ErrorOr.createValue(Pair.create(setValue, outputValue));
  }

  /**
   * Validates the offset request.
   *
   * @param request the offset request
   * @return an error code if the offset request is invalid, otherwise an empty optional
   */
  protected <U> Optional<ErrorCode> validateOffsetRequest(OffsetRequest<U> request) {
    // Default implementation for offset type and property type being the same.
    if ((request instanceof OffsetRequest.IntOffset && propertyTypeClazz.equals(Integer.class))
        || (request instanceof OffsetRequest.FloatOffset
            && propertyTypeClazz.equals(Float.class))) {
      return Optional.empty();
    }
    return Optional.of(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
  }

  /**
   * Executes "Offset" action for the set of {@code elements} with {@code offset}. If fails it will
   * return an error code.
   *
   * <p>Caller should check that {@link #isActionSupported()} is {@code true} before using this
   * function.
   */
  public <U> ErrorOr<OffsetActionResult<U>> applyOffset(OffsetRequest<U> request) {
    Set<String> elements = request.getElements();
    Optional<ErrorCode> elementsSupportedErrorCode = checkAreElementsSupported(elements);
    if (elementsSupportedErrorCode.isPresent()) {
      Log.e(
          TAG,
          "applyOffset() - checkAreElementsSupported returned error: "
              + elementsSupportedErrorCode.get()
              + " - action: "
              + actionName
              + " - request: "
              + request);
      return ErrorOr.createError(elementsSupportedErrorCode.get());
    }

    Optional<ErrorCode> offsetRequestErrorCode = validateOffsetRequest(request);
    if (offsetRequestErrorCode.isPresent()) {
      Log.e(
          TAG,
          "applyOffset() - validateOffsetRequest returned error: "
              + offsetRequestErrorCode.get()
              + " - action: "
              + actionName
              + " - request: "
              + request);
      return ErrorOr.createError(offsetRequestErrorCode.get());
    }

    ErrorOr<ImmutableMap<String, ErrorOr<T>>> elementToCurrentValue = getInternal(elements);
    if (elementToCurrentValue.isError()) {
      Log.e(
          TAG,
          "applyOffset() - getInternal returned error: "
              + elementToCurrentValue.errorCode()
              + " - action: "
              + actionName
              + " - request: "
              + request);
      return ErrorOr.createError(elementToCurrentValue.errorCode());
    }

    ErrorOr<ImmutableMap<String, ValueRange<T>>> elementToValueRange = getElementToValueRangeMap();

    ImmutableMap.Builder<String, ErrorOr<U>> elementToResultBuilder = ImmutableMap.builder();
    ImmutableSetMultimap.Builder<Pair<T, U>, String> setAndOutputValueToElementBuilder =
        ImmutableSetMultimap.builder();
    for (Map.Entry<String, ErrorOr<T>> entry : elementToCurrentValue.value().entrySet()) {
      String element = entry.getKey();
      ErrorOr<T> currentValue = entry.getValue();
      if (currentValue.isError()) {
        Log.e(
            TAG,
            "applyOffset() - getInternal returned error: "
                + currentValue.errorCode()
                + " - action: "
                + actionName
                + " - request: "
                + request);
        elementToResultBuilder.put(element, ErrorOr.createError(currentValue.errorCode()));
        continue;
      }

      ImmutableList<T> supportedValues = elementToValueRange.value().get(element).supportedValues();
      ErrorOr<Pair<T, U>> setAndOutputValue =
          calculateSetAndOutputValue(currentValue.value(), request, element, supportedValues);
      if (setAndOutputValue.isError()) {
        elementToResultBuilder.put(element, ErrorOr.createError(setAndOutputValue.errorCode()));
        continue;
      }

      T setValue = setAndOutputValue.value().first;
      U outputValue = setAndOutputValue.value().second;
      if (currentValue.value().equals(setValue)) {
        elementToResultBuilder.put(element, ErrorOr.createValue(outputValue));
        continue;
      }

      setAndOutputValueToElementBuilder.put(setAndOutputValue.value(), element);
    }

    ImmutableSetMultimap<Pair<T, U>, String> newValueToElement =
        setAndOutputValueToElementBuilder.build();
    for (Pair<T, U> setAndOutputValue : newValueToElement.keySet()) {
      T setValue = setAndOutputValue.first;
      U outputValue = setAndOutputValue.second;
      ImmutableSet<String> elementsToUpdate = newValueToElement.get(setAndOutputValue);

      ErrorOr<ImmutableMap<String, ErrorCode>> elementToErrorCode =
          setInternal(elementsToUpdate, setValue);
      if (elementToErrorCode.isError()) {
        Log.e(
            TAG,
            "applyOffset() - setInternal returned error: "
                + elementToErrorCode.errorCode()
                + " - action: "
                + actionName
                + " - currentElements: "
                + elementsToUpdate
                + " - setValue: "
                + setValue);
        return ErrorOr.createError(elementToErrorCode.errorCode());
      }

      for (String currentElement : elementsToUpdate) {
        if (elementToErrorCode.value().containsKey(currentElement)) {
          elementToResultBuilder.put(
              currentElement, ErrorOr.createError(elementToErrorCode.value().get(currentElement)));
        } else {
          elementToResultBuilder.put(currentElement, ErrorOr.createValue(outputValue));
        }
      }
    }

    return ErrorOr.createValue(
        OffsetActionResult.create(actionName, elementToResultBuilder.buildOrThrow()));
  }

  private T getUpdatedValue(T value, T offset) {
    if (propertyTypeClazz.equals(Integer.class)) {
      return propertyTypeClazz.cast(Integer.valueOf((value.intValue() + offset.intValue())));
    } else { // if (propertyTypeClazz.equals(Float.class))
      return propertyTypeClazz.cast(Float.valueOf((value.floatValue() + offset.floatValue())));
    }
  }
}

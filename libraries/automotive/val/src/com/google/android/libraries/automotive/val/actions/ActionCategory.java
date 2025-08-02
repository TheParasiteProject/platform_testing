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
import com.google.android.libraries.automotive.val.api.Temperature;
import com.google.android.libraries.automotive.val.api.Temperature.TemperatureUnit;
import com.google.android.libraries.automotive.val.api.UpdateTargetTemperatureRequest;
import com.google.android.libraries.automotive.val.api.ValueRange;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.Set;

/** An abstract class that contains the common code for the specific vehicle actions categories. */
public abstract class ActionCategory {
  private static final String TAG = ActionCategory.class.getSimpleName();

  protected static final String GLOBAL_ELEMENT = "GLOBAL";
  protected static final ImmutableSet<String> GLOBAL_ELEMENTS = ImmutableSet.of(GLOBAL_ELEMENT);
  protected static final int GLOBAL_AREA_ID = 0;
  protected static final ImmutableBiMap<String, Integer> GLOBAL_ELEMENT_TO_AREA =
      ImmutableBiMap.of(GLOBAL_ELEMENT, GLOBAL_AREA_ID);

  protected final ListeningExecutorService listeningExecutorService;
  protected final ImmutableMap<String, Action<?>> actionMap;

  protected ActionCategory(
      ListeningExecutorService listeningExecutorService,
      ImmutableMap<String, Action<?>> actionMap) {
    Preconditions.checkNotNull(listeningExecutorService);
    Preconditions.checkNotNull(actionMap);
    Preconditions.checkArgument(!actionMap.isEmpty());
    this.listeningExecutorService = listeningExecutorService;
    this.actionMap = actionMap;
  }

  /**
   * Returns {@code true} if {@code action} is supported on vehicle. Otherwise {@code false} or an
   * error code.
   */
  protected ErrorOr<Boolean> isActionSupportedInternal(String action) {
    if (!isActionValid(action)) {
      Log.e(TAG, "isActionSupported() - isActionValid returned false - action: " + action);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_INVALID_API_ARGUMENT);
    }
    return actionMap.get(action).isActionSupported();
  }

  public <E> ErrorOr<ImmutableMap<String, ValueRange<E>>> getElementToValueRangeMap(
      Class<E> clazz, String action) {
    if (!isActionValid(action)) {
      Log.e(
          TAG,
          "getElementToValueRangeInternalMap() - isActionValid returned false - action: " + action);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_INVALID_API_ARGUMENT);
    }
    if (!clazz.equals(actionMap.get(action).getPropertyTypeClazz())) {
      Log.e(
          TAG,
          String.format(
              "getElementToValueRangeInternalMap() - clazz does not match action property type -"
                  + " action: %s",
              action));
      return ErrorOr.createError(ErrorCode.ERROR_CODE_INVALID_API_ARGUMENT);
    }
    @SuppressWarnings("unchecked")
    Action<E> actionObj = (Action<E>) actionMap.get(action);
    return actionObj.getElementToValueRangeMap();
  }

  /** Returns a non-empty set of supported elements for the {@code action}. */
  protected ErrorOr<ImmutableSet<String>> getSupportedElements(String action) {
    if (!isActionValid(action)) {
      Log.e(TAG, "getSupportedElements() - isActionValid returned false - action: " + action);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_INVALID_API_ARGUMENT);
    }
    return actionMap.get(action).getSupportedElements();
  }

  protected <T> ListenableFuture<ErrorOr<GetActionResult<T>>> executeGetActionOnListenableFuture(
      String action, ImmutableSet<String> elements) {
    return listeningExecutorService.submit(() -> executeGetActionInternal(action, elements));
  }

  protected ListenableFuture<ErrorOr<GetActionResult<Temperature>>>
      executeGetTemperatureActionOnListenableFuture(String action, ImmutableSet<String> elements) {
    return listeningExecutorService.submit(
        () -> executeGetTemperatureActionInternal(action, elements));
  }

  protected ListenableFuture<ErrorOr<GetActionResult<Temperature>>>
      executeGetTemperatureActionOnListenableFuture(
          String action, ImmutableSet<String> elements, TemperatureUnit unit) {
    return listeningExecutorService.submit(
        () -> executeGetTemperatureActionInternal(action, elements, unit));
  }

  protected <T> ErrorOr<GetActionResult<T>> executeGetActionInternal(
      String action, ImmutableSet<String> elements) {
    if (!isActionValid(action, GetAction.class)) {
      Log.e(
          TAG,
          "executeGetActionInternal() - not a valid GetAction: action: "
              + action
              + " - elements: "
              + elements);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }
    @SuppressWarnings("unchecked")
    GetAction<T> getAction = (GetAction<T>) actionMap.get(action);

    return getAction.get(elements);
  }

  protected ErrorOr<GetActionResult<Temperature>> executeGetTemperatureActionInternal(
      String action, ImmutableSet<String> elements) {
    if (!isActionValid(action, GetAction.class)) {
      Log.e(
          TAG,
          "executeGetActionInternal() - not a valid GetAction: action: "
              + action
              + " - elements: "
              + elements);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }
    GetAction<?> getAction = (GetAction<?>) actionMap.get(action);

    return getAction.getTemperature(elements);
  }

  protected ErrorOr<GetActionResult<Temperature>> executeGetTemperatureActionInternal(
      String action, ImmutableSet<String> elements, TemperatureUnit unit) {
    if (!isActionValid(action, GetAction.class)) {
      Log.e(
          TAG,
          "executeGetActionInternal() - not a valid GetAction: action: "
              + action
              + " - elements: "
              + elements);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }
    GetAction<?> getAction = (GetAction<?>) actionMap.get(action);

    return getAction.getTemperature(elements, unit);
  }

  protected <T> ListenableFuture<ErrorOr<SetActionResult>> executeSetActionOnListenableFuture(
      String action, Set<String> elements, T valueToSet) {
    return listeningExecutorService.submit(
        () -> executeSetActionInternal(action, elements, valueToSet));
  }

  protected ListenableFuture<ErrorOr<SetActionResult>> executeSetActionOnListenableFuture(
      String action, UpdateTargetTemperatureRequest request) {
    return listeningExecutorService.submit(() -> executeSetActionInternal(action, request));
  }

  protected <T> ErrorOr<SetActionResult> executeSetActionInternal(
      String action, Set<String> elements, T valueToSet) {
    if (!isActionValid(action, SetAction.class)) {
      Log.e(
          TAG,
          "executeSetActionInternal() - not a valid SetAction: action: "
              + action
              + " - elements: "
              + elements);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }
    @SuppressWarnings("unchecked")
    SetAction<T> setAction = (SetAction<T>) actionMap.get(action);

    ImmutableSet.Builder<String> elementsToUpdateBuilder = ImmutableSet.builder();
    ImmutableMap.Builder<String, ErrorCode> elementToErrorCodeBuilder = ImmutableMap.builder();
    for (String element : elements) {
      ErrorOr<Boolean> doesElementSupportValue =
          setAction.doesElementSupportValue(element, valueToSet);
      if (doesElementSupportValue.isError()) {
        Log.e(
            TAG,
            "executeSetActionInternal() - doesElementSupportValue returned error: "
                + doesElementSupportValue.errorCode()
                + " - action: "
                + action
                + " - element: "
                + element
                + " - valueToSet: "
                + valueToSet);
        elementToErrorCodeBuilder.put(element, doesElementSupportValue.errorCode());
        continue;
      }
      if (!doesElementSupportValue.value()) {
        Log.e(
            TAG,
            "executeSetActionInternal() - doesElementSupportValue returned false - action: "
                + action
                + " - element: "
                + element
                + " - valueToSet: "
                + valueToSet);
        elementToErrorCodeBuilder.put(element, ErrorCode.ERROR_CODE_VALUE_NOT_SUPPORTED);
        continue;
      }
      elementsToUpdateBuilder.add(element);
    }
    ImmutableSet<String> elementsToUpdate = elementsToUpdateBuilder.build();
    if (elementsToUpdate.isEmpty()) {
      return ErrorOr.createValue(
          SetActionResult.create(action, elementToErrorCodeBuilder.buildOrThrow()));
    }

    ErrorOr<SetActionResult> setActionResult = setAction.set(elementsToUpdate, valueToSet);
    if (setActionResult.isError()) {
      Log.e(
          TAG,
          "executeSetActionInternal() - setActionResult returned error: "
              + setActionResult.errorCode()
              + " - elementsToUpdate: "
              + elementsToUpdate
              + " - action: "
              + action
              + " - valueToSet: "
              + valueToSet);
      return ErrorOr.createError(setActionResult.errorCode());
    }

    elementToErrorCodeBuilder.putAll(setActionResult.value().elementToErrorCode());
    return ErrorOr.createValue(
        SetActionResult.create(action, elementToErrorCodeBuilder.buildOrThrow()));
  }

  protected ErrorOr<SetActionResult> executeSetActionInternal(
      String action, UpdateTargetTemperatureRequest request) {
    if (!isActionValid(action, SetAction.class)) {
      Log.e(
          TAG,
          "executeSetActionInternal() - not a valid SetAction: action: "
              + action
              + " - seats: "
              + request.getSeats());
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }
    SetAction<?> setAction = (SetAction<?>) actionMap.get(action);

    return setAction.set(request);
  }

  protected <T>
      ListenableFuture<ErrorOr<OffsetActionResult<T>>> executeApplyOffsetActionOnListenableFuture(
          String action, OffsetRequest<T> request) {
    return listeningExecutorService.submit(() -> executeApplyOffsetActionInternal(action, request));
  }

  protected <T> ErrorOr<OffsetActionResult<T>> executeApplyOffsetActionInternal(
      String action, OffsetRequest<T> request) {
    if (!isActionValid(action, OffsetAction.class)) {
      Log.e(
          TAG,
          "executeApplyOffsetActionInternal() - not a valid OffsetAction: "
              + action
              + " - request: "
              + request);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }
    OffsetAction<?> offsetAction = (OffsetAction<?>) actionMap.get(action);

    return offsetAction.applyOffset(request);
  }

  /** A custom value range generator that returns a value range from 0 to the max value. */
  public static class ZeroOrGreaterThanCustomValueRangeGenerator
      implements OptionalActionParameters.CustomValueRangeGenerator<Integer> {
    @Override
    public ErrorOr<ValueRange<Integer>> getValueRange(
        Integer minValue, Integer maxValue, ImmutableList<Integer> configArray) {
      if (maxValue <= 0 || minValue > 0) {
        Log.e(
            TAG,
            "ZeroOrGreaterThanCustomValueRangeGenerator.getValueRange() - invalid min and max"
                + " values: "
                + minValue
                + " "
                + maxValue);
        return ErrorOr.createError(ErrorCode.ERROR_CODE_AREA_NOT_SUPPORTED);
      }
      return ErrorOr.createValue(ValueRange.create(0, maxValue));
    }
  }

  /** A custom value range generator that returns a value range from 0 to the min value * -1. */
  public static class ZeroOrLessThanCustomValueRangeGenerator
      implements OptionalActionParameters.CustomValueRangeGenerator<Integer> {
    @Override
    public ErrorOr<ValueRange<Integer>> getValueRange(
        Integer minValue, Integer maxValue, ImmutableList<Integer> configArray) {
      if (maxValue < 0 || minValue >= 0) {
        Log.e(
            TAG,
            "ZeroOrLessThanCustomValueRangeGenerator.getValueRange() - invalid min and max values: "
                + minValue
                + " "
                + maxValue);
        return ErrorOr.createError(ErrorCode.ERROR_CODE_AREA_NOT_SUPPORTED);
      }
      return ErrorOr.createValue(ValueRange.create(minValue, 0));
    }
  }

  private boolean isActionValid(String action) {
    if (action == null) {
      Log.e(TAG, "isActionValid() - action is null");
      return false;
    }
    if (!actionMap.containsKey(action)) {
      Log.e(
          TAG,
          "isActionValid() - action is unknown: "
              + action
              + " - valid actions: "
              + actionMap.keySet());
      return false;
    }
    return true;
  }

  private boolean isActionValid(String action, Class<?> expectedClazz) {
    return isActionValid(action) && expectedClazz.isInstance(actionMap.get(action));
  }
}

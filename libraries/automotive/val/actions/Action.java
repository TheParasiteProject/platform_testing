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
import android.util.Pair;
import com.google.android.libraries.automotive.val.actions.OptionalActionParameters.CustomValueRangeGenerator;
import com.google.android.libraries.automotive.val.api.ErrorOr;
import com.google.android.libraries.automotive.val.api.ErrorOr.ErrorCode;
import com.google.android.libraries.automotive.val.api.ValueRange;
import com.google.android.libraries.automotive.val.compat.CarPropertyConfigCompat;
import com.google.android.libraries.automotive.val.compat.CarPropertyManagerCompat;
import com.google.android.libraries.automotive.val.compat.CarPropertyValueCompat;
import com.google.android.libraries.automotive.val.utils.ActionUtils;
import com.google.android.libraries.automotive.val.utils.PermissionUtility;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;

/** Class encapsulates a single action for the VAL. */
public abstract class Action<T> {
  private static final String TAG = Action.class.getSimpleName();

  protected final CarPropertyManagerCompat carPropertyManagerCompat;
  protected final PermissionUtility permissionUtility;
  protected final String actionName;
  protected final int propertyId;
  protected final String propertyName;
  protected final Class<T> propertyTypeClazz;
  protected final String requiredPermission;
  protected final ImmutableBiMap<String, Integer> elementToArea;
  protected final boolean isMinMaxProperty;
  protected final ImmutableSet<T> requiredSupportedValues;
  protected final @Nullable CustomValueRangeGenerator<T> customValueRangeGenerator;

  private final AtomicReference<ErrorOr<Boolean>> atomicCachedIsActionSupportedResult =
      new AtomicReference<>();
  private final AtomicReference<ErrorOr<ImmutableSet<String>>> atomicCachedSupportedElements =
      new AtomicReference<>();
  private final AtomicReference<ErrorOr<ImmutableMap<String, ValueRange<T>>>>
      atomicCachedElementToValueRangeMap = new AtomicReference<>();

  protected Action(
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

  protected Action(
      CarPropertyManagerCompat carPropertyManagerCompat,
      PermissionUtility permissionUtility,
      String actionName,
      int propertyId,
      Class<T> propertyTypeClazz,
      String requiredPermission,
      ImmutableBiMap<String, Integer> elementToArea,
      OptionalActionParameters<T> optionalActionParameters) {
    Preconditions.checkNotNull(carPropertyManagerCompat);
    Preconditions.checkNotNull(permissionUtility);
    Preconditions.checkNotNull(actionName);
    Preconditions.checkArgument(!actionName.isEmpty());
    Preconditions.checkNotNull(propertyTypeClazz);
    Preconditions.checkNotNull(requiredPermission);
    Preconditions.checkArgument(!requiredPermission.isEmpty());
    Preconditions.checkNotNull(elementToArea);
    Preconditions.checkArgument(!elementToArea.isEmpty());
    Preconditions.checkNotNull(optionalActionParameters);
    this.carPropertyManagerCompat = carPropertyManagerCompat;
    this.permissionUtility = permissionUtility;
    this.actionName = actionName;
    this.propertyId = propertyId;
    this.propertyName = VehiclePropertyIds.toString(propertyId);
    this.propertyTypeClazz = propertyTypeClazz;
    this.requiredPermission = requiredPermission;
    this.elementToArea = elementToArea;
    this.isMinMaxProperty = optionalActionParameters.isMinMaxProperty();
    this.requiredSupportedValues = optionalActionParameters.requiredSupportedValues();
    this.customValueRangeGenerator = optionalActionParameters.customValueRangeGenerator();

    Preconditions.checkArgument(
        !isMinMaxProperty
            || propertyTypeClazz.equals(Integer.class)
            || propertyTypeClazz.equals(Float.class));
    Preconditions.checkArgument(requiredSupportedValues.isEmpty() || isMinMaxProperty);
    Preconditions.checkArgument(
        requiredSupportedValues.isEmpty()
            || propertyTypeClazz.equals(Integer.class)
            || propertyTypeClazz.equals(Float.class));
  }

  public String getActionName() {
    return actionName;
  }

  public Class<T> getPropertyTypeClazz() {
    return propertyTypeClazz;
  }

  /**
   * Returns {@code true} if {@link #actionName} is supported on vehicle. Otherwise {@code false} or
   * an error code.
   */
  public abstract ErrorOr<Boolean> isActionSupported();

  protected ErrorOr<Boolean> isActionSupported(boolean requiresWriteAccess) {
    ErrorOr<Boolean> cachedIsActionSupportedResult = atomicCachedIsActionSupportedResult.get();
    if (cachedIsActionSupportedResult != null) {
      return cachedIsActionSupportedResult;
    }

    ErrorOr<Boolean> isActionSupported = generateIsActionSupported(requiresWriteAccess);
    if (!isActionSupported.isError() && isActionSupported.value()) {
      isActionSupported = determineIsActionSupportedFromDependencies();
    }

    atomicCachedIsActionSupportedResult.set(isActionSupported);
    return isActionSupported;
  }

  protected ErrorOr<Boolean> determineIsActionSupportedFromDependencies() {
    ErrorOr<Pair<ImmutableSet<String>, ImmutableMap<String, ValueRange<T>>>>
        supportedElementsAndValueRangeMap = generateSupportedElementsAndValueRangeMap();
    if (supportedElementsAndValueRangeMap.isError()) {
      return ErrorOr.createError(supportedElementsAndValueRangeMap.errorCode());
    }

    ImmutableSet<String> supportedElements = supportedElementsAndValueRangeMap.value().first;
    ImmutableMap<String, ValueRange<T>> elementToValueRangeMap =
        supportedElementsAndValueRangeMap.value().second;
    if (supportedElements.isEmpty()) {
      if (customValueRangeGenerator != null) {
        Log.d(
            TAG,
            "isActionSupported() - supportedElements is empty for custom level range action: "
                + actionName
                + " property ID: "
                + propertyName);
        // if supportedElements is empty for actions with custom level range logic,
        // then the action is not supported.
        return ErrorOr.createValue(false);
      }

      Log.e(
          TAG,
          "isActionSupported() - supportedElements is empty for action: "
              + actionName
              + " property ID: "
              + propertyName);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }

    atomicCachedSupportedElements.set(ErrorOr.createValue(supportedElements));
    atomicCachedElementToValueRangeMap.set(ErrorOr.createValue(elementToValueRangeMap));
    return ErrorOr.createValue(true);
  }

  private ErrorOr<Boolean> generateIsActionSupported(boolean requiresWriteAccess) {
    if (!permissionUtility.isPermissionGranted(requiredPermission)) {
      Log.e(
          TAG,
          "isActionSupported() - requiredPermission: "
              + requiredPermission
              + " is not granted for action: "
              + actionName
              + " property ID: "
              + propertyName);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_MISSING_REQUIRED_PERMISSION);
    }
    ErrorOr<Boolean> isPropertySupported = carPropertyManagerCompat.isPropertySupported(propertyId);
    if (isPropertySupported.isError()) {
      Log.e(
          TAG,
          "isActionSupported() - isPropertySupported returned an errorCode: "
              + isPropertySupported.errorCode()
              + " for action: "
              + actionName
              + " property ID: "
              + propertyName);
      return ErrorOr.createError(isPropertySupported.errorCode());
    }
    if (!isPropertySupported.value()) {
      Log.i(
          TAG,
          "isActionSupported() - isPropertySupported returned false for action: "
              + actionName
              + " property ID: "
              + propertyName);
      return ErrorOr.createValue(false);
    }

    ErrorOr<CarPropertyConfigCompat<T>> carPropertyConfigCompat =
        carPropertyManagerCompat.getConfig(propertyId);
    if (carPropertyConfigCompat.isError()) {
      Log.e(
          TAG,
          "isActionSupported() - getConfig returned an errorCode: "
              + carPropertyConfigCompat.errorCode()
              + " for action: "
              + actionName
              + " property ID: "
              + propertyName);
      return ErrorOr.createError(carPropertyConfigCompat.errorCode());
    }
    if (!carPropertyConfigCompat.value().propertyClazz().equals(propertyTypeClazz)) {
      Log.e(
          TAG,
          "isActionSupported() - getConfig returned a wrong class for action: "
              + actionName
              + " property ID: "
              + propertyName
              + " expected Clazz: "
              + propertyTypeClazz
              + " "
              + carPropertyConfigCompat.value());
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
    }
    if (!carPropertyConfigCompat.value().areAllAreasReadable()
        || (requiresWriteAccess && !carPropertyConfigCompat.value().areAllAreasWritable())) {
      Log.e(
          TAG,
          "isActionSupported() - getConfig does not have correct access for action: "
              + actionName
              + " property ID: "
              + propertyName
              + " "
              + carPropertyConfigCompat.value()
              + " expects read access: true and write access: "
              + requiresWriteAccess);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
    }

    return ErrorOr.createValue(true);
  }

  /** Returns a set of supported elements for this action. */
  public ErrorOr<ImmutableSet<String>> getSupportedElements() {
    Optional<ErrorCode> isActionSupportedErrorCode = checkIsActionSupported();
    if (isActionSupportedErrorCode.isPresent()) {
      Log.e(
          TAG,
          "getSupportedElements() - isActionSupported returned an errorCode: "
              + isActionSupportedErrorCode.get()
              + " for action: "
              + actionName
              + " property ID: "
              + propertyName);
      return ErrorOr.createError(isActionSupportedErrorCode.get());
    }

    return atomicCachedSupportedElements.get();
  }

  private ErrorOr<Pair<ImmutableSet<String>, ImmutableMap<String, ValueRange<T>>>>
      generateSupportedElementsAndValueRangeMap() {
    // getConfig is assumed to return a valid value since we check it in isActionSupported().
    ErrorOr<CarPropertyConfigCompat<T>> carPropertyConfigCompat =
        carPropertyManagerCompat.getConfig(propertyId);
    ImmutableSet.Builder<String> supportedElementsBuilder = ImmutableSet.builder();
    ImmutableMap.Builder<String, ValueRange<T>> elementToValueRangeMapBuilder =
        ImmutableMap.builder();

    for (int area : elementToArea.values()) {
      ErrorOr<Integer> areaId = carPropertyConfigCompat.value().getAreaId(area);
      if (areaId.isError()) {
        Log.d(
            TAG,
            "getSupportedElements() - area does not exist for action: "
                + actionName
                + " property ID: "
                + propertyName
                + " area: "
                + area);
        continue;
      }

      if (isMinMaxProperty) {
        T minValue =
            carPropertyConfigCompat
                .value()
                .areaIdToCarAreaConfig()
                .get(areaId.value())
                .minSupportedValue();
        T maxValue =
            carPropertyConfigCompat
                .value()
                .areaIdToCarAreaConfig()
                .get(areaId.value())
                .maxSupportedValue();

        if (propertyTypeClazz.equals(Integer.class)) {
          if (minValue == null || maxValue == null || (Integer) minValue >= (Integer) maxValue) {
            Log.e(
                TAG,
                "getValueRange() - getMin or getMax is missing or invalid for action: "
                    + actionName
                    + " property ID: "
                    + propertyName
                    + " area ID: "
                    + areaId
                    + " carPropertyConfig: "
                    + carPropertyConfigCompat.value());
            return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
          }
        } else if (propertyTypeClazz.equals(Float.class)) {
          if (minValue == null || maxValue == null || (Float) minValue >= (Float) maxValue) {
            Log.e(
                TAG,
                "getValueRange() - getMin or getMax is missing or invalid for action: "
                    + actionName
                    + " property ID: "
                    + propertyName
                    + " area ID: "
                    + areaId
                    + " carPropertyConfig: "
                    + carPropertyConfigCompat.value());
            return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
          }
        }
        ErrorOr<ValueRange<T>> valueRange =
            getValueRange(minValue, maxValue, carPropertyConfigCompat.value().configArray());

        if (valueRange.isError()) {
          if (valueRange.errorCode() == ErrorCode.ERROR_CODE_AREA_NOT_SUPPORTED) {
            Log.d(
                TAG,
                "getSupportedElements() - area is not supported for action: "
                    + actionName
                    + " property ID: "
                    + propertyName
                    + " area: "
                    + area);
            continue;
          }

          Log.e(
              TAG,
              "getSupportedElements() - getValueRange() returned an errorCode: "
                  + valueRange.errorCode()
                  + " for action: "
                  + actionName
                  + " property ID: "
                  + propertyName);
          return ErrorOr.createError(valueRange.errorCode());
        }

        if (!requiredSupportedValues.stream().allMatch(valueRange.value()::isValueSupported)) {
          Log.d(
              TAG,
              "generateSupportedElementsAndValueRangeMap() - requiredSupportedValues is not"
                  + " supported for valueRange: "
                  + valueRange.value()
                  + " requiredSupportedValues: "
                  + requiredSupportedValues
                  + " for action: "
                  + actionName
                  + " property ID: "
                  + propertyName
                  + " area: "
                  + area);
          return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
        }

        // elementToArea is bidirectional, thus allowing area to be a unique value.
        elementToValueRangeMapBuilder.put(elementToArea.inverse().get(area), valueRange.value());
      } else if (customValueRangeGenerator != null) {
        Log.d(
            TAG,
            "generateSupportedElementsAndValueRangeMap() - customValueRangeGenerator is not null"
                + " for action: "
                + actionName
                + " property ID: "
                + propertyName
                + " area: "
                + area);
        ErrorOr<ValueRange<T>> valueRange =
            customValueRangeGenerator.getValueRange(carPropertyManagerCompat, area);
        if (valueRange.isError()) {
          Log.e(
              TAG,
              "generateSupportedElementsAndValueRangeMap() -"
                  + " customValueRangeGenerator.getValueRange() returned an errorCode: "
                  + valueRange.errorCode()
                  + " for action: "
                  + actionName
                  + " property ID: "
                  + propertyName
                  + " area: "
                  + area);
          return ErrorOr.createError(valueRange.errorCode());
        }
        elementToValueRangeMapBuilder.put(elementToArea.inverse().get(area), valueRange.value());
      }

      supportedElementsBuilder.add(elementToArea.inverse().get(area));
    }

    return ErrorOr.createValue(
        Pair.create(
            supportedElementsBuilder.build(), elementToValueRangeMapBuilder.buildOrThrow()));
  }

  public ErrorOr<Boolean> doesElementSupportValue(String element, T value) {
    if (element == null) {
      Log.e(
          TAG,
          "doesElementSupportValue() - elements is null for action: "
              + actionName
              + " property ID: "
              + propertyName);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }
    if (value == null) {
      Log.e(
          TAG,
          "doesElementSupportValue() - value is null for action: "
              + actionName
              + " property ID: "
              + propertyName);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }

    Optional<ErrorCode> isActionSupportedErrorCode = checkIsActionSupported();
    if (isActionSupportedErrorCode.isPresent()) {
      Log.e(
          TAG,
          "doesElementSupportValue() - isActionSupported returned an errorCode: "
              + isActionSupportedErrorCode.get()
              + " for action: "
              + actionName
              + " property ID: "
              + propertyName);
      return ErrorOr.createError(isActionSupportedErrorCode.get());
    }

    ErrorOr<ImmutableSet<String>> supportedElements = getSupportedElements();
    if (supportedElements.isError()) {
      Log.e(
          TAG,
          "doesElementSupportValue() - getSupportedElements returned an errorCode: "
              + supportedElements.errorCode()
              + " for action: "
              + actionName
              + " property ID: "
              + propertyName);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }

    if (!supportedElements.value().contains(element)) {
      Log.e(
          TAG,
          "doesElementSupportValue() - supportedElements: "
              + supportedElements.value()
              + " does not contain element: "
              + element
              + " for action: "
              + actionName
              + " property ID: "
              + propertyName);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_ELEMENT_NOT_SUPPORTED);
    }

    if (propertyTypeClazz.equals(Boolean.class)) {
      return ErrorOr.createValue(true);
    } else if (propertyTypeClazz.equals(Integer.class) || propertyTypeClazz.equals(Float.class)) {
      // element is assumed to be in the cache since it is in the supportedElements set.
      ValueRange<T> valueRange = atomicCachedElementToValueRangeMap.get().value().get(element);
      return ErrorOr.createValue(valueRange.isValueSupported(value));
    } else {
      Log.e(
          TAG,
          "doesElementSupportValue() - propertyTypeClazz: "
              + propertyTypeClazz
              + " is not supported for action: "
              + actionName
              + " property ID: "
              + propertyName);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }
  }

  public ErrorOr<ImmutableMap<String, ValueRange<T>>> getElementToValueRangeMap() {
    Optional<ErrorCode> isActionSupportedErrorCode = checkIsActionSupported();
    if (isActionSupportedErrorCode.isPresent()) {
      Log.e(
          TAG,
          "getElementToValueRangeMap() - isActionSupported returned an errorCode: "
              + isActionSupportedErrorCode.get()
              + " for action: "
              + actionName
              + " property ID: "
              + propertyName);
      return ErrorOr.createError(isActionSupportedErrorCode.get());
    }

    return atomicCachedElementToValueRangeMap.get();
  }

  @SuppressWarnings("unchecked")
  private ErrorOr<ValueRange<T>> getValueRange(
      T minValue, T maxValue, ImmutableList<Integer> configArray) {
    if (customValueRangeGenerator != null) {
      return customValueRangeGenerator.getValueRange(minValue, maxValue, configArray);
    }
    ValueRange<T> valueRange;
    if (propertyTypeClazz.equals(Integer.class)) {
      valueRange = (ValueRange<T>) ValueRange.create((Integer) minValue, (Integer) maxValue);
    } else { // if (propertyTypeClazz.equals(Float.class)) {
      valueRange = (ValueRange<T>) ValueRange.create((Float) minValue, (Float) maxValue);
    }
    return ErrorOr.createValue(valueRange);
  }

  protected ErrorOr<ImmutableSet<Integer>> getAreas(Set<String> elements) {
    Optional<ErrorCode> isActionSupportedErrorCode = checkIsActionSupported();
    if (isActionSupportedErrorCode.isPresent()) {
      Log.e(
          TAG,
          "getAreas() - isActionSupported returned an errorCode: "
              + isActionSupportedErrorCode.get()
              + " for action: "
              + actionName
              + " property ID: "
              + propertyName);
      return ErrorOr.createError(isActionSupportedErrorCode.get());
    }

    ErrorOr<ImmutableSet<Integer>> areas = ActionUtils.getAreas(elements, elementToArea);
    if (areas.isError()) {
      Log.e(
          TAG,
          "getAreas() - getAreas() returned an errorCode: "
              + areas.errorCode()
              + " for elements: "
              + elements
              + " actionName: "
              + actionName
              + " property ID: "
              + propertyName);
      return ErrorOr.createError(areas.errorCode());
    }

    ErrorOr<Boolean> areAllAreasSupported =
        carPropertyManagerCompat.areAllAreasSupported(propertyId, areas.value());
    if (areAllAreasSupported.isError()) {
      Log.e(
          TAG,
          "getAreas() - areAllAreasSupported returned an errorCode: "
              + areAllAreasSupported.errorCode()
              + " for areas: "
              + areas.value()
              + " actionName: "
              + actionName
              + " property ID: "
              + propertyName);
      return ErrorOr.createError(areAllAreasSupported.errorCode());
    }
    if (!areAllAreasSupported.value()) {
      Log.e(
          TAG,
          "getAreas() - areAllAreasSupported returned false for areas: "
              + areas.value()
              + " actionName: "
              + actionName
              + " property ID: "
              + propertyName);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_ELEMENT_NOT_SUPPORTED);
    }
    return ErrorOr.createValue(areas.value());
  }

  private Optional<ErrorCode> checkIsActionSupported() {
    ErrorOr<Boolean> isActionSupported = isActionSupported();
    if (isActionSupported.isError()) {
      Log.e(
          TAG,
          "isActionSupported returned an errorCode: "
              + isActionSupported.errorCode()
              + " for action: "
              + actionName
              + " property ID: "
              + propertyName);
      return Optional.of(isActionSupported.errorCode());
    }
    if (!isActionSupported.value()) {
      Log.e(
          TAG,
          "isActionSupported returned false for action: "
              + actionName
              + " property ID: "
              + propertyName);
      return Optional.of(ErrorCode.ERROR_CODE_ACTION_NOT_SUPPORTED);
    }

    return Optional.empty();
  }

  /**
   * Gets the value of the property for the given elements.
   *
   * <p>All parameters must not be null and {@code elements} must not be empty. Values will be added
   * to the {@code elementToValueBuilder}, otherwise an error code will be added.
   */
  protected ErrorOr<ImmutableMap<String, ErrorOr<T>>> getInternal(Set<String> elements) {
    ErrorOr<ImmutableSet<Integer>> areas = getAreas(elements);
    if (areas.isError()) {
      Log.e(
          TAG,
          "getInternal() - getAreas() returned an errorCode: "
              + areas.errorCode()
              + " for elements: "
              + elements
              + " actionName: "
              + actionName
              + " property: "
              + propertyName);
      return ErrorOr.createError(areas.errorCode());
    }

    ErrorOr<ImmutableMap<Integer, ErrorOr<CarPropertyValueCompat<T>>>> areaToValue =
        carPropertyManagerCompat.getValues(propertyId, areas.value());
    if (areaToValue.isError()) {
      Log.e(
          TAG,
          "getInternal() - getValues returned an errorCode: "
              + areaToValue.errorCode()
              + " for areas: "
              + areas
              + " actionName: "
              + actionName
              + " property: "
              + propertyName);
      return ErrorOr.createError(areaToValue.errorCode());
    }

    ImmutableMap.Builder<String, ErrorOr<T>> elementToValueBuilder = ImmutableMap.builder();
    for (Map.Entry<Integer, ErrorOr<CarPropertyValueCompat<T>>> entry :
        areaToValue.value().entrySet()) {
      Integer area = entry.getKey();
      ErrorOr<CarPropertyValueCompat<T>> carPropertyValueCompat = entry.getValue();
      if (carPropertyValueCompat.isError()) {
        Log.e(
            TAG,
            "getInternal() - getValues returned an errorCode: "
                + carPropertyValueCompat.errorCode()
                + " for area: "
                + area
                + " actionName: "
                + actionName
                + " property: "
                + propertyName
                + " areaToValue"
                + areaToValue);
        elementToValueBuilder.put(
            elementToArea.inverse().get(area),
            ErrorOr.createError(carPropertyValueCompat.errorCode()));
      } else {
        elementToValueBuilder.put(
            elementToArea.inverse().get(area),
            ErrorOr.createValue(carPropertyValueCompat.value().value()));
      }
    }

    return ErrorOr.createValue(elementToValueBuilder.buildOrThrow());
  }

  /**
   * Sets the value of the property for the given elements.
   *
   * <p>All parameters must not be null and {@code elements} must not be empty. If any errors occur
   * they will be added to the {@code elementToErrorCodeBuilder}.
   */
  protected ErrorOr<ImmutableMap<String, ErrorCode>> setInternal(
      Set<String> elements, T valueToSet) {
    if (valueToSet == null) {
      Log.e(
          TAG,
          "setInternal() - valueToSet is null for action: "
              + actionName
              + " property: "
              + propertyName);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }

    ErrorOr<ImmutableSet<Integer>> areas = getAreas(elements);
    if (areas.isError()) {
      Log.e(
          TAG,
          "setInternal() - getAreas() returned an errorCode: "
              + areas.errorCode()
              + " for elements: "
              + elements
              + " actionName: "
              + actionName
              + " property: "
              + propertyName);
      return ErrorOr.createError(areas.errorCode());
    }

    ErrorOr<ImmutableMap<Integer, ErrorOr<CarPropertyValueCompat<T>>>> areaToUpdatedValue =
        carPropertyManagerCompat.setValueWithConfirmation(propertyId, areas.value(), valueToSet);
    if (areaToUpdatedValue.isError()) {
      Log.e(
          TAG,
          "setInternal() - setValueWithConfirmation returned an errorCode: "
              + areaToUpdatedValue.errorCode()
              + " for areas: "
              + areas.value()
              + " actionName: "
              + actionName
              + " property: "
              + propertyName);
      return ErrorOr.createError(areaToUpdatedValue.errorCode());
    }

    ImmutableMap.Builder<String, ErrorCode> elementToErrorCodeBuilder = ImmutableMap.builder();
    for (Map.Entry<Integer, ErrorOr<CarPropertyValueCompat<T>>> entry :
        areaToUpdatedValue.value().entrySet()) {
      String element = elementToArea.inverse().get(entry.getKey());
      ErrorOr<CarPropertyValueCompat<T>> carPropertyValueCompat = entry.getValue();
      if (!carPropertyValueCompat.isError()) {
        Log.d(TAG, "setInternal() successfully updated - " + carPropertyValueCompat.value());
        continue;
      }
      Log.e(
          TAG,
          "setInternal() - setValueWithConfirmation returned an errorCode: "
              + carPropertyValueCompat.errorCode()
              + " for area: "
              + entry.getKey()
              + " actionName: "
              + actionName
              + " property: "
              + propertyName);
      elementToErrorCodeBuilder.put(element, carPropertyValueCompat.errorCode());
    }

    return ErrorOr.createValue(elementToErrorCodeBuilder.buildOrThrow());
  }

  /** Checks if the given elements are supported by the action. */
  protected Optional<ErrorCode> checkAreElementsSupported(Set<String> elements) {
    if (elements == null) {
      Log.e(
          TAG,
          "checkAreElementsSupported() - elements is null for action: "
              + actionName
              + " property ID: "
              + propertyName);
      return Optional.of(ErrorCode.ERROR_CODE_INVALID_API_ARGUMENT);
    }
    if (elements.isEmpty()) {
      Log.e(
          TAG,
          "checkAreElementsSupported() - elements is empty for action: "
              + actionName
              + " property ID: "
              + propertyName);
      return Optional.of(ErrorCode.ERROR_CODE_INVALID_API_ARGUMENT);
    }

    Optional<ErrorCode> isActionSupportedErrorCode = checkIsActionSupported();
    if (isActionSupportedErrorCode.isPresent()) {
      Log.e(
          TAG,
          "checkAreElementsSupported() - isActionSupported returned an errorCode: "
              + isActionSupportedErrorCode.get()
              + " for action: "
              + actionName
              + " property ID: "
              + propertyName);
      return isActionSupportedErrorCode;
    }

    // getSupportedElements is assumed to return a valid value since we check it in
    // isActionSupported().
    ErrorOr<ImmutableSet<String>> supportedElements = getSupportedElements();
    if (supportedElements.isError()) {
      Log.e(
          TAG,
          "checkAreElementsSupported() - getSupportedElements returned an errorCode: "
              + supportedElements.errorCode()
              + " for action: "
              + actionName
              + " property ID: "
              + propertyName);
      return Optional.of(supportedElements.errorCode());
    }

    if (!supportedElements.value().containsAll(elements)) {
      Log.e(
          TAG,
          "checkAreElementsSupported() - elements argument contains unsupported elements: "
              + elements
              + " - supported elements: "
              + supportedElements.value()
              + " for action: "
              + actionName
              + " property ID: "
              + propertyName);
      return Optional.of(ErrorCode.ERROR_CODE_INVALID_API_ARGUMENT);
    }

    return Optional.empty();
  }
}

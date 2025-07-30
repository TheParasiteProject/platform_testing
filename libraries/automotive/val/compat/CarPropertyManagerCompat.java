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

package com.google.android.libraries.automotive.val.compat;

import static java.util.Arrays.stream;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.car.VehicleAreaType;
import android.car.VehiclePropertyIds;
import android.car.hardware.CarPropertyConfig;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.property.CarInternalErrorException;
import android.car.hardware.property.CarPropertyManager;
import android.car.hardware.property.PropertyAccessDeniedSecurityException;
import android.car.hardware.property.PropertyNotAvailableAndRetryException;
import android.car.hardware.property.PropertyNotAvailableException;
import android.os.Build;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import com.google.android.libraries.automotive.val.api.ErrorOr;
import com.google.android.libraries.automotive.val.api.ErrorOr.ErrorCode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import org.jspecify.annotations.Nullable;

/** Compatibility class that wraps the AAOS {@link CarPropertyManager}. */
public class CarPropertyManagerCompat {
  private static final String TAG = "CPMCompat";
  private static final ImmutableSet<Integer> VALID_CAR_PROPERTY_VALUE_STATUSES =
      ImmutableSet.of(
          CarPropertyValue.STATUS_AVAILABLE,
          CarPropertyValue.STATUS_UNAVAILABLE,
          CarPropertyValue.STATUS_ERROR);
  private static final ImmutableSet<Integer> VALID_ACCESSES =
      ImmutableSet.of(
          CarPropertyConfig.VEHICLE_PROPERTY_ACCESS_READ,
          CarPropertyConfig.VEHICLE_PROPERTY_ACCESS_READ_WRITE,
          CarPropertyConfig.VEHICLE_PROPERTY_ACCESS_WRITE);
  private static final ImmutableMap<Class<? extends Exception>, ErrorCode>
      EXCEPTION_CLAZZ_TO_ERROR_CODE =
          ImmutableMap.<Class<? extends Exception>, ErrorCode>builder()
              .put(CarInternalErrorException.class, ErrorCode.ERROR_CODE_PLATFORM_INTERNAL_ERROR)
              .put(
                  PropertyAccessDeniedSecurityException.class,
                  ErrorCode.ERROR_CODE_PROPERTY_ACCESS_DENIED_SECURITY)
              .put(
                  PropertyNotAvailableAndRetryException.class,
                  ErrorCode.ERROR_CODE_PROPERTY_NOT_AVAILABLE)
              .put(PropertyNotAvailableException.class, ErrorCode.ERROR_CODE_PROPERTY_NOT_AVAILABLE)
              .put(IllegalArgumentException.class, ErrorCode.ERROR_CODE_BAD_VAL_IMPL)
              .buildOrThrow();
  private static final float FLOAT_INEQUALITY_THRESHOLD = 0.00001f;
  private static final int GLOBAL_AREA_ID = 0;
  private final CarPropertyManager carPropertyManager;
  private final Object lock = new Object();

  @GuardedBy("lock")
  private final SparseArray<CarPropertyConfigCompat<?>> propertyIdToCarPropertyConfigCompat =
      new SparseArray<>();

  private Callable<Void> afterSetPropertyFunc = null;

  public CarPropertyManagerCompat(CarPropertyManager carPropertyManager) {
    Preconditions.checkNotNull(carPropertyManager);
    this.carPropertyManager = carPropertyManager;
  }

  /**
   * Returns {@code true} if {@code propertyId} is supported on vehicle. Otherwise {@code false} or
   * an error code.
   */
  public ErrorOr<Boolean> isPropertySupported(int propertyId) {
    ErrorOr<CarPropertyConfigCompat<Object>> carPropertyConfigCompat = getConfig(propertyId);
    if (carPropertyConfigCompat.isError()) {
      if (carPropertyConfigCompat.errorCode() == ErrorCode.ERROR_CODE_PROPERTY_NOT_SUPPORTED) {
        return ErrorOr.createValue(false);
      } else {
        return ErrorOr.createError(carPropertyConfigCompat.errorCode());
      }
    }
    return ErrorOr.createValue(true);
  }

  /** Returns {@link CarPropertyConfig} for {@code propertyId}. Otherwise an error code. */
  public <T> ErrorOr<CarPropertyConfigCompat<T>> getConfig(int propertyId) {
    @SuppressWarnings("unchecked")
    CarPropertyConfigCompat<T> cachedCarPropertyConfigCompat =
        (CarPropertyConfigCompat<T>) getCachedConfig(propertyId);
    if (cachedCarPropertyConfigCompat != null) {
      return ErrorOr.createValue(cachedCarPropertyConfigCompat);
    }
    ErrorOr<CarPropertyConfigCompat<T>> carPropertyConfigCompat =
        generateCarPropertyConfigCompat(propertyId);
    if (!carPropertyConfigCompat.isError()) {
      cacheConfig(carPropertyConfigCompat.value());
    }
    return carPropertyConfigCompat;
  }

  @SuppressWarnings("unchecked")
  private <T> ErrorOr<CarPropertyConfigCompat<T>> generateCarPropertyConfigCompat(int propertyId) {
    @SuppressWarnings("rawtypes")
    List<CarPropertyConfig> carPropertyConfigs =
        carPropertyManager.getPropertyList(new ArraySet<Integer>(ImmutableList.of(propertyId)));
    if (carPropertyConfigs == null) {
      Log.e(
          TAG,
          "generateCarPropertyConfigCompat() - getPropertyList returned null for "
              + VehiclePropertyIds.toString(propertyId));
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
    }
    if (carPropertyConfigs.isEmpty()) {
      Log.d(
          TAG,
          "generateCarPropertyConfigCompat() - No CarPropertyConfig found for "
              + VehiclePropertyIds.toString(propertyId));
      return ErrorOr.createError(ErrorCode.ERROR_CODE_PROPERTY_NOT_SUPPORTED);
    }
    if (carPropertyConfigs.size() > 1) {
      Log.e(
          TAG,
          "generateCarPropertyConfigCompat() - More than one CarPropertyConfig found for "
              + VehiclePropertyIds.toString(propertyId)
              + " - carPropertyConfigs: "
              + carPropertyConfigs);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
    }

    CarPropertyConfig<Object> carPropertyConfig = carPropertyConfigs.get(0);
    if (carPropertyConfig.getPropertyId() != propertyId) {
      Log.e(
          TAG,
          "generateCarPropertyConfigCompat() - CarPropertyConfig found does not have correct"
              + " propertyId for "
              + VehiclePropertyIds.toString(propertyId)
              + " - carPropertyConfigs: "
              + carPropertyConfigs);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
    }
    if (carPropertyConfig.getPropertyType() == null) {
      Log.e(
          TAG,
          "generateCarPropertyConfigCompat() - CarPropertyConfig#getPropertyType() returned null"
              + " for "
              + VehiclePropertyIds.toString(propertyId));
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
    }
    if (carPropertyConfig.getAreaIds() == null) {
      Log.e(
          TAG,
          "generateCarPropertyConfigCompat() - CarPropertyConfig#getAreaIds() returned null for "
              + VehiclePropertyIds.toString(propertyId));
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
    }
    if (carPropertyConfig.getAreaIds().length == 0) {
      Log.e(
          TAG,
          "generateCarPropertyConfigCompat() - CarPropertyConfig#getAreaIds() returned empty array"
              + " for "
              + VehiclePropertyIds.toString(propertyId));
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
    }
    if (carPropertyConfig.getAreaType() == VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL
        && (carPropertyConfig.getAreaIds().length > 1
            || carPropertyConfig.getAreaIds()[0] != GLOBAL_AREA_ID)) {
      Log.e(
          TAG,
          "generateCarPropertyConfigCompat() - CarPropertyConfig#getAreaType() returned "
              + "VEHICLE_AREA_TYPE_GLOBAL but CarPropertyConfig#getAreaIds() returned more than one"
              + " area ID or an area ID other than 0 for "
              + VehiclePropertyIds.toString(propertyId));
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
    }
    if (carPropertyConfig.getAreaType() != VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL
        && stream(carPropertyConfig.getAreaIds()).anyMatch(areaId -> areaId == GLOBAL_AREA_ID)) {
      Log.e(
          TAG,
          "generateCarPropertyConfigCompat() - CarPropertyConfig#getAreaType() returned non"
              + " VEHICLE_AREA_TYPE_GLOBAL but CarPropertyConfig#getAreaIds() returned an area ID"
              + " equal to 0 for "
              + VehiclePropertyIds.toString(propertyId));
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
    }
    if (!VALID_ACCESSES.contains(carPropertyConfig.getAccess())) {
      Log.e(
          TAG,
          "generateCarPropertyConfigCompat() - CarPropertyConfig#getAccess() returned invalid"
              + " access for "
              + VehiclePropertyIds.toString(propertyId)
              + " - carPropertyConfigs: "
              + carPropertyConfigs
              + " - VALID_ACCESSES: "
              + VALID_ACCESSES);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
    }
    if (carPropertyConfig.getConfigArray() == null) {
      Log.e(
          TAG,
          "generateCarPropertyConfigCompat() - CarPropertyConfig#getConfigArray() returned null for"
              + " "
              + VehiclePropertyIds.toString(propertyId));
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
    }

    return ErrorOr.createValue(CompatUtils.toCompat(carPropertyConfigs.get(0)));
  }

  /**
   * Returns {@code true} if all {@code areas} for the {@code propertyId} are supported on vehicle.
   * Otherwise {@code false} or an error code.
   */
  public ErrorOr<Boolean> areAllAreasSupported(int propertyId, ImmutableSet<Integer> areas) {
    if (areas == null) {
      Log.e(
          TAG,
          "areAllAreasSupported() - This shouldn't happen! areas is null for "
              + VehiclePropertyIds.toString(propertyId));
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }
    if (areas.isEmpty()) {
      Log.e(
          TAG,
          "areAllAreasSupported() - This shouldn't happen! areas is empty for "
              + VehiclePropertyIds.toString(propertyId));
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }

    ErrorOr<CarPropertyConfigCompat<Object>> carPropertyConfigCompat = getConfig(propertyId);
    if (carPropertyConfigCompat.isError()) {
      Log.e(
          TAG,
          "areAllAreasSupported() - getConfig() returned error for "
              + VehiclePropertyIds.toString(propertyId)
              + " areas: "
              + areas
              + " - errorCode: "
              + carPropertyConfigCompat.errorCode());
      return ErrorOr.createError(carPropertyConfigCompat.errorCode());
    }

    for (Integer area : areas) {
      ErrorOr<Integer> areaId = carPropertyConfigCompat.value().getAreaId(area);
      if (areaId.isError()) {
        Log.d(
            TAG,
            "areAllAreasSupported() - getAreaId() returned error for "
                + VehiclePropertyIds.toString(propertyId)
                + " area: "
                + area
                + " areas: "
                + areas
                + " "
                + carPropertyConfigCompat);
        return ErrorOr.createValue(false);
      }
    }
    return ErrorOr.createValue(true);
  }

  /**
   * Returns an {@link ImmutableMap} of an area to an {@link ErrorOr} of a {@link
   * CarPropertyValueCompat} for all {@code propertyId} and {@code areas} combos. Otherwise an error
   * code.
   */
  public <T> ErrorOr<ImmutableMap<Integer, ErrorOr<CarPropertyValueCompat<T>>>> getValues(
      int propertyId, ImmutableSet<Integer> areas) {
    if (areas == null) {
      Log.e(
          TAG,
          "getValue() - This shouldn't happen! areas is null for "
              + VehiclePropertyIds.toString(propertyId));
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }
    if (areas.isEmpty()) {
      Log.e(
          TAG,
          "getValue() - This shouldn't happen! areas is empty for "
              + VehiclePropertyIds.toString(propertyId));
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }

    ErrorOr<CarPropertyConfigCompat<T>> carPropertyConfigCompat = getConfig(propertyId);
    if (carPropertyConfigCompat.isError()) {
      Log.e(
          TAG,
          "getValues() - getConfig() returned error for "
              + VehiclePropertyIds.toString(propertyId)
              + " areas: "
              + areas
              + " - errorCode: "
              + carPropertyConfigCompat.errorCode());
      return ErrorOr.createError(carPropertyConfigCompat.errorCode());
    }

    ImmutableMap.Builder<Integer, ErrorOr<CarPropertyValueCompat<T>>>
        areaToCarPropertyValueCompatBuilder = ImmutableMap.builder();
    SparseArray<ErrorOr<CarPropertyValueCompat<T>>> areaIdToCarPropertyValueCompat =
        new SparseArray<>(0);
    for (Integer area : areas) {
      ErrorOr<Integer> areaId = carPropertyConfigCompat.value().getAreaId(area);
      if (areaId.isError()) {
        Log.e(
            TAG,
            "getValue() - getAreaId returned an error for "
                + VehiclePropertyIds.toString(propertyId)
                + " area: "
                + area
                + " - errorCode: "
                + areaId.errorCode());
        areaToCarPropertyValueCompatBuilder.put(area, ErrorOr.createError(areaId.errorCode()));
        continue;
      }
      if (areaIdToCarPropertyValueCompat.indexOfKey(areaId.value()) >= 0) {
        areaToCarPropertyValueCompatBuilder.put(
            area, areaIdToCarPropertyValueCompat.get(areaId.value()));
        continue;
      }
      if (!carPropertyConfigCompat
          .value()
          .areaIdToCarAreaConfig()
          .get(areaId.value())
          .isReadable()) {
        Log.e(
            TAG,
            "getValue() - area is not readable for "
                + VehiclePropertyIds.toString(propertyId)
                + " area: "
                + area
                + " areaId: "
                + areaId);
        ErrorOr<CarPropertyValueCompat<T>> areaNotReadableError =
            ErrorOr.createError(ErrorCode.ERROR_CODE_AREA_NOT_READABLE);
        areaToCarPropertyValueCompatBuilder.put(area, areaNotReadableError);
        areaIdToCarPropertyValueCompat.put(areaId.value(), areaNotReadableError);
        continue;
      }

      ErrorOr<CarPropertyValueCompat<T>> carPropertyValueCompat =
          getValue(carPropertyConfigCompat.value().propertyClazz(), propertyId, areaId.value());
      if (carPropertyValueCompat.isError()) {
        Log.e(
            TAG,
            "getValues() - getValue() returned an error for "
                + VehiclePropertyIds.toString(propertyId)
                + " area: "
                + area
                + " areaId: "
                + areaId
                + " - errorCode: "
                + carPropertyValueCompat.errorCode());
      }
      areaToCarPropertyValueCompatBuilder.put(area, carPropertyValueCompat);
      areaIdToCarPropertyValueCompat.put(areaId.value(), carPropertyValueCompat);
    }
    return ErrorOr.createValue(areaToCarPropertyValueCompatBuilder.buildOrThrow());
  }

  /**
   * Sets all the {@code propertyId} and {@code areas} combos to the {@code valueToSet}.
   *
   * <p>Returns an {@link ImmutableMap} of an {@code areas} to {@link CarPropertyValueCompat} for
   * all {@code propertyId} and {@code areas} combos if the set was successful. Otherwise an error
   * code.
   */
  public <T>
      ErrorOr<ImmutableMap<Integer, ErrorOr<CarPropertyValueCompat<T>>>> setValueWithConfirmation(
          int propertyId, ImmutableSet<Integer> areas, T valueToSet) {
    if (areas == null) {
      Log.e(
          TAG,
          "setValueWithConfirmation() - This shouldn't happen! areas is null for "
              + VehiclePropertyIds.toString(propertyId)
              + " valueToSet: "
              + valueToSet);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }
    if (areas.isEmpty()) {
      Log.e(
          TAG,
          "setValueWithConfirmation() - This shouldn't happen! areas is empty for "
              + VehiclePropertyIds.toString(propertyId)
              + " valueToSet: "
              + valueToSet);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }
    if (valueToSet == null) {
      Log.e(
          TAG,
          "setValueWithConfirmation() - This shouldn't happen! valueToSet is null for "
              + VehiclePropertyIds.toString(propertyId)
              + " areas: "
              + areas);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }

    ErrorOr<CarPropertyConfigCompat<T>> carPropertyConfigCompat = getConfig(propertyId);
    if (carPropertyConfigCompat.isError()) {
      Log.e(
          TAG,
          "setValueWithConfirmation() - getConfig returned error for "
              + VehiclePropertyIds.toString(propertyId)
              + " areas: "
              + areas
              + " valueToSet: "
              + valueToSet
              + " - errorCode: "
              + carPropertyConfigCompat.errorCode());
      return ErrorOr.createError(carPropertyConfigCompat.errorCode());
    }

    if (!carPropertyConfigCompat.value().propertyClazz().equals(valueToSet.getClass())) {
      Log.e(
          TAG,
          "setValueWithConfirmation - valueToSet: "
              + valueToSet
              + " type mismatch with property "
              + carPropertyConfigCompat.value()
              + " areas: "
              + areas);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }

    ErrorOr<ImmutableMap<Integer, ErrorOr<CarPropertyValueCompat<T>>>> areaToCurrentValue =
        getValues(propertyId, areas);
    if (areaToCurrentValue.isError()) {
      Log.e(
          TAG,
          "setValueWithConfirmation() - This should not happen! getValues returned an error for "
              + VehiclePropertyIds.toString(propertyId)
              + " areas: "
              + areas
              + " valueToSet: "
              + valueToSet
              + " - errorCode: "
              + areaToCurrentValue.errorCode());
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }

    ImmutableMap.Builder<Integer, ErrorOr<CarPropertyValueCompat<T>>> areaToOutputValueBuilder =
        ImmutableMap.builder();
    ImmutableSet.Builder<Integer> areaIdsToUpdateBuilder = ImmutableSet.builder();
    for (Integer area : areaToCurrentValue.value().keySet()) {
      ErrorOr<CarPropertyValueCompat<T>> currentValue = areaToCurrentValue.value().get(area);
      if (currentValue.isError()) {
        Log.e(
            TAG,
            "setValueWithConfirmation() - skipping setting value because getValues returned an"
                + " error for "
                + VehiclePropertyIds.toString(propertyId)
                + " area: "
                + area
                + " valueToSet: "
                + valueToSet
                + " - errorCode: "
                + currentValue.errorCode());
        areaToOutputValueBuilder.put(area, currentValue);
        continue;
      }
      if (valueEquals(valueToSet, currentValue.value().value())) {
        Log.e(
            TAG,
            "setValueWithConfirmation - skipping because propertyId: "
                + VehiclePropertyIds.toString(propertyId)
                + " area: "
                + area
                + " valueToSet: "
                + valueToSet
                + " is already equal to the current value: "
                + currentValue.value());
        areaToOutputValueBuilder.put(
            area, ErrorOr.createError(ErrorCode.ERROR_CODE_VALUE_ALREADY_SET));
        continue;
      }
      areaIdsToUpdateBuilder.add(currentValue.value().areaId());
    }
    ImmutableSet<Integer> areaIdsToUpdate = areaIdsToUpdateBuilder.build();

    if (areaIdsToUpdate.isEmpty()) {
      Log.w(
          TAG,
          "setValueWithConfirmation - skipping because areaIdsToUpdate is empty - propertyId: "
              + VehiclePropertyIds.toString(propertyId));
      return ErrorOr.createValue(areaToOutputValueBuilder.buildOrThrow());
    }

    ErrorOr<ImmutableMap<Integer, ErrorOr<CarPropertyValueCompat<T>>>> areaIdToUpdatedValue =
        setValueWithConfirmation(
            carPropertyConfigCompat.value(), propertyId, areaIdsToUpdate, valueToSet);
    if (areaIdToUpdatedValue.isError()) {
      Log.e(
          TAG,
          "setValueWithConfirmation() - setValueWithConfirmation returned an error for "
              + VehiclePropertyIds.toString(propertyId)
              + " areas: "
              + areas
              + " areaIdsToUpdate: "
              + areaIdsToUpdate
              + " valueToSet: "
              + valueToSet
              + " - errorCode: "
              + areaIdToUpdatedValue.errorCode());
      return areaIdToUpdatedValue;
    }

    for (Integer area : areaToCurrentValue.value().keySet()) {
      ErrorOr<CarPropertyValueCompat<T>> previousValue = areaToCurrentValue.value().get(area);
      if (previousValue.isError()) {
        continue;
      }
      if (!areaIdsToUpdate.contains(previousValue.value().areaId())) {
        continue;
      }
      areaToOutputValueBuilder.put(
          area, areaIdToUpdatedValue.value().get(previousValue.value().areaId()));
    }

    ImmutableMap<Integer, ErrorOr<CarPropertyValueCompat<T>>> areaToOutputValue =
        areaToOutputValueBuilder.buildOrThrow();

    if (!areas.equals(areaToOutputValue.keySet())) {
      Log.e(
          TAG,
          "setValueWithConfirmation() - areas did not match areas from set output! property ID: "
              + VehiclePropertyIds.toString(propertyId)
              + " - expected: "
              + areas
              + " -  set output:"
              + areaToOutputValue
              + " valueToSet: "
              + valueToSet);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
    }

    return ErrorOr.createValue(areaToOutputValue);
  }

  private <T>
      ErrorOr<ImmutableMap<Integer, ErrorOr<CarPropertyValueCompat<T>>>> setValueWithConfirmation(
          CarPropertyConfigCompat<T> carPropertyConfigCompat,
          int propertyId,
          ImmutableSet<Integer> areaIds,
          T valueToSet) {
    SetConfirmationCallback<T> setConfirmationCallback =
        new SetConfirmationCallback<T>(propertyId, areaIds, valueToSet);
    if (!carPropertyManager.registerCallback(
        setConfirmationCallback, propertyId, CarPropertyManager.SENSOR_RATE_FASTEST)) {
      Log.e(
          TAG,
          "setValueWithConfirmation() - Failed to register set confirmation callback for"
              + " propertyId: "
              + VehiclePropertyIds.toString(propertyId)
              + " areaIds: "
              + areaIds
              + " valueToSet: "
              + valueToSet);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
    }

    ImmutableMap.Builder<Integer, ErrorOr<CarPropertyValueCompat<T>>> areaIdToUpdatedValueBuilder =
        ImmutableMap.builder();
    ImmutableSet.Builder<Integer> areaIdsThatSetThrewAnExceptionBuilder = ImmutableSet.builder();
    for (Integer areaId : areaIds) {
      if (!carPropertyConfigCompat.areaIdToCarAreaConfig().get(areaId).isWritable()) {
        Log.e(
            TAG,
            "setValueWithConfirmation() - areaId: "
                + areaId
                + " is not writeable for propertyId: "
                + VehiclePropertyIds.toString(propertyId));
        areaIdToUpdatedValueBuilder.put(
            areaId, ErrorOr.createError(ErrorCode.ERROR_CODE_AREA_NOT_WRITABLE));
        areaIdsThatSetThrewAnExceptionBuilder.add(areaId);
        continue;
      }

      try {
        carPropertyManager.setProperty(
            carPropertyConfigCompat.propertyClazz(), propertyId, areaId, valueToSet);
      } catch (CarInternalErrorException
          | PropertyAccessDeniedSecurityException
          | PropertyNotAvailableAndRetryException
          | PropertyNotAvailableException
          | IllegalArgumentException e) {
        Log.e(
            TAG,
            "setValueWithConfirmation() - Received CarInternalErrorException from"
                + " setProperty on propertyId: "
                + VehiclePropertyIds.toString(propertyId)
                + " areaId: "
                + areaId
                + " valueToSet: "
                + valueToSet
                + " exception: "
                + e);
        areaIdToUpdatedValueBuilder.put(
            areaId, ErrorOr.createError(EXCEPTION_CLAZZ_TO_ERROR_CODE.get(e.getClass())));
        areaIdsThatSetThrewAnExceptionBuilder.add(areaId);
        continue;
      } catch (IllegalStateException e) {
        Log.e(
            TAG,
            "setValueWithConfirmation() - Received IllegalStateException from setProperty"
                + " on propertyId: "
                + VehiclePropertyIds.toString(propertyId)
                + " areaId: "
                + areaId
                + " valueToSet: "
                + valueToSet
                + " exception: "
                + e);
        areaIdToUpdatedValueBuilder.put(
            areaId, ErrorOr.createError(ErrorCode.ERROR_CODE_PLATFORM_INTERNAL_ERROR));
        areaIdsThatSetThrewAnExceptionBuilder.add(areaId);
        continue;
      } catch (RuntimeException e) {
        Log.e(
            TAG,
            "setValueWithConfirmation() - Received RuntimeException from setProperty on"
                + " propertyId: "
                + VehiclePropertyIds.toString(propertyId)
                + " areaId: "
                + areaId
                + " valueToSet: "
                + valueToSet
                + " exception: "
                + e);
        areaIdToUpdatedValueBuilder.put(
            areaId, ErrorOr.createError(ErrorCode.ERROR_CODE_PROPERTY_NOT_AVAILABLE));
        areaIdsThatSetThrewAnExceptionBuilder.add(areaId);
        continue;
      }
    }

    if (afterSetPropertyFunc != null) {
      try {
        afterSetPropertyFunc.call();
      } catch (Exception e) {
        Log.e(
            TAG,
            "setValueWithConfirmation() - Received Exception from afterSetPropertyFunc - "
                + " propertyId: "
                + VehiclePropertyIds.toString(propertyId)
                + " areaIds: "
                + areaIds
                + " valueToSet: "
                + valueToSet
                + " exception: "
                + e);
      }
    }

    ErrorOr<ImmutableMap<Integer, ErrorOr<CarPropertyValueCompat<T>>>> areaIdToSetResultValue =
        setConfirmationCallback.waitForSetPropertyEvents(
            areaIdsThatSetThrewAnExceptionBuilder.build());
    carPropertyManager.unregisterCallback(setConfirmationCallback, propertyId);

    if (areaIdToSetResultValue.isError()) {
      Log.e(
          TAG,
          "setValueWithConfirmation() - setProperty returned an error for "
              + VehiclePropertyIds.toString(propertyId)
              + " areaIds: "
              + areaIds
              + " valueToSet: "
              + valueToSet
              + " - errorCode: "
              + areaIdToSetResultValue.errorCode());
      return areaIdToSetResultValue;
    }

    for (Integer areaId : areaIdToSetResultValue.value().keySet()) {
      areaIdToUpdatedValueBuilder.put(areaId, areaIdToSetResultValue.value().get(areaId));
    }

    ImmutableMap<Integer, ErrorOr<CarPropertyValueCompat<T>>> areaIdToUpdatedValue =
        areaIdToUpdatedValueBuilder.buildOrThrow();
    if (!areaIds.equals(areaIdToUpdatedValue.keySet())) {
      Log.e(
          TAG,
          "setValueWithConfirmation() - areaIds did not match areaIds from set output!"
              + " property ID: "
              + VehiclePropertyIds.toString(propertyId)
              + " - expected: "
              + areaIds
              + " -  set output:"
              + areaIdToUpdatedValue
              + " valueToSet: "
              + valueToSet);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
    }

    return ErrorOr.createValue(areaIdToUpdatedValue);
  }

  /**
   * Internal method that gets the {@link CarPropertyValueCompat} for one {@code propertyId} and
   * {@code areaId} combo. Otherwise returns an error code.
   */
  private <T> ErrorOr<CarPropertyValueCompat<T>> getValue(
      Class<T> propertyValueClazz, int propertyId, int areaId) {
    CarPropertyValue<T> carPropertyValue;
    try {
      carPropertyValue = carPropertyManager.getProperty(propertyId, areaId);
    } catch (CarInternalErrorException
        | PropertyAccessDeniedSecurityException
        | PropertyNotAvailableAndRetryException
        | PropertyNotAvailableException
        | IllegalArgumentException e) {
      Log.e(
          TAG,
          "getValue() - Received "
              + e.getClass().getName()
              + " from "
              + "CarPropertyManager#getProperty() on propertyId: "
              + VehiclePropertyIds.toString(propertyId)
              + " areaId: "
              + areaId
              + " exception: "
              + e);
      return ErrorOr.createError(EXCEPTION_CLAZZ_TO_ERROR_CODE.get(e.getClass()));
    } catch (IllegalStateException e) {
      Log.e(
          TAG,
          "getValue() - Received IllegalStateException "
              + "from "
              + "CarPropertyManager#getProperty() on propertyId: "
              + VehiclePropertyIds.toString(propertyId)
              + " areaId: "
              + areaId
              + " exception: "
              + e);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_PLATFORM_INTERNAL_ERROR);
    }

    if (carPropertyValue == null) {
      Log.e(
          TAG,
          "getValue() - returned null CarPropertyValue for propertyId: "
              + VehiclePropertyIds.toString(propertyId)
              + " areaId: "
              + areaId);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
      } else {
        return ErrorOr.createError(ErrorCode.ERROR_CODE_PROPERTY_NOT_AVAILABLE);
      }
    }
    if (carPropertyValue.getPropertyId() != propertyId) {
      Log.e(
          TAG,
          "getValue() - propertyId did not match expected "
              + "propertyId: "
              + propertyId
              + " - "
              + carPropertyValue);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
    }
    if (carPropertyValue.getAreaId() != areaId) {
      Log.e(
          TAG,
          "getValue() - areaId did not match expected areaId: "
              + areaId
              + " - "
              + carPropertyValue);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
    }
    if (!VALID_CAR_PROPERTY_VALUE_STATUSES.contains(carPropertyValue.getStatus())) {
      Log.e(
          TAG,
          "getValue() - status not a valid status: "
              + VALID_CAR_PROPERTY_VALUE_STATUSES
              + " - "
              + carPropertyValue);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
    }
    if (carPropertyValue.getStatus() != CarPropertyValue.STATUS_AVAILABLE) {
      Log.e(TAG, "getValue() - status not available  - " + carPropertyValue);
      return ErrorOr.createError(
          carPropertyValue.getStatus() == CarPropertyValue.STATUS_UNAVAILABLE
              ? ErrorCode.ERROR_CODE_PROPERTY_NOT_AVAILABLE
              : ErrorCode.ERROR_CODE_PLATFORM_INTERNAL_ERROR);
    }
    if (carPropertyValue.getTimestamp() >= SystemClock.elapsedRealtimeNanos()) {
      Log.e(TAG, "getValue() - timestamp too new - " + carPropertyValue);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
    }
    if (carPropertyValue.getValue() == null) {
      Log.e(TAG, "getValue() - returned null value - " + carPropertyValue);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
    }
    if (!carPropertyValue.getValue().getClass().equals(propertyValueClazz)) {
      Log.e(
          TAG,
          "getValue() - value type did not match expected: "
              + propertyValueClazz
              + " - "
              + carPropertyValue);
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
    }
    return ErrorOr.createValue(CarPropertyValueCompat.create(carPropertyValue));
  }

  private void cacheConfig(CarPropertyConfigCompat<?> carPropertyConfigCompat) {
    synchronized (lock) {
      propertyIdToCarPropertyConfigCompat.put(
          carPropertyConfigCompat.propertyId(), carPropertyConfigCompat);
    }
  }

  private @Nullable CarPropertyConfigCompat<?> getCachedConfig(int propertyId) {
    synchronized (lock) {
      return propertyIdToCarPropertyConfigCompat.get(propertyId);
    }
  }

  private static class SetConfirmationCallback<T>
      implements CarPropertyManager.CarPropertyEventCallback {
    private static final ImmutableMap<Integer, ErrorCode>
        SET_PROPERTY_ERROR_CODE_TO_ERROR_OR_ERROR_CODE =
            ImmutableMap.of(
                CarPropertyManager.CAR_SET_PROPERTY_ERROR_CODE_TRY_AGAIN,
                ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL,
                CarPropertyManager.CAR_SET_PROPERTY_ERROR_CODE_INVALID_ARG,
                ErrorCode.ERROR_CODE_BAD_VAL_IMPL,
                CarPropertyManager.CAR_SET_PROPERTY_ERROR_CODE_PROPERTY_NOT_AVAILABLE,
                ErrorCode.ERROR_CODE_PROPERTY_NOT_AVAILABLE,
                CarPropertyManager.CAR_SET_PROPERTY_ERROR_CODE_ACCESS_DENIED,
                ErrorCode.ERROR_CODE_PROPERTY_ACCESS_DENIED_SECURITY,
                CarPropertyManager.CAR_SET_PROPERTY_ERROR_CODE_UNKNOWN,
                ErrorCode.ERROR_CODE_PLATFORM_INTERNAL_ERROR);

    private final int propertyId;
    private final ImmutableSet<Integer> areaIds;
    private final CountDownLatch countDownLatch;
    private final T expectedSetValue;
    private final long creationTimeNanos = SystemClock.elapsedRealtimeNanos();
    private final Object lock = new Object();

    @GuardedBy("lock")
    private final HashSet<Integer> areaIdsThatReceivedAnEvent = new HashSet<>();

    @GuardedBy("lock")
    private final ImmutableMap.Builder<Integer, ErrorOr<CarPropertyValueCompat<T>>>
        areaIdToSetCarPropertyValueBuilder = ImmutableMap.builder();

    SetConfirmationCallback(int propertyId, ImmutableSet<Integer> areaIds, T expectedSetValue) {
      Preconditions.checkNotNull(areaIds);
      Preconditions.checkArgument(!areaIds.isEmpty());
      Preconditions.checkNotNull(expectedSetValue);

      this.propertyId = propertyId;
      this.areaIds = areaIds;
      countDownLatch = new CountDownLatch(areaIds.size());
      this.expectedSetValue = expectedSetValue;
    }

    public ErrorOr<ImmutableMap<Integer, ErrorOr<CarPropertyValueCompat<T>>>>
        waitForSetPropertyEvents(ImmutableSet<Integer> areaIdsThatSetThrewAnException) {
      synchronized (lock) {
        for (int areaId : areaIdsThatSetThrewAnException) {
          if (areaIdsThatReceivedAnEvent.contains(areaId)) {
            Log.e(
                TAG,
                "SetConfirmationCallback - waitForSetPropertyEvents() - This shouldn't happen!!"
                    + " Received an event for an area ID that threw an exception when setProperty()"
                    + " was called! propertyId: "
                    + VehiclePropertyIds.toString(propertyId)
                    + " areaId: "
                    + areaId
                    + " expectedValue: "
                    + expectedSetValue
                    + " areaIds: "
                    + areaIds
                    + " areaIdsThatReceivedAnEvent: "
                    + areaIdsThatReceivedAnEvent
                    + " areaIdsThatThrewAnException: "
                    + areaIdsThatSetThrewAnException);
            return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
          }
          areaIdsThatReceivedAnEvent.add(areaId);
          countDownLatch.countDown();
        }
      }
      try {
        if (!countDownLatch.await(5, SECONDS)) {
          synchronized (lock) {
            Log.e(
                TAG,
                "SetConfirmationCallback - Received only "
                    + areaIdsThatReceivedAnEvent.size()
                    + " out of "
                    + areaIds.size()
                    + " SET events for "
                    + VehiclePropertyIds.toString(propertyId)
                    + " areaIds: "
                    + areaIds
                    + " expectedSetValue: "
                    + expectedSetValue
                    + "  before 5s timeout.");
            finishAnyUnfinishedAreaIdWithErrorCode(
                ErrorCode.ERROR_CODE_SET_PROPERTY_CALLBACK_TIMED_OUT);
          }
        }
      } catch (InterruptedException e) {
        Log.e(
            TAG,
            "SetConfirmationCallback - Waiting for set events for "
                + VehiclePropertyIds.toString(propertyId)
                + " areaIds: "
                + areaIds
                + " expectedSetValue: "
                + expectedSetValue
                + " threw an exception: "
                + e);
        finishAnyUnfinishedAreaIdWithErrorCode(
            ErrorCode.ERROR_CODE_SET_PROPERTY_CALLBACK_INTERRUPT_EXCEPTION);
      }
      ImmutableMap<Integer, ErrorOr<CarPropertyValueCompat<T>>> areaIdToSetCarPropertyValue;
      synchronized (lock) {
        areaIdToSetCarPropertyValue = areaIdToSetCarPropertyValueBuilder.buildOrThrow();
      }
      if (areaIdToSetCarPropertyValue.size()
          != (areaIds.size() - areaIdsThatSetThrewAnException.size())) {
        Log.e(
            TAG,
            "SetConfirmationCallback - # of set values does not match number of area Ids "
                + areaIdToSetCarPropertyValue
                + " for propertyId: "
                + VehiclePropertyIds.toString(propertyId)
                + " areaIds: "
                + areaIds
                + " areaIdsThatSetThrewAnException: "
                + areaIdsThatSetThrewAnException
                + " expectedSetValue: "
                + expectedSetValue);
        return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
      }
      return ErrorOr.createValue(areaIdToSetCarPropertyValue);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void onChangeEvent(CarPropertyValue carPropertyValue) {
      synchronized (lock) {
        if (receivedEventsForAllAreaIds()) {
          Log.w(
              TAG,
              "SetConfirmationCallback - Dropping onChangeEvent. Received an onChangeEvent call"
                  + " after we received events already for all areaIds: "
                  + areaIds
                  + " - "
                  + carPropertyValue);
          return;
        }
        if (carPropertyValue.getPropertyId() != propertyId) {
          Log.w(
              TAG,
              "SetConfirmationCallback - Dropping onChangeEvent. Property ID does not match"
                  + " expected property ID: "
                  + VehiclePropertyIds.toString(propertyId)
                  + " - "
                  + carPropertyValue);
          return;
        }
        if (!areaIds.contains(carPropertyValue.getAreaId())) {
          Log.w(
              TAG,
              "SetConfirmationCallback - Dropping onChangeEvent. Area ID does not match expected"
                  + " area IDs: "
                  + areaIds
                  + " - "
                  + carPropertyValue);
          return;
        }
        if (areaIdsThatReceivedAnEvent.contains(carPropertyValue.getAreaId())) {
          Log.w(
              TAG,
              "SetConfirmationCallback - Dropping onChangeEvent. Area ID already received an event"
                  + " - "
                  + carPropertyValue);
          return;
        }
        if (carPropertyValue.getTimestamp() <= creationTimeNanos) {
          Log.w(
              TAG,
              "SetConfirmationCallback - Dropping onChangeEvent. Timestamp is older than this"
                  + " callback's creation - "
                  + creationTimeNanos
                  + " - "
                  + carPropertyValue);
          return;
        }
        if (carPropertyValue.getTimestamp() >= SystemClock.elapsedRealtimeNanos()) {
          Log.w(
              TAG,
              "SetConfirmationCallback - Dropping onChangeEvent. Timestamp is newer than the"
                  + " current time - "
                  + carPropertyValue);
          return;
        }
        if (carPropertyValue.getStatus() == CarPropertyValue.STATUS_AVAILABLE
            && !valueEquals(expectedSetValue, (T) carPropertyValue.getValue())) {
          Log.w(
              TAG,
              "SetConfirmationCallback - Dropping onChangeEvent. Status is available, but value"
                  + " does not match expected value: "
                  + expectedSetValue
                  + " - "
                  + carPropertyValue);
          return;
        }

        if (carPropertyValue.getStatus() == CarPropertyValue.STATUS_AVAILABLE) {
          areaIdToSetCarPropertyValueBuilder.put(
              carPropertyValue.getAreaId(),
              ErrorOr.createValue(
                  CarPropertyValueCompat.create((CarPropertyValue<T>) carPropertyValue)));
        } else if (VALID_CAR_PROPERTY_VALUE_STATUSES.contains(carPropertyValue.getStatus())) {
          Log.e(TAG, "SetConfirmationCallback - Status is not available: - " + carPropertyValue);
          areaIdToSetCarPropertyValueBuilder.put(
              carPropertyValue.getAreaId(),
              ErrorOr.createError(
                  carPropertyValue.getStatus() == CarPropertyValue.STATUS_UNAVAILABLE
                      ? ErrorCode.ERROR_CODE_PROPERTY_NOT_AVAILABLE
                      : ErrorCode.ERROR_CODE_PLATFORM_INTERNAL_ERROR));
        } else {
          Log.e(
              TAG,
              "SetConfirmationCallback - Status is not a valid status: "
                  + VALID_CAR_PROPERTY_VALUE_STATUSES
                  + " - "
                  + carPropertyValue);
          areaIdToSetCarPropertyValueBuilder.put(
              carPropertyValue.getAreaId(),
              ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL));
        }
        areaIdsThatReceivedAnEvent.add(carPropertyValue.getAreaId());
        countDownLatch.countDown();
      }
    }

    @Override
    public void onErrorEvent(int propertyId, int areaId) {
      onErrorEvent(propertyId, areaId, CarPropertyManager.CAR_SET_PROPERTY_ERROR_CODE_UNKNOWN);
    }

    @Override
    public void onErrorEvent(int propertyId, int areaId, int errorCode) {
      synchronized (lock) {
        if (receivedEventsForAllAreaIds()) {
          Log.w(
              TAG,
              "SetConfirmationCallback - Dropping onErrorEvent. Received an onErrorEvent call after"
                  + " the we already received events for all areaIds: "
                  + areaIds
                  + " - propertyId: "
                  + VehiclePropertyIds.toString(propertyId)
                  + " areaId: "
                  + areaId
                  + " errorCode: "
                  + errorCode);
          return;
        }
        if (propertyId != this.propertyId) {
          Log.w(
              TAG,
              "SetConfirmationCallback - Dropping onErrorEvent. Property ID does not match expected"
                  + " property ID: "
                  + VehiclePropertyIds.toString(this.propertyId)
                  + " - propertyId: "
                  + VehiclePropertyIds.toString(propertyId)
                  + " areaId: "
                  + areaId
                  + " errorCode: "
                  + errorCode);
          return;
        }
        if (!areaIds.contains(areaId)) {
          Log.w(
              TAG,
              "SetConfirmationCallback - Dropping onErrorEvent. Area ID does not match expected"
                  + " area IDs: "
                  + areaIds
                  + " - propertyId: "
                  + VehiclePropertyIds.toString(propertyId)
                  + " areaId: "
                  + areaId
                  + " errorCode: "
                  + errorCode);
          return;
        }
        if (areaIdsThatReceivedAnEvent.contains(areaId)) {
          Log.w(
              TAG,
              "SetConfirmationCallback - Dropping onErrorEvent. Area ID already received an event"
                  + " - propertyId: "
                  + VehiclePropertyIds.toString(propertyId)
                  + " areaId: "
                  + areaId
                  + " errorCode: "
                  + errorCode);
          return;
        }
        if (!SET_PROPERTY_ERROR_CODE_TO_ERROR_OR_ERROR_CODE.containsKey(errorCode)) {
          Log.e(
              TAG,
              "SetConfirmationCallback - Dropping onErrorEvent. error code is not a valid error"
                  + " code: "
                  + SET_PROPERTY_ERROR_CODE_TO_ERROR_OR_ERROR_CODE.keySet()
                  + " - propertyId: "
                  + VehiclePropertyIds.toString(propertyId)
                  + " areaId: "
                  + areaId
                  + " errorCode: "
                  + errorCode);
          areaIdToSetCarPropertyValueBuilder.put(
              areaId, ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL));
        } else {
          areaIdToSetCarPropertyValueBuilder.put(
              areaId,
              ErrorOr.createError(SET_PROPERTY_ERROR_CODE_TO_ERROR_OR_ERROR_CODE.get(errorCode)));
        }
        areaIdsThatReceivedAnEvent.add(areaId);
        countDownLatch.countDown();
      }
    }

    @GuardedBy("lock")
    private boolean receivedEventsForAllAreaIds() {
      return areaIdsThatReceivedAnEvent.size() == areaIds.size();
    }

    private void finishAnyUnfinishedAreaIdWithErrorCode(ErrorCode errorCode) {
      synchronized (lock) {
        for (Integer areaId : areaIds) {
          if (areaIdsThatReceivedAnEvent.contains(areaId)) {
            continue;
          }
          areaIdsThatReceivedAnEvent.add(areaId);
          areaIdToSetCarPropertyValueBuilder.put(areaId, ErrorOr.createError(errorCode));
        }
      }
    }
  }

  @VisibleForTesting
  protected void setAfterSetPropertyFunc(Callable<Void> afterSetPropertyFunc) {
    this.afterSetPropertyFunc = afterSetPropertyFunc;
  }

  private static <V> boolean valueEquals(V v1, V v2) {
    return (v1 instanceof Float && floatEquals((Float) v1, (Float) v2))
        || (v1 instanceof Object[] && Arrays.equals((Object[]) v1, (Object[]) v2))
        || v1.equals(v2);
  }

  private static boolean floatEquals(float f1, float f2) {
    return Math.abs(f1 - f2) < FLOAT_INEQUALITY_THRESHOLD;
  }
}

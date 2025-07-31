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

package com.google.android.libraries.automotive.val.api;

import static com.google.common.util.concurrent.Futures.immediateFuture;

import android.car.VehiclePropertyIds;
import android.util.Log;
import com.google.android.libraries.automotive.val.actions.Action;
import com.google.android.libraries.automotive.val.actions.GetAction;
import com.google.android.libraries.automotive.val.actions.OptionalActionParameters;
import com.google.android.libraries.automotive.val.actions.SetAction;
import com.google.android.libraries.automotive.val.api.ErrorOr.ErrorCode;
import com.google.android.libraries.automotive.val.compat.CarPropertyManagerCompat;
import com.google.android.libraries.automotive.val.utils.PermissionUtility;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * The class that contains the steering wheel related actions for the Vehicle Action Library (VAL).
 */
public class SteeringWheelActions extends ApiActionCategory {
  private static final String TAG = SteeringWheelActions.class.getSimpleName();

  // Steering Wheel Actions
  public static final String STEERING_WHEEL_ACTION_GET_HEATING_LEVEL =
      "STEERING_WHEEL_ACTION_GET_HEATING_LEVEL";
  public static final String STEERING_WHEEL_ACTION_SET_HEATING_LEVEL =
      "STEERING_WHEEL_ACTION_SET_HEATING_LEVEL";
  public static final String STEERING_WHEEL_ACTION_GET_COOLING_LEVEL =
      "STEERING_WHEEL_ACTION_GET_COOLING_LEVEL";
  public static final String STEERING_WHEEL_ACTION_SET_COOLING_LEVEL =
      "STEERING_WHEEL_ACTION_SET_COOLING_LEVEL";
  private static final ImmutableSet<String> VALID_STEERING_WHEEL_ACTIONS =
      ImmutableSet.of(
          STEERING_WHEEL_ACTION_GET_HEATING_LEVEL,
          STEERING_WHEEL_ACTION_SET_HEATING_LEVEL,
          STEERING_WHEEL_ACTION_GET_COOLING_LEVEL,
          STEERING_WHEEL_ACTION_SET_COOLING_LEVEL);

  // This is a copy of {@code Car#PERMISSION_CONTROL_CAR_CLIMATE} because it is a system API.
  private static final String PERMISSION_CONTROL_CAR_CLIMATE =
      "android.car.permission.CONTROL_CAR_CLIMATE";

  SteeringWheelActions(
      ListeningExecutorService listeningExecutorService,
      CarPropertyManagerCompat carPropertyManagerCompat,
      PermissionUtility permissionUtility) {
    this(
        listeningExecutorService,
        ImmutableMap.<String, Action<?>>builder()
            .put(
                STEERING_WHEEL_ACTION_GET_HEATING_LEVEL,
                new GetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    STEERING_WHEEL_ACTION_GET_HEATING_LEVEL,
                    VehiclePropertyIds.HVAC_STEERING_WHEEL_HEAT,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    GLOBAL_ELEMENT_TO_AREA,
                    OptionalActionParameters.<Integer>builder()
                        .setIsMinMaxProperty()
                        .setRequiredSupportedValues(0)
                        .setCustomValueRangeGenerator(
                            new ZeroOrGreaterThanCustomValueRangeGenerator())
                        .build()))
            .put(
                STEERING_WHEEL_ACTION_SET_HEATING_LEVEL,
                new SetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    STEERING_WHEEL_ACTION_SET_HEATING_LEVEL,
                    VehiclePropertyIds.HVAC_STEERING_WHEEL_HEAT,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    GLOBAL_ELEMENT_TO_AREA,
                    OptionalActionParameters.<Integer>builder()
                        .setIsMinMaxProperty()
                        .setRequiredSupportedValues(0)
                        .setCustomValueRangeGenerator(
                            new ZeroOrGreaterThanCustomValueRangeGenerator())
                        .build()))
            .put(
                STEERING_WHEEL_ACTION_GET_COOLING_LEVEL,
                new GetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    STEERING_WHEEL_ACTION_GET_COOLING_LEVEL,
                    VehiclePropertyIds.HVAC_STEERING_WHEEL_HEAT,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    GLOBAL_ELEMENT_TO_AREA,
                    OptionalActionParameters.<Integer>builder()
                        .setIsMinMaxProperty()
                        .setRequiredSupportedValues(0)
                        .setCustomValueRangeGenerator(new ZeroOrLessThanCustomValueRangeGenerator())
                        .build()))
            .put(
                STEERING_WHEEL_ACTION_SET_COOLING_LEVEL,
                new SetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    STEERING_WHEEL_ACTION_SET_COOLING_LEVEL,
                    VehiclePropertyIds.HVAC_STEERING_WHEEL_HEAT,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    GLOBAL_ELEMENT_TO_AREA,
                    OptionalActionParameters.<Integer>builder()
                        .setIsMinMaxProperty()
                        .setRequiredSupportedValues(0)
                        .setCustomValueRangeGenerator(new ZeroOrLessThanCustomValueRangeGenerator())
                        .build()))
            .buildOrThrow());
  }

  @VisibleForTesting
  protected SteeringWheelActions(
      ListeningExecutorService listeningExecutorService,
      ImmutableMap<String, Action<?>> actionMap) {
    super(listeningExecutorService, actionMap);
    Preconditions.checkArgument(actionMap.keySet().equals(VALID_STEERING_WHEEL_ACTIONS));
  }

  /** Async function that gets the steering wheel heating level. */
  public ListenableFuture<ErrorOr<GlobalGetResult<Integer>>> getHeatingLevel() {
    return executeGlobalGetAction(
        STEERING_WHEEL_ACTION_GET_HEATING_LEVEL, ApiActionCategory::zeroOrGreaterTransform);
  }

  /** Async function that sets the steering wheel heating level. */
  public ListenableFuture<ErrorOr<GlobalSetResult>> setHeatingLevel(
      @ParameterName("level") int level) {
    if (level < 0) {
      Log.e(TAG, "setHeatingLevel() - level is negative");
      return immediateFuture(ErrorOr.createError(ErrorCode.ERROR_CODE_INVALID_API_ARGUMENT));
    }
    return executeGlobalSetAction(STEERING_WHEEL_ACTION_SET_HEATING_LEVEL, level);
  }

  /** Async function that gets the steering wheel cooling level. */
  public ListenableFuture<ErrorOr<GlobalGetResult<Integer>>> getCoolingLevel() {
    return executeGlobalGetAction(
        STEERING_WHEEL_ACTION_GET_COOLING_LEVEL, ApiActionCategory::zeroOrLessTransform);
  }

  /** Async function that sets the steering wheel cooling level. */
  public ListenableFuture<ErrorOr<GlobalSetResult>> setCoolingLevel(
      @ParameterName("level") int level) {
    if (level < 0) {
      Log.e(TAG, "setCoolingLevel() - level is negative");
      return immediateFuture(ErrorOr.createError(ErrorCode.ERROR_CODE_INVALID_API_ARGUMENT));
    }
    return executeGlobalSetAction(STEERING_WHEEL_ACTION_SET_COOLING_LEVEL, switchSign(level));
  }
}

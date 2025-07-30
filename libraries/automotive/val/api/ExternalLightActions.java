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

import android.car.VehiclePropertyIds;
import com.google.android.libraries.automotive.val.actions.Action;
import com.google.android.libraries.automotive.val.actions.GetAction;
import com.google.android.libraries.automotive.val.actions.OptionalActionParameters;
import com.google.android.libraries.automotive.val.actions.OptionalActionParameters.CustomValueRangeGenerator;
import com.google.android.libraries.automotive.val.actions.SetAction;
import com.google.android.libraries.automotive.val.compat.CarPropertyManagerCompat;
import com.google.android.libraries.automotive.val.utils.PermissionUtility;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * The class that contains the steering wheel related actions for the Vehicle Action Library (VAL).
 */
public class ExternalLightActions extends ApiActionCategory {
  private static final String TAG = ExternalLightActions.class.getSimpleName();

  // External Light Actions
  public static final String EXTERNAL_LIGHT_ACTION_ENABLE_HEADLIGHTS =
      "EXTERNAL_LIGHT_ACTION_ENABLE_HEADLIGHTS";
  public static final String EXTERNAL_LIGHT_ACTION_DISABLE_HEADLIGHTS =
      "EXTERNAL_LIGHT_ACTION_DISABLE_HEADLIGHTS";
  public static final String EXTERNAL_LIGHT_ACTION_ARE_HEADLIGHTS_ENABLED =
      "EXTERNAL_LIGHT_ACTION_ARE_HEADLIGHTS_ENABLED";
  private static final ImmutableSet<String> VALID_EXTERNAL_LIGHT_ACTIONS =
      ImmutableSet.of(
          EXTERNAL_LIGHT_ACTION_ENABLE_HEADLIGHTS,
          EXTERNAL_LIGHT_ACTION_DISABLE_HEADLIGHTS,
          EXTERNAL_LIGHT_ACTION_ARE_HEADLIGHTS_ENABLED);

  // This is a copy of {@link Car#PERMISSION_CAR_EXTERIOR_LIGHTS} because it is a system API.
  private static final String PERMISSION_CAR_EXTERIOR_LIGHTS =
      "android.car.permission.CAR_EXTERIOR_LIGHTS";
  // This is a copy of {@link Car#PERMISSION_CONTROL_CAR_EXTERIOR_LIGHTS} because it is a system
  // API.
  private static final String PERMISSION_CONTROL_CAR_EXTERIOR_LIGHTS =
      "android.car.permission.CONTROL_CAR_EXTERIOR_LIGHTS";

  ExternalLightActions(
      ListeningExecutorService listeningExecutorService,
      CarPropertyManagerCompat carPropertyManagerCompat,
      PermissionUtility permissionUtility) {
    this(
        listeningExecutorService,
        ImmutableMap.<String, Action<?>>builder()
            .put(
                EXTERNAL_LIGHT_ACTION_ARE_HEADLIGHTS_ENABLED,
                new GetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    EXTERNAL_LIGHT_ACTION_ARE_HEADLIGHTS_ENABLED,
                    VehiclePropertyIds.HEADLIGHTS_STATE,
                    Integer.class,
                    PERMISSION_CAR_EXTERIOR_LIGHTS,
                    GLOBAL_ELEMENT_TO_AREA))
            .put(
                EXTERNAL_LIGHT_ACTION_ENABLE_HEADLIGHTS,
                new SetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    EXTERNAL_LIGHT_ACTION_ENABLE_HEADLIGHTS,
                    VehiclePropertyIds.HEADLIGHTS_SWITCH,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_EXTERIOR_LIGHTS,
                    GLOBAL_ELEMENT_TO_AREA,
                    OptionalActionParameters.<Integer>builder()
                        .setCustomValueRangeGenerator(
                            new CustomValueRangeGenerator<Integer>() {
                              @Override
                              public ErrorOr<ValueRange<Integer>> getValueRange(
                                  CarPropertyManagerCompat carPropertyManagerCompat, int area) {
                                return ErrorOr.createValue(
                                    ValueRange.<Integer>create(
                                        Integer.class,
                                        ImmutableList.of(/*VehicleLightSwitch.STATE_ON=*/ 1)));
                              }
                            })
                        .build()))
            .put(
                EXTERNAL_LIGHT_ACTION_DISABLE_HEADLIGHTS,
                new SetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    EXTERNAL_LIGHT_ACTION_DISABLE_HEADLIGHTS,
                    VehiclePropertyIds.HEADLIGHTS_SWITCH,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_EXTERIOR_LIGHTS,
                    GLOBAL_ELEMENT_TO_AREA,
                    OptionalActionParameters.<Integer>builder()
                        .setCustomValueRangeGenerator(
                            new CustomValueRangeGenerator<Integer>() {
                              @Override
                              public ErrorOr<ValueRange<Integer>> getValueRange(
                                  CarPropertyManagerCompat carPropertyManagerCompat, int area) {
                                return ErrorOr.createValue(
                                    ValueRange.<Integer>create(
                                        Integer.class,
                                        ImmutableList.of(/*VehicleLightSwitch.STATE_OFF=*/ 0)));
                              }
                            })
                        .build()))
            .buildOrThrow());
  }

  @VisibleForTesting
  protected ExternalLightActions(
      ListeningExecutorService listeningExecutorService,
      ImmutableMap<String, Action<?>> actionMap) {
    super(listeningExecutorService, actionMap);
    Preconditions.checkArgument(actionMap.keySet().equals(VALID_EXTERNAL_LIGHT_ACTIONS));
  }

  /** Async function that checks if the headlights are on. */
  public ListenableFuture<ErrorOr<GlobalGetResult<Boolean>>> areHeadlightsEnabled() {
    return executeGlobalGetAction(
        EXTERNAL_LIGHT_ACTION_ARE_HEADLIGHTS_ENABLED,
        ExternalLightActions::areHeadlightsEnabledTransform);
  }

  /** Async function that turns on the headlights. */
  public ListenableFuture<ErrorOr<GlobalSetResult>> enableHeadlights() {
    return executeGlobalSetAction(
        EXTERNAL_LIGHT_ACTION_ENABLE_HEADLIGHTS, /*VehicleLightSwitch.STATE_ON=*/ 1);
  }

  /** Async function that turns off the headlights. */
  public ListenableFuture<ErrorOr<GlobalSetResult>> disableHeadlights() {
    return executeGlobalSetAction(
        EXTERNAL_LIGHT_ACTION_DISABLE_HEADLIGHTS, /*VehicleLightSwitch.STATE_OFF=*/ 0);
  }

  private static boolean areHeadlightsEnabledTransform(String element, int value) {
    return value == /*VehicleLightState.STATE_ON=*/ 1;
  }
}

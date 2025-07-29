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
import static java.util.Map.entry;

import android.car.VehicleAreaSeat;
import android.car.VehiclePropertyIds;
import android.util.Log;
import com.google.android.libraries.automotive.val.actions.Action;
import com.google.android.libraries.automotive.val.actions.ActionCategory.ZeroOrGreaterThanCustomValueRangeGenerator;
import com.google.android.libraries.automotive.val.actions.ActionCategory.ZeroOrLessThanCustomValueRangeGenerator;
import com.google.android.libraries.automotive.val.actions.GetAction;
import com.google.android.libraries.automotive.val.actions.HvacPowerDependentGetAction;
import com.google.android.libraries.automotive.val.actions.HvacPowerDependentOffsetAction;
import com.google.android.libraries.automotive.val.actions.HvacPowerDependentSetAction;
import com.google.android.libraries.automotive.val.actions.HvacTargetTemperatureGetAction;
import com.google.android.libraries.automotive.val.actions.HvacTargetTemperatureOffsetAction;
import com.google.android.libraries.automotive.val.actions.HvacTargetTemperatureSetAction;
import com.google.android.libraries.automotive.val.actions.OptionalActionParameters;
import com.google.android.libraries.automotive.val.actions.OptionalActionParameters.CustomValueRangeGenerator;
import com.google.android.libraries.automotive.val.actions.SetAction;
import com.google.android.libraries.automotive.val.api.ErrorOr.ErrorCode;
import com.google.android.libraries.automotive.val.api.Temperature.TemperatureUnit;
import com.google.android.libraries.automotive.val.compat.CarPropertyManagerCompat;
import com.google.android.libraries.automotive.val.compat.CarPropertyValueCompat;
import com.google.android.libraries.automotive.val.utils.PermissionUtility;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

/** The class that contains the seat related actions for the Vehicle Action Library (VAL). */
public class SeatActions extends ApiActionCategory {
  private static final String TAG = SeatActions.class.getSimpleName();

  // Seat Elements
  public static final String SEAT_ROW_1_LEFT = "SEAT_ROW_1_LEFT";
  public static final String SEAT_ROW_1_CENTER = "SEAT_ROW_1_CENTER";
  public static final String SEAT_ROW_1_RIGHT = "SEAT_ROW_1_RIGHT";
  public static final String SEAT_ROW_2_LEFT = "SEAT_ROW_2_LEFT";
  public static final String SEAT_ROW_2_CENTER = "SEAT_ROW_2_CENTER";
  public static final String SEAT_ROW_2_RIGHT = "SEAT_ROW_2_RIGHT";
  public static final String SEAT_ROW_3_LEFT = "SEAT_ROW_3_LEFT";
  public static final String SEAT_ROW_3_CENTER = "SEAT_ROW_3_CENTER";
  public static final String SEAT_ROW_3_RIGHT = "SEAT_ROW_3_RIGHT";

  public static final String SEAT_ACTION_PREFIX = "SEAT_ACTION_";

  // Seat Actions
  public static final String SEAT_ACTION_IS_HVAC_POWER_ENABLED =
      "SEAT_ACTION_IS_HVAC_POWER_ENABLED";
  public static final String SEAT_ACTION_ENABLE_HVAC_POWER = "SEAT_ACTION_ENABLE_HVAC_POWER";
  public static final String SEAT_ACTION_DISABLE_HVAC_POWER = "SEAT_ACTION_DISABLE_HVAC_POWER";
  public static final String SEAT_ACTION_IS_AC_ENABLED = "SEAT_ACTION_IS_AC_ENABLED";
  public static final String SEAT_ACTION_ENABLE_AC = "SEAT_ACTION_ENABLE_AC";
  public static final String SEAT_ACTION_DISABLE_AC = "SEAT_ACTION_DISABLE_AC";
  public static final String SEAT_ACTION_IS_HVAC_RECIRCULATION_ENABLED =
      "SEAT_ACTION_IS_HVAC_RECIRCULATION_ENABLED";
  public static final String SEAT_ACTION_ENABLE_HVAC_RECIRCULATION =
      "SEAT_ACTION_ENABLE_HVAC_RECIRCULATION";
  public static final String SEAT_ACTION_DISABLE_HVAC_RECIRCULATION =
      "SEAT_ACTION_DISABLE_HVAC_RECIRCULATION";
  public static final String SEAT_ACTION_IS_HVAC_AUTO_MODE_ENABLED =
      "SEAT_ACTION_IS_HVAC_AUTO_MODE_ENABLED";
  public static final String SEAT_ACTION_ENABLE_HVAC_AUTO_MODE =
      "SEAT_ACTION_ENABLE_HVAC_AUTO_MODE";
  public static final String SEAT_ACTION_DISABLE_HVAC_AUTO_MODE =
      "SEAT_ACTION_DISABLE_HVAC_AUTO_MODE";
  public static final String SEAT_ACTION_GET_HVAC_FAN_SPEED = "SEAT_ACTION_GET_HVAC_FAN_SPEED";
  public static final String SEAT_ACTION_SET_HVAC_FAN_SPEED = "SEAT_ACTION_SET_HVAC_FAN_SPEED";
  public static final String SEAT_ACTION_INCREMENT_HVAC_FAN_SPEED =
      "SEAT_ACTION_INCREMENT_HVAC_FAN_SPEED";
  public static final String SEAT_ACTION_DECREMENT_HVAC_FAN_SPEED =
      "SEAT_ACTION_DECREMENT_HVAC_FAN_SPEED";
  public static final String SEAT_ACTION_GET_HVAC_FAN_DIRECTION =
      "SEAT_ACTION_GET_HVAC_FAN_DIRECTION";
  public static final String SEAT_ACTION_SET_HVAC_FAN_DIRECTION =
      "SEAT_ACTION_SET_HVAC_FAN_DIRECTION";
  public static final String SEAT_ACTION_GET_SEAT_HEATING_LEVEL =
      "SEAT_ACTION_GET_SEAT_HEATING_LEVEL";
  public static final String SEAT_ACTION_SET_SEAT_HEATING_LEVEL =
      "SEAT_ACTION_SET_SEAT_HEATING_LEVEL";
  public static final String SEAT_ACTION_GET_SEAT_COOLING_LEVEL =
      "SEAT_ACTION_GET_SEAT_COOLING_LEVEL";
  public static final String SEAT_ACTION_SET_SEAT_COOLING_LEVEL =
      "SEAT_ACTION_SET_SEAT_COOLING_LEVEL";
  public static final String SEAT_ACTION_GET_HVAC_TARGET_TEMPERATURE =
      "SEAT_ACTION_GET_HVAC_TARGET_TEMPERATURE";
  public static final String SEAT_ACTION_SET_HVAC_TARGET_TEMPERATURE =
      "SEAT_ACTION_SET_HVAC_TARGET_TEMPERATURE";
  public static final String SEAT_ACTION_INCREMENT_HVAC_TARGET_TEMPERATURE =
      "SEAT_ACTION_INCREMENT_HVAC_TARGET_TEMPERATURE";
  public static final String SEAT_ACTION_DECREMENT_HVAC_TARGET_TEMPERATURE =
      "SEAT_ACTION_DECREMENT_HVAC_TARGET_TEMPERATURE";
  public static final String SEAT_ACTION_IS_MAX_AC_ENABLED = "SEAT_ACTION_IS_MAX_AC_ENABLED";
  public static final String SEAT_ACTION_ENABLE_MAX_AC = "SEAT_ACTION_ENABLE_MAX_AC";
  public static final String SEAT_ACTION_DISABLE_MAX_AC = "SEAT_ACTION_DISABLE_MAX_AC";
  public static final String SEAT_ACTION_IS_MAX_HEAT_ENABLED = "SEAT_ACTION_IS_MAX_HEAT_ENABLED";
  public static final String SEAT_ACTION_ENABLE_MAX_HEAT = "SEAT_ACTION_ENABLE_MAX_HEAT";
  public static final String SEAT_ACTION_DISABLE_MAX_HEAT = "SEAT_ACTION_DISABLE_MAX_HEAT";
  public static final String SEAT_ACTION_IS_HVAC_AUTO_RECIRCULATION_ENABLED =
      "SEAT_ACTION_IS_HVAC_AUTO_RECIRCULATION_ENABLED";
  public static final String SEAT_ACTION_ENABLE_HVAC_AUTO_RECIRCULATION =
      "SEAT_ACTION_ENABLE_HVAC_AUTO_RECIRCULATION";
  public static final String SEAT_ACTION_DISABLE_HVAC_AUTO_RECIRCULATION =
      "SEAT_ACTION_DISABLE_HVAC_AUTO_RECIRCULATION";
  private static final ImmutableSet<String> VALID_SEAT_ACTIONS =
      ImmutableSet.of(
          SEAT_ACTION_IS_HVAC_POWER_ENABLED,
          SEAT_ACTION_ENABLE_HVAC_POWER,
          SEAT_ACTION_DISABLE_HVAC_POWER,
          SEAT_ACTION_IS_AC_ENABLED,
          SEAT_ACTION_ENABLE_AC,
          SEAT_ACTION_DISABLE_AC,
          SEAT_ACTION_IS_HVAC_RECIRCULATION_ENABLED,
          SEAT_ACTION_ENABLE_HVAC_RECIRCULATION,
          SEAT_ACTION_DISABLE_HVAC_RECIRCULATION,
          SEAT_ACTION_IS_HVAC_AUTO_MODE_ENABLED,
          SEAT_ACTION_ENABLE_HVAC_AUTO_MODE,
          SEAT_ACTION_DISABLE_HVAC_AUTO_MODE,
          SEAT_ACTION_GET_HVAC_FAN_SPEED,
          SEAT_ACTION_SET_HVAC_FAN_SPEED,
          SEAT_ACTION_INCREMENT_HVAC_FAN_SPEED,
          SEAT_ACTION_DECREMENT_HVAC_FAN_SPEED,
          SEAT_ACTION_GET_HVAC_FAN_DIRECTION,
          SEAT_ACTION_SET_HVAC_FAN_DIRECTION,
          SEAT_ACTION_GET_SEAT_HEATING_LEVEL,
          SEAT_ACTION_SET_SEAT_HEATING_LEVEL,
          SEAT_ACTION_GET_SEAT_COOLING_LEVEL,
          SEAT_ACTION_SET_SEAT_COOLING_LEVEL,
          SEAT_ACTION_GET_HVAC_TARGET_TEMPERATURE,
          SEAT_ACTION_SET_HVAC_TARGET_TEMPERATURE,
          SEAT_ACTION_INCREMENT_HVAC_TARGET_TEMPERATURE,
          SEAT_ACTION_DECREMENT_HVAC_TARGET_TEMPERATURE,
          SEAT_ACTION_IS_MAX_AC_ENABLED,
          SEAT_ACTION_ENABLE_MAX_AC,
          SEAT_ACTION_DISABLE_MAX_AC,
          SEAT_ACTION_IS_MAX_HEAT_ENABLED,
          SEAT_ACTION_ENABLE_MAX_HEAT,
          SEAT_ACTION_DISABLE_MAX_HEAT,
          SEAT_ACTION_IS_HVAC_AUTO_RECIRCULATION_ENABLED,
          SEAT_ACTION_ENABLE_HVAC_AUTO_RECIRCULATION,
          SEAT_ACTION_DISABLE_HVAC_AUTO_RECIRCULATION);

  private static final ImmutableBiMap<String, Integer> SEAT_TO_AREA =
      ImmutableBiMap.of(
          SEAT_ROW_1_LEFT,
          VehicleAreaSeat.SEAT_ROW_1_LEFT,
          SEAT_ROW_1_CENTER,
          VehicleAreaSeat.SEAT_ROW_1_CENTER,
          SEAT_ROW_1_RIGHT,
          VehicleAreaSeat.SEAT_ROW_1_RIGHT,
          SEAT_ROW_2_LEFT,
          VehicleAreaSeat.SEAT_ROW_2_LEFT,
          SEAT_ROW_2_CENTER,
          VehicleAreaSeat.SEAT_ROW_2_CENTER,
          SEAT_ROW_2_RIGHT,
          VehicleAreaSeat.SEAT_ROW_2_RIGHT,
          SEAT_ROW_3_LEFT,
          VehicleAreaSeat.SEAT_ROW_3_LEFT,
          SEAT_ROW_3_CENTER,
          VehicleAreaSeat.SEAT_ROW_3_CENTER,
          SEAT_ROW_3_RIGHT,
          VehicleAreaSeat.SEAT_ROW_3_RIGHT);
  // This is a copy of {@link Car#PERMISSION_CONTROL_CAR_CLIMATE} because it is a system API.
  private static final String PERMISSION_CONTROL_CAR_CLIMATE =
      "android.car.permission.CONTROL_CAR_CLIMATE";

  // The values in this map are a copy of {@code CarHvacFanDirection} because it is a system API.
  private static final ImmutableBiMap<HvacFanDirection, Integer>
      HVAC_FAN_DIRECTION_ENUM_TO_INTEGER_MAP =
          ImmutableBiMap.ofEntries(
              entry(HvacFanDirection.DIRECTION_UNKNOWN, 0x0),
              entry(HvacFanDirection.DIRECTION_FACE, 0x1),
              entry(HvacFanDirection.DIRECTION_FLOOR, 0x2),
              entry(HvacFanDirection.DIRECTION_DEFROST, 0x4),
              entry(HvacFanDirection.DIRECTION_FACE_AND_FLOOR, 0x3),
              entry(HvacFanDirection.DIRECTION_FACE_AND_DEFROST, 0x5),
              entry(HvacFanDirection.DIRECTION_DEFROST_AND_FLOOR, 0x6),
              entry(HvacFanDirection.DIRECTION_FACE_AND_DEFROST_AND_FLOOR, 0x7));

  protected SeatActions(
      ListeningExecutorService listeningExecutorService,
      CarPropertyManagerCompat carPropertyManagerCompat,
      PermissionUtility permissionUtility) {
    this(
        listeningExecutorService,
        ImmutableMap.<String, Action<?>>builder()
            .put(
                SEAT_ACTION_IS_HVAC_POWER_ENABLED,
                new GetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_IS_HVAC_POWER_ENABLED,
                    VehiclePropertyIds.HVAC_POWER_ON,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA))
            .put(
                SEAT_ACTION_ENABLE_HVAC_POWER,
                new SetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_ENABLE_HVAC_POWER,
                    VehiclePropertyIds.HVAC_POWER_ON,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA))
            .put(
                SEAT_ACTION_DISABLE_HVAC_POWER,
                new SetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_DISABLE_HVAC_POWER,
                    VehiclePropertyIds.HVAC_POWER_ON,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA))
            .put(
                SEAT_ACTION_IS_AC_ENABLED,
                new HvacPowerDependentGetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_IS_AC_ENABLED,
                    VehiclePropertyIds.HVAC_AC_ON,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA))
            .put(
                SEAT_ACTION_ENABLE_AC,
                new HvacPowerDependentSetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_ENABLE_AC,
                    VehiclePropertyIds.HVAC_AC_ON,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA,
                    OptionalActionParameters.<Boolean>builder()
                        .setEnableHvacPowerIfDependent()
                        .build()))
            .put(
                SEAT_ACTION_DISABLE_AC,
                new HvacPowerDependentSetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_DISABLE_AC,
                    VehiclePropertyIds.HVAC_AC_ON,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA))
            .put(
                SEAT_ACTION_IS_HVAC_RECIRCULATION_ENABLED,
                new HvacPowerDependentGetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_IS_HVAC_RECIRCULATION_ENABLED,
                    VehiclePropertyIds.HVAC_RECIRC_ON,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA))
            .put(
                SEAT_ACTION_ENABLE_HVAC_RECIRCULATION,
                new HvacPowerDependentSetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_ENABLE_HVAC_RECIRCULATION,
                    VehiclePropertyIds.HVAC_RECIRC_ON,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA,
                    OptionalActionParameters.<Boolean>builder()
                        .setEnableHvacPowerIfDependent()
                        .build()))
            .put(
                SEAT_ACTION_DISABLE_HVAC_RECIRCULATION,
                new HvacPowerDependentSetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_DISABLE_HVAC_RECIRCULATION,
                    VehiclePropertyIds.HVAC_RECIRC_ON,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA))
            .put(
                SEAT_ACTION_IS_HVAC_AUTO_MODE_ENABLED,
                new HvacPowerDependentGetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_IS_HVAC_AUTO_MODE_ENABLED,
                    VehiclePropertyIds.HVAC_AUTO_ON,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA))
            .put(
                SEAT_ACTION_ENABLE_HVAC_AUTO_MODE,
                new HvacPowerDependentSetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_ENABLE_HVAC_AUTO_MODE,
                    VehiclePropertyIds.HVAC_AUTO_ON,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA,
                    OptionalActionParameters.<Boolean>builder()
                        .setEnableHvacPowerIfDependent()
                        .build()))
            .put(
                SEAT_ACTION_DISABLE_HVAC_AUTO_MODE,
                new HvacPowerDependentSetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_DISABLE_HVAC_AUTO_MODE,
                    VehiclePropertyIds.HVAC_AUTO_ON,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA))
            .put(
                SEAT_ACTION_GET_HVAC_FAN_SPEED,
                new HvacPowerDependentGetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_GET_HVAC_FAN_SPEED,
                    VehiclePropertyIds.HVAC_FAN_SPEED,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA,
                    OptionalActionParameters.<Integer>builder().setIsMinMaxProperty().build()))
            .put(
                SEAT_ACTION_SET_HVAC_FAN_SPEED,
                new HvacPowerDependentSetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_SET_HVAC_FAN_SPEED,
                    VehiclePropertyIds.HVAC_FAN_SPEED,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA,
                    OptionalActionParameters.<Integer>builder()
                        .setEnableHvacPowerIfDependent()
                        .setIsMinMaxProperty()
                        .build()))
            .put(
                SEAT_ACTION_INCREMENT_HVAC_FAN_SPEED,
                new HvacPowerDependentOffsetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_INCREMENT_HVAC_FAN_SPEED,
                    VehiclePropertyIds.HVAC_FAN_SPEED,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA,
                    OptionalActionParameters.<Integer>builder()
                        .setEnableHvacPowerIfDependent()
                        .setIsMinMaxProperty()
                        .build()))
            .put(
                SEAT_ACTION_DECREMENT_HVAC_FAN_SPEED,
                new HvacPowerDependentOffsetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_DECREMENT_HVAC_FAN_SPEED,
                    VehiclePropertyIds.HVAC_FAN_SPEED,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA,
                    OptionalActionParameters.<Integer>builder()
                        .setEnableHvacPowerIfDependent()
                        .setIsMinMaxProperty()
                        .build()))
            .put(
                SEAT_ACTION_GET_HVAC_FAN_DIRECTION,
                new HvacPowerDependentGetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_GET_HVAC_FAN_DIRECTION,
                    VehiclePropertyIds.HVAC_FAN_DIRECTION,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA))
            .put(
                SEAT_ACTION_SET_HVAC_FAN_DIRECTION,
                new HvacPowerDependentSetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_SET_HVAC_FAN_DIRECTION,
                    VehiclePropertyIds.HVAC_FAN_DIRECTION,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA,
                    OptionalActionParameters.<Integer>builder()
                        .setEnableHvacPowerIfDependent()
                        .setCustomValueRangeGenerator(
                            new CustomValueRangeGenerator<Integer>() {
                              @Override
                              public ErrorOr<ValueRange<Integer>> getValueRange(
                                  CarPropertyManagerCompat carPropertyManagerCompat, int area) {
                                ErrorOr<
                                        ImmutableMap<
                                            Integer, ErrorOr<CarPropertyValueCompat<Integer[]>>>>
                                    areaToValue =
                                        carPropertyManagerCompat.getValues(
                                            VehiclePropertyIds.HVAC_FAN_DIRECTION_AVAILABLE,
                                            ImmutableSet.of(area));
                                if (areaToValue.isError()) {
                                  return ErrorOr.createError(areaToValue.errorCode());
                                }
                                if (areaToValue.value().get(area).isError()) {
                                  return ErrorOr.createError(
                                      areaToValue.value().get(area).errorCode());
                                }
                                return ErrorOr.createValue(
                                    ValueRange.<Integer>create(
                                        Integer.class,
                                        ImmutableList.copyOf(
                                            areaToValue.value().get(area).value().value())));
                              }
                            })
                        .build()))
            .put(
                SEAT_ACTION_GET_SEAT_HEATING_LEVEL,
                new HvacPowerDependentGetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_GET_SEAT_HEATING_LEVEL,
                    VehiclePropertyIds.HVAC_SEAT_TEMPERATURE,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA,
                    OptionalActionParameters.<Integer>builder()
                        .setIsMinMaxProperty()
                        .setRequiredSupportedValues(0)
                        .setCustomValueRangeGenerator(
                            new ZeroOrGreaterThanCustomValueRangeGenerator())
                        .build()))
            .put(
                SEAT_ACTION_SET_SEAT_HEATING_LEVEL,
                new HvacPowerDependentSetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_SET_SEAT_HEATING_LEVEL,
                    VehiclePropertyIds.HVAC_SEAT_TEMPERATURE,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA,
                    OptionalActionParameters.<Integer>builder()
                        .setEnableHvacPowerIfDependent()
                        .setIsMinMaxProperty()
                        .setRequiredSupportedValues(0)
                        .setCustomValueRangeGenerator(
                            new ZeroOrGreaterThanCustomValueRangeGenerator())
                        .build()))
            .put(
                SEAT_ACTION_GET_SEAT_COOLING_LEVEL,
                new HvacPowerDependentGetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_GET_SEAT_COOLING_LEVEL,
                    VehiclePropertyIds.HVAC_SEAT_TEMPERATURE,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA,
                    OptionalActionParameters.<Integer>builder()
                        .setIsMinMaxProperty()
                        .setRequiredSupportedValues(0)
                        .setCustomValueRangeGenerator(new ZeroOrLessThanCustomValueRangeGenerator())
                        .build()))
            .put(
                SEAT_ACTION_SET_SEAT_COOLING_LEVEL,
                new HvacPowerDependentSetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_SET_SEAT_COOLING_LEVEL,
                    VehiclePropertyIds.HVAC_SEAT_TEMPERATURE,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA,
                    OptionalActionParameters.<Integer>builder()
                        .setEnableHvacPowerIfDependent()
                        .setIsMinMaxProperty()
                        .setRequiredSupportedValues(0)
                        .setCustomValueRangeGenerator(new ZeroOrLessThanCustomValueRangeGenerator())
                        .build()))
            .put(
                SEAT_ACTION_GET_HVAC_TARGET_TEMPERATURE,
                new HvacTargetTemperatureGetAction(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_GET_HVAC_TARGET_TEMPERATURE,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA,
                    OptionalActionParameters.<Float>builder()
                        .setIsMinMaxProperty()
                        .setCustomValueRangeGenerator(new HvacTemperatureSetValueRangeGenerator())
                        .build()))
            .put(
                SEAT_ACTION_SET_HVAC_TARGET_TEMPERATURE,
                new HvacTargetTemperatureSetAction(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_SET_HVAC_TARGET_TEMPERATURE,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA,
                    OptionalActionParameters.<Float>builder()
                        .setEnableHvacPowerIfDependent()
                        .setIsMinMaxProperty()
                        .setCustomValueRangeGenerator(new HvacTemperatureSetValueRangeGenerator())
                        .build()))
            .put(
                SEAT_ACTION_INCREMENT_HVAC_TARGET_TEMPERATURE,
                new HvacTargetTemperatureOffsetAction(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_INCREMENT_HVAC_TARGET_TEMPERATURE,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA,
                    OptionalActionParameters.<Float>builder()
                        .setEnableHvacPowerIfDependent()
                        .setIsMinMaxProperty()
                        .setCustomValueRangeGenerator(new HvacTemperatureSetValueRangeGenerator())
                        .build()))
            .put(
                SEAT_ACTION_DECREMENT_HVAC_TARGET_TEMPERATURE,
                new HvacTargetTemperatureOffsetAction(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_DECREMENT_HVAC_TARGET_TEMPERATURE,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA,
                    OptionalActionParameters.<Float>builder()
                        .setEnableHvacPowerIfDependent()
                        .setIsMinMaxProperty()
                        .setCustomValueRangeGenerator(new HvacTemperatureSetValueRangeGenerator())
                        .build()))
            .put(
                SEAT_ACTION_IS_MAX_AC_ENABLED,
                new HvacPowerDependentGetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_IS_MAX_AC_ENABLED,
                    VehiclePropertyIds.HVAC_MAX_AC_ON,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA))
            .put(
                SEAT_ACTION_ENABLE_MAX_AC,
                new HvacPowerDependentSetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_ENABLE_MAX_AC,
                    VehiclePropertyIds.HVAC_MAX_AC_ON,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA,
                    OptionalActionParameters.<Boolean>builder()
                        .setEnableHvacPowerIfDependent()
                        .build()))
            .put(
                SEAT_ACTION_DISABLE_MAX_AC,
                new HvacPowerDependentSetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_DISABLE_MAX_AC,
                    VehiclePropertyIds.HVAC_MAX_AC_ON,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA))
            .put(
                SEAT_ACTION_IS_MAX_HEAT_ENABLED,
                new HvacPowerDependentGetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_IS_MAX_HEAT_ENABLED,
                    VehiclePropertyIds.HVAC_MAX_DEFROST_ON,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA))
            .put(
                SEAT_ACTION_ENABLE_MAX_HEAT,
                new HvacPowerDependentSetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_ENABLE_MAX_HEAT,
                    VehiclePropertyIds.HVAC_MAX_DEFROST_ON,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA,
                    OptionalActionParameters.<Boolean>builder()
                        .setEnableHvacPowerIfDependent()
                        .build()))
            .put(
                SEAT_ACTION_DISABLE_MAX_HEAT,
                new HvacPowerDependentSetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_DISABLE_MAX_HEAT,
                    VehiclePropertyIds.HVAC_MAX_DEFROST_ON,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA))
            .put(
                SEAT_ACTION_IS_HVAC_AUTO_RECIRCULATION_ENABLED,
                new HvacPowerDependentGetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_IS_HVAC_AUTO_RECIRCULATION_ENABLED,
                    VehiclePropertyIds.HVAC_AUTO_RECIRC_ON,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA))
            .put(
                SEAT_ACTION_ENABLE_HVAC_AUTO_RECIRCULATION,
                new HvacPowerDependentSetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_ENABLE_HVAC_AUTO_RECIRCULATION,
                    VehiclePropertyIds.HVAC_AUTO_RECIRC_ON,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA,
                    OptionalActionParameters.<Boolean>builder()
                        .setEnableHvacPowerIfDependent()
                        .build()))
            .put(
                SEAT_ACTION_DISABLE_HVAC_AUTO_RECIRCULATION,
                new HvacPowerDependentSetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    SEAT_ACTION_DISABLE_HVAC_AUTO_RECIRCULATION,
                    VehiclePropertyIds.HVAC_AUTO_RECIRC_ON,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    SEAT_TO_AREA))
            .buildOrThrow());
  }

  @VisibleForTesting
  SeatActions(
      ListeningExecutorService listeningExecutorService,
      ImmutableMap<String, Action<?>> actionMap) {
    super(listeningExecutorService, actionMap);
    Preconditions.checkArgument(actionMap.keySet().equals(VALID_SEAT_ACTIONS));
  }

  /** Returns a non-empty set of supported seats for the {@code action}. */
  public ErrorOr<ImmutableSet<String>> getSupportedSeats(String action) {
    return getSupportedElements(action);
  }

  public <E> ErrorOr<ImmutableMap<String, ValueRange<E>>> getSeatToValueRangeMap(
      Class<E> clazz, String action) {
    return getElementToValueRangeMap(clazz, action);
  }

  /** Async function that checks if the HVAC power is enabled for the passed {@code seats}. */
  public ListenableFuture<ErrorOr<GetResult<Boolean>>> isHvacPowerEnabled(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeGetAction(SEAT_ACTION_IS_HVAC_POWER_ENABLED, seats);
  }

  /** Async function that enables HVAC power for the passed {@code seats}. */
  public ListenableFuture<ErrorOr<SetResult>> enableHvacPower(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeSetAction(SEAT_ACTION_ENABLE_HVAC_POWER, seats, true);
  }

  /** Async function that disables HVAC power for the passed {@code seats}. */
  public ListenableFuture<ErrorOr<SetResult>> disableHvacPower(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeSetAction(SEAT_ACTION_DISABLE_HVAC_POWER, seats, false);
  }

  /**
   * Async function that checks if the HVAC's AC setting is enabled for the passed {@code seats}.
   */
  public ListenableFuture<ErrorOr<GetResult<Boolean>>> isAcEnabled(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeGetAction(SEAT_ACTION_IS_AC_ENABLED, seats);
  }

  /** Async function that enables HVAC power for the passed {@code seats}. */
  public ListenableFuture<ErrorOr<SetResult>> enableAc(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeSetAction(SEAT_ACTION_ENABLE_AC, seats, true);
  }

  /** Async function that disables HVAC power for the passed {@code seats}. */
  public ListenableFuture<ErrorOr<SetResult>> disableAc(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeSetAction(SEAT_ACTION_DISABLE_AC, seats, false);
  }

  /**
   * Async function that checks if the HVAC's recirculation setting is enabled for the passed {@code
   * seats}
   */
  public ListenableFuture<ErrorOr<GetResult<Boolean>>> isHvacRecirculationEnabled(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeGetAction(SEAT_ACTION_IS_HVAC_RECIRCULATION_ENABLED, seats);
  }

  /** Async function that enables HVAC's recirculation setting for the passed {@code seats}. */
  public ListenableFuture<ErrorOr<SetResult>> enableHvacRecirculation(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeSetAction(SEAT_ACTION_ENABLE_HVAC_RECIRCULATION, seats, true);
  }

  /** Async function that disables HVAC's recirculation setting for the passed {@code seats}. */
  public ListenableFuture<ErrorOr<SetResult>> disableHvacRecirculation(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeSetAction(SEAT_ACTION_DISABLE_HVAC_RECIRCULATION, seats, false);
  }

  /** Async function that checks if the HVAC's auto mode is enabled for the passed {@code seats} */
  public ListenableFuture<ErrorOr<GetResult<Boolean>>> isHvacAutoModeEnabled(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeGetAction(SEAT_ACTION_IS_HVAC_AUTO_MODE_ENABLED, seats);
  }

  /** Async function that enables HVAC auto mode for the passed {@code seats}. */
  public ListenableFuture<ErrorOr<SetResult>> enableHvacAutoMode(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeSetAction(SEAT_ACTION_ENABLE_HVAC_AUTO_MODE, seats, true);
  }

  /** Async function that disables HVAC's auto mode for the passed {@code seats}. */
  public ListenableFuture<ErrorOr<SetResult>> disableHvacAutoMode(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeSetAction(SEAT_ACTION_DISABLE_HVAC_AUTO_MODE, seats, false);
  }

  /** Async function that gets the HVAC fan speed for the passed {@code seats}. */
  public ListenableFuture<ErrorOr<GetResult<Integer>>> getHvacFanSpeed(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeGetAction(SEAT_ACTION_GET_HVAC_FAN_SPEED, seats);
  }

  /** Async function that sets the HVAC fan speed for the passed {@code seats}. */
  public ListenableFuture<ErrorOr<SetResult>> setHvacFanSpeed(
      @ParameterName("seats") ImmutableSet<String> seats, @ParameterName("fanSpeed") int fanSpeed) {
    return executeSetAction(SEAT_ACTION_SET_HVAC_FAN_SPEED, seats, fanSpeed);
  }

  /** Async function that increments the HVAC fan speed by 1 for the passed {@code seats}. */
  public ListenableFuture<ErrorOr<OffsetResult<Integer>>> incrementHvacFanSpeed(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return incrementHvacFanSpeed(seats, /* incrementAmount= */ 1);
  }

  /**
   * Async function that increments the HVAC fan speed by {@code incrementAmount} for the passed
   * {@code seats}.
   */
  public ListenableFuture<ErrorOr<OffsetResult<Integer>>> incrementHvacFanSpeed(
      @ParameterName("seats") ImmutableSet<String> seats,
      @ParameterName("incrementAmount") int incrementAmount) {
    if (incrementAmount <= 0) {
      Log.e(TAG, "incrementHvacFanSpeed() - incrementAmount must be positive.");
      return immediateFuture(ErrorOr.createError(ErrorCode.ERROR_CODE_INVALID_API_ARGUMENT));
    }
    return executeApplyOffsetAction(
        SEAT_ACTION_INCREMENT_HVAC_FAN_SPEED, new OffsetRequest.IntOffset(incrementAmount, seats));
  }

  /** Async function that decrements the HVAC fan speed by 1 for the passed {@code seats}. */
  public ListenableFuture<ErrorOr<OffsetResult<Integer>>> decrementHvacFanSpeed(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return decrementHvacFanSpeed(seats, /* decrementAmount= */ 1);
  }

  /**
   * Async function that decrements the HVAC fan speed by {@code decrementAmount} for the passed
   * {@code seats}.
   */
  public ListenableFuture<ErrorOr<OffsetResult<Integer>>> decrementHvacFanSpeed(
      @ParameterName("seats") ImmutableSet<String> seats,
      @ParameterName("decrementAmount") int decrementAmount) {
    if (decrementAmount <= 0) {
      Log.e(TAG, "decrementHvacFanSpeed() - decrementAmount must be positive.");
      return immediateFuture(ErrorOr.createError(ErrorCode.ERROR_CODE_INVALID_API_ARGUMENT));
    }
    return executeApplyOffsetAction(
        SEAT_ACTION_DECREMENT_HVAC_FAN_SPEED,
        new OffsetRequest.IntOffset(switchSign(decrementAmount), seats));
  }

  /** The possible values for the HVAC fan direction. */
  public enum HvacFanDirection {
    DIRECTION_UNKNOWN,
    DIRECTION_FACE,
    DIRECTION_FLOOR,
    DIRECTION_DEFROST,
    DIRECTION_FACE_AND_FLOOR,
    DIRECTION_FACE_AND_DEFROST,
    DIRECTION_DEFROST_AND_FLOOR,
    DIRECTION_FACE_AND_DEFROST_AND_FLOOR,
  }

  /** Async function that gets the HVAC fan direction for the passed {@code seats}. */
  public ListenableFuture<ErrorOr<GetResult<HvacFanDirection>>> getHvacFanDirection(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeGetAction(
        SEAT_ACTION_GET_HVAC_FAN_DIRECTION, seats, SeatActions::hvacFanDirectionTransform);
  }

  /** Async function that sets the HVAC fan direction for the passed {@code seats}. */
  public ListenableFuture<ErrorOr<SetResult>> setHvacFanDirection(
      @ParameterName("seats") ImmutableSet<String> seats,
      @ParameterName("fanDirection") HvacFanDirection fanDirection) {
    return executeSetAction(
        SEAT_ACTION_SET_HVAC_FAN_DIRECTION,
        seats,
        HVAC_FAN_DIRECTION_ENUM_TO_INTEGER_MAP.get(fanDirection));
  }

  /** Async function that gets the seat heating level for the passed {@code seats}. */
  public ListenableFuture<ErrorOr<GetResult<Integer>>> getSeatHeatingLevel(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeGetAction(
        SEAT_ACTION_GET_SEAT_HEATING_LEVEL, seats, ApiActionCategory::zeroOrGreaterTransform);
  }

  /** Async function that sets the seat heating level for the passed {@code seats}. */
  public ListenableFuture<ErrorOr<SetResult>> setSeatHeatingLevel(
      @ParameterName("seats") ImmutableSet<String> seats, @ParameterName("level") int level) {
    if (level < 0) {
      Log.e(TAG, "setSeatHeatingLevel() - level is negative");
      return immediateFuture(ErrorOr.createError(ErrorCode.ERROR_CODE_INVALID_API_ARGUMENT));
    }
    return executeSetAction(SEAT_ACTION_SET_SEAT_HEATING_LEVEL, seats, level);
  }

  /** Async function that gets the seat cooling level for the passed {@code seats}. */
  public ListenableFuture<ErrorOr<GetResult<Integer>>> getSeatCoolingLevel(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeGetAction(
        SEAT_ACTION_GET_SEAT_COOLING_LEVEL, seats, ApiActionCategory::zeroOrLessTransform);
  }

  /** Async function that sets the seat cooling level for the passed {@code seats}. */
  public ListenableFuture<ErrorOr<SetResult>> setSeatCoolingLevel(
      @ParameterName("seats") ImmutableSet<String> seats, @ParameterName("level") int level) {
    if (level < 0) {
      Log.e(TAG, "setSeatCoolingLevel() - level is negative");
      return immediateFuture(ErrorOr.createError(ErrorCode.ERROR_CODE_INVALID_API_ARGUMENT));
    }
    return executeSetAction(SEAT_ACTION_SET_SEAT_COOLING_LEVEL, seats, switchSign(level));
  }

  /**
   * Async function that gets the hvac target temperature in the units specified for the passed
   * {@code seats}.
   *
   * @param seats the seats to get the target temperature for. This cannot be empty.
   * @param isCelsius if true, the target temperature will be returned in Celsius. Otherwise, it
   *     will be returned in Fahrenheit.
   */
  public ListenableFuture<ErrorOr<GetResult<Temperature>>> getHvacTargetTemperature(
      @ParameterName("seats") ImmutableSet<String> seats,
      @ParameterName("isCelsius") boolean isCelsius) {
    return executeGetTemperatureAction(
        SEAT_ACTION_GET_HVAC_TARGET_TEMPERATURE,
        seats,
        isCelsius ? TemperatureUnit.CELSIUS : TemperatureUnit.FAHRENHEIT);
  }

  /**
   * Async function that gets the hvac target temperature according to the current display units for
   * the passed {@code seats}.
   *
   * @param seats the seats to get the target temperature for. This cannot be empty.
   */
  public ListenableFuture<ErrorOr<GetResult<Temperature>>> getHvacTargetTemperature(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeGetTemperatureAction(SEAT_ACTION_GET_HVAC_TARGET_TEMPERATURE, seats);
  }

  /**
   * Async function that sets the hvac target temperature in the units specified for the passed
   * {@code request.seats()}.
   *
   * <p>This method will set the temperature according to the display units and within the bounds of
   * the supported values. If the input temperature is not within the supported values and the
   * client requests {@link UpdateTargetTemperatureRequest#getRoundToNearestSupportedValue()}, the
   * input temperature will be rounded to the nearest supported value. Otherwise, an error will be
   * returned.
   *
   * <p>If the temperature is in Celsius and the display units are in Fahrenheit or vice versa, the
   * temperature will be converted first using standard conversion and then set.
   */
  public ListenableFuture<ErrorOr<SetResult>> setHvacTargetTemperature(
      @ParameterName("request") UpdateTargetTemperatureRequest request) {
    return executeSetAction(SEAT_ACTION_SET_HVAC_TARGET_TEMPERATURE, request);
  }

  /**
   * Async function that increments the HVAC target temperature by {@code request.temperature()} for
   * the passed {@code seats}.
   *
   * <p>This method will increase the temperature according to the display units and within the
   * bounds of the supported values. If the resulting temperature is not within the supported values
   * and the client requests {@link
   * OffsetTargetTemperatureRequest#getRoundToNearestSupportedValue()}, the resulting temperature
   * will be rounded to the nearest supported value. Otherwise, an error will be returned.
   *
   * <p>If the temperature is in Celsius and the display units are in Fahrenheit or vice versa, the
   * temperature will be converted first using the standard ratio of 1C to 1.8F and then offsetted.
   */
  public ListenableFuture<ErrorOr<OffsetResult<Temperature>>> incrementHvacTargetTemperature(
      @ParameterName("request") OffsetTargetTemperatureRequest request) {
    return executeApplyOffsetAction(
        SEAT_ACTION_INCREMENT_HVAC_TARGET_TEMPERATURE,
        new OffsetRequest.TemperatureOffset(
            request.getOffset(), request.getSeats(), request.getRoundToNearestSupportedValue()));
  }

  /**
   * Async function that decrements the HVAC target temperature by {@code decrementAmount} for the
   * passed {@code seats}.
   *
   * <p>This method will decrease the temperature according to the display units and within the
   * bounds of the supported values. If the resulting temperature is not within the supported values
   * and the client requests {@link
   * OffsetTargetTemperatureRequest#getRoundToNearestSupportedValue()}, the resulting temperature
   * will be rounded to the nearest supported value. Otherwise, an error will be returned.
   *
   * <p>If the temperature is in Celsius and the display units are in Fahrenheit or vice versa, the
   * temperature will be converted first using the standard ratio of 1C to 1.8F and then offsetted.
   */
  public ListenableFuture<ErrorOr<OffsetResult<Temperature>>> decrementHvacTargetTemperature(
      @ParameterName("request") OffsetTargetTemperatureRequest request) {
    return executeApplyOffsetAction(
        SEAT_ACTION_DECREMENT_HVAC_TARGET_TEMPERATURE,
        new OffsetRequest.TemperatureOffset(
            new Temperature(
                switchSign(request.getOffset().getValue()), request.getOffset().getUnit()),
            request.getSeats(),
            request.getRoundToNearestSupportedValue()));
  }

  /**
   * Async function that checks if the HVAC's max AC setting is enabled for the passed {@code
   * seats}.
   */
  public ListenableFuture<ErrorOr<GetResult<Boolean>>> isMaxAcEnabled(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeGetAction(SEAT_ACTION_IS_MAX_AC_ENABLED, seats);
  }

  /** Async function that enables HVAC's max AC setting for the passed {@code seats}. */
  public ListenableFuture<ErrorOr<SetResult>> enableMaxAc(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeSetAction(SEAT_ACTION_ENABLE_MAX_AC, seats, true);
  }

  /** Async function that disables HVAC's max AC setting for the passed {@code seats}. */
  public ListenableFuture<ErrorOr<SetResult>> disableMaxAc(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeSetAction(SEAT_ACTION_DISABLE_MAX_AC, seats, false);
  }

  /**
   * Async function that checks if the HVAC's max heat setting is enabled for the passed {@code
   * seats}
   */
  public ListenableFuture<ErrorOr<GetResult<Boolean>>> isMaxHeatEnabled(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeGetAction(SEAT_ACTION_IS_MAX_HEAT_ENABLED, seats);
  }

  /** Async function that enables HVAC's max heat setting for the passed {@code seats}. */
  public ListenableFuture<ErrorOr<SetResult>> enableMaxHeat(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeSetAction(SEAT_ACTION_ENABLE_MAX_HEAT, seats, true);
  }

  /** Async function that disables HVAC's max heat setting for the passed {@code seats}. */
  public ListenableFuture<ErrorOr<SetResult>> disableMaxHeat(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeSetAction(SEAT_ACTION_DISABLE_MAX_HEAT, seats, false);
  }

  /**
   * Async function that checks if the HVAC's automatic recirculation setting is enabled for the
   * passed {@code seats}.
   */
  public ListenableFuture<ErrorOr<GetResult<Boolean>>> isHvacAutoRecirculationEnabled(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeGetAction(SEAT_ACTION_IS_HVAC_AUTO_RECIRCULATION_ENABLED, seats);
  }

  /**
   * Async function that enables HVAC's automatic recirculation setting for the passed {@code
   * seats}.
   */
  public ListenableFuture<ErrorOr<SetResult>> enableHvacAutoRecirculation(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeSetAction(SEAT_ACTION_ENABLE_HVAC_AUTO_RECIRCULATION, seats, true);
  }

  /**
   * Async function that disables HVAC's automatic recirculation setting for the passed {@code
   * seats}.
   */
  public ListenableFuture<ErrorOr<SetResult>> disableHvacAutoRecirculation(
      @ParameterName("seats") ImmutableSet<String> seats) {
    return executeSetAction(SEAT_ACTION_DISABLE_HVAC_AUTO_RECIRCULATION, seats, false);
  }

  /** The HVAC Temperature Set value generator. */
  @VisibleForTesting
  static class HvacTemperatureSetValueRangeGenerator
      implements OptionalActionParameters.CustomValueRangeGenerator<Float> {
    @Override
    public ErrorOr<ValueRange<Float>> getValueRange(
        Float minValue, Float maxValue, ImmutableList<Integer> configArray) {
      int incrementC;
      int incrementF;
      int minTempC;
      int maxTempC;
      int minTempF;
      int maxTempF;
      if (configArray.size() == 6) {
        minTempC = configArray.get(0);
        maxTempC = configArray.get(1);
        incrementC = configArray.get(2);
        minTempF = configArray.get(3);
        maxTempF = configArray.get(4);
        incrementF = configArray.get(5);
      } else {
        // By default assume ratio of 0.5C to 1F.
        incrementC = 5;
        incrementF = 10;

        minTempC = Math.round(minValue * 10f);
        maxTempC = Math.round(maxValue * 10f);
        if (minTempC % 5 != 0 || maxTempC % 5 != 0) {
          Log.e(
              TAG,
              "HvacTemperatureSetValueRangeGenerator.getValueRange() - invalid min/max temp: "
                  + minValue
                  + " "
                  + maxValue);
          return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
        }

        // To create a one to one mapping of Celsius to Fahrenheit, we start with the floor of the
        // converted min temperature in celsius. Then we increment for each celsius value so that we
        // have a one to one mapping.
        int numCelsiusValues = (maxTempC - minTempC) / incrementC;
        minTempF = (int) Math.floor(standardConversionToFahrenheit(minValue)) * 10;
        maxTempF = numCelsiusValues * incrementF + minTempF;
      }

      if (incrementC <= 0
          || incrementF <= 0
          || ((maxTempC - minTempC) % incrementC != 0)
          || ((maxTempF - minTempF) % incrementF != 0)
          || ((maxTempF - minTempF) / incrementF) != ((maxTempC - minTempC) / incrementC)) {
        Log.e(
            TAG,
            "HvacTemperatureSetValueRangeGenerator.getValueRange() - invalid config array: "
                + configArray);
        return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_PLATFORM_IMPL);
      }

      ImmutableList.Builder<Float> supportedValues = ImmutableList.builder();
      for (int temp = minTempC; temp <= maxTempC; temp += incrementC) {
        supportedValues.add(temp / 10f);
      }
      for (int temp = minTempF; temp <= maxTempF; temp += incrementF) {
        supportedValues.add(temp / 10f);
      }

      return ErrorOr.createValue(ValueRange.create(Float.class, supportedValues.build()));
    }
  }

  private static float standardConversionToFahrenheit(float celsius) {
    return (celsius * 1.8f) + 32f;
  }

  private static HvacFanDirection hvacFanDirectionTransform(String element, int value) {
    return HVAC_FAN_DIRECTION_ENUM_TO_INTEGER_MAP.inverse().get(value);
  }
}

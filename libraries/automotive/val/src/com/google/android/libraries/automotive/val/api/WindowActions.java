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
import com.google.android.libraries.automotive.val.actions.ActionCategory.ZeroOrGreaterThanCustomValueRangeGenerator;
import com.google.android.libraries.automotive.val.actions.ActionCategory.ZeroOrLessThanCustomValueRangeGenerator;
import com.google.android.libraries.automotive.val.actions.GetAction;
import com.google.android.libraries.automotive.val.actions.OptionalActionParameters;
import com.google.android.libraries.automotive.val.actions.SetAction;
import com.google.android.libraries.automotive.val.api.ErrorOr.ErrorCode;
import com.google.android.libraries.automotive.val.compat.CarPropertyManagerCompat;
import com.google.android.libraries.automotive.val.utils.PermissionUtility;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

/** The class that contains the window related actions for the Vehicle Action Library (VAL). */
public class WindowActions extends ApiActionCategory {
  private static final String TAG = WindowActions.class.getSimpleName();

  // Windows Elements
  public static final String WINDOW_FRONT_WINDSHIELD = "WINDOW_FRONT_WINDSHIELD";
  public static final String WINDOW_REAR_WINDSHIELD = "WINDOW_REAR_WINDSHIELD";
  public static final String WINDOW_ROW_1_LEFT = "WINDOW_ROW_1_LEFT";
  public static final String WINDOW_ROW_1_RIGHT = "WINDOW_ROW_1_RIGHT";
  public static final String WINDOW_ROW_2_LEFT = "WINDOW_ROW_2_LEFT";
  public static final String WINDOW_ROW_2_RIGHT = "WINDOW_ROW_2_RIGHT";
  public static final String WINDOW_ROW_3_LEFT = "WINDOW_ROW_3_LEFT";
  public static final String WINDOW_ROW_3_RIGHT = "WINDOW_ROW_3_RIGHT";
  public static final String WINDOW_ROOF_TOP_1 = "WINDOW_ROOF_TOP_1";
  public static final String WINDOW_ROOF_TOP_2 = "WINDOW_ROOF_TOP_2";

  // Window Actions
  public static final String WINDOW_ACTION_IS_OPEN = "WINDOW_ACTION_IS_OPEN";
  public static final String WINDOW_ACTION_CLOSE = "WINDOW_ACTION_CLOSE";
  public static final String WINDOW_ACTION_SET_OPEN_POSITION = "WINDOW_ACTION_SET_OPEN_POSITION";
  public static final String WINDOW_ACTION_GET_OPEN_POSITION = "WINDOW_ACTION_GET_OPEN_POSITION";
  public static final String WINDOW_ACTION_SET_VENT_POSITION = "WINDOW_ACTION_SET_VENT_POSITION";
  public static final String WINDOW_ACTION_GET_VENT_POSITION = "WINDOW_ACTION_GET_VENT_POSITION";
  public static final String WINDOW_ACTION_IS_CHILD_LOCKED = "WINDOW_ACTION_IS_CHILD_LOCKED";
  public static final String WINDOW_ACTION_ENABLE_CHILD_LOCK = "WINDOW_ACTION_ENABLE_CHILD_LOCK";
  public static final String WINDOW_ACTION_DISABLE_CHILD_LOCK = "WINDOW_ACTION_DISABLE_CHILD_LOCK";
  public static final String WINDOW_ACTION_IS_HVAC_DEFROSTER_ENABLED =
      "WINDOW_ACTION_IS_HVAC_DEFROSTER_ENABLED";
  public static final String WINDOW_ACTION_ENABLE_HVAC_DEFROSTER =
      "WINDOW_ACTION_ENABLE_HVAC_DEFROSTER";
  public static final String WINDOW_ACTION_DISABLE_HVAC_DEFROSTER =
      "WINDOW_ACTION_DISABLE_HVAC_DEFROSTER";
  public static final String WINDOW_ACTION_IS_ELECTRIC_DEFROSTER_ENABLED =
      "WINDOW_ACTION_IS_ELECTRIC_DEFROSTER_ENABLED";
  public static final String WINDOW_ACTION_ENABLE_ELECTRIC_DEFROSTER =
      "WINDOW_ACTION_ENABLE_ELECTRIC_DEFROSTER";
  public static final String WINDOW_ACTION_DISABLE_ELECTRIC_DEFROSTER =
      "WINDOW_ACTION_DISABLE_ELECTRIC_DEFROSTER";
  private static final ImmutableSet<String> VALID_WINDOW_ACTIONS =
      ImmutableSet.of(
          WINDOW_ACTION_IS_OPEN,
          WINDOW_ACTION_CLOSE,
          WINDOW_ACTION_SET_OPEN_POSITION,
          WINDOW_ACTION_GET_OPEN_POSITION,
          WINDOW_ACTION_SET_VENT_POSITION,
          WINDOW_ACTION_GET_VENT_POSITION,
          WINDOW_ACTION_IS_CHILD_LOCKED,
          WINDOW_ACTION_ENABLE_CHILD_LOCK,
          WINDOW_ACTION_DISABLE_CHILD_LOCK,
          WINDOW_ACTION_IS_HVAC_DEFROSTER_ENABLED,
          WINDOW_ACTION_ENABLE_HVAC_DEFROSTER,
          WINDOW_ACTION_DISABLE_HVAC_DEFROSTER,
          WINDOW_ACTION_IS_ELECTRIC_DEFROSTER_ENABLED,
          WINDOW_ACTION_ENABLE_ELECTRIC_DEFROSTER,
          WINDOW_ACTION_DISABLE_ELECTRIC_DEFROSTER);

  private static final ImmutableBiMap<String, Integer> WINDOW_TO_AREA =
      ImmutableBiMap.of(
          WINDOW_FRONT_WINDSHIELD,
          /*VehicleAreaWindow.WINDOW_FRONT_WINDSHIELD=*/ 0x1,
          WINDOW_REAR_WINDSHIELD,
          /*VehicleAreaWindow.WINDOW_REAR_WINDSHIELD=*/ 0x2,
          WINDOW_ROW_1_LEFT,
          /*VehicleAreaWindow.WINDOW_ROW_1_LEFT=*/ 0x10,
          WINDOW_ROW_1_RIGHT,
          /*VehicleAreaWindow.WINDOW_ROW_1_RIGHT=*/ 0x40,
          WINDOW_ROW_2_LEFT,
          /*VehicleAreaWindow.WINDOW_ROW_2_LEFT=*/ 0x100,
          WINDOW_ROW_2_RIGHT,
          /*VehicleAreaWindow.WINDOW_ROW_2_RIGHT=*/ 0x400,
          WINDOW_ROW_3_LEFT,
          /*VehicleAreaWindow.WINDOW_ROW_3_LEFT=*/ 0x1000,
          WINDOW_ROW_3_RIGHT,
          /*VehicleAreaWindow.WINDOW_ROW_3_RIGHT=*/ 0x4000,
          WINDOW_ROOF_TOP_1,
          /*VehicleAreaWindow.WINDOW_ROOF_TOP_1=*/ 0x10000,
          WINDOW_ROOF_TOP_2,
          /*VehicleAreaWindow.WINDOW_ROOF_TOP_2=*/ 0x20000);

  // This is a copy of {@code Car#PERMISSION_CONTROL_CAR_CLIMATE} because it is a system API.
  private static final String PERMISSION_CONTROL_CAR_CLIMATE =
      "android.car.permission.CONTROL_CAR_CLIMATE";
  // This is a copy of {@code Car#PERMISSION_CONTROL_CAR_WINDOWS} because it is a system API.
  private static final String PERMISSION_CONTROL_CAR_WINDOWS =
      "android.car.permission.CONTROL_CAR_WINDOWS";
  // This is a copy of {@code VehiclePropertyIds#HVAC_ELECTRIC_DEFROSTER_ON} because it is a system
  // API.
  public static final int HVAC_ELECTRIC_DEFROSTER_ON = 320865556;

  WindowActions(
      ListeningExecutorService listeningExecutorService,
      CarPropertyManagerCompat carPropertyManagerCompat,
      PermissionUtility permissionUtility) {
    this(
        listeningExecutorService,
        ImmutableMap.<String, Action<?>>builder()
            .put(
                WINDOW_ACTION_IS_OPEN,
                new GetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    WINDOW_ACTION_IS_OPEN,
                    VehiclePropertyIds.WINDOW_POS,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_WINDOWS,
                    WINDOW_TO_AREA,
                    OptionalActionParameters.<Integer>builder()
                        .setIsMinMaxProperty()
                        .setRequiredSupportedValues(0)
                        .build()))
            .put(
                WINDOW_ACTION_CLOSE,
                new SetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    WINDOW_ACTION_CLOSE,
                    VehiclePropertyIds.WINDOW_POS,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_WINDOWS,
                    WINDOW_TO_AREA,
                    OptionalActionParameters.<Integer>builder()
                        .setIsMinMaxProperty()
                        .setRequiredSupportedValues(0)
                        .build()))
            .put(
                WINDOW_ACTION_GET_OPEN_POSITION,
                new GetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    WINDOW_ACTION_GET_OPEN_POSITION,
                    VehiclePropertyIds.WINDOW_POS,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_WINDOWS,
                    WINDOW_TO_AREA,
                    OptionalActionParameters.<Integer>builder()
                        .setIsMinMaxProperty()
                        .setRequiredSupportedValues(0)
                        .setCustomValueRangeGenerator(
                            new ZeroOrGreaterThanCustomValueRangeGenerator())
                        .build()))
            .put(
                WINDOW_ACTION_SET_OPEN_POSITION,
                new SetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    WINDOW_ACTION_SET_OPEN_POSITION,
                    VehiclePropertyIds.WINDOW_POS,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_WINDOWS,
                    WINDOW_TO_AREA,
                    OptionalActionParameters.<Integer>builder()
                        .setIsMinMaxProperty()
                        .setRequiredSupportedValues(0)
                        .setCustomValueRangeGenerator(
                            new ZeroOrGreaterThanCustomValueRangeGenerator())
                        .build()))
            .put(
                WINDOW_ACTION_GET_VENT_POSITION,
                new GetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    WINDOW_ACTION_GET_VENT_POSITION,
                    VehiclePropertyIds.WINDOW_POS,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_WINDOWS,
                    WINDOW_TO_AREA,
                    OptionalActionParameters.<Integer>builder()
                        .setIsMinMaxProperty()
                        .setRequiredSupportedValues(0)
                        .setCustomValueRangeGenerator(new ZeroOrLessThanCustomValueRangeGenerator())
                        .build()))
            .put(
                WINDOW_ACTION_SET_VENT_POSITION,
                new SetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    WINDOW_ACTION_GET_VENT_POSITION,
                    VehiclePropertyIds.WINDOW_POS,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_WINDOWS,
                    WINDOW_TO_AREA,
                    OptionalActionParameters.<Integer>builder()
                        .setIsMinMaxProperty()
                        .setRequiredSupportedValues(0)
                        .setCustomValueRangeGenerator(new ZeroOrLessThanCustomValueRangeGenerator())
                        .build()))
            .put(
                WINDOW_ACTION_IS_CHILD_LOCKED,
                new GetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    WINDOW_ACTION_IS_CHILD_LOCKED,
                    VehiclePropertyIds.WINDOW_LOCK,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_WINDOWS,
                    WINDOW_TO_AREA))
            .put(
                WINDOW_ACTION_ENABLE_CHILD_LOCK,
                new SetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    WINDOW_ACTION_ENABLE_CHILD_LOCK,
                    VehiclePropertyIds.WINDOW_LOCK,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_WINDOWS,
                    WINDOW_TO_AREA))
            .put(
                WINDOW_ACTION_DISABLE_CHILD_LOCK,
                new SetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    WINDOW_ACTION_DISABLE_CHILD_LOCK,
                    VehiclePropertyIds.WINDOW_LOCK,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_WINDOWS,
                    WINDOW_TO_AREA))
            .put(
                WINDOW_ACTION_IS_HVAC_DEFROSTER_ENABLED,
                new GetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    WINDOW_ACTION_IS_HVAC_DEFROSTER_ENABLED,
                    VehiclePropertyIds.HVAC_DEFROSTER,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    WINDOW_TO_AREA))
            .put(
                WINDOW_ACTION_ENABLE_HVAC_DEFROSTER,
                new SetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    WINDOW_ACTION_IS_HVAC_DEFROSTER_ENABLED,
                    VehiclePropertyIds.HVAC_DEFROSTER,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    WINDOW_TO_AREA))
            .put(
                WINDOW_ACTION_DISABLE_HVAC_DEFROSTER,
                new SetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    WINDOW_ACTION_DISABLE_HVAC_DEFROSTER,
                    VehiclePropertyIds.HVAC_DEFROSTER,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    WINDOW_TO_AREA))
            .put(
                WINDOW_ACTION_IS_ELECTRIC_DEFROSTER_ENABLED,
                new GetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    WINDOW_ACTION_IS_ELECTRIC_DEFROSTER_ENABLED,
                    HVAC_ELECTRIC_DEFROSTER_ON,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    WINDOW_TO_AREA))
            .put(
                WINDOW_ACTION_ENABLE_ELECTRIC_DEFROSTER,
                new SetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    WINDOW_ACTION_IS_ELECTRIC_DEFROSTER_ENABLED,
                    HVAC_ELECTRIC_DEFROSTER_ON,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    WINDOW_TO_AREA))
            .put(
                WINDOW_ACTION_DISABLE_ELECTRIC_DEFROSTER,
                new SetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    WINDOW_ACTION_DISABLE_ELECTRIC_DEFROSTER,
                    HVAC_ELECTRIC_DEFROSTER_ON,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_CLIMATE,
                    WINDOW_TO_AREA))
            .buildOrThrow());
  }

  @VisibleForTesting
  protected WindowActions(
      ListeningExecutorService listeningExecutorService,
      ImmutableMap<String, Action<?>> actionMap) {
    super(listeningExecutorService, actionMap);
    Preconditions.checkArgument(actionMap.keySet().equals(VALID_WINDOW_ACTIONS));
  }

  /** Returns a non-empty set of supported windows for the {@code action}. */
  public ErrorOr<ImmutableSet<String>> getSupportedWindows(String action) {
    return getSupportedElements(action);
  }

  public <E> ErrorOr<ImmutableMap<String, ValueRange<E>>> getWindowToValueRangeMap(
      Class<E> clazz, String action) {
    return getElementToValueRangeMap(clazz, action);
  }

  /**
   * Async function that checks if the window is open for the passed set of {@code windows}.
   *
   * <p>Returns true regardless if the window is opened normally or vented.
   */
  public ListenableFuture<ErrorOr<GetResult<Boolean>>> isOpen(
      @ParameterName("windows") ImmutableSet<String> windows) {
    return Futures.transform(
        this.<Integer>executeGetActionOnListenableFuture(WINDOW_ACTION_IS_OPEN, windows),
        getActionResult -> {
          if (getActionResult.isError()) {
            return ErrorOr.createError(getActionResult.errorCode());
          }

          ImmutableMap.Builder<String, ErrorOr<Boolean>> newElementToValue = ImmutableMap.builder();
          ImmutableMap<String, ErrorOr<Integer>> oldElementToValue =
              getActionResult.value().elementToValue();
          for (String element : oldElementToValue.keySet()) {
            ErrorOr<Integer> value = oldElementToValue.get(element);
            if (value.isError()) {
              newElementToValue.put(element, ErrorOr.createError(value.errorCode()));
              continue;
            }
            newElementToValue.put(element, ErrorOr.createValue(value.value() != 0));
          }
          return ErrorOr.createValue(
              GetResult.create(WINDOW_ACTION_IS_OPEN, newElementToValue.buildOrThrow()));
        },
        listeningExecutorService);
  }

  /**
   * Async function that closes the passed {@code windows}.
   *
   * <p>This will close the window regardless if is opened normally or vented.
   */
  public ListenableFuture<ErrorOr<SetResult>> close(
      @ParameterName("windows") ImmutableSet<String> windows) {
    return executeSetAction(WINDOW_ACTION_CLOSE, windows, 0);
  }

  /**
   * Async function that gets the open position for the passed {@code windows}.
   *
   * <p>See {@link #getWindowToValueRangeMap(Class, String)} for the possible positions. If 0, that
   * means the window is closed or vented, so use {@link #isOpen(ImmutableSet)} to check if the
   * window is open.
   */
  public ListenableFuture<ErrorOr<GetResult<Integer>>> getOpenPosition(
      @ParameterName("windows") ImmutableSet<String> windows) {
    return executeGetAction(
        WINDOW_ACTION_GET_OPEN_POSITION, windows, ApiActionCategory::zeroOrGreaterTransform);
  }

  /**
   * Async function that sets the open position for the passed {@code windows}.
   *
   * <p>See {@link #getWindowToValueRangeMap(Class, String)} for the supported values for {@code
   * position}. Setting {@code position} to 0 will close the window.
   */
  public ListenableFuture<ErrorOr<SetResult>> setOpenPosition(
      @ParameterName("windows") ImmutableSet<String> windows,
      @ParameterName("position") int position) {
    if (position < 0) {
      Log.e(TAG, "setOpenPosition() - position is negative");
      return immediateFuture(ErrorOr.createError(ErrorCode.ERROR_CODE_INVALID_API_ARGUMENT));
    }
    return executeSetAction(WINDOW_ACTION_SET_OPEN_POSITION, windows, position);
  }

  /**
   * Async function that gets the vent position for the passed {@code windows}.
   *
   * <p>See {@link #getWindowToValueRangeMap(Class, String)} for the possible positions. If 0, that
   * means the window is closed or opened normally, so use {@link #isOpen(ImmutableSet)} to check if
   * the window is open.
   */
  public ListenableFuture<ErrorOr<GetResult<Integer>>> getVentPosition(
      @ParameterName("windows") ImmutableSet<String> windows) {
    return executeGetAction(
        WINDOW_ACTION_GET_VENT_POSITION, windows, ApiActionCategory::zeroOrLessTransform);
  }

  /**
   * Async function that sets the vent position for the passed {@code windows}.
   *
   * <p>See {@link #getWindowToValueRangeMap(Class, String)} for the supported values for {@code
   * position}. Setting {@code position} to 0 will close the window.
   */
  public ListenableFuture<ErrorOr<SetResult>> setVentPosition(
      @ParameterName("windows") ImmutableSet<String> windows,
      @ParameterName("position") int position) {
    if (position < 0) {
      Log.e(TAG, "setOpenPosition() - position is negative");
      return immediateFuture(ErrorOr.createError(ErrorCode.ERROR_CODE_INVALID_API_ARGUMENT));
    }
    return executeSetAction(WINDOW_ACTION_SET_VENT_POSITION, windows, switchSign(position));
  }

  /** Async function that checks if the window is child locked for the passed {@code windows}. */
  public ListenableFuture<ErrorOr<GetResult<Boolean>>> isChildLocked(
      @ParameterName("windows") ImmutableSet<String> windows) {
    return executeGetAction(WINDOW_ACTION_IS_CHILD_LOCKED, windows);
  }

  /** Async function that enables child lock for the passed {@code windows}. */
  public ListenableFuture<ErrorOr<SetResult>> enableChildLock(
      @ParameterName("windows") ImmutableSet<String> windows) {
    return executeSetAction(WINDOW_ACTION_ENABLE_CHILD_LOCK, windows, true);
  }

  /** Async function that disables child lock for the passed {@code windows}. */
  public ListenableFuture<ErrorOr<SetResult>> disableChildLock(
      @ParameterName("windows") ImmutableSet<String> windows) {
    return executeSetAction(WINDOW_ACTION_DISABLE_CHILD_LOCK, windows, false);
  }

  /**
   * Async function that checks if the HVAC-based defroster is enabled for the passed {@code
   * windows}.
   */
  public ListenableFuture<ErrorOr<GetResult<Boolean>>> isHvacDefrosterEnabled(
      @ParameterName("windows") ImmutableSet<String> windows) {
    return executeGetAction(WINDOW_ACTION_IS_HVAC_DEFROSTER_ENABLED, windows);
  }

  /** Async function that enables HVAC-based defroster for the passed {@code windows}. */
  public ListenableFuture<ErrorOr<SetResult>> enableHvacDefroster(
      @ParameterName("windows") ImmutableSet<String> windows) {
    return executeSetAction(WINDOW_ACTION_ENABLE_HVAC_DEFROSTER, windows, true);
  }

  /** Async function that disables HVAC-based defroster for the passed {@code windows}. */
  public ListenableFuture<ErrorOr<SetResult>> disableHvacDefroster(
      @ParameterName("windows") ImmutableSet<String> windows) {
    return executeSetAction(WINDOW_ACTION_DISABLE_HVAC_DEFROSTER, windows, false);
  }

  /**
   * Async function that checks if the electric defroster is enabled for the passed {@code windows}.
   */
  public ListenableFuture<ErrorOr<GetResult<Boolean>>> isElectricDefrosterEnabled(
      @ParameterName("windows") ImmutableSet<String> windows) {
    return executeGetAction(WINDOW_ACTION_IS_ELECTRIC_DEFROSTER_ENABLED, windows);
  }

  /** Async function that enables electric defroster for the passed {@code windows}. */
  public ListenableFuture<ErrorOr<SetResult>> enableElectricDefroster(
      @ParameterName("windows") ImmutableSet<String> windows) {
    return executeSetAction(WINDOW_ACTION_ENABLE_ELECTRIC_DEFROSTER, windows, true);
  }

  /** Async function that disables electric defroster for the passed {@code windows}. */
  public ListenableFuture<ErrorOr<SetResult>> disableElectricDefroster(
      @ParameterName("windows") ImmutableSet<String> windows) {
    return executeSetAction(WINDOW_ACTION_DISABLE_ELECTRIC_DEFROSTER, windows, false);
  }
}

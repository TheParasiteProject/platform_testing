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
import com.google.android.libraries.automotive.val.actions.SetAction;
import com.google.android.libraries.automotive.val.compat.CarPropertyManagerCompat;
import com.google.android.libraries.automotive.val.utils.PermissionUtility;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

/** The class that contains the door related actions for the Vehicle Action Library (VAL). */
public class DoorActions extends ApiActionCategory {
  private static final String TAG = DoorActions.class.getSimpleName();

  // Door Elements
  public static final String DOOR_HOOD = "DOOR_HOOD";
  public static final String DOOR_REAR = "DOOR_REAR";
  public static final String DOOR_ROW_1_LEFT = "DOOR_ROW_1_LEFT";
  public static final String DOOR_ROW_1_RIGHT = "DOOR_ROW_1_RIGHT";
  public static final String DOOR_ROW_2_LEFT = "DOOR_ROW_2_LEFT";
  public static final String DOOR_ROW_2_RIGHT = "DOOR_ROW_2_RIGHT";
  public static final String DOOR_ROW_3_LEFT = "DOOR_ROW_3_LEFT";
  public static final String DOOR_ROW_3_RIGHT = "DOOR_ROW_3_RIGHT";
  public static final String DOOR_ROOF_TOP_1 = "DOOR_ROOF_TOP_1";
  public static final String DOOR_ROOF_TOP_2 = "DOOR_ROOF_TOP_2";

  // Door Actions
  public static final String DOOR_ACTION_IS_OPEN = "DOOR_ACTION_IS_OPEN";
  public static final String DOOR_ACTION_CLOSE = "DOOR_ACTION_CLOSE";
  public static final String DOOR_ACTION_SET_OPEN_POSITION = "DOOR_ACTION_SET_OPEN_POSITION";
  public static final String DOOR_ACTION_GET_OPEN_POSITION = "DOOR_ACTION_GET_OPEN_POSITION";
  public static final String DOOR_ACTION_IS_LOCKED = "DOOR_ACTION_IS_LOCKED";
  public static final String DOOR_ACTION_LOCK = "DOOR_ACTION_LOCK";
  public static final String DOOR_ACTION_UNLOCK = "DOOR_ACTION_UNLOCK";
  private static final ImmutableSet<String> VALID_DOOR_ACTIONS =
      ImmutableSet.of(
          DOOR_ACTION_IS_OPEN,
          DOOR_ACTION_CLOSE,
          DOOR_ACTION_SET_OPEN_POSITION,
          DOOR_ACTION_GET_OPEN_POSITION,
          DOOR_ACTION_IS_LOCKED,
          DOOR_ACTION_LOCK,
          DOOR_ACTION_UNLOCK);

  private static final ImmutableBiMap<String, Integer> DOOR_TO_AREA =
      ImmutableBiMap.of(
          DOOR_HOOD,
          /*VehicleAreaDoor.DOOR_HOOD=*/ 0x10000000,
          DOOR_REAR,
          /*VehicleAreaDoor.DOOR_REAR=*/ 0x20000000,
          DOOR_ROW_1_LEFT,
          /*VehicleAreaDoor.DOOR_ROW_1_LEFT=*/ 0x1,
          DOOR_ROW_1_RIGHT,
          /*VehicleAreaDoor.DOOR_ROW_1_RIGHT=*/ 0x4,
          DOOR_ROW_2_LEFT,
          /*VehicleAreaDoor.DOOR_ROW_2_LEFT=*/ 0x10,
          DOOR_ROW_2_RIGHT,
          /*VehicleAreaDoor.DOOR_ROW_2_RIGHT=*/ 0x40,
          DOOR_ROW_3_LEFT,
          /*VehicleAreaDoor.DOOR_ROW_3_LEFT=*/ 0x100,
          DOOR_ROW_3_RIGHT,
          /*VehicleAreaDoor.DOOR_ROW_3_RIGHT=*/ 0x400);

  // This is a copy of {@code Car#PERMISSION_CONTROL_CAR_DOORS} because it is a system API.
  private static final String PERMISSION_CONTROL_CAR_DOORS =
      "android.car.permission.CONTROL_CAR_DOORS";

  DoorActions(
      ListeningExecutorService listeningExecutorService,
      CarPropertyManagerCompat carPropertyManagerCompat,
      PermissionUtility permissionUtility) {
    this(
        listeningExecutorService,
        ImmutableMap.<String, Action<?>>builder()
            .put(
                DOOR_ACTION_IS_OPEN,
                new GetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    DOOR_ACTION_IS_OPEN,
                    VehiclePropertyIds.DOOR_POS,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_DOORS,
                    DOOR_TO_AREA,
                    OptionalActionParameters.<Integer>builder()
                        .setIsMinMaxProperty()
                        .setRequiredSupportedValues(0)
                        .build()))
            .put(
                DOOR_ACTION_CLOSE,
                new SetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    DOOR_ACTION_CLOSE,
                    VehiclePropertyIds.DOOR_POS,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_DOORS,
                    DOOR_TO_AREA,
                    OptionalActionParameters.<Integer>builder()
                        .setIsMinMaxProperty()
                        .setRequiredSupportedValues(0)
                        .build()))
            .put(
                DOOR_ACTION_GET_OPEN_POSITION,
                new GetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    DOOR_ACTION_GET_OPEN_POSITION,
                    VehiclePropertyIds.DOOR_POS,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_DOORS,
                    DOOR_TO_AREA,
                    OptionalActionParameters.<Integer>builder()
                        .setIsMinMaxProperty()
                        .setRequiredSupportedValues(0)
                        .build()))
            .put(
                DOOR_ACTION_SET_OPEN_POSITION,
                new SetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    DOOR_ACTION_SET_OPEN_POSITION,
                    VehiclePropertyIds.DOOR_POS,
                    Integer.class,
                    PERMISSION_CONTROL_CAR_DOORS,
                    DOOR_TO_AREA,
                    OptionalActionParameters.<Integer>builder()
                        .setIsMinMaxProperty()
                        .setRequiredSupportedValues(0)
                        .build()))
            .put(
                DOOR_ACTION_IS_LOCKED,
                new GetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    DOOR_ACTION_IS_LOCKED,
                    VehiclePropertyIds.DOOR_LOCK,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_DOORS,
                    DOOR_TO_AREA))
            .put(
                DOOR_ACTION_LOCK,
                new SetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    DOOR_ACTION_LOCK,
                    VehiclePropertyIds.DOOR_LOCK,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_DOORS,
                    DOOR_TO_AREA))
            .put(
                DOOR_ACTION_UNLOCK,
                new SetAction<>(
                    carPropertyManagerCompat,
                    permissionUtility,
                    DOOR_ACTION_UNLOCK,
                    VehiclePropertyIds.DOOR_LOCK,
                    Boolean.class,
                    PERMISSION_CONTROL_CAR_DOORS,
                    DOOR_TO_AREA))
            .buildOrThrow());
  }

  @VisibleForTesting
  protected DoorActions(
      ListeningExecutorService listeningExecutorService,
      ImmutableMap<String, Action<?>> actionMap) {
    super(listeningExecutorService, actionMap);
    Preconditions.checkArgument(actionMap.keySet().equals(VALID_DOOR_ACTIONS));
  }

  /** Returns a non-empty set of supported doors for the {@code action}. */
  public ErrorOr<ImmutableSet<String>> getSupportedDoors(String action) {
    return getSupportedElements(action);
  }

  public <E> ErrorOr<ImmutableMap<String, ValueRange<E>>> getDoorToValueRangeMap(
      Class<E> clazz, String action) {
    return getElementToValueRangeMap(clazz, action);
  }

  /**
   * Async function that checks if the door is open for the passed set of {@code doors}.
   *
   * <p>Returns true regardless if the door is open.
   */
  public ListenableFuture<ErrorOr<GetResult<Boolean>>> isOpen(
      @ParameterName("doors") ImmutableSet<String> doors) {
    return executeGetAction(DOOR_ACTION_IS_OPEN, doors, (door, value) -> !value.equals(0));
  }

  /** Async function that closes the passed {@code doors}. */
  public ListenableFuture<ErrorOr<SetResult>> close(
      @ParameterName("doors") ImmutableSet<String> doors) {
    return executeSetAction(DOOR_ACTION_CLOSE, doors, 0);
  }

  /**
   * Async function that gets the open position for the passed {@code doors}.
   *
   * <p>See {@link #getDoorToValueRangeMap(Class, String)} for the possible positions. If 0, that
   * means the door is closed.
   */
  public ListenableFuture<ErrorOr<GetResult<Integer>>> getOpenPosition(
      @ParameterName("doors") ImmutableSet<String> doors) {
    return executeGetAction(DOOR_ACTION_GET_OPEN_POSITION, doors);
  }

  /**
   * Async function that sets the open position for the passed {@code doors}.
   *
   * <p>See {@link #getDoorToValueRangeMap(Class, String)} for the supported values for {@code
   * position}. Setting {@code position} to 0 will close the door.
   */
  public ListenableFuture<ErrorOr<SetResult>> setOpenPosition(
      @ParameterName("doors") ImmutableSet<String> doors, @ParameterName("position") int position) {
    return executeSetAction(DOOR_ACTION_SET_OPEN_POSITION, doors, position);
  }

  /** Async function that checks if the passed {@code doors} are locked. */
  public ListenableFuture<ErrorOr<GetResult<Boolean>>> isLocked(
      @ParameterName("doors") ImmutableSet<String> doors) {
    return executeGetAction(DOOR_ACTION_IS_LOCKED, doors);
  }

  /** Async function that locks the passed {@code doors}. */
  public ListenableFuture<ErrorOr<SetResult>> lock(
      @ParameterName("doors") ImmutableSet<String> doors) {
    return executeSetAction(DOOR_ACTION_LOCK, doors, true);
  }

  /** Async function that unlocks the passed {@code doors}. */
  public ListenableFuture<ErrorOr<SetResult>> unlock(
      @ParameterName("doors") ImmutableSet<String> doors) {
    return executeSetAction(DOOR_ACTION_UNLOCK, doors, false);
  }
}

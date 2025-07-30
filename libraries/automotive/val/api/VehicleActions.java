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

import android.car.Car;
import android.car.hardware.property.CarPropertyManager;
import android.content.Context;
import android.util.Log;
import com.google.android.libraries.automotive.val.compat.CarPropertyManagerCompat;
import com.google.android.libraries.automotive.val.utils.PermissionUtility;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListeningExecutorService;

/** Top level API class for the Vehicle Actions Library (VAL). */
public class VehicleActions {
  private static final String TAG = VehicleActions.class.getSimpleName();

  private final DoorActions doorActions;
  private final ExternalLightActions externalLightActions;
  private final SeatActions seatActions;
  private final SteeringWheelActions steeringWheelActions;
  private final WindowActions windowActions;
  private final ImmutableMap<String, ApiActionCategory> apiActionCategoryMap;

  public VehicleActions(Context context, ListeningExecutorService listeningExecutorService) {
    this(context, listeningExecutorService, Car.createCar(context));
  }

  public VehicleActions(
      Context context, ListeningExecutorService listeningExecutorService, Car car) {
    Preconditions.checkNotNull(context);
    Preconditions.checkNotNull(listeningExecutorService);
    Preconditions.checkNotNull(car);
    CarPropertyManagerCompat carPropertyManagerCompat =
        new CarPropertyManagerCompat((CarPropertyManager) car.getCarManager(Car.PROPERTY_SERVICE));
    PermissionUtility permissionUtility = new PermissionUtility(context);
    doorActions =
        new DoorActions(listeningExecutorService, carPropertyManagerCompat, permissionUtility);
    externalLightActions =
        new ExternalLightActions(
            listeningExecutorService, carPropertyManagerCompat, permissionUtility);
    seatActions =
        new SeatActions(listeningExecutorService, carPropertyManagerCompat, permissionUtility);
    steeringWheelActions =
        new SteeringWheelActions(
            listeningExecutorService, carPropertyManagerCompat, permissionUtility);
    windowActions =
        new WindowActions(listeningExecutorService, carPropertyManagerCompat, permissionUtility);
    apiActionCategoryMap =
        ImmutableMap.of(
            DoorActions.class.getSimpleName(),
            doorActions,
            ExternalLightActions.class.getSimpleName(),
            externalLightActions,
            SeatActions.class.getSimpleName(),
            seatActions,
            SteeringWheelActions.class.getSimpleName(),
            steeringWheelActions,
            WindowActions.class.getSimpleName(),
            windowActions);
  }

  @VisibleForTesting
  protected VehicleActions(
      DoorActions doorActions,
      ExternalLightActions externalLightActions,
      SeatActions seatActions,
      SteeringWheelActions steeringWheelActions,
      WindowActions windowActions) {
    this.doorActions = doorActions;
    this.externalLightActions = externalLightActions;
    this.seatActions = seatActions;
    this.steeringWheelActions = steeringWheelActions;
    this.windowActions = windowActions;
    apiActionCategoryMap =
        ImmutableMap.of(
            DoorActions.class.getSimpleName(),
            doorActions,
            ExternalLightActions.class.getSimpleName(),
            externalLightActions,
            SeatActions.class.getSimpleName(),
            seatActions,
            SteeringWheelActions.class.getSimpleName(),
            steeringWheelActions,
            WindowActions.class.getSimpleName(),
            windowActions);
  }

  public DoorActions getDoorActions() {
    return doorActions;
  }

  public ExternalLightActions getExternalLightActions() {
    return externalLightActions;
  }

  public SeatActions getSeatActions() {
    return seatActions;
  }

  public SteeringWheelActions getSteeringWheelActions() {
    return steeringWheelActions;
  }

  public WindowActions getWindowActions() {
    return windowActions;
  }

  /** Returns a set of all supported actions across all categories. */
  public ImmutableSet<FunctionDeclaration> fetchAllSupportedFunctionDeclarations() {
    // Store all supported action declarations from each action category.
    ImmutableSet.Builder<FunctionDeclaration> supportedFunctionDeclarationsBuilder =
        ImmutableSet.builder();

    for (ApiActionCategory apiActionCategory : apiActionCategoryMap.values()) {
      ImmutableSet<FunctionDeclaration> supportedFunctionDeclarationsPerCategory =
          apiActionCategory.fetchAllSupportedFunctionDeclarations();

      supportedFunctionDeclarationsBuilder.addAll(supportedFunctionDeclarationsPerCategory);
    }
    return supportedFunctionDeclarationsBuilder.build();
  }

  public FunctionResponse execute(FunctionCall functionCall) {
    Log.e(TAG, "execute() - functionCall: " + functionCall.getName());

    ApiActionCategory apiActionCategory =
        apiActionCategoryMap.get(functionCall.getApiActionCategory());

    ImmutableSet<FunctionDeclaration> supportedFunctionDeclarations =
        apiActionCategory.fetchAllSupportedFunctionDeclarations();

    Log.e(
        TAG,
        "execute() - supportedFunctionDeclarations: "
            + apiActionCategory
            + "below\n: "
            + supportedFunctionDeclarations);
    if (apiActionCategory.fetchAllSupportedFunctionDeclarations().stream()
        .anyMatch(
            functionDeclaration -> functionDeclaration.getName().equals(functionCall.getName()))) {
      return apiActionCategory.execute(functionCall);
    }

    return new FunctionResponse(
        functionCall.getId(),
        functionCall.getName(),
        ImmutableMap.of(
            "status",
            "ERROR",
            "message",
            "No action category found for function call: " + functionCall.getName()));
  }
}

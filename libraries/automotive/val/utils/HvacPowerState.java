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

package com.google.android.libraries.automotive.val.utils;

import com.google.android.libraries.automotive.val.api.ErrorOr.ErrorCode;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;

/** Class that represents the state of the HVAC power. */
@AutoValue
public abstract class HvacPowerState {
  public abstract Optional<ErrorCode> errorCode();

  public abstract ImmutableSet<String> seatsThatEnabledHvacPower();

  public abstract ImmutableSet<String> seatsToUpdate();

  public abstract ImmutableMap<String, ErrorCode> seatToErrorCode();

  public static Builder builder() {
    return new AutoValue_HvacPowerState.Builder()
        .setErrorCode(Optional.empty())
        .setSeatsThatEnabledHvacPower(ImmutableSet.of())
        .setSeatsToUpdate(ImmutableSet.of())
        .setSeatToErrorCode(ImmutableMap.of());
  }

  /** Builder for {@link HvacPowerState}. */
  @AutoValue.Builder
  public abstract static class Builder {
    abstract Builder setErrorCode(Optional<ErrorCode> errorCode);

    public abstract Builder setSeatsThatEnabledHvacPower(
        ImmutableSet<String> seatsThatEnabledHvacPower);

    public abstract Builder setSeatsToUpdate(ImmutableSet<String> seatsToUpdate);

    public abstract Builder setSeatToErrorCode(ImmutableMap<String, ErrorCode> seatToErrorCode);

    public abstract HvacPowerState build();
  }
}

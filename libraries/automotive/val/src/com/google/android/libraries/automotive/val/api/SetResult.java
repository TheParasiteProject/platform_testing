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

import com.google.android.libraries.automotive.val.api.ErrorOr.ErrorCode;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * Result from the execution of a {@code SeatActions#enable*(ImmutableSet<String>)}, a {@code
 * SeatActions#disable*(ImmutableSet<String>)} or a {@code SeatActions#set*(ImmutableSet<String>)}
 * action.
 */
@AutoValue
public abstract class SetResult {

  public static SetResult create(
      String actionName, ImmutableMap<String, ErrorCode> seatToErrorCode) {
    Preconditions.checkNotNull(actionName);
    Preconditions.checkArgument(!actionName.isEmpty());
    Preconditions.checkNotNull(seatToErrorCode);
    return new AutoValue_SetResult(actionName, seatToErrorCode);
  }

  /** Returns {@code true} if {@link #seatToErrorCode()} is empty. Otherwise {@code false}. */
  public boolean isSuccess() {
    return seatToErrorCode().isEmpty();
  }

  public abstract String actionName();

  public abstract ImmutableMap<String, ErrorCode> seatToErrorCode();
}

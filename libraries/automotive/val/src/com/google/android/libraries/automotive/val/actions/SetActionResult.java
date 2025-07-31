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

import com.google.android.libraries.automotive.val.api.ErrorOr.ErrorCode;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/** Result from the execution of a {@link SetAction}. */
@AutoValue
public abstract class SetActionResult {

  public static SetActionResult create(
      String actionName, ImmutableMap<String, ErrorCode> elementToErrorCode) {
    Preconditions.checkNotNull(actionName);
    Preconditions.checkArgument(!actionName.isEmpty());
    Preconditions.checkNotNull(elementToErrorCode);
    return new AutoValue_SetActionResult(actionName, elementToErrorCode);
  }

  /** Returns {@code true} if {@link #elementToErrorCode()} is empty. Otherwise {@code false}. */
  public boolean isSuccess() {
    return elementToErrorCode().isEmpty();
  }

  public abstract String actionName();

  public abstract ImmutableMap<String, ErrorCode> elementToErrorCode();
}

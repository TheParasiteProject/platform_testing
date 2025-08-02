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

import com.google.android.libraries.automotive.val.api.ErrorOr;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/** Result from the execution of a {@link GetAction}. */
@AutoValue
public abstract class GetActionResult<T> {

  public static <U> GetActionResult<U> create(
      String actionName, ImmutableMap<String, ErrorOr<U>> elementToValue) {
    Preconditions.checkNotNull(actionName);
    Preconditions.checkArgument(!actionName.isEmpty());
    Preconditions.checkNotNull(elementToValue);
    Preconditions.checkArgument(!elementToValue.isEmpty());
    return new AutoValue_GetActionResult<U>(actionName, elementToValue);
  }

  public abstract String actionName();

  public abstract ImmutableMap<String, ErrorOr<T>> elementToValue();
}

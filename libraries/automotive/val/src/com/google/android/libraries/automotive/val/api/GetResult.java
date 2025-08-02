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

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/** Result from the execution of a "Get" action. */
@AutoValue
public abstract class GetResult<T> {

  public static <T> GetResult<T> create(
      String actionName, ImmutableMap<String, ErrorOr<T>> elementToValue) {
    Preconditions.checkNotNull(actionName);
    Preconditions.checkArgument(!actionName.isEmpty());
    Preconditions.checkNotNull(elementToValue);
    Preconditions.checkArgument(!elementToValue.isEmpty());
    return new AutoValue_GetResult<>(actionName, elementToValue);
  }

  /**
   * Returns {@code true} if no error codes are set in {@link #elementToValue} values. Otherwise
   * {@code false}.
   */
  public boolean isSuccess() {
    for (ErrorOr<T> value : elementToValue().values()) {
      if (value.isError()) {
        return false;
      }
    }
    return true;
  }

  public abstract String actionName();

  public abstract ImmutableMap<String, ErrorOr<T>> elementToValue();
}

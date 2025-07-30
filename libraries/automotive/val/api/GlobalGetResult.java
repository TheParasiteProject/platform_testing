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

/** Result from the execution of a {@code GetAction} that applies to the whole vehicle. */
@AutoValue
public abstract class GlobalGetResult<T> {

  public static <T> GlobalGetResult<T> create(String actionName, ErrorOr<T> value) {
    Preconditions.checkNotNull(actionName);
    Preconditions.checkArgument(!actionName.isEmpty());
    Preconditions.checkNotNull(value);
    return new AutoValue_GlobalGetResult<>(actionName, value);
  }

  /** Returns {@code true} if {@link #value()} is not an error. Otherwise {@code false}. */
  public boolean isSuccess() {
    return !value().isError();
  }

  public abstract String actionName();

  public abstract ErrorOr<T> value();
}

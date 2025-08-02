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

package com.google.android.libraries.automotive.val.compat;

import com.google.auto.value.AutoValue;
import org.jspecify.annotations.Nullable;

/** Compatibility version of the AAOS AreaIdConfig. */
@AutoValue
public abstract class CarAreaConfigCompat<T> {

  public abstract int areaId();

  public abstract boolean isReadable();

  public abstract boolean isWritable();

  public abstract @Nullable T minSupportedValue();

  public abstract @Nullable T maxSupportedValue();

  public static <U> Builder<U> builder(int areaId) {
    return new AutoValue_CarAreaConfigCompat.Builder<U>()
        .setAreaId(areaId)
        .setIsReadable(false)
        .setIsWritable(false)
        .setMinSupportedValue(null)
        .setMaxSupportedValue(null);
  }

  /** Builder for {@link CarAreaConfigCompat}. */
  @AutoValue.Builder
  public abstract static class Builder<T> {
    abstract Builder<T> setAreaId(int areaId);

    public abstract Builder<T> setIsReadable(boolean isReadable);

    public abstract Builder<T> setIsWritable(boolean isWritable);

    public abstract Builder<T> setMinSupportedValue(T minSupportedValue);

    public abstract Builder<T> setMaxSupportedValue(T maxSupportedValue);

    public abstract CarAreaConfigCompat<T> build();
  }
}

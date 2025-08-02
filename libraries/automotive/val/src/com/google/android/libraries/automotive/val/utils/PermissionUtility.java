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

import android.content.Context;
import android.content.pm.PackageManager;
import com.google.common.base.Preconditions;

/** Simple wrapper around {@link Context} to check permissions. */
public class PermissionUtility {

  private final Context context;

  public PermissionUtility(Context context) {
    Preconditions.checkNotNull(context);
    this.context = context;
  }

  /** Check if {@code permission} is granted. */
  public boolean isPermissionGranted(String permission) {
    Preconditions.checkNotNull(permission);
    Preconditions.checkArgument(!permission.isEmpty());
    return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
  }
}

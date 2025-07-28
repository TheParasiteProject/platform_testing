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

import static java.lang.Math.abs;

import android.util.Log;
import com.google.android.libraries.automotive.val.api.ErrorOr;
import com.google.android.libraries.automotive.val.api.ErrorOr.ErrorCode;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

/** A utility class for actions. */
public final class ActionUtils {
  private static final String TAG = ActionUtils.class.getSimpleName();
  private static final float FLOAT_TOLERANCE = 1e-5f;

  /**
   * Returns the areas for the passed {@code elements}.
   *
   * <p>The areas are returned if all the passed {@code elements} are contained in the {@code
   * elementToArea} map. Otherwise an error code is returned.
   */
  public static ErrorOr<ImmutableSet<Integer>> getAreas(
      Set<String> elements, BiMap<String, Integer> elementToArea) {
    if (elements == null) {
      Log.e(TAG, "getAreas() - elements is null");
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }
    if (elements.isEmpty()) {
      Log.e(TAG, "getAreas() - elements is empty");
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }
    if (elementToArea == null) {
      Log.e(TAG, "getAreas() - elementToArea is null");
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }
    if (elementToArea.isEmpty()) {
      Log.e(TAG, "getAreas() - elementToArea is empty");
      return ErrorOr.createError(ErrorCode.ERROR_CODE_BAD_VAL_IMPL);
    }

    ImmutableSet.Builder<Integer> areasBuilder = ImmutableSet.builder();
    for (String element : elements) {
      Integer area = elementToArea.get(element);
      if (area == null) {
        Log.e(TAG, "getAreas() - elementToArea does not contain element: " + element);
        return ErrorOr.createError(ErrorCode.ERROR_CODE_UNDEFINED_ELEMENT);
      }
      areasBuilder.add(area);
    }
    return ErrorOr.createValue(areasBuilder.build());
  }

  public static boolean floatEquals(float a, float b) {
    return abs(a - b) < FLOAT_TOLERANCE;
  }

  private ActionUtils() {}
}

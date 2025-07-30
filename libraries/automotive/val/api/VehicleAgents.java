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

import android.util.Log;
import androidx.annotation.Nullable;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.Parameter;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.ListenableFuture;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Static utility class for Vehicle Agent operations. */
final class VehicleAgents {
  private static final String TAG = VehicleAgents.class.getSimpleName();
  private static final String FIELD_NAME_SUFFIX = "_TO_AREA";

  /** Converts a property class to a [Type]. */
  static Schema.Type convertType(TypeToken<?> propertyType) {

    if (propertyType.isSubtypeOf(new TypeToken<Collection<?>>() {})) {
      return Schema.Type.ARRAY;
    }
    if (propertyType.isSubtypeOf(new TypeToken<String>() {})) {
      return Schema.Type.STRING;
    }
    if (propertyType.isSubtypeOf(new TypeToken<Integer>() {})
        || propertyType.getRawType().equals(int.class)) {
      return Schema.Type.INTEGER;
    }
    if (propertyType.isSubtypeOf(new TypeToken<Boolean>() {})
        || propertyType.getRawType().equals(boolean.class)) {
      return Schema.Type.BOOLEAN;
    }
    if (propertyType.isSubtypeOf(new TypeToken<Float>() {})
        || propertyType.isSubtypeOf(new TypeToken<Double>() {})
        || propertyType.getRawType().equals(float.class)
        || propertyType.getRawType().equals(double.class)) {
      return Schema.Type.NUMBER;
    }

    return Schema.Type.TYPE_UNSPECIFIED;
  }

  /**
   * Returns a set of parameter values in String format for function declaration's parameters.
   *
   * <p>eg. For {@code SeatActions} class, it returns a set of String values of seat names.
   */
  static ImmutableList<String> getElementNames(Class<?> clazz) {
    Field foundField = null;
    for (Field field : clazz.getDeclaredFields()) {
      if (field.getName().endsWith(FIELD_NAME_SUFFIX)) {
        foundField = field;
        break;
      }
    }
    if (foundField == null) {
      Log.w(
          TAG,
          String.format(
              "getParameterStringValues: Could not find field name with suffix %s in class %s",
              FIELD_NAME_SUFFIX, clazz.getSimpleName()));
      return ImmutableList.of();
    }

    try {

      if (isImmutableBiMapPrivateFinalStaticField(foundField)) {

        foundField.setAccessible(true); // Access the private field.

        // Safe to cast because we checked the type above.
        @SuppressWarnings("unchecked")
        ImmutableBiMap<String, Integer> specificMap =
            (ImmutableBiMap<String, Integer>) foundField.get(null);
        return specificMap.keySet().asList();
      }
    } catch (IllegalAccessException e) {
      Log.w(
          TAG,
          "getParameterStringValues: Could not access field '"
              + foundField.getName()
              + "': "
              + e.getMessage());
      return ImmutableList.of();
    }

    return ImmutableList.of();
  }

  /**
   * Returns a set of all async action-related method in {@code ApiActionCategory} class each of
   * which return a ListenableFuture type.
   *
   * <p>This is used to populate the {@code FunctionDeclaration} class.
   */
  static ImmutableSet<Method> getActionMethods(Class<?> clazz) {

    Set<Method> actionMethods = new HashSet<>();
    Method[] declaredMethods = clazz.getDeclaredMethods();

    for (Method method : declaredMethods) {
      if (Modifier.isPublic(method.getModifiers())
          && ListenableFuture.class.isAssignableFrom(method.getReturnType())) {
        actionMethods.add(method);
      }
    }
    return ImmutableSet.copyOf(actionMethods);
  }

  /** Returns a list of parameter names for the given method. */
  static ImmutableList<String> getParameterNames(Method method) {
    Annotation[][] parameterAnnotations = method.getParameterAnnotations();
    ImmutableList.Builder<String> parameterNames = ImmutableList.builder();

    for (Annotation[] annotations : parameterAnnotations) {
      String paramName = null;
      for (Annotation annotation : annotations) {
        if (annotation instanceof ParameterName parameterName) {
          paramName = parameterName.value();
          break;
        }
      }
      if (paramName != null) {
        parameterNames.add(paramName);
      }
    }
    return parameterNames.build();
  }

  /**
   * Puts the parameter name and type in the map to populate {@code FunctionDeclaration}.
   *
   * <p>If the type is not supported, it will be logged and not added to the map.
   */
  static void putParameterNameAndTypeInMap(
      String parameterName,
      TypeToken<?> typeToken,
      ImmutableMap.Builder<String, Schema.Type> parameterNamesToTypesMap) {
    Schema.Type type = convertType(typeToken);

    if (type == Schema.Type.TYPE_UNSPECIFIED) {
      Log.w(
          TAG,
          String.format(
              "putParameterNameAndTypeInMap: Type [%s] of parameter [%s] is not supported: %s",
              typeToken.getRawType().getSimpleName(), parameterName, type.name()));
    } else {
      parameterNamesToTypesMap.put(parameterName, type);
    }
  }

  /** Returns true if the field is a static final {@code ImmutableBiMap<String, Integer>}. */
  private static boolean isImmutableBiMapPrivateFinalStaticField(Field field) {
    if (!Modifier.isPrivate(field.getModifiers())
        || !Modifier.isStatic(field.getModifiers())
        || !Modifier.isFinal(field.getModifiers())
        || !ImmutableBiMap.class.isAssignableFrom(field.getType())) {
      Log.w(
          TAG,
          "isImmutableBiMapFinalStaticField: Field '"
              + field.getName()
              + "' is not a static final ImmutableBiMap.");
      return false;
    }
    java.lang.reflect.Type genericType = field.getGenericType();
    if (!(genericType instanceof ParameterizedType parameterizedType)) {
      Log.w(
          TAG,
          "isImmutableBiMapFinalStaticField: Field '"
              + field.getName()
              + "' is not a ParameterizedType.");
      return false;
    }
    java.lang.reflect.Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
    if (actualTypeArguments.length != 2
        || !actualTypeArguments[0].equals(String.class)
        || !actualTypeArguments[1].equals(Integer.class)) {
      Log.w(
          TAG,
          "isImmutableBiMapFinalStaticField: Field '"
              + field.getName()
              + "' is not an ImmutableBiMap<String, Integer>.");
      return false;
    }
    return true;
  }

  /**
   * Returns a list of parameters for the given invokable.
   *
   * <p>For UpdateTargetTemperatureRequest and OffsetTargetTemperatureRequest, the parameters are
   * extracted from these classes instead of the method itself.
   */
  static ImmutableMap<String, Schema.Type> getParameterNamesToTypesMap(Method method) {
    ImmutableMap.Builder<String, Schema.Type> parameterNamesToTypesMap = ImmutableMap.builder();

    ImmutableList<String> parameterNames = VehicleAgents.getParameterNames(method);
    ImmutableList<Parameter> parameters = Invokable.from(method).getParameters();

    for (int i = 0; i < parameters.size(); i++) {
      TypeToken<?> typeToken = parameters.get(i).getType();

      // Special case: Populate the member variables inside the field classes for custom
      // temperature-related java class types instead of directly populating the fields themselves.
      if (typeToken.getRawType().equals(UpdateTargetTemperatureRequest.class)
          || typeToken.getRawType().equals(OffsetTargetTemperatureRequest.class)) {

        // Fields include a set of `seats` and Temperature Object.
        for (Method tempMethod : typeToken.getRawType().getMethods()) {
          // Special case for Temperature Object. Use method name of Temperature class - `offset()`
          // or `temperature()` - as the parameter name.
          if (tempMethod.getName().equals("getSeats")) {
            VehicleAgents.putParameterNameAndTypeInMap(
                "seats", TypeToken.of(tempMethod.getReturnType()), parameterNamesToTypesMap);
          } else if (tempMethod.getName().equals("getTemperature")) {
            VehicleAgents.putParameterNameAndTypeInMap(
                "temperature", TypeToken.of(Float.class), parameterNamesToTypesMap);
          } else if (tempMethod.getName().equals("getOffset")) {
            VehicleAgents.putParameterNameAndTypeInMap(
                "offset", TypeToken.of(Float.class), parameterNamesToTypesMap);
          }
        }

      } else {
        // Populate other parameters.
        VehicleAgents.putParameterNameAndTypeInMap(
            parameterNames.get(i), typeToken, parameterNamesToTypesMap);
      }
    }

    return parameterNamesToTypesMap.buildOrThrow();
  }

  /** Returns the overlapping range of a collection of {@link ValueRange} instances. */
  @Nullable
  static <N extends Number & Comparable<N>> ValueRange<Float> findOverlappingRange(
      Collection<ValueRange<N>> ranges) {
    if (ranges == null || ranges.isEmpty()) {
      Log.w(TAG, "findOverlappingRange() - null or empty collection of `ValueRange` provided.");
      return null;
    }

    List<ValueRange<N>> sortedRanges = new ArrayList<>(ranges);
    // Sort the ranges based on their minimum values
    sortedRanges.sort(Comparator.comparing(ValueRange::min));

    N overlapMin = sortedRanges.get(0).min();
    N overlapMax = sortedRanges.get(0).max();

    @SuppressWarnings("unchecked")
    Class<N> clazz = (Class<N>) overlapMin.getClass();

    if (!clazz.equals(Integer.class) && !clazz.equals(Float.class)) {

      Log.w(
          TAG,
          String.format(
              "findOverlappingRange() - Invalid type found in `ValueRange`: %s. Must be either"
                  + " Integer or Float.",
              clazz.getSimpleName()));
      return null;
    }

    for (int i = 1; i < sortedRanges.size(); i++) {
      ValueRange<N> currentRange = sortedRanges.get(i);

      // If the current range starts after the previous overlap ends, there's no continuous overlap.
      if (currentRange.min().compareTo(overlapMax) > 0
          || currentRange.max().compareTo(overlapMin) < 0) {
        Log.w(TAG, "findOverlappingRange() - No continuous overlap found.");
        return null;
      }

      // Update the overlap boundaries
      if (overlapMin.compareTo(currentRange.min()) < 0) {
        overlapMin = currentRange.min();
      }
      if (overlapMax.compareTo(currentRange.max()) > 0) {
        overlapMax = currentRange.max();
      }
    }

    return ValueRange.create(overlapMin.floatValue(), overlapMax.floatValue());
  }

  private VehicleAgents() {}
}

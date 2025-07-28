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

import static java.lang.Math.max;

import android.util.Log;
import androidx.annotation.Nullable;
import com.google.android.libraries.automotive.val.actions.Action;
import com.google.android.libraries.automotive.val.actions.ActionCategory;
import com.google.android.libraries.automotive.val.api.Temperature.TemperatureUnit;
import com.google.android.libraries.automotive.val.utils.VehicleActionStringUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;

/**
 * An abstract class that contains the common API specific code for the vehicle actions categories.
 */
abstract class ApiActionCategory extends ActionCategory {
  private static final String TAG = ApiActionCategory.class.getSimpleName();

  private static final String TEMPERATURE = "temperature";
  private static final String OFFSET = "offset";
  private static final String SEATS = "seats";
  private static final String WINDOWS = "windows";
  private static final String DOORS = "doors";
  private static final ImmutableSet<String> ENUM_PARAMETERS =
      ImmutableSet.of(SEATS, WINDOWS, DOORS);
  // If the temperature is above the threshold, safe to assume that the value is in Fahrenheit.
  // Else, Celsius.
  private static final float TEMPERATURE_UNIT_THRESHOLD = 35.0f;

  protected ApiActionCategory(
      ListeningExecutorService listeningExecutorService,
      ImmutableMap<String, Action<?>> actionMap) {
    super(listeningExecutorService, actionMap);
  }

  public ErrorOr<Boolean> isActionSupported(String action) {
    return super.isActionSupportedInternal(action);
  }

  /**
   * Returns a set of {@link FunctionDeclaration} instances for all supported actions for a given
   * {@link ApiActionCategory}.
   */
  public ImmutableSet<FunctionDeclaration> fetchAllSupportedFunctionDeclarations() {
    ImmutableSet.Builder<FunctionDeclaration> functionDeclarations = ImmutableSet.builder();

    for (Method method : VehicleAgents.getActionMethods(this.getClass())) {
      String methodName = method.getName();
      Map<String, Schema> propertiesMap = new HashMap<>();

      for (Map.Entry<String, Schema.Type> entry :
          VehicleAgents.getParameterNamesToTypesMap(method).entrySet()) {
        String parameterName = entry.getKey();
        Schema.Type parameterType = entry.getValue();

        boolean isEnumParameter = ENUM_PARAMETERS.contains(parameterName);

        // Common properties for both numeric and non-numeric types
        String description = VehicleActionStringUtils.convertMethodNameToSentence(parameterName);
        String format = null;
        List<String> enumValues = null;
        Double minimum = null;
        Double maximum = null;

        // For numeric types.
        if (parameterType.equals(Schema.Type.NUMBER) || parameterType.equals(Schema.Type.INTEGER)) {

          String actionName =
              VehicleActionStringUtils.convertMethodNameToActionName(
                  methodName, this.getClass().getSimpleName());
          Log.d(
              TAG,
              String.format(
                  "fetchAllSupportedFunctionDeclarations() - actionName: [%s], methodName: [%s],"
                      + " action category name: [%s]",
                  actionName, methodName, this.getClass().getSimpleName()));

          ValueRange<Float> range = getSupportedOverlappingMinMax(actionName);

          if (range != null) {
            minimum = Double.valueOf(range.min().floatValue());
            maximum = Double.valueOf(range.max().floatValue());
          }
        } else {
          // For all non-numeric parameter types.
          format = isEnumParameter ? "enum" : null;
          enumValues = isEnumParameter ? VehicleAgents.getElementNames(this.getClass()) : null;
        }

        Schema schema =
            new Schema(
                parameterType,
                description,
                format,
                enumValues,
                /* properties= */ null,
                /* required= */ null,
                minimum,
                maximum);
        propertiesMap.put(parameterName, schema);
      }

      FunctionDeclaration functionDeclaration =
          new FunctionDeclaration(
              methodName,
              VehicleActionStringUtils.convertMethodNameToSentence(methodName),
              new Schema(
                  /* type= */ Schema.Type.OBJECT,
                  /* description= */ null,
                  /* format= */ null,
                  /* enum= */ null,
                  /* properties= */ propertiesMap,
                  /* required= */ ImmutableList.copyOf(propertiesMap.keySet()),
                  /* minimum= */ null,
                  /* maximum= */ null),
              /* response= */ null);

      functionDeclarations.add(functionDeclaration);
    }

    return functionDeclarations.build();
  }

  /** Executes the given {@link FunctionCall}. */
  public FunctionResponse execute(FunctionCall functionCall) {
    Log.d(TAG, "execute() - functionCall: " + functionCall);
    Optional<FunctionDeclaration> functionDeclaration =
        fetchAllSupportedFunctionDeclarations().stream()
            .filter(funcDec -> funcDec.getName().equals(functionCall.getName()))
            .findFirst();
    if (functionDeclaration.isEmpty()) {
      Log.w(
          TAG,
          "execute() - no matching function declaration found for functionCall: " + functionCall);
      return new FunctionResponse(
          functionCall.getId(),
          functionCall.getName(),
          ImmutableMap.of(
              "status",
              "ERROR",
              "message",
              "function is not supported: " + functionCall.getName()));
    }

    ImmutableMap<String, Object> arguments = extractArgs(functionDeclaration.get(), functionCall);

    Log.d(TAG, "execute() - arguments: " + arguments);

    Optional<Method> method =
        VehicleAgents.getActionMethods(this.getClass()).stream()
            .filter(m -> m.getName().equals(functionCall.getName()))
            .findFirst();
    if (method.isEmpty()) {
      Log.wtf(TAG, "execute() - no matching action method found for functionCall: " + functionCall);
      return new FunctionResponse(
          functionCall.getId(),
          functionCall.getName(),
          ImmutableMap.of(
              "status",
              "ERROR",
              "message",
              "no matching action method found for functionCall: " + functionCall));
    }
    ImmutableList<String> parameterNames = VehicleAgents.getParameterNames(method.get());

    Object[] orderedArguments = orderArguments(parameterNames, arguments).toArray();

    try {
      Object rawInvocationResult;
      Log.d(TAG, "execute() - method name: " + method.get().getName());

      if (orderedArguments.length == 0) {
        rawInvocationResult = method.get().invoke(this);
      } else {
        rawInvocationResult = method.get().invoke(this, orderedArguments);
      }

      // Single point for the unchecked cast and its suppression
      @SuppressWarnings("unchecked")
      ListenableFuture<ErrorOr<?>> futureResult =
          (ListenableFuture<ErrorOr<?>>) rawInvocationResult;

      ErrorOr<?> actionResult = futureResult.get();

      Log.d(TAG, "execute() - VAL actionResult: " + actionResult);
      if (actionResult.isError()) {
        Log.e(
            TAG,
            "execute() - failed to invoke method: "
                + method.get().getName()
                + " for functionCall: "
                + functionCall
                + " with error: "
                + actionResult.errorCode());
        return new FunctionResponse(
            functionCall.getId(),
            functionCall.getName(),
            ImmutableMap.of(
                "status",
                "ERROR",
                "message",
                "failed to execute functionCall: "
                    + functionCall
                    + " with error: "
                    + actionResult.errorCode()));
      } else {
        Log.d(
            TAG,
            "execute() - functionCall: "
                + functionCall
                + " executed successfully with result: "
                + actionResult.value());
        return new FunctionResponse(
            functionCall.getId(),
            functionCall.getName(),
            ImmutableMap.of("status", "SUCCESS", "message", actionResult.value().toString()));
      }
    } catch (ReflectiveOperationException | InterruptedException | ExecutionException e) {
      Log.w(
          TAG, "execute() - failed to execute functionCall: " + functionCall + " with error: " + e);
      return new FunctionResponse(
          functionCall.getId(),
          functionCall.getName(),
          ImmutableMap.of(
              "status",
              "ERROR",
              "message",
              "failed to execute functionCall: "
                  + functionCall
                  + " with error: "
                  + e.getMessage()));
    }
  }

  private static ImmutableList<Object> orderArguments(
      ImmutableList<String> parameterNames, ImmutableMap<String, Object> arguments) {
    ImmutableList.Builder<Object> orderedArgumentsBuilder = ImmutableList.builder();
    for (String parameterName : parameterNames) {
      orderedArgumentsBuilder.add(arguments.get(parameterName));
    }
    ImmutableList<Object> orderedArguments = orderedArgumentsBuilder.build();

    Log.d(TAG, "orderArguments() - ordered arguments: " + orderedArguments);
    return orderedArguments;
  }

  /**
   * Determines the temperature unit based on the given temperature value.
   *
   * <p>If the temperature is above the threshold, safe to assume it is in Fahrenheit. Otherwise,
   * the value is in Celsius.
   */
  private static TemperatureUnit determineTemperatureUnit(float temperature) {
    if (temperature > TEMPERATURE_UNIT_THRESHOLD) {
      Log.d(
          TAG,
          "getTemperatureUnit() - The given temperature "
              + temperature
              + " is above "
              + TEMPERATURE_UNIT_THRESHOLD
              + " degrees, and determined to be in Fahrheit.");
      return TemperatureUnit.FAHRENHEIT;
    }
    Log.d(
        TAG,
        "getTemperatureUnit() - The given temperature "
            + temperature
            + " is equal to or below "
            + TEMPERATURE_UNIT_THRESHOLD
            + " degrees, and determined to be in Celsius");
    return TemperatureUnit.CELSIUS;
  }

  private static ImmutableSet<String> convertToImmutableStringSet(
      Object rawArgumentValue, String parameterName) {
    // Special case 1: List of Strings must be converted to ImmutableSet<String>.
    // If the argument is a collection, filter for strings and build the set.
    ImmutableSet.Builder<String> setBuilder = ImmutableSet.builder();
    for (Object item : (Collection<?>) rawArgumentValue) {
      if (item instanceof String stringValue) {
        setBuilder.add(stringValue);
      } else {
        // Expects a list of strings only for Collection type.
        throw new IllegalArgumentException(
            String.format(
                "Parameter '%s' contained non-string element '%s'. Skipping.",
                parameterName, item));
      }
    }

    ImmutableSet<String> valueSet = setBuilder.build();
    Log.d(TAG, "convertToImmutableSet() - value set: " + valueSet);
    return valueSet;
  }

  private static ImmutableMap<String, Object> extractArgs(
      FunctionDeclaration functionDeclaration, FunctionCall functionCall) {

    Log.d(
        TAG,
        "Extracting arguments for functionDeclaration: "
            + functionDeclaration
            + " and functionCall: "
            + functionCall);

    ImmutableMap.Builder<String, Object> nameToArgumentBuilder = ImmutableMap.builder();

    boolean isTemperatureFunction =
        functionDeclaration.getParameters().getProperties().containsKey(TEMPERATURE)
            || functionDeclaration.getParameters().getProperties().containsKey(OFFSET);

    for (String parameterName : functionDeclaration.getParameters().getProperties().keySet()) {
      Object rawArgumentValue = functionCall.getArgs().get(parameterName);

      String type = (rawArgumentValue == null ? "null" : rawArgumentValue.getClass().getName());
      Log.d(
          TAG,
          "RawArgumentValue before conversion in VAL: parameterName: "
              + parameterName
              + ", value:"
              + rawArgumentValue
              + ", type: "
              + type);

      Object argumentValue = null;

      // Convert LLM argument types to VAL argument types.
      // GenAI function call only supports list not set, but VAL function requires argument type
      // of ImmutableSet<String>.
      if (rawArgumentValue == null) {
        // If the argument is null, treat it as an empty set of strings.
        throw new IllegalArgumentException(
            String.format("Argument for parameter '%s' is null. Skipping.", parameterName));
      } else if (rawArgumentValue instanceof Collection) {
        if (isTemperatureFunction) {
          continue; // Skip populating seats for temperature functions.
        }

        // Special case 1: List of Strings must be converted to ImmutableSet<String>.
        // If the argument is a collection, filter for strings and build the set.
        argumentValue = convertToImmutableStringSet(rawArgumentValue, parameterName);

      } else if (rawArgumentValue instanceof Double value) {
        if (parameterName.equals(TEMPERATURE)) {
          // Special case 2: Temperature and offset arguments must be converted to float.
          float floatValue = value.floatValue();

          // TODO(b/432546592): Use the temperature display units instead of inferring temperature
          // units.
          TemperatureUnit unit = determineTemperatureUnit(floatValue);

          ImmutableSet<String> temperatureSeats =
              convertToImmutableStringSet(functionCall.getArgs().get(SEATS), SEATS);

          Log.d(
              TAG,
              "Temperature unit: "
                  + unit
                  + " temperatureSeats: "
                  + temperatureSeats
                  + " floatValue: "
                  + floatValue);

          // Create temperature object using seats, temperature value and unit and do not populate
          // seats as a separate argument.
          argumentValue =
              new UpdateTargetTemperatureRequest(
                  temperatureSeats, new Temperature(floatValue, unit));
          Log.d(TAG, "ArgumentValue temperature converted: " + argumentValue);

          parameterName = "request";

        } else if (parameterName.equals(OFFSET)) {
          // Special case 2: Temperature and offset arguments must be converted to float.
          float floatValue = value.floatValue();

          TemperatureUnit unit = determineTemperatureUnit(floatValue);
          ImmutableSet<String> temperatureSeats =
              convertToImmutableStringSet(functionCall.getArgs().get(SEATS), SEATS);

          Log.d(
              TAG,
              "Temperature unit: "
                  + unit
                  + " temperatureSeats: "
                  + temperatureSeats
                  + " floatValue: "
                  + floatValue);

          // Create temperature object using seats, temperature value and unit and do not populate
          // seats as a separate argument.
          argumentValue =
              new OffsetTargetTemperatureRequest(
                  temperatureSeats, new Temperature(floatValue, unit));
          Log.d(TAG, "ArgumentValue is converted to offset: " + argumentValue);

          parameterName = "request";

        } else if (parameterName.equals("fanSpeed")
            || parameterName.equals("level")
            || parameterName.equals("position")) {
          // Special case 3: Fan speed, window/door position or seat/cooling level must be converted
          // to int.
          argumentValue = (int) Math.round(value.doubleValue());

          Log.d(TAG, "ArgumentValue is converted to int: " + argumentValue);
        }
      } else {
        throw new IllegalArgumentException(
            String.format(
                "Argument for parameter '%s' has unexpected value type: %s. Value: %s'. Expected"
                    + " String or Collection<String> or double.",
                parameterName, rawArgumentValue.getClass().getName(), rawArgumentValue));
      }

      if (argumentValue != null) {
        nameToArgumentBuilder.put(parameterName, argumentValue);

        Log.d(
            TAG,
            "Putting argument value in map: parameterName: "
                + parameterName
                + ", value: "
                + argumentValue);
      }
    }
    return nameToArgumentBuilder.buildOrThrow();
  }

  /**
   * Executes "Get" action for the set of {@code elements} with no value transformation.
   *
   * @see #executeGetAction(String, ImmutableSet<String>, BiFunction)
   */
  protected <T> ListenableFuture<ErrorOr<GetResult<T>>> executeGetAction(
      String action, ImmutableSet<String> elements) {
    return executeGetAction(action, elements, (String element, T value) -> value);
  }

  /**
   * Executes "Get" action for the set of {@code elements} with {@code valueTransformFunction}.
   *
   * @param valueTransformFunction a function with the element name and value as input, and the
   *     output is the transformed value.
   */
  protected <T, U> ListenableFuture<ErrorOr<GetResult<U>>> executeGetAction(
      String action,
      ImmutableSet<String> elements,
      BiFunction<String, T, U> valueTransformFunction) {
    return Futures.transform(
        this.<T>executeGetActionOnListenableFuture(action, elements),
        getActionResult -> {
          if (getActionResult.isError()) {
            return ErrorOr.createError(getActionResult.errorCode());
          }

          return ErrorOr.createValue(
              GetResult.create(
                  action,
                  generateTransformedElementToValueMap(
                      getActionResult.value().elementToValue(), valueTransformFunction)));
        },
        listeningExecutorService);
  }

  protected ListenableFuture<ErrorOr<GetResult<Temperature>>> executeGetTemperatureAction(
      String action, ImmutableSet<String> elements) {
    return Futures.transform(
        this.executeGetTemperatureActionOnListenableFuture(action, elements),
        getActionResult -> {
          if (getActionResult.isError()) {
            return ErrorOr.createError(getActionResult.errorCode());
          }
          return ErrorOr.createValue(
              GetResult.create(action, getActionResult.value().elementToValue()));
        },
        listeningExecutorService);
  }

  protected ListenableFuture<ErrorOr<GetResult<Temperature>>> executeGetTemperatureAction(
      String action, ImmutableSet<String> elements, TemperatureUnit unit) {
    return Futures.transform(
        this.executeGetTemperatureActionOnListenableFuture(action, elements, unit),
        getActionResult -> {
          if (getActionResult.isError()) {
            return ErrorOr.createError(getActionResult.errorCode());
          }
          return ErrorOr.createValue(
              GetResult.create(action, getActionResult.value().elementToValue()));
        },
        listeningExecutorService);
  }

  protected <T, U> ListenableFuture<ErrorOr<GlobalGetResult<U>>> executeGlobalGetAction(
      String action, BiFunction<String, T, U> valueTransformFunction) {
    return Futures.transform(
        this.executeGetAction(action, GLOBAL_ELEMENTS, valueTransformFunction),
        getResult -> {
          if (getResult.isError()) {
            return ErrorOr.createError(getResult.errorCode());
          }
          return ErrorOr.createValue(
              GlobalGetResult.<U>create(
                  action,
                  getResult.value().elementToValue().get(GLOBAL_ELEMENT).isError()
                      ? ErrorOr.createError(
                          getResult.value().elementToValue().get(GLOBAL_ELEMENT).errorCode())
                      : ErrorOr.createValue(
                          getResult.value().elementToValue().get(GLOBAL_ELEMENT).value())));
        },
        listeningExecutorService);
  }

  protected <T> ListenableFuture<ErrorOr<SetResult>> executeSetAction(
      String action, Set<String> elements, T valueToSet) {
    return Futures.transform(
        this.<T>executeSetActionOnListenableFuture(action, elements, valueToSet),
        setActionResult -> {
          if (setActionResult.isError()) {
            return ErrorOr.createError(setActionResult.errorCode());
          }
          return ErrorOr.createValue(
              SetResult.create(action, setActionResult.value().elementToErrorCode()));
        },
        listeningExecutorService);
  }

  protected ListenableFuture<ErrorOr<SetResult>> executeSetAction(
      String action, UpdateTargetTemperatureRequest request) {
    return Futures.transform(
        this.executeSetActionOnListenableFuture(action, request),
        setActionResult -> {
          if (setActionResult.isError()) {
            return ErrorOr.createError(setActionResult.errorCode());
          }
          return ErrorOr.createValue(
              SetResult.create(action, setActionResult.value().elementToErrorCode()));
        },
        listeningExecutorService);
  }

  protected <T> ListenableFuture<ErrorOr<GlobalSetResult>> executeGlobalSetAction(
      String action, T valueToSet) {
    return Futures.transform(
        this.<T>executeSetAction(action, GLOBAL_ELEMENTS, valueToSet),
        setResult -> {
          if (setResult.isError()) {
            return ErrorOr.createError(setResult.errorCode());
          }
          return ErrorOr.createValue(
              GlobalSetResult.create(
                  action,
                  setResult.value().seatToErrorCode().containsKey(GLOBAL_ELEMENT)
                      ? Optional.of(setResult.value().seatToErrorCode().get(GLOBAL_ELEMENT))
                      : Optional.empty()));
        },
        listeningExecutorService);
  }

  protected <T> ListenableFuture<ErrorOr<OffsetResult<T>>> executeApplyOffsetAction(
      String action, OffsetRequest<T> request) {
    return executeApplyOffsetAction(action, request, (String element, T value) -> value);
  }

  protected <T, U> ListenableFuture<ErrorOr<OffsetResult<U>>> executeApplyOffsetAction(
      String action, OffsetRequest<T> request, BiFunction<String, T, U> valueTransformFunction) {
    return Futures.transform(
        executeApplyOffsetActionOnListenableFuture(action, request),
        offsetActionResult -> {
          if (offsetActionResult.isError()) {
            return ErrorOr.createError(offsetActionResult.errorCode());
          }

          return ErrorOr.createValue(
              OffsetResult.create(
                  action,
                  generateTransformedElementToValueMap(
                      offsetActionResult.value().elementToNewValue(), valueTransformFunction)));
        },
        listeningExecutorService);
  }

  protected static int zeroOrGreaterTransform(String element, int value) {
    return max(value, 0);
  }

  protected static int zeroOrLessTransform(String element, int value) {
    return value >= 0 ? 0 : switchSign(value);
  }

  protected static int switchSign(int value) {
    return -1 * value;
  }

  protected static float switchSign(float value) {
    return -1f * value;
  }

  private static <T, U> ImmutableMap<String, ErrorOr<U>> generateTransformedElementToValueMap(
      ImmutableMap<String, ErrorOr<T>> elementToValueMap,
      BiFunction<String, T, U> valueTransformFunction) {
    ImmutableMap.Builder<String, ErrorOr<U>> transformedElementToValue = ImmutableMap.builder();
    for (Map.Entry<String, ErrorOr<T>> entry : elementToValueMap.entrySet()) {
      if (entry.getValue().isError()) {
        transformedElementToValue.put(
            entry.getKey(), ErrorOr.createError(entry.getValue().errorCode()));
      } else {
        transformedElementToValue.put(
            entry.getKey(),
            ErrorOr.createValue(
                valueTransformFunction.apply(entry.getKey(), entry.getValue().value())));
      }
    }
    return transformedElementToValue.buildOrThrow();
  }

  /**
   * Returns a set of overlapping min and max values for the given {@code actionName} if the action
   * is supported.
   *
   * <p>This set is currently static, so a client should only call this once. However, this may
   * change in the future.
   */
  @Nullable
  private <N extends Number & Comparable<N>> ValueRange<Float> getSupportedOverlappingMinMax(
      String actionName) {
    // Populate the action declaration if the action is supported.

    if (actionMap.isEmpty()) {
      Log.e(
          TAG,
          String.format(
              "getSupportedOverlappingMinMax() - Action [%s] cannot be found since the action map"
                  + " is empty.",
              actionName));
      return null;
    }
    if (!actionMap.containsKey(actionName)) {
      Log.w(
          TAG,
          String.format(
              "getSupportedOverlappingMinMax() - Action [%s] is not contained in the action map.",
              actionName));
      return null;
    }

    Action<?> action = actionMap.get(actionName);

    @SuppressWarnings("unchecked")
    ErrorOr<ImmutableMap<String, ValueRange<N>>> elementToValueRange =
        getElementToValueRangeMap((Class<N>) action.getPropertyTypeClazz(), actionName);

    if (elementToValueRange.isError()) {
      Log.w(
          TAG,
          String.format(
              "getSupportedOverlappingMinMax() - getElementToValueRangeMap() returned an"
                  + " errorCode: %s for action: %s. Skipping this action.",
              elementToValueRange.errorCode(), actionName));
      return null;
    }

    return VehicleAgents.findOverlappingRange(elementToValueRange.value().values());
  }
}

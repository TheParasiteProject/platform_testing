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

package com.google.android.libraries.automotive.`val`.utils

/** Utility class for vehicle action related functions. */
object VehicleActionStringUtils {

  /**
   * Count the number of words in the text.
   *
   * @param text The text to count the words of.
   * @return The number of words in the text.
   */
  fun totalWords(text: String): Int {
    // Remove whitespace, split by whitespace, filter out empty strings, and count the number of
    // words.
    return text.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
  }

  // TODO(b/409140570): Deprecate this after adding ApiActionCategory's method descriptions as
  // annotations.
  /**
   * Converts a Java method name to a human readable description. eg. "setHvacFanSpeed" -> "Set HVAC
   * fan speed."
   *
   * @param actionName The action name to convert.
   * @return The converted action name.
   */
  @JvmStatic
  fun convertMethodNameToSentence(camelCaseString: String): String {
    if (camelCaseString.isEmpty()) {
      return ""
    }

    val regex = "(?<=.)(?=[A-Z])".toRegex()
    val words = camelCaseString.replace(regex, " ").split(" ")
    val result =
      words.joinToString(" ") { word ->
        val lowerCaseWord = word.lowercase()
        if (lowerCaseWord == "hvac" || lowerCaseWord == "ac") {
          word.uppercase()
        } else if (word.length > 1 && word.all { char -> char.isUpperCase() }) {
          word
        } else if (words.indexOf(word) == 0) {
          lowerCaseWord.replaceFirstChar { char -> char.uppercase() }
        } else {
          lowerCaseWord
        }
      }
    return if (camelCaseString.startsWith("is")) "$result?" else "$result."
  }

  /**
   * Converts a Java method name to a snake_case action name.
   *
   * eg. "setHvacFanSpeed", "SeatActions"-> "SEAT_ACTION_SET_HVAC_FAN_SPEED". (with suffix 's')
   *
   * @param camelCaseString The camelCase string to convert.
   * @return The snake_case string.
   */
  @JvmStatic
  fun convertMethodNameToActionName(methodName: String, className: String): String {
    if (methodName.isEmpty() || className.isEmpty()) {
      return ""
    }

    var classNameWithoutSuffixS = className

    // Remove the suffix 's' from the class name if it exists.
    // eg. Convert "SeatActions" to "SeatAction".
    if (className.isNotEmpty() && className.endsWith("s")) {
      classNameWithoutSuffixS = className.substring(0, className.length - 1)
    }

    val methodNameSnakeCase = convertToSnakeCase(methodName)
    val classNameSnakeCase = convertToSnakeCase(classNameWithoutSuffixS)

    return classNameSnakeCase + "_" + methodNameSnakeCase
  }

  /**
   * Converts a camelCase string to a UPPER_SNAKE_CASE string.
   *
   * eg. "SteeringWheelActions" -> "STEERING_WHEEL_ACTIONS". eg. "setHvacFanSpeed" ->
   * "SET_HVAC_FAN_SPEED".
   */
  private fun convertToSnakeCase(camelCase: String): String {
    val pattern = Regex("(?<=[a-z])(?=[A-Z])")
    val words = pattern.split(camelCase)
    return words.joinToString("_").uppercase()
  }

  /**
   * Splits a function expression into a pair of class name and function name.
   *
   * eg. "SeatActions.setSeatTemperature()" -> Pair("SeatActions", "setSeatTemperature()")
   *
   * @param functionExpression The function expression to split.
   * @return The pair of class name and function name.
   */
  fun splitIntoClassAndFunctionName(functionExpression: String): Pair<String, String> {
    val classNameAndFunctionName = functionExpression.split('.')

    if (classNameAndFunctionName.size < 2) {
      throw IllegalArgumentException(
        "Failed to split function expression into class name and function name. Expected format is 'ClassName.functionName()'. Found: $functionExpression"
      )
    }
    return Pair(classNameAndFunctionName[0], classNameAndFunctionName[1])
  }
}

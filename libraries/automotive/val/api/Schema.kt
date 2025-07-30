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

package com.google.android.libraries.automotive.`val`.api

/**
 * The `Schema` object allows the definition of input and output data types. These types can be
 * objects, but also primitives and arrays. Represents a select subset of an
 * [OpenAPI 3.0 schema object](https://spec.openapis.org/oas/v3.0.3#schema).
 */
data class Schema(
  val type: Type,
  val format: String? = null,
  val title: String? = null,
  val description: String? = null,
  val nullable: Boolean? = null,
  val enum: List<String>? = null,
  val items: Schema? = null,
  val maxItems: Long? = null,
  val minItems: Long? = null,
  val properties: Map<String, Schema>? = null,
  val required: List<String>? = null,
  val minimum: Double? = null,
  val maximum: Double? = null,
  val anyOf: List<Schema>? = null,
  val propertyOrdering: List<String>? = null,
) {

  /**
   * Constructor for `Schema` object with fewer parameters.
   *
   * @param type The type of the schema.
   * @param description The description of the schema.
   * @param format The format of the schema.
   * @param enum The enum of the schema.
   * @param minimum The minimum value of the schema.
   * @param maximum The maximum value of the schema.
   * @param properties The properties of the schema.
   * @param required The required properties of the schema.
   */
  constructor(
    type: Type,
    description: String? = null,
    format: String? = null,
    enum: List<String>? = null,
    properties: Map<String, Schema>? = null,
    required: List<String>? = null,
    minimum: Double? = null,
    maximum: Double? = null,
  ) : this(
    type = type,
    format = format,
    title = null,
    description = description,
    nullable = null,
    enum = enum,
    items = null,
    maxItems = null,
    minItems = null,
    properties = properties,
    required = required,
    minimum = minimum,
    maximum = maximum,
    anyOf = null,
    propertyOrdering = null,
  )

  enum class Type {
    TYPE_UNSPECIFIED,
    STRING,
    NUMBER,
    INTEGER,
    BOOLEAN,
    ARRAY,
    OBJECT,
    NULL,
  }
}

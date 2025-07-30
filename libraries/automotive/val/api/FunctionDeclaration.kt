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
 * Structured representation of a function declaration as defined by the
 * [OpenAPI 3.03 specification](https://spec.openapis.org/oas/v3.0.3). Included in this declaration
 * are the function name and parameters. This FunctionDeclaration is a representation of a block of
 * code that can be used as a `Tool` by the model and executed by the client.
 */
data class FunctionDeclaration(
  val name: String,
  val description: String,
  val parameters: Schema? = null,
  val response: Schema? = null,
)

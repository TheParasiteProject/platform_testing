/*
 * Copyright (C) 2024 The Android Open Source Project
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

package platform.test.motion.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isFinite
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import platform.test.motion.golden.DataPointType
import platform.test.motion.golden.UnknownTypeException
import platform.test.motion.isApproximatelyEqualTo

fun Dp.asDataPoint() = DataPointTypes.dp.makeDataPoint(this)

fun IntSize.asDataPoint() = DataPointTypes.intSize.makeDataPoint(this)

fun Offset.asDataPoint() = DataPointTypes.offset.makeDataPoint(this)

fun DpSize.asDataPoint() = DataPointTypes.dpSize.makeDataPoint(this)

fun DpOffset.asDataPoint() = DataPointTypes.dpOffset.makeDataPoint(this)

object DataPointTypes {

    val dp: DataPointType<Dp> =
        DataPointType(
            "dp",
            jsonToValue = {
                when (it) {
                    is Float -> it.dp
                    is Number -> it.toFloat().dp
                    is String -> it.toFloatOrNull()?.dp ?: throw UnknownTypeException()
                    else -> throw UnknownTypeException()
                }
            },
            valueToJson = { it.value },
            isApproximateEqual = { actual, expected ->
                actual.value.isApproximatelyEqualTo(expected.value)
            }
        )

    val intSize: DataPointType<IntSize> =
        DataPointType(
            "intSize",
            jsonToValue = {
                with(it as? JSONObject ?: throw UnknownTypeException()) {
                    IntSize(getInt("width"), getInt("height"))
                }
            },
            valueToJson = {
                JSONObject().apply {
                    put("width", it.width)
                    put("height", it.height)
                }
            },
        )

    val intOffset: DataPointType<IntOffset> =
        DataPointType(
            "intOffset",
            jsonToValue = {
                with(it as? JSONObject ?: throw UnknownTypeException()) {
                    IntOffset(getInt("x"), getInt("y"))
                }
            },
            valueToJson = {
                JSONObject().apply {
                    put("x", it.x)
                    put("y", it.y)
                }
            }
        )

    val dpSize: DataPointType<DpSize> =
        DataPointType(
            "dpSize",
            jsonToValue = {
                with(it as? JSONObject ?: throw UnknownTypeException()) {
                    DpSize(getDouble("width").dp, getDouble("height").dp)
                }
            },
            valueToJson = {
                JSONObject().apply {
                    put("width", it.width.value)
                    put("height", it.height.value)
                }
            },
             isApproximateEqual = { actual, expected ->
                 actual.width.value.isApproximatelyEqualTo(expected.width.value) &&
                 actual.height.value.isApproximatelyEqualTo(expected.height.value)
            }
        )

    val dpOffset: DataPointType<DpOffset> =
        DataPointType(
            "dpOffset",
            jsonToValue = {
                with(it as? JSONObject ?: throw UnknownTypeException()) {
                    DpOffset(getDouble("x").dp, getDouble("y").dp)
                }
            },
            valueToJson = {
                JSONObject().apply {
                    put("x", it.x.value)
                    put("y", it.y.value)
                }
            },
            isApproximateEqual = { actual, expected ->
                actual.x.value.isApproximatelyEqualTo(expected.x.value) &&
                        actual.y.value.isApproximatelyEqualTo(expected.y.value)
            }
        )

    val offset: DataPointType<Offset> =
        DataPointType(
            "offset",
            jsonToValue = {
                when (it) {
                    "unspecified" -> Offset.Unspecified
                    "infinite" -> Offset.Infinite
                    is JSONObject ->
                        Offset(it.getDouble("x").toFloat(), it.getDouble("y").toFloat())
                    else -> throw UnknownTypeException()
                }
            },
            valueToJson = {
                when {
                    it.isUnspecified -> "unspecified"
                    !it.isFinite -> "infinite"
                    else ->
                        JSONObject().apply {
                            put("x", it.x)
                            put("y", it.y)
                        }
                }
            },
            isApproximateEqual = { actual, expected ->
                when(expected){
                    Offset.Unspecified -> actual == Offset.Unspecified
                    Offset.Infinite -> actual == Offset.Infinite
                    Offset.Zero -> actual.x.isApproximatelyEqualTo(0f) && actual.y.isApproximatelyEqualTo(0f)
                    else -> actual.x.isApproximatelyEqualTo(expected.x)
                            && actual.y.isApproximatelyEqualTo(expected.y)
                }
            }
        )
}

/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.server.wm.flicker.assertiongenerator.common

import com.android.server.wm.flicker.assertiongenerator.layers.LayersTraceConfiguration
import com.android.server.wm.flicker.assertiongenerator.layers.LayersTraceContent
import com.android.server.wm.flicker.assertiongenerator.layers.LayersTraceLifecycle
import com.android.server.wm.flicker.assertiongenerator.windowmanager.WmTraceConfiguration
import com.android.server.wm.flicker.assertiongenerator.windowmanager.WmTraceContent
import com.android.server.wm.flicker.assertiongenerator.windowmanager.WmTraceLifecycle

/**
 * Contains the trace information needed to produce an assertion.
 */
open class TraceContent(
    open val traceLifecycle: ITraceLifecycle,
    open val traceConfiguration: ITraceConfiguration?
) {

    companion object {
        fun byTraceType(
            traceLifecycle: ITraceLifecycle,
            traceConfiguration: ITraceConfiguration?
        ): TraceContent? {
            if (traceLifecycle is LayersTraceLifecycle &&
                (traceConfiguration is LayersTraceConfiguration?)
            ) {
                return LayersTraceContent(traceLifecycle, traceConfiguration)
            }
            if (traceLifecycle is WmTraceLifecycle &&
                traceConfiguration is WmTraceConfiguration?
            ) {
                return WmTraceContent(traceLifecycle, traceConfiguration)
            }
            return null
        }
    }
}
/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.tools.flicker.subject.events

import android.tools.Timestamps
import android.tools.flicker.assertions.Fact
import android.tools.flicker.subject.FlickerSubject
import android.tools.flicker.subject.exceptions.ExceptionMessageBuilder
import android.tools.flicker.subject.exceptions.IncorrectFocusException
import android.tools.io.Reader
import android.tools.traces.events.EventLog
import android.tools.traces.events.FocusEvent

/** Truth subject for [FocusEvent] objects. */
class EventLogSubject(val eventLog: EventLog, override val reader: Reader) : FlickerSubject() {
    override val timestamp = eventLog.entries.firstOrNull()?.timestamp ?: Timestamps.empty()

    private val _focusChanges by lazy {
        val focusList = mutableListOf<FocusEvent>()
        eventLog.focusEvents.firstOrNull { !it.hasFocus() }?.let { focusList.add(it) }
        focusList + eventLog.focusEvents.filter { it.hasFocus() }
    }

    private val exceptionMessageBuilder
        get() = ExceptionMessageBuilder().forSubject(this)

    fun focusChanges(vararg windows: String) = apply {
        val builder =
            exceptionMessageBuilder
                .setExpected(windows.joinToString(" -> "))
                .setActual(_focusChanges.map { Fact("Focus change", it) })

        if (windows.isEmpty()) {
            val errorMsgBuilder =
                builder.setMessage("No windows specified for focus change assertion")
            throw IncorrectFocusException(errorMsgBuilder)
        }

        if (_focusChanges.isEmpty()) {
            val errorMsgBuilder = builder.setMessage("Focus did not change")
            throw IncorrectFocusException(errorMsgBuilder)
        }

        if (windows.size > _focusChanges.size) {
            val errorMsgBuilder = builder.setMessage("More windows specified than focus changes")
            throw IncorrectFocusException(errorMsgBuilder)
        }

        for ((index, focusChange) in _focusChanges.withIndex()) {
            if (index + windows.size > _focusChanges.size) {
                break
            }

            if (!focusChange.window.contains(windows.first())) {
                continue
            }

            val subsequence = _focusChanges.subList(index, index + windows.size)
            if (subsequence.zip(windows).all { (focus, window) -> focus.window.contains(window) }) {
                return@apply
            }
        }

        val errorMsgBuilder = builder.setMessage("Incorrect focus change")
        throw IncorrectFocusException(errorMsgBuilder)
    }

    fun focusDoesNotChange() = apply {
        if (_focusChanges.isNotEmpty()) {
            val actual = _focusChanges.map { Fact("Focus change", it) }
            val errorMsgBuilder =
                exceptionMessageBuilder
                    .setMessage("Focus changes")
                    .setExpected("")
                    .setActual(actual)
            throw IncorrectFocusException(errorMsgBuilder)
        }
    }
}

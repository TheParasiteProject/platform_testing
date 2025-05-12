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

package android.tools.traces.parsers.perfetto

import android.tools.Timestamp
import android.tools.Timestamps
import android.tools.parsers.AbstractTraceParser
import android.tools.traces.wm.ShellTransitionData
import android.tools.traces.wm.Transition
import android.tools.traces.wm.TransitionChange
import android.tools.traces.wm.TransitionType
import android.tools.traces.wm.TransitionsTrace
import android.tools.traces.wm.WmTransitionData

open class TransitionsTraceParser :
    AbstractTraceParser<TraceProcessorSession, Transition, Transition, TransitionsTrace>() {

    override val traceName = "Transitions Trace"

    override fun createTrace(entries: Collection<Transition>): TransitionsTrace {
        return TransitionsTrace(entries)
    }

    override fun doDecodeByteArray(bytes: ByteArray): TraceProcessorSession {
        error("This parser can only read from perfetto trace processor")
    }

    override fun shouldParseEntry(entry: Transition) = true

    override fun getEntries(input: TraceProcessorSession): List<Transition> {
        val transitions = ArrayList<Transition>()

        val handlerMapping = mutableMapOf<Int, String>()
        input.query(getSqlQueryHandlerMappings()) {
            it.forEach { mapping ->
                handlerMapping[(mapping["handler_id"] as Long).toInt()] =
                    mapping["handler_name"] as String
            }
        }

        input.query(getSqlQueryTransitions()) { transitionsRows ->
            val transitionRowsGrouped =
                transitionsRows.groupBy {
                    it["transition_entry_id"]
                        ?: error("transition_entry_id column should not be null")
                }

            transitionRowsGrouped.values.forEach { transitionRows ->
                transitions.add(buildTransition(input, transitionRows, handlerMapping))
            }
        }

        return transitions
    }

    override fun getTimestamp(entry: Transition): Timestamp = entry.timestamp

    override fun onBeforeParse(input: TraceProcessorSession) {}

    override fun doParse(
        input: TraceProcessorSession,
        from: Timestamp,
        to: Timestamp,
        addInitialEntry: Boolean,
    ): TransitionsTrace {
        val sliceEntries =
            getEntries(input).filter {
                (it.wmData.sendTime ?: it.shellData.dispatchTime ?: Timestamps.min()) >= from &&
                    (it.wmData.finishTime
                        ?: it.wmData.abortTime
                        ?: it.shellData.abortTime
                        ?: it.shellData.mergeTime
                        ?: Timestamps.max()) <= to
            }

        return createTrace(sliceEntries)
    }

    override fun doParseEntry(entry: Transition) = entry

    companion object {
        private fun getSqlQueryHandlerMappings() =
            "SELECT handler_id, handler_name FROM window_manager_shell_transition_handlers;"

        private fun getSqlQueryTransitions() =
            """
               SELECT transitions.id AS transition_entry_id, args.key, args.display_value AS value, args.value_type
               FROM window_manager_shell_transitions AS transitions
               INNER JOIN args ON transitions.arg_set_id = args.arg_set_id;
            """
                .trimIndent()

        private fun buildTransition(
            input: TraceProcessorSession,
            transitionRows: List<Row>,
            handlerMapping: Map<Int, String>,
        ): Transition {
            val args = Args.build(transitionRows)
            return Transition(
                id = args.getChild("id")?.getInt() ?: error("Missing transition id"),
                wmData =
                    WmTransitionData(
                        createTime = args.getChild("create_time_ns")?.getLong()?.toTimestamp(input),
                        sendTime = args.getChild("send_time_ns")?.getLong()?.toTimestamp(input),
                        abortTime =
                            args.getChild("wm_abort_time_ns")?.getLong()?.toTimestamp(input),
                        finishTime = args.getChild("finish_time_ns")?.getLong()?.toTimestamp(input),
                        startingWindowRemoveTime =
                            args
                                .getChild("starting_window_remove_time_ns")
                                ?.getLong()
                                ?.toTimestamp(input),
                        startTransactionId = args.getChild("start_transaction_id")?.getLong(),
                        finishTransactionId = args.getChild("finish_transaction_id")?.getLong(),
                        type = args.getChild("type")?.getInt()?.toTransitionType(),
                        changes =
                            args
                                .getChildren("targets")
                                ?.map {
                                    TransitionChange(
                                        it.getChild("mode")?.getInt()?.toTransitionType()
                                            ?: error("Missing mode (${it.getChild("mode")})"),
                                        it.getChild("layer_id")?.getInt()
                                            ?: error("Missing layer id ${it.getChild("layer_id")}"),
                                        it.getChild("window_id")?.getInt()
                                            ?: error(
                                                "Missing window id ${it.getChild("window_id")}"
                                            ),
                                    )
                                }
                                ?.ifEmpty { null },
                    ),
                shellData =
                    ShellTransitionData(
                        dispatchTime =
                            args.getChild("dispatch_time_ns")?.getLong()?.toTimestamp(input),
                        mergeRequestTime =
                            args.getChild("merge_request_time_ns")?.getLong()?.toTimestamp(input),
                        mergeTime = args.getChild("merge_time_ns")?.getLong()?.toTimestamp(input),
                        abortTime =
                            args.getChild("shell_abort_time_ns")?.getLong()?.toTimestamp(input),
                        handler = args.getChild("handler")?.getInt()?.let { handlerMapping[it] },
                        mergeTarget = args.getChild("merge_target")?.getInt(),
                    ),
            )
        }

        private fun Long.toTimestamp(input: TraceProcessorSession): Timestamp? {
            if (this == 0L) {
                return null
            }

            val ts = this
            return input.query(
                "SELECT TO_REALTIME($ts) as real_ts, TO_MONOTONIC($ts) as monotonic_ts"
            ) {
                require(it.size == 1)
                Timestamps.from(
                    unixNanos = it[0]["real_ts"] as Long,
                    systemUptimeNanos = it[0]["monotonic_ts"] as Long,
                    elapsedNanos = this,
                )
            }
        }

        private fun Int.toTransitionType() = TransitionType.fromInt(this)
    }
}

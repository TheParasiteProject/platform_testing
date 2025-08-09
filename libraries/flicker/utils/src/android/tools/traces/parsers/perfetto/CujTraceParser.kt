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

package android.tools.traces.parsers.perfetto

import android.tools.Timestamp
import android.tools.Timestamps
import android.tools.parsers.AbstractTraceParser
import android.tools.traces.events.Cuj
import android.tools.traces.events.CujTrace
import android.tools.traces.events.CujType

class CujTraceParser : AbstractTraceParser<TraceProcessorSession, Cuj, Cuj, CujTrace>() {

    override val traceName: String = "Cuj Trace"

    override fun doDecodeByteArray(bytes: ByteArray): TraceProcessorSession {
        error("This parser can only read from perfetto trace processor")
    }

    override fun shouldParseEntry(entry: Cuj) = true

    override fun getEntries(input: TraceProcessorSession): Collection<Cuj> {
        val cujs: List<Cuj> =
            mutableListOf<Cuj>().apply {
                input.query(getSqlQueryCuj()) { rows ->
                    this.addAll(
                        rows.map {
                            Cuj(
                                cuj = CujType.from(it["base_cuj_name"] as String),
                                startTimestamp = Timestamps.from(elapsedNanos = it["ts"] as Long),
                                endTimestamp = Timestamps.from(elapsedNanos = it["ts_end"] as Long),
                                canceled = it["is_canceled"] as Long == 1L,
                                tag = it["cuj_tag"] as String?,
                            )
                        }
                    )
                }
            }

        return cujs
    }

    override fun getTimestamp(entry: Cuj): Timestamp {
        return entry.startTimestamp
    }

    override fun doParseEntry(entry: Cuj): Cuj {
        return entry
    }

    override fun createTrace(entries: Collection<Cuj>): CujTrace {
        return CujTrace(entries)
    }

    companion object {
        private fun getSqlQueryCuj() =
            """
            SELECT RUN_METRIC('android/android_jank_cuj.sql');

            SELECT
              cuj.cuj_id,
              cuj.upid,
              cuj.cuj_name AS full_cuj_name,
              -- Extract the base CUJ name (before the colon)
              CASE
                WHEN cuj.cuj_name LIKE '%:%' THEN SUBSTR(cuj.cuj_name, 1, INSTR(cuj.cuj_name, ':') - 1)
                ELSE cuj.cuj_name
              END AS base_cuj_name,
              ts,
              ts_end,
              -- Extract the tag (after the colon), if present
              CASE
                WHEN cuj.cuj_name LIKE '%:%' THEN SUBSTR(cuj.cuj_name, INSTR(cuj.cuj_name, ':') + 1)
                ELSE NULL
              END AS cuj_tag,
              -- Check if the CUJ was canceled
              EXISTS (
                SELECT 1
                FROM slice AS cuj_state_marker
                JOIN track marker_track ON marker_track.id = cuj_state_marker.track_id
                WHERE
                  -- Marker must be within the CUJ duration
                  cuj_state_marker.ts >= cuj.ts AND cuj_state_marker.ts + cuj_state_marker.dur <= cuj.ts + cuj.dur
                  AND process.upid = cuj.upid -- Make sure marker is in the same process
                  AND (
                    -- Legacy format: e.g., J<SHADE_EXPAND_COLLAPSE>#FT#cancel#0
                    cuj_state_marker.name GLOB (cuj.cuj_slice_name || '#FT#cancel*')
                    OR
                    -- New format: Track name is CUJ name, slice name is FT#cancel*
                    (marker_track.name = cuj.cuj_slice_name AND cuj_state_marker.name GLOB 'FT#cancel*')
                  )
              ) AS is_canceled
            FROM android_jank_cuj cuj
            LEFT JOIN process ON cuj.upid = process.upid;
        """
                .trimIndent()
    }
}

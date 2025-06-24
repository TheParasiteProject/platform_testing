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

package android.tools.flicker.datastore

import android.tools.flicker.ScenarioInstance
import android.tools.flicker.assertions.ScenarioAssertion
import android.tools.traces.io.IResultData
import androidx.annotation.VisibleForTesting

/** In memory data store for flicker transitions, assertions and results */
object DataStore {
    private var cachedResults = mutableMapOf<String, IResultData>()
    private var cachedFlickerServiceAssertions =
        mutableMapOf<String, Map<ScenarioInstance, Collection<ScenarioAssertion>>>()

    data class Backup(
        val cachedResults: MutableMap<String, IResultData>,
        val cachedFlickerServiceAssertions:
            MutableMap<String, Map<ScenarioInstance, Collection<ScenarioAssertion>>>,
    )

    @VisibleForTesting
    fun clear() {
        cachedResults = mutableMapOf()
        cachedFlickerServiceAssertions = mutableMapOf()
    }

    fun backup(): Backup {
        return Backup(cachedResults.toMutableMap(), cachedFlickerServiceAssertions.toMutableMap())
    }

    fun restore(backup: Backup) {
        cachedResults = backup.cachedResults
        cachedFlickerServiceAssertions = backup.cachedFlickerServiceAssertions
    }

    /** @return if the store has results for [key] */
    fun containsResult(key: String): Boolean = cachedResults.containsKey(key)

    /**
     * Adds [result] to the store with [key] as id
     *
     * @throws IllegalStateException is [key] already exists in the data store
     */
    fun addResult(key: String, result: IResultData) {
        require(!containsResult(key)) { "Result for $key already in data store" }
        cachedResults[key] = result
    }

    /**
     * Replaces the old value [key] result in the store by [newResult]
     *
     * @throws IllegalStateException is [key] doesn't exist in the data store
     */
    fun replaceResult(key: String, newResult: IResultData) {
        if (!containsResult(key)) {
            error("Result for $key not in data store")
        }
        cachedResults[key] = newResult
    }

    /**
     * @return the result for [key]
     * @throws IllegalStateException is [key] doesn't exist in the data store
     */
    fun getResult(key: String): IResultData = cachedResults[key] ?: error("No value for $key")

    /** @return if the store has results for [key] */
    fun containsFlickerServiceResult(key: String): Boolean =
        cachedFlickerServiceAssertions.containsKey(key)

    fun addFlickerServiceAssertions(
        key: String,
        groupedAssertions: Map<ScenarioInstance, Collection<ScenarioAssertion>>,
    ) {
        if (containsFlickerServiceResult(key)) {
            error("Result for $key already in data store")
        }
        cachedFlickerServiceAssertions[key] = groupedAssertions
    }

    fun getFlickerServiceAssertions(
        key: String
    ): Map<ScenarioInstance, Collection<ScenarioAssertion>> {
        return cachedFlickerServiceAssertions[key] ?: error("No flicker service results for $key")
    }
}

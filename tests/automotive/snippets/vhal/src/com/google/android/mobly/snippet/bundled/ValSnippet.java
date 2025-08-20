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

package com.google.android.mobly.snippet.bundled;

import static com.google.common.util.concurrent.MoreExecutors.newDirectExecutorService;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.libraries.automotive.val.api.SeatActions;
import com.google.android.libraries.automotive.val.api.Temperature;
import com.google.android.libraries.automotive.val.api.UpdateTargetTemperatureRequest;
import com.google.android.libraries.automotive.val.api.VehicleActions;
import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.rpc.Rpc;
import com.google.common.collect.ImmutableSet;

public class ValSnippet implements Snippet {
    private final VehicleActions mActions;

    public ValSnippet() {
        mActions = new VehicleActions(
                InstrumentationRegistry.getInstrumentation().getContext(),
                newDirectExecutorService()
        );
    }

    @Rpc(description = "Sets the cabin temperature for the driver side")
    public void setDriverHvacTemperature(String fahrenheit) {
        mActions.getSeatActions().setHvacTargetTemperature(new UpdateTargetTemperatureRequest(
                ImmutableSet.of(SeatActions.SEAT_ROW_1_LEFT),
                new Temperature(
                        Float.parseFloat(fahrenheit),
                        Temperature.TemperatureUnit.FAHRENHEIT
                ),
                true
        ));
    }
}

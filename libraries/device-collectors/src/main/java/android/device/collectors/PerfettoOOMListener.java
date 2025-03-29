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
package android.device.collectors;

import android.device.collectors.annotations.OptionClass;

/**
 * A {@link PerfettoOOMListener} that captures the perfetto trace if there is an OOM during the
 * test.
 */
@OptionClass(alias = "perfetto-oom-collector")
public class PerfettoOOMListener extends PerfettoListener {

    public PerfettoOOMListener() {
        super();
    }
}

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
package android.platform.test.rule;

import androidx.annotation.VisibleForTesting;

import org.junit.runner.Description;

/**
 * This rule will stop new rendering before/after running each test method.
 *
 * If an application is already open, then it will continue to be rendered. It's
 * only for new applications that rendering will stop. This shouldn't restrict
 * the use of UI automation.
 *
 * The current use of this rule is to test some performance user journeys on
 * Cuttlefish, which has performance overhead under certain configurations.
 */
public class NoAppRenderingRule extends TestWatcher {

    @VisibleForTesting
    static final String STOP_NEW_RENDERING = "stop-new-rendering";

    @Override
    protected void starting(Description description) {
        if (isEnabled()) {
            executeShellCommand("setprop debug.hwui.drawing_enabled 0");
        }
    }

    @Override
    protected void finished(Description description) {
        if (isEnabled()) {
            executeShellCommand("setprop debug.hwui.drawing_enabled 1");
        }
    }

    private boolean isEnabled() {
        return Boolean.parseBoolean(getArguments().getString(STOP_NEW_RENDERING, "false"));
    }
}

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

package com.google.android.mobly.snippet.bundled;

import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoNotificationHelper;

import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.rpc.Rpc;

/** Snippet for notification center. */
public class NotificationSnippet implements Snippet {

    private final HelperAccessor<IAutoNotificationHelper> mNotificationHelper;

    public NotificationSnippet() {
        mNotificationHelper = new HelperAccessor<>(IAutoNotificationHelper.class);
    }

    @Rpc(description = "Check whether notification is displayed in the car's notification center.")
    public boolean isNotificationWithTitleExists(String title) {
        return mNotificationHelper.get().isNotificationWithTitleExists(title);
    }
}

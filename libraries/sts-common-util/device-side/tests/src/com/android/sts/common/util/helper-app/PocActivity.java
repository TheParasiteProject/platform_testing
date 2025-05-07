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

package com.android.sts.common.util.tests.helperapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;

public class PocActivity extends Activity {

    @Override
    public void onResume() {
        try {
            super.onResume();

            // Create HandlerThread and post the Runnable task to send broadcast after ~2 seconds.
            HandlerThread handlerThread = new HandlerThread(getPackageName());
            handlerThread.start();
            Handler handler = new Handler(handlerThread.getLooper());
            handler.postDelayed(
                    () -> sendBroadcast(new Intent(getPackageName())), 2_000L /* delay */);
        } catch (Exception e) {
            // Ignore unexpected exceptions.
        }
    }
}

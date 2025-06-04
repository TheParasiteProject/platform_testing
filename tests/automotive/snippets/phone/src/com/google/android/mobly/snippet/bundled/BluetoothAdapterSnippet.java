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

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;

import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.bundled.utils.Utils;
import com.google.android.mobly.snippet.rpc.Rpc;

import java.util.regex.Pattern;

public class BluetoothAdapterSnippet implements Snippet {

    private static class BluetoothAdapterSnippetException extends Exception {

        BluetoothAdapterSnippetException(String msg) {
            super(msg);
        }

        BluetoothAdapterSnippetException(String msg, Throwable err) {
            super(msg, err);
        }
    }

    // Timeout to measure consistent BT state.
    private static final int BT_MATCHING_STATE_INTERVAL_SEC = 5;
    // Default timeout in seconds.
    private static final int TIMEOUT_TOGGLE_STATE_SEC = 30;
    // Default timeout in seconds for UI update.
    private static final int TIMEOUT_UI_UPDATE_SEC = 4;
    private final Context mContext;
    private static final BluetoothAdapter sBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static final Pattern TEXT_PATTERN_ALLOW =
            Pattern.compile("allow", Pattern.CASE_INSENSITIVE);

    public BluetoothAdapterSnippet() throws Throwable {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        Utils.adaptShellPermissionIfRequired(mContext);
    }

    /* Gets the UiDevice instance for UI operations. */
    private static UiDevice getUiDevice() throws BluetoothAdapterSnippetException {
        try {
            return UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        } catch (IllegalStateException e) {
            throw new BluetoothAdapterSnippetException(
                    "Failed to get UiDevice. Please ensure that "
                            + "no other UiAutomation service is running.",
                    e);
        }
    }

    /**
     * Waits until the bluetooth adapter state has stabilized. We consider BT state stabilized if it
     * hasn't changed within 5 sec.
     */
    private static void waitForStableBtState() throws BluetoothAdapterSnippetException {
        long timeoutMs = System.currentTimeMillis() + TIMEOUT_TOGGLE_STATE_SEC * 1000;
        long continuousStateIntervalMs =
                System.currentTimeMillis() + BT_MATCHING_STATE_INTERVAL_SEC * 1000;
        int prevState = sBluetoothAdapter.getState();
        while (System.currentTimeMillis() < timeoutMs) {
            // Delay.
            Utils.waitUntil(() -> false, /* timeout= */ 1);

            int currentState = sBluetoothAdapter.getState();
            if (currentState != prevState) {
                continuousStateIntervalMs =
                        System.currentTimeMillis() + BT_MATCHING_STATE_INTERVAL_SEC * 1000;
            }
            if (continuousStateIntervalMs <= System.currentTimeMillis()) {
                return;
            }
            prevState = currentState;
        }
        throw new BluetoothAdapterSnippetException(
                String.format(
                        "Failed to reach a stable Bluetooth state within %d s",
                        TIMEOUT_TOGGLE_STATE_SEC));
    }

    /**
     * Enable bluetooth with larger tolerance on UI update timeout than btEnable from
     * https://github.com/google/mobly-bundled-snippets/blob/master/src/main/java/com/google/android/mobly/snippet/bundled/bluetooth/BluetoothAdapterSnippet.java.
     */
    @Rpc(description = "Enable bluetooth with a 30s timeout.")
    public void btEnableWithLongerWait()
            throws BluetoothAdapterSnippetException, InterruptedException {
        if (sBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
            return;
        }
        waitForStableBtState();

        if (Build.VERSION.SDK_INT >= 33) {
            // BluetoothAdapter#enable is removed from public SDK for 33 and above, so uses an
            // intent instead.
            UiDevice uiDevice = getUiDevice();
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // Triggers the system UI popup to ask for explicit permission.
            mContext.startActivity(enableIntent);
            // Clicks the "ALLOW" button.
            BySelector allowButtonSelector = By.text(TEXT_PATTERN_ALLOW).clickable(true);
            if (!Utils.waitUntil(
                    () -> uiDevice.findObject(allowButtonSelector) != null,
                    TIMEOUT_UI_UPDATE_SEC)) {
                throw new BluetoothAdapterSnippetException(
                        String.format(
                                "Bluetooth permission request dialog did not show up within %ss.",
                                TIMEOUT_UI_UPDATE_SEC));
            }
            uiDevice.findObject(allowButtonSelector).click();
        } else if (!sBluetoothAdapter.enable()) {
            throw new BluetoothAdapterSnippetException("Failed to start enabling bluetooth.");
        }
        if (!Utils.waitUntil(
                () -> sBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON,
                TIMEOUT_TOGGLE_STATE_SEC)) {
            throw new BluetoothAdapterSnippetException(
                    String.format(
                            "Bluetooth did not turn on within %ss.", TIMEOUT_TOGGLE_STATE_SEC));
        }
    }
}

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

package com.android.sts.common.util.tests;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static com.android.compatibility.common.util.SystemUtil.runShellCommand;
import static com.android.sts.common.SystemUtil.DEFAULT_MAX_POLL_TIME_MS;
import static com.android.sts.common.SystemUtil.DEFAULT_POLL_TIME_MS;
import static com.android.sts.common.SystemUtil.poll;

import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.truth.TruthJUnit.assume;

import android.app.Instrumentation;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

import com.android.sts.common.DumpsysUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** Unit tests for {@link DumpsysUtils}. */
public class DumpsysUtilsTest {
    private String mActivityName = null;
    private UiDevice mUiDevice = null;
    private BySelector mSelector = null;
    private Intent mIntent = null;
    private Context mContext = null;
    private BroadcastReceiver mBroadcastReceiver = null;
    private Semaphore mBroadcastReceived = null;
    private long timeout = 5_000L;

    @Before
    public void setUp() throws Exception {
        runShellCommand("input keyevent KEYCODE_WAKEUP");
        runShellCommand("wm dismiss-keyguard");
        runShellCommand("input keyevent KEYCODE_HOME");

        Instrumentation instrumentation = getInstrumentation();
        mContext = instrumentation.getContext();
        mUiDevice = UiDevice.getInstance(instrumentation);
        ComponentName componentName =
                ComponentName.createRelative(mContext, ".helperapp.PocActivity");
        mIntent =
                new Intent()
                        .setComponent(componentName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mActivityName = componentName.flattenToString();
        mSelector = By.pkg(componentName.getPackageName());

        // Check if device is in locked/secured state.
        KeyguardManager keyguardManager = mContext.getSystemService(KeyguardManager.class);
        assume().withMessage("Device is in secured state")
                .that(keyguardManager.isDeviceSecure() || keyguardManager.isDeviceLocked())
                .isFalse();

        // Register a broadcast receiver to detect whether the 'PocActivity' was launched.
        mBroadcastReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        mBroadcastReceived.release();
                    }
                };
        mContext.registerReceiver(
                mBroadcastReceiver,
                new IntentFilter(mContext.getPackageName()),
                Context.RECEIVER_EXPORTED);
    }

    @After
    public void tearDown() throws Exception {
        if (mUiDevice != null) {
            mUiDevice.pressHome();

            if (mSelector != null) {
                mUiDevice.wait(Until.gone(mSelector), timeout);
            }
        }

        if (mBroadcastReceived != null) {
            mContext.unregisterReceiver(mBroadcastReceiver);
        }
    }

    @Test
    public void testActivityResumed() throws Exception {
        assertWithMessage("Activity was not resumed")
                .that(startPocActivity(() -> DumpsysUtils.isActivityResumed(mActivityName)))
                .isTrue();
    }

    @Test
    public void testActivityVisible() throws Exception {
        assertWithMessage("Activity was not visible")
                .that(startPocActivity(() -> DumpsysUtils.isActivityVisible(mActivityName)))
                .isTrue();
    }

    @Test
    public void testActivityLaunched() throws Exception {
        assertWithMessage("Activity was not launched")
                .that(startPocActivity(() -> DumpsysUtils.isActivityLaunched(mActivityName)))
                .isTrue();
    }

    private boolean startPocActivity(final BooleanSupplier dumpsysCommand) throws Exception {
        // Start PocActivity and wait for broadcast from 'onResume()'.
        mBroadcastReceived = new Semaphore(0);
        Semaphore isPocActivityInOnResume = new Semaphore(0);
        boolean isStatusExpected =
                poll(
                        () -> {
                            try {
                                mContext.startActivity(mIntent);
                                isPocActivityInOnResume.tryAcquire();
                                if (mBroadcastReceived.tryAcquire(timeout, TimeUnit.MILLISECONDS)) {
                                    boolean result =
                                            poll(
                                                    () -> dumpsysCommand.getAsBoolean(),
                                                    DEFAULT_POLL_TIME_MS,
                                                    DEFAULT_MAX_POLL_TIME_MS / 3);
                                    isPocActivityInOnResume.release();
                                    return result;
                                }
                            } catch (Exception e) {
                                // Ignore unexpected exceptions.
                            }
                            return false;
                        });
        assume().withMessage("PocActivity did not start")
                .that(isPocActivityInOnResume.tryAcquire())
                .isTrue();
        return isStatusExpected;
    }
}

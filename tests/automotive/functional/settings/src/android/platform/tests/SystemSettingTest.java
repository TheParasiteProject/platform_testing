/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.platform.tests;

import static junit.framework.Assert.assertTrue;

import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.IAutoSystemSettingsHelper;
import android.platform.helpers.SettingsConstants;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(AndroidJUnit4.class)
public class SystemSettingTest {
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private HelperAccessor<IAutoSystemSettingsHelper> mSystemSettingsHelper;
    private static final String LOG_TAG = SystemSettingTest.class.getSimpleName();

    public SystemSettingTest() throws Exception {
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
        mSystemSettingsHelper = new HelperAccessor<>(IAutoSystemSettingsHelper.class);
    }


    @Before
    public void openSystemFacet() {
        Log.i(LOG_TAG, "Act: Open the System Settings");
        mSettingHelper.get().openSetting(SettingsConstants.SYSTEM_SETTINGS);
        Log.i(LOG_TAG, "Assert: System settings is open");
        assertTrue(
                "System settings did not open",
                mSettingHelper.get().checkMenuExists("Languages & input"));
    }

    @After
    public void goBackToSettingsScreen() {
        mSettingHelper.get().goBackToSettingsScreen();
    }

    @Test
    public void testDeviceModel() {
        String model = android.os.Build.MODEL;
        Log.i(LOG_TAG, "Assert: Model from API and Model from UI are the same");
        assertTrue(
                "Model from API and Model from UI are not the same",
                mSystemSettingsHelper.get().getDeviceModel().endsWith(model));
    }

    @Test
    public void testAndroidVersion() {
        String androidVersion = android.os.Build.VERSION.RELEASE;
        String androidVersionCodename = android.os.Build.VERSION.RELEASE_OR_CODENAME;
        Log.i(LOG_TAG, "Assert: Android Version from API and Android Version from UI are the same");
        assertTrue(
                "Android Version from API and Android Version from UI are not the same",
                mSystemSettingsHelper.get().getAndroidVersion().endsWith(androidVersion)
                        || mSystemSettingsHelper
                                .get()
                                .getAndroidVersion()
                                .endsWith(androidVersionCodename));
    }

    @Test
    public void testAndroidSecurityPatchLevel() {
        String day = android.os.Build.VERSION.SECURITY_PATCH;
        String[] arr = day.split("-");
        Date date =
                new Date(
                        Integer.valueOf(arr[0]),
                        Integer.valueOf(arr[1]) - 1,
                        Integer.valueOf(arr[2]));
        Log.i(LOG_TAG, "Assert: security patch from API and security patch from UI are the same");
        assertTrue(
                "security patch from API and security patch from UI are not the same",
                date.equals(mSystemSettingsHelper.get().getAndroidSecurityPatchLevel()));
    }

    @Test
    public void testKernelVersion() {
        String kernelVersion = System.getProperty("os.version");
        Log.i(LOG_TAG, "Assert: kernel version from API and kernel from UI are the same");
        assertTrue(
                "kernel version from API and kernel from UI are not the same",
                mSystemSettingsHelper.get().getKernelVersion().startsWith(kernelVersion));
    }

    @Test
    public void testBuildNumber() {
        String buildNumber = android.os.Build.DISPLAY;
        Log.i(LOG_TAG, "Assert: Build number from API and Build number from UI are the same");
        assertTrue(
                "Build number from API and Build number from UI are not the same",
                buildNumber.equals(mSystemSettingsHelper.get().getBuildNumber()));
    }
}

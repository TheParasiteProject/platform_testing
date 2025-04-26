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

import static junit.framework.Assert.assertEquals;

import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoStatusBarHelper;
import android.platform.test.rules.ConditionalIgnore;
import android.platform.test.rules.ConditionalIgnoreRule;
import android.platform.test.rules.IgnoreOnPortrait;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CurrentDateTimeTest {
    @Rule public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    private HelperAccessor<IAutoStatusBarHelper> mStatusBarHelper;
    private static final String LOG_TAG = CurrentDateTimeTest.class.getSimpleName();

    public CurrentDateTimeTest() {
        mStatusBarHelper = new HelperAccessor<>(IAutoStatusBarHelper.class);
    }

    @Test
    @ConditionalIgnore(condition = IgnoreOnPortrait.class)
    public void testCurrentTime() {
        Log.i(LOG_TAG, "Assert: Current local time");
        assertEquals(
                "Current local Time",
                mStatusBarHelper.get().getClockTime(),
                mStatusBarHelper
                        .get()
                        .getCurrentTimeWithTimeZone(mStatusBarHelper.get().getCurrentTimeZone()));
    }

    @Test
    public void testCurrentTimeZone() {
        Log.i(LOG_TAG, "Assert: Current local time zone");
        assertEquals(
                "Current local Time Zone",
                mStatusBarHelper.get().getCurrentTimeZone(),
                mStatusBarHelper.get().getDeviceCurrentTimeZone());
    }
}

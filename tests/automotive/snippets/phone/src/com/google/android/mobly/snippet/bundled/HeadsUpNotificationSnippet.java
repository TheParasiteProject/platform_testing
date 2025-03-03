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

import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoHeadsUpNotificationHelper;

import androidx.test.uiautomator.UiObject2;
import com.google.android.mobly.snippet.Snippet;
import com.google.android.mobly.snippet.rpc.Rpc;

/** Snippet for heads-up notification. */
public class HeadsUpNotificationSnippet implements Snippet {

    private final HelperAccessor<IAutoHeadsUpNotificationHelper> mHeadsUpNotificationHelper;

    public HeadsUpNotificationSnippet() {
        mHeadsUpNotificationHelper = new HelperAccessor<>(IAutoHeadsUpNotificationHelper.class);
    }

    @Rpc(description = "Find the HUN in the car's head unit.")
    public UiObject2 findHun() {
        return mHeadsUpNotificationHelper.get().findHun();
    }

    @Rpc(description = "Find the HUN in the car's head unit with the given title.")
    public UiObject2 findHunWithTitle(String text) {
        return mHeadsUpNotificationHelper.get().findHunWithTitle(text);
    }

    @Rpc(description = "Check if HUN is displayed in the car's head unit.")
    public boolean isHunDisplayed() {
        return mHeadsUpNotificationHelper.get().isHunDisplayed();
    }

    @Rpc(description = "Check if HUN with the given title is displayed in the car's head unit.")
    public boolean isHunDisplayedWithTitle(String text) {
        return mHeadsUpNotificationHelper.get().isHunDisplayedWithTitle(text);
    }

    @Rpc(description = "Swipe the HUN in the car's head unit.")
    public void swipeHun(String text) {
        mHeadsUpNotificationHelper.get().swipeHun(text);
    }

    @Rpc(description = "Find the SMS HUN in the car's head unit.")
    public UiObject2 findSmsHun() {
        return mHeadsUpNotificationHelper.get().findSmsHun();
    }

    @Rpc(description = "Find the SMS HUN in the car's head unit with the given title.")
    public UiObject2 findSmsHunWithTitle(String text) {
        return mHeadsUpNotificationHelper.get().findSmsHunWithTitle(text);
    }

    @Rpc(description = "Check if SMS HUN is displayed in the car's head unit.")
    public boolean isSmsHunDisplayed() {
        return mHeadsUpNotificationHelper.get().isSmsHunDisplayed();
    }

    @Rpc(description = "Check if SMS HUN from the given title is displayed in the car's head unit.")
    public boolean isSmsHunDisplayedWithTitle(String text) {
        return mHeadsUpNotificationHelper.get().isSmsHunDisplayedWithTitle(text);
    }

    @Rpc(description = "Get the content of the SMS HUN in the car's head unit.")
    public String getSmsHunContent(String text) {
        return mHeadsUpNotificationHelper.get().getSmsHunContent(text);
    }

    @Rpc(description = "Mute the SMS HUN with the given title in the car's head unit.")
    public void muteSmsHun(String text) {
        mHeadsUpNotificationHelper.get().muteSmsHun(text);
    }

    @Rpc(description = "Play the SMS HUN with the given title in the car's head unit.")
    public void playSmsHun(String text) {
        mHeadsUpNotificationHelper.get().playSmsHun(text);
    }

    @Rpc(description = "Check if the SMS is played on the car's head unit via car's speaker.")
    public boolean isSmsHunPlayedViaCarSpeaker() {
        return mHeadsUpNotificationHelper.get().isSmsHunPlayedViaCarSpeaker();
    }

    @Rpc(description = "Wait for the HUN in the car's head unit to disappear.")
    public boolean waitForHunToDisappear() {
        return mHeadsUpNotificationHelper.get().waitForHunToDisappear();
    }
}

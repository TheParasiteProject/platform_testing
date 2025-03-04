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

package android.platform.helpers;

import androidx.test.uiautomator.UiObject2;

public interface IAutoHeadsUpNotificationHelper extends Scrollable, IAppHelper {
    /**
     * Setup expectations: A heads-up notification (HUN) is posted.
     * Find the heads-up notification in the car's head unit.
     * Returns the first heads-up notification found.
     *
     * @return UiObject2 representing the heads-up notification, or null if it's not found.
     */
    UiObject2 findHun();

    /**
     * Setup expectations: A heads-up notification (HUN) is posted.
     *
     * Return the heads-up notification object.
     * Imlemented in case other notification pops up in the car's head unit while performing swipe, mute, etc.
     *
     * @param notification The UiObject2 representing the heads-up notification.
     * @return UiObject2 representing the heads-up notification, or null if it's not found.
     */
    UiObject2 findHun(UiObject2 notification);

    /**
     * Setup expectations: A heads-up notification (HUN) is posted.
     *
     * Find the heads-up notification in the car's head unit with the given title.
     * Returns the first heads-up notification found with the given title.
     * Swipes all heads-up notifications in the car's head unit if there are
     * multiple to find the one with the given title.
     *
     * @param text The text to match with the heads-up notification title.
     * @return UiObject2 representing the heads-up notification, or null if it's not found.
     */
    UiObject2 findHunWithTitle(String text);

    /**
     * Setup expectations: A heads-up notification (HUN) is posted.
     *
     * Check if HUN is displayed in the car's head unit.
     *
     * @return True if the HUN is displayed, false otherwise.
     */
    boolean isHunDisplayed();

    /**
     * Setup expectations: A heads-up notification (HUN) is posted.
     *
     * Check if HUN is displayed in the car's head unit with the given title.
     *
     * @param text The text to match with the heads-up notification title.
     * @return True if the HUN is displayed with the given title, false otherwise.
     */
    boolean isHunDisplayedWithTitle(String text);


    /**
     * Setup expectations: A heads-up notification (HUN) is posted.
     *
     * Swipe the heads-up notification in the car's head unit.
     *
     * @param notification The UiObject2 representing the heads-up notification.
     */
    void swipeHun(UiObject2 notification);

    /**
     * Setup expectations: Heads-up notification is posted.
     *
     * Swipe HUN with the given title in the car's head unit.
     *
     * @param text The text to match with the heads-up notification title.
     */
    void swipeHun(String text);

    /**
     * Setup expectations: A heads-up notification (HUN) is posted.
     *
     * Find the SMS heads-up notification in the car's head unit.
     * Returns the first SMS heads-up notification found.
     *
     * @return UiObject2 representing the SMS heads-up notification, or null if it's not found.
     */
    UiObject2 findSmsHun();

    /**
     * Setup expectations: A heads-up notification (HUN) is posted.
     *
     * Find the SMS heads-up notification in the car's head unit with the given title.
     * Returns the first SMS heads-up notification found with the given title.
     * Swipes all heads-up notifications in the car's head unit if there are
     * multiple to find the one with the given title.
     *
     * @param text The text to match with the heads-up notification title.
     * @return UiObject2 representing the SMS heads-up notification, or null if it's not found.
     */
    UiObject2 findSmsHunWithTitle(String text);

    /**
     * Setup expectations: A heads-up notification (HUN) is posted.
     *
     * Check if SMS HUN is displayed in the car's head unit.
     *
     * @return True if the SMS HUN is displayed, false otherwise.
     */
    boolean isSmsHunDisplayed();

    /**
     * Setup expectations: A heads-up notification (HUN) is posted.
     *
     * Check if SMS HUN is displayed in the car's head unit with the given title.
     *
     * @param text The text to match with the heads-up notification title.
     * @return True if the SMS HUN is displayed with the given title, false otherwise.
     */
    boolean isSmsHunDisplayedWithTitle(String text);

    /**
     * Setup expectations: A heads-up notification (HUN) is posted.
     *
     * Get the content of the SMS heads-up notification in the car's head unit.
     *
     * @param text The text to match with the heads-up notification title.
     * @return The content of the SMS heads-up notification, or null if it's not found.
     */
    String getSmsHunContent(String text);

    /**
     * Setup expectations: SMS is sent to the paired phone which is connected to the car.
     *
     * Mute the SMS HUN with the given title in the car's head unit.
     *
     * @param text The text to match with the heads-up notification title.
     */
    void muteSmsHun(String text);

    /**
     * Setup expectations: SMS is sent to the paired phone which is connected to the car.
     *
     * Play the SMS HUN with the given title in the car's head unit.
     *
     * @param text The text to match with the heads-up notification title.
     */
    void playSmsHun(String text);

    /**
     * Setup expectations: SMS is sent to the paired phone which is connected to the car.
     *
     * Check whether SMS HUN is played in the car's head unit.
     *
     * @return True if the SMS HUN is played in the car's head unit, false otherwise.
     */
    boolean isSmsHunPlayedViaCarSpeaker();


    /**
     * Setup expectations: A heads-up notification (HUN) is posted.
     *
     * Wait for the heads-up notification in the car's head unit to disappear.
     *     *
     * @return True if the heads-up notification in the car's head unit disappears within the given timeout, false otherwise.
     */
    boolean waitForHunToDisappear();
}

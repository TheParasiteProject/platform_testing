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

import android.util.Log;
import android.app.Instrumentation;
import android.platform.helpers.ScrollUtility.ScrollActions;
import android.platform.helpers.ScrollUtility.ScrollDirection;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiObject2;

import java.util.List;
import java.util.concurrent.*;
import java.lang.Exception;

/**
 * Helper for heads-up notifications on Automotive device.
 */
public class AutoHeadsUpNotificationHelperImpl extends AbstractStandardAppHelper
    implements IAutoHeadsUpNotificationHelper {

    private static final String LOG_TAG = AutoHeadsUpNotificationHelperImpl.class.getSimpleName();
    private static final int TIMEOUT_SECONDS = 10;

    public AutoHeadsUpNotificationHelperImpl(Instrumentation instr) {
        super(instr);
    }

    /** {@inheritDoc} */
    @Override
    public String getLauncherName() {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public String getPackage() {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void dismissInitialDialogs() {
        // Nothing to dismiss
    }

    /**
     * General heads-up notification related methods.
     */

    /** {@inheritDoc} */
    @Override
    public UiObject2 findHun() {
        return runWithTimeout(() -> {
            Log.i(LOG_TAG, "Checking for heads-up notification in the car's head unit.");
            BySelector notificationSelector = getUiElementFromConfig(AutomotiveConfigConstants.HEADSUP_NOTIFICATION);
            UiObject2 notification = getSpectatioUiUtil().waitForUiObject(notificationSelector);
            if (notification == null) {
                Log.w(LOG_TAG, "Cannot to find heads-up notification in the car's head unit.");
            }
            return notification;
        });
    }

    /** {@inheritDoc} */
    @Override
    public UiObject2 findHun(UiObject2 notification) {
        return runWithTimeout(() -> {
            Log.i(LOG_TAG, "Checking for heads-up notification in the car's head unit.");
            UiObject2 currentNotification = findHun();
            while (currentNotification != null && !currentNotification.equals(notification)) {
                swipeHun(currentNotification);
                currentNotification = findHun();
            }
            if (currentNotification == null) {
                Log.w(LOG_TAG, "The HUN object is not found.");
            }
            return currentNotification;
        });
    }

    /** {@inheritDoc} */
    @Override
    public UiObject2 findHunWithTitle(String text) {
        return runWithTimeout(() -> {
            Log.i(LOG_TAG, String.format("Checking for heads-up notification with title %s in the car's head unit.", text));
            UiObject2 notification = findHun();
            while (notification != null) {
                if (isHunTitleMatched(notification, text)) {
                    Log.i(LOG_TAG, "Heads-up notification displayed.");
                    return notification;
                }
                swipeHun(notification);
                notification = findHun();
            }
            Log.i(LOG_TAG, String.format("Cannot find heads-up notification with title %s.", text));
            return null;
        });
    }

    /** {@inheritDoc} */
    @Override
    public boolean isHunDisplayed() {
        UiObject2 notification = findHun();
        return notification != null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isHunDisplayedWithTitle(String text) {
        UiObject2 notification = findHunWithTitle(text);
        return notification != null;
    }

    /** {@inheritDoc} */
    @Override
    public void swipeHun(UiObject2 notification) {
        Log.i(LOG_TAG, "Swiping the heads-up notification in the car's head unit.");
        notification = findHun(notification);
        getSpectatioUiUtil().swipeRight(notification);
        // Wait for the notification to dismiss, if it takes more than 2 seconds to dismiss, it is a performance issue.
        getSpectatioUiUtil().waitNSeconds(2000);
    }

    /** {@inheritDoc} */
    @Override
    public void swipeHun(String text) {
        UiObject2 notification = findHunWithTitle(text);
        swipeHun(notification);
    }

    /**
     * SMS heads-up notification related methods.
     */

    /** {@inheritDoc} */
    @Override
    public UiObject2 findSmsHun() {
        return runWithTimeout(() -> {
            Log.i(LOG_TAG, "Checking for SMS HUN in the car's head unit.");
            UiObject2 notification = findHun();
            while (notification != null) {
                if (isSmsHun(notification)) {
                    Log.i(LOG_TAG, "SMS HUN displayed.");
                    return notification;
                }
                swipeHun(notification);
                notification = findHun();
            }
            Log.w(LOG_TAG, "Cannot to find SMS HUN in the car's head unit.");
            return null;
        });
    }

    /** {@inheritDoc} */
    @Override
    public UiObject2 findSmsHunWithTitle(String text) {
        return runWithTimeout(() -> {
            Log.i(LOG_TAG, String.format("Checking for SMS HUN with title %s in the car's head unit.", text));
            UiObject2 notification = findSmsHun();
            while (notification != null) {
                if (isHunTitleMatched(notification, text)) {
                    Log.i(LOG_TAG, String.format("SMS HUN displayed with title %s.", text));
                    return notification;
                }
                swipeHun(notification);
                notification = findSmsHun();
            }
            Log.i(LOG_TAG, String.format("Cannot find SMS HUN with title %s.", text));
            return null;
        });
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSmsHunDisplayed() {
        UiObject2 notification = findSmsHun();
        return notification != null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSmsHunDisplayedWithTitle(String text) {
        Log.i(LOG_TAG, String.format("Checking if SMS HUN with title %s is displayed in the car's head unit.", text));
        UiObject2 notification = findSmsHunWithTitle(text);
        return notification != null;
    }

    /** {@inheritDoc} */
    @Override
    public String getSmsHunContent(String text) {
        Log.i(LOG_TAG, "Getting the content of SMS HUN.");
        UiObject2 notification = findHunWithTitle(text);
        UiObject2 notificationContent = notification.findObject(
            getUiElementFromConfig(AutomotiveConfigConstants.HEADSUP_NOTIFICATION_CONTENT)
        );
        if (notificationContent == null) {
            Log.w(LOG_TAG, "Cannot to find SMS HUN content.");
            return null;
        }
        return notificationContent.getText();
    }

    /** {@inheritDoc} */
    @Override
    public void muteSmsHun(String text) {
        Log.i(LOG_TAG, "Clicking on mute button of SMS HUN.");
        UiObject2 notification = findHunWithTitle(text);
        UiObject2 muteButton = notification.findObject(
            getUiElementFromConfig(AutomotiveConfigConstants.HEADSUP_NOTIFICATION_MUTE_BUTTON)
        );
        getSpectatioUiUtil().clickAndWait(muteButton, 2000);
    }

    /** {@inheritDoc} */
    @Override
    public void playSmsHun(String text) {
        Log.i(LOG_TAG, "Clicking on play button of SMS HUN.");
        UiObject2 notification = findHunWithTitle(text);
        UiObject2 playButton = notification.findObject(
            getUiElementFromConfig(AutomotiveConfigConstants.HEADSUP_NOTIFICATION_PLAY_BUTTON)
        );
        getSpectatioUiUtil().clickAndWait(playButton);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSmsHunPlayedViaCarSpeaker() {
        Log.i(LOG_TAG, "Checking if SMS HUN is played in the car's head unit.");
        // TODO: Implement this method. Need to verify if the sound is played from special channel.
        return true;
    }

    /**
     * Check if the heads-up notification title is matched with the given text.
     *
     * @param text The text to match with the heads-up notification title.
     * @return True if the heads-up notification title is matched with the given text, false
     * otherwise.
     */
    private boolean isHunTitleMatched(UiObject2 notification, String text) {
        UiObject2 notificationTitle = notification.findObject(
            getUiElementFromConfig(AutomotiveConfigConstants.HEADSUP_NOTIFICATION_TITLE)
        );
        if (notificationTitle == null) {
            Log.w(LOG_TAG, "Cannot to find heads-up notification title in the car's head unit.");
            return false;
        }
        String titleText = notificationTitle.getText().toLowerCase();
        return titleText != null && titleText.contains(text.toLowerCase());
    }

    /**
     * Check if the heads-up notification is an SMS heads-up notification.
     * If play button, if mute button is present, then it is SMS HUN.
     *
     * @param notification UiObject2 representing the heads-up notification.
     * @return True if the heads-up notification is an SMS heads-up notification, false otherwise.
     */
    private boolean isSmsHun(UiObject2 notification) {
        UiObject2 playButton = notification.findObject(
            getUiElementFromConfig(AutomotiveConfigConstants.HEADSUP_NOTIFICATION_PLAY_BUTTON)
        );
        UiObject2 muteButton = notification.findObject(
            getUiElementFromConfig(AutomotiveConfigConstants.HEADSUP_NOTIFICATION_MUTE_BUTTON)
        );
        if (playButton != null && muteButton != null) {
            return true;
        }
        return false;
    }

    /**
     * Run the given task with a default timeout.
     *
     * @param task The task to run.
     * @return The result of the task, or null if the task times out.
     */
    private static <T> T runWithTimeout(Callable<T> task) {
        return runWithTimeout(task, TIMEOUT_SECONDS);
    }

    /**
     * Run the given task with the given timeout.
     *
     * @param task The task to run.
     * @param timeoutSeconds The timeout in seconds.
     * @return The result of the task, or null if the task times out.
     */
    private static <T> T runWithTimeout(Callable<T> task, int timeoutSeconds) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<T> future = executor.submit(task);
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Log.i(LOG_TAG, "Function execution exceeded time limit.");
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            future.cancel(true);
            executor.shutdown();
        }
    }
}

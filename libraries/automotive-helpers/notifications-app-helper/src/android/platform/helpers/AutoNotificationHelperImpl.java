/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.app.Instrumentation;
import android.platform.helpers.ScrollUtility.ScrollActions;
import android.platform.helpers.ScrollUtility.ScrollDirection;
import android.util.Log;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiObject2;

import java.util.List;

/**
 * Helper for Notifications on Automotive device openNotification() for swipeDown is removed- Not
 * supported in UDC- bug b/285387870
 */
public class AutoNotificationHelperImpl extends AbstractStandardAppHelper
        implements IAutoNotificationHelper {

    private static final String LOG_TAG = AutoNotificationHelperImpl.class.getSimpleName();
    private ScrollUtility mScrollUtility;
    private ScrollActions mScrollAction;
    private BySelector mBackwardButtonSelector;
    private BySelector mForwardButtonSelector;
    private BySelector mScrollableElementSelector;
    private ScrollDirection mScrollDirection;

    public AutoNotificationHelperImpl(Instrumentation instr) {
        super(instr);
        mScrollUtility = ScrollUtility.getInstance(getSpectatioUiUtil());
        mScrollAction = ScrollActions.valueOf(
            getActionFromConfig(AutomotiveConfigConstants.NOTIFICATION_LIST_SCROLL_ACTION)
        );
        mBackwardButtonSelector = getUiElementFromConfig(
            AutomotiveConfigConstants.NOTIFICATION_LIST_SCROLL_BACKWARD_BUTTON
        );
        mForwardButtonSelector = getUiElementFromConfig(
            AutomotiveConfigConstants.NOTIFICATION_LIST_SCROLL_FORWARD_BUTTON
        );
        mScrollableElementSelector = getUiElementFromConfig(
            AutomotiveConfigConstants.NOTIFICATION_LIST
        );
        mScrollDirection = ScrollDirection.valueOf(
            getActionFromConfig(AutomotiveConfigConstants.NOTIFICATION_LIST_SCROLL_DIRECTION)
        );
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

    /** {@inheritDoc} */
    @Override
    public void openNotificationCenter() {
        if (isAppInForeground())
            return;

        getSpectatioUiUtil().executeShellCommand(
            getCommandFromConfig(AutomotiveConfigConstants.OPEN_NOTIFICATIONS_COMMAND)
        );
        getSpectatioUiUtil().waitNSeconds(2000);
    }

    /** {@inheritDoc} */
    @Override
    public void exitNotificationCenter() {
        getSpectatioUiUtil().pressHome();
        getSpectatioUiUtil().waitNSeconds(2000);
    }

    @Override
    public boolean isAppInForeground() {
        BySelector notificationViewSelector = getUiElementFromConfig(
            AutomotiveConfigConstants.NOTIFICATION_VIEW
        );
        return getSpectatioUiUtil().hasUiElement(notificationViewSelector);
    }

    @Override
    public UiObject2 findNotificationInCenterWithTitle(String title) {
        Log.i(LOG_TAG, "Searching for notification in the notification center with title: " + title);

        openNotificationCenter();
        List<UiObject2> notifications = getSpectatioUiUtil().findUiObjects(
            getUiElementFromConfig(AutomotiveConfigConstants.NOTIFICATION_BODY)
        );

        for (UiObject2 notification : notifications) {
            UiObject2 titleObj = notification.findObject(
                getUiElementFromConfig(AutomotiveConfigConstants.NOTIFICATION_TITLE)
            );
            String titleText = titleObj.getText().toLowerCase();
            if (titleObj != null && titleText.contains(title.toLowerCase())) {
                return notification;
            }
        }

        Log.w(LOG_TAG, "Cannot find notification in the notification center with title: " + title);
        return null;
    }

    public UiObject2 findInNotificationList(BySelector selector) {
        Log.i(LOG_TAG, "Searching for notification in the notification center with title.");

        openNotificationCenter();
        UiObject2 notification = getSpectatioUiUtil().findUiObject(selector);
        if (notification != null) {
            return notification;
        }

        UiObject2 notificationList = getSpectatioUiUtil().findUiObject(mScrollableElementSelector);
        if (notificationList != null && notificationList.isScrollable()) {
            String scrollDescription = String.format("Scroll on notification list to find %s", selector);
            notification = mScrollUtility.scrollAndFindUiObject(
                    mScrollAction,
                    mScrollDirection,
                    mForwardButtonSelector,
                    mBackwardButtonSelector,
                    mScrollableElementSelector,
                    selector,
                    scrollDescription
            );
        }

        return notification;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isNotificationDisplayedInCenterWithTitle(String title) {
        Log.i(LOG_TAG, "Checking if notification in the notification center with title: " + title + " is displayed.");
        // UiObject2 notification = findNotificationInCenterWithTitle(title);
        BySelector selector = By.text(title);
        UiObject2 notification = findInNotificationList(selector);
        return notification != null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isNotificationDisplayedInCenterWithContent(String content) {
        Log.i(LOG_TAG, "Checking if notification in the notification center with content: " + content + " is displayed.");
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String getNotificationContent(String title) {
        Log.i(LOG_TAG, String.format("Getting the content of notification in the notification center with title %s.", title));

        UiObject2 notification = findNotificationInCenterWithTitle(title);
        UiObject2 content = notification.findObject(
            getUiElementFromConfig(AutomotiveConfigConstants.NOTIFICATION_CONTENT)
        );
        if (content == null) {
            throw new RuntimeException(String.format("Cannot find content for notification with title '%s'.", title));
        }

        return content.getText();
    }

    /** {@inheritDoc} */
    @Override
    public int getSmsNotificationCount(String title) {
        Log.i(LOG_TAG, String.format("Getting the count of SMS notification in the notification center with title %s.", title));

        UiObject2 notification = findNotificationInCenterWithTitle(title);
        UiObject2 smsCount = notification.findObject(
            getUiElementFromConfig(AutomotiveConfigConstants.NOTIFICATION_SMS_COUNT)
        );
        if (smsCount == null) {
            throw new RuntimeException(String.format("Cannot find SMS count for notification with title '%s'.", title));
        }

        String smsCountText = smsCount.getText();
        Log.i(LOG_TAG, "Sms count text: " + smsCountText);
        int count = Integer.parseInt(smsCountText.split(" ")[0]);

        return count;
    }

    /** {@inheritDoc} */
    @Override
    public String getSmsNotificationContent(String title) {
        Log.i(LOG_TAG, String.format("Getting the content of SMS notification in the notification center with title %s.", title));

        // Click on SMS count to expand the notification and get the content. If present.
        UiObject2 notification = findNotificationInCenterWithTitle(title);
        UiObject2 smsCount = notification.findObject(
            getUiElementFromConfig(AutomotiveConfigConstants.NOTIFICATION_SMS_COUNT)
        );
        if (smsCount != null) {
            getSpectatioUiUtil().clickAndWait(smsCount);
        }
        return getNotificationContent(title);
    }

    /** {@inheritDoc} */
    @Override
    public void removeNotification(String title) {
        Log.i(LOG_TAG, "Removing notification with title: " + title);

        openNotificationCenter();
        BySelector selector = By.text(title);
        UiObject2 notification = findInNotificationList(selector);
        getSpectatioUiUtil().swipeRight(notification);
        getSpectatioUiUtil().waitNSeconds(2000);
    }

    /** {@inheritDoc} */
    @Override
    public void clickClearAllBtn() {
        Log.i(LOG_TAG, "Clicking on clear all button.");

        openNotificationCenter();
        BySelector clearButtonSelector = getUiElementFromConfig(AutomotiveConfigConstants.CLEAR_ALL_BUTTON);
        if (findInNotificationList(clearButtonSelector) != null) {
            UiObject2 clear_all_btn = getSpectatioUiUtil().findUiObject(clearButtonSelector);
            getSpectatioUiUtil().clickAndWait(clear_all_btn);
        } else {
            throw new RuntimeException("Cannot find 'Clear All' button");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void clickManageBtn() {
        Log.i(LOG_TAG, "Clicking on manage button.");

        openNotificationCenter();
        BySelector manageButtonSelector = getUiElementFromConfig(AutomotiveConfigConstants.MANAGE_BUTTON);
        if (findInNotificationList(manageButtonSelector) != null) {
            UiObject2 manage_btn = getSpectatioUiUtil().findUiObject(manageButtonSelector);
            getSpectatioUiUtil().clickAndWaitUntilNewWindowAppears(manage_btn);
        } else {
            throw new RuntimeException("Cannot find 'Manage' button");
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isNotificationSettingsOpened() {
        Log.i(LOG_TAG, String.format("Checking if notification settings is opened."));

        List<UiObject2> settingsObj = getSpectatioUiUtil().findUiObjects(
            getUiElementFromConfig(AutomotiveConfigConstants.NOTIFICATION_SETTINGS_LAYOUT)
        );
        for (UiObject2 settings : settingsObj) {
            UiObject2 settingsTitle = settings.findObject(
                getUiElementFromConfig(AutomotiveConfigConstants.NOTIFICATION_SETTINGS_TITLE)
            );
            if (settingsTitle != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isRecentNotification() {
        Log.i(LOG_TAG, "Check if notification is present under recent category.");

        openNotificationCenter();
        BySelector recentNotificationsPanel = getUiElementFromConfig(AutomotiveConfigConstants.RECENT_NOTIFICATIONS);
        UiObject2 recentNotificationLayOut = getSpectatioUiUtil().findUiObject(recentNotificationsPanel);
        getSpectatioUiUtil().validateUiObject(
            recentNotificationLayOut,
            AutomotiveConfigConstants.RECENT_NOTIFICATIONS
        );

        // TODO: Don't hardcode the notification title.
        BySelector testNotification =
                getUiElementFromConfig(AutomotiveConfigConstants.TEST_NOTIFICATION);
        UiObject2 testNotificationLayout =
                getSpectatioUiUtil()
                        .findUiObjectInGivenElement(recentNotificationLayOut, testNotification);
        return testNotificationLayout != null;
    }

    @Override
    public boolean isOlderNotification() {
        Log.i(LOG_TAG, "Check if notification is present under older category.");

        openNotificationCenter();
        BySelector olderNotificationsPanel = getUiElementFromConfig(AutomotiveConfigConstants.OLDER_NOTIFICATIONS);
        UiObject2 olderNotificationLayOut = getSpectatioUiUtil().findUiObject(olderNotificationsPanel);
        getSpectatioUiUtil().validateUiObject(
            olderNotificationLayOut,
            AutomotiveConfigConstants.OLDER_NOTIFICATIONS
        );

        // TODO: Don't hardcode the notification title.
        BySelector testNotification =
                getUiElementFromConfig(AutomotiveConfigConstants.TEST_NOTIFICATION);
        UiObject2 testNotificationLayout =
                getSpectatioUiUtil()
                        .findUiObjectInGivenElement(olderNotificationLayOut, testNotification);
        return testNotificationLayout != null;
    }

     @Override
    public void clickOnCheckRecentPermissions(String title) {
        BySelector notificationSelector = By.text(title);
        UiObject2 notification = getSpectatioUiUtil().findUiObject(notificationSelector);
        getSpectatioUiUtil().clickAndWait(notification);
    }

    @Override
    public boolean checkAppPermissionsExists(String title) {
        return getSpectatioUiUtil().hasUiElement(title);
    }

    // TODO: Remove this method as it is not used anywhere.
    @Override
    public boolean scrollDownOnePage() {
        UiObject2 notification_list = getSpectatioUiUtil().findUiObject(mScrollableElementSelector);
        boolean swipeResult = false;
        if (notification_list != null && notification_list.isScrollable()) {
            swipeResult =
                    mScrollUtility.scrollForward(
                            mScrollAction,
                            mScrollDirection,
                            mForwardButtonSelector,
                            mScrollableElementSelector,
                            String.format("Scroll down one page on notification list"));
        }
        return swipeResult;
    }

    @Override
    public boolean scrollUpOnePage() {
        UiObject2 notification_list = getSpectatioUiUtil().findUiObject(mScrollableElementSelector);
        boolean swipeResult = false;
        if (notification_list != null && notification_list.isScrollable()) {
            swipeResult =
                    mScrollUtility.scrollBackward(
                            mScrollAction,
                            mScrollDirection,
                            mBackwardButtonSelector,
                            mScrollableElementSelector,
                            String.format("Scroll up one page on notification list"));
        }
        return swipeResult;
    }

}

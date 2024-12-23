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
package android.support.test.launcherhelper2;

import android.graphics.Point;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;
import android.widget.TextView;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import junit.framework.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class BaseLauncher3Strategy implements ILauncherStrategy {
    private static final String LOG_TAG = BaseLauncher3Strategy.class.getSimpleName();
    protected UiDevice mDevice;

    /** {@inheritDoc} */
    @Override
    public void setUiDevice(UiDevice uiDevice) {
        mDevice = uiDevice;
    }

    /** {@inheritDoc} */
    @Override
    public void open() {
        // if we see hotseat, assume at home screen already
        if (!mDevice.hasObject(getHotSeatSelector())) {
            mDevice.pressHome();
            // ensure launcher is shown
            if (!mDevice.wait(Until.hasObject(getHotSeatSelector()), 5000)) {
                // HACK: dump hierarchy to logcat
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    mDevice.dumpWindowHierarchy(baos);
                    baos.flush();
                    baos.close();
                    String[] lines = baos.toString().split("\\r?\\n");
                    for (String line : lines) {
                        Log.d(LOG_TAG, line.trim());
                    }
                } catch (IOException ioe) {
                    Log.e(LOG_TAG, "error dumping XML to logcat", ioe);
                }
                Assert.fail("Failed to open launcher");
            }
            mDevice.waitForIdle();
        }
        dismissHomeScreenCling();
    }

    /** Checks and dismisses home screen cling */
    protected void dismissHomeScreenCling() {
        // empty default implementation
    }

    boolean isOreoOrAbove() {
        return Build.VERSION.DEVICE_INITIAL_SDK_INT >= Build.VERSION_CODES.O;
    }

    /** {@inheritDoc} */
    @Override
    public UiObject2 openAllApps(boolean reset) {
        // if we see all apps container, skip the opening step
        if (!mDevice.hasObject(getAllAppsSelector()) || reset) {
            open();
            mDevice.waitForIdle();
            Assert.assertTrue(
                    "openAllApps: can't go to home screen",
                    !mDevice.hasObject(getAllAppsSelector()));
            if (isOreoOrAbove()) {
                int midX = mDevice.getDisplayWidth() / 2;
                int height = mDevice.getDisplayHeight();
                // Swipe from 6/7ths down the screen to 1/7th down the screen.
                mDevice.swipe(
                        midX,
                        height * 6 / 7,
                        midX,
                        height / 7,
                        (height * 2 / 3) / 100); // 100 px/step
            } else {
                // Swipe from the hotseat to near the top, e.g. 10% of the screen.
                UiObject2 hotseat = mDevice.wait(Until.findObject(getHotSeatSelector()), 2500);
                Point start = hotseat.getVisibleCenter();
                int endY = (int) (mDevice.getDisplayHeight() * 0.1f);
                mDevice.swipe(
                        start.x, start.y, start.x, endY, (start.y - endY) / 100); // 100 px/step
            }
        }
        UiObject2 allAppsContainer = mDevice.wait(Until.findObject(getAllAppsSelector()), 2000);

        final int topGestureMargin = (int) (allAppsContainer.getVisibleBounds().height() * 0.15f);
        allAppsContainer.setGestureMargins(0, topGestureMargin, 0, 1);

        Assert.assertNotNull("openAllApps: did not find all apps container", allAppsContainer);
        return allAppsContainer;
    }

    protected boolean isInOverview() {
        BySelector noRecentItemsSelector = getOverviewSelector().desc("No recent items");
        UiObject2 noRecentItems = mDevice.findObject(noRecentItemsSelector);
        return noRecentItems != null;
    }

    /** {@inheritDoc} */
    @Override
    public void openOverview() {
        if (!isInOverview()) {
            try {
                mDevice.pressRecentApps();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }

            // use a long timeout to wait until recents populated
            UiObject2 overviewScreen = mDevice.wait(Until.findObject(getOverviewSelector()), 2000);
            Assert.assertNotNull("openOverview: did not find overview", overviewScreen);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean clearRecentAppsFromOverview() {
        for (int i = 0; i < 10; i++) {
            mDevice.swipe(
                    mDevice.getDisplayWidth() / 2,
                    mDevice.getDisplayHeight() / 2,
                    mDevice.getDisplayWidth(),
                    mDevice.getDisplayHeight() / 2,
                    5);

            BySelector noRecentItemsSelector = getOverviewSelector().desc("No recent items");
            UiObject2 noRecentItems = mDevice.wait(Until.findObject(noRecentItemsSelector), 100);

            // If "No recent items"  is displayed, there're no apps to remove
            if (noRecentItems != null) {
                return true;
            }

            // If "Clear all"  button appears, use it
            BySelector clearAllSelector = By.res(mDevice.getLauncherPackageName(), "clear_all");
            UiObject2 clearAllButton = mDevice.wait(Until.findObject(clearAllSelector), 100);
            if (clearAllButton != null) {
                clearAllButton.click();
                return true;
            }
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Direction getAllAppsScrollDirection() {
        return Direction.DOWN;
    }

    /** {@inheritDoc} */
    @Override
    public UiObject2 openAllWidgets(boolean reset) {
        if (!mDevice.hasObject(getAllWidgetsSelector())) {
            open();
            // trigger the wallpapers/widgets/settings view
            mDevice.pressMenu();
            mDevice.waitForIdle();
            mDevice.findObject(By.res(getSupportedLauncherPackage(), "widget_button")).click();
        }
        UiObject2 allWidgetsContainer =
                mDevice.wait(Until.findObject(getAllWidgetsSelector()), 2000);
        Assert.assertNotNull(
                "openAllWidgets: did not find all widgets container", allWidgetsContainer);
        if (reset) {
            CommonLauncherHelper.getInstance(mDevice)
                    .scrollBackToBeginning(
                            allWidgetsContainer, Direction.reverse(getAllWidgetsScrollDirection()));
        }
        return allWidgetsContainer;
    }

    /** {@inheritDoc} */
    @Override
    public Direction getAllWidgetsScrollDirection() {
        return Direction.DOWN;
    }

    /** {@inheritDoc} */
    @Override
    public long launch(String appName, String packageName) {
        BySelector app = By.clazz(TextView.class).text(appName);
        return CommonLauncherHelper.getInstance(mDevice).launchApp(this, app, packageName);
    }

    /** {@inheritDoc} */
    @Override
    public BySelector getAllAppsSelector() {
        return By.res(getSupportedLauncherPackage(), "apps_list_view");
    }

    /** {@inheritDoc} */
    @Override
    public BySelector getAllWidgetsSelector() {
        return By.res(getSupportedLauncherPackage(), "widgets_list_view");
    }

    /** {@inheritDoc} */
    @Override
    public BySelector getWorkspaceSelector() {
        return By.res(getSupportedLauncherPackage(), "workspace");
    }

    /** {@inheritDoc} */
    @Override
    public BySelector getHotSeatSelector() {
        return By.res(getSupportedLauncherPackage(), "hotseat");
    }

    /** {@inheritDoc} */
    @Override
    public BySelector getOverviewSelector() {
        return By.res(getSupportedLauncherPackage(), "overview_panel");
    }

    /** {@inheritDoc} */
    @Override
    public Direction getWorkspaceScrollDirection() {
        return Direction.RIGHT;
    }
}

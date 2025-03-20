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

package android.platform.helpers;

import android.app.Instrumentation;
import android.platform.helpers.exceptions.UnknownUiException;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiObject2;

/** Helper class for functional test for LockScreen test */
public class LockScreenHelperImpl extends AbstractStandardAppHelper
        implements IAutoLockScreenHelper {

    private static final int DEFAULT_WAIT_TIME = 5000;
    private static final int TEN_SECONDS_WAIT_TIME = 10000;
    private static final int KEY_ENTER = 66;

    private HelperAccessor<IAutoSecuritySettingsHelper> mSecuritySettingsHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;

    public LockScreenHelperImpl(Instrumentation instr) {
        super(instr);
        mSecuritySettingsHelper = new HelperAccessor<>(IAutoSecuritySettingsHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
    }

    /** {@inheritDoc} */
    @Override
    public void dismissInitialDialogs() {
        // Nothing to dismiss
    }
    /** {@inheritDoc} */
    @Override
    public String getPackage() {
        return getPackageFromConfig(AutomotiveConfigConstants.LOCK_SCREEN_PACKAGE);
    }

    /** {@inheritDoc} */
    @Override
    public String getLauncherName() {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    protected void pressEnter() {
        getSpectatioUiUtil().pressKeyCode(KEY_ENTER);
    }

    @Override
    public void lockScreenBy(LockType lockType, String credential) {
        if (lockType == LockType.PIN) {
            mSecuritySettingsHelper.get().setLockByPin(credential);
        } else {
            mSecuritySettingsHelper.get().setLockByPassword(credential);
        }
        getSpectatioUiUtil().pressPower();
        getSpectatioUiUtil().waitForIdle();
    }

    @Override
    public void unlockScreenBy(LockType lockType, String credential) {
        getSpectatioUiUtil().pressPower();
        getSpectatioUiUtil().waitForIdle();
        BySelector lockscreenSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.LOCK_SCREEN_PASSWORD);
        getSpectatioUiUtil().waitForUiObject(lockscreenSelector, TEN_SECONDS_WAIT_TIME);
        if (lockType == LockType.PIN) {
            unlockByPin(credential);
        } else {
            unlockByPassword(credential);
        }
    }

    private void unlockByPassword(String password) {
        BySelector lockscreenSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.LOCK_SCREEN_EDIT_TEXT);
        UiObject2 editPasswordButtonObject = getSpectatioUiUtil().findUiObject(lockscreenSelector);
        getSpectatioUiUtil()
                .validateUiObject(
                        editPasswordButtonObject, AutomotiveConfigConstants.LOCK_SCREEN_EDIT_TEXT);
        getSpectatioUiUtil().clickAndWait(editPasswordButtonObject, DEFAULT_WAIT_TIME);
        getSpectatioUiUtil()
                .executeShellCommand(
                        String.format(
                                getCommandFromConfig(AutomotiveConfigConstants.LOCK_SCREEN_COMMAND),
                                password));
        pressEnter();
        mSettingHelper.get().openSetting(SettingsConstants.SECURITY_SETTINGS);
    }

    private void unlockByPin(String pin) {
        selectPinOnPinPad(pin);
        BySelector enterButtonSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.LOCK_SCREEN_ENTER_KEY);
        UiObject2 enter_button = getSpectatioUiUtil().findUiObject(enterButtonSelector);
        getSpectatioUiUtil()
                .validateUiObject(enter_button, AutomotiveConfigConstants.LOCK_SCREEN_ENTER_KEY);
        getSpectatioUiUtil().clickAndWait(enter_button);
        getSpectatioUiUtil().waitForIdle();
        BySelector pinPadSelector =
                getUiElementFromConfig(AutomotiveConfigConstants.LOCK_SCREEN_PIN_PAD);
        UiObject2 pinPad = getSpectatioUiUtil().findUiObject(pinPadSelector);
        if (pinPad != null) {
            throw new UnknownUiException("Unable to Unlock by Pin.");
        }
    }

    private void selectPinOnPinPad(String pin) {
        int length = pin.length();
        for (int i = 0; i < length; i++) {
            char c = pin.charAt(i);
            String resourceId = "key" + c;
            BySelector number_selector =
                    By.res(
                            getPackageFromConfig(AutomotiveConfigConstants.LOCK_SCREEN_PACKAGE),
                            resourceId);
            UiObject2 number = getSpectatioUiUtil().findUiObject(number_selector);
            number.click();
        }
    }
}

/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.platform.spectatio.configs;

import android.platform.spectatio.constants.JsonConfigConstants.FindType;
import android.platform.spectatio.constants.JsonConfigConstants.ScrollActions;
import android.platform.spectatio.constants.JsonConfigConstants.ScrollDirection;
import android.platform.spectatio.constants.JsonConfigConstants.SupportedWorkFlowTasks;
import android.platform.spectatio.exceptions.MissingUiElementException;
import android.platform.spectatio.utils.SpectatioUiUtil;
import android.platform.spectatio.utils.SpectatioUiUtil.SwipeDirection;
import android.platform.spectatio.utils.SpectatioUiUtil.SwipeFraction;
import android.util.Log;

import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiObject2;

import com.google.common.base.Strings;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Workflow Task For Workflows in Spectatio Config JSON Config */
public class WorkflowTask {
    private static final String LOG_TAG = WorkflowTask.class.getSimpleName();

    @SerializedName("NAME")
    private String mName;

    @SerializedName("TYPE")
    private String mType;

    // TEXT or UI_ELEMENT based on the Type of Workflow Task
    @SerializedName("CONFIG")
    private WorkflowTaskConfig mTaskConfig;

    // Number of times to repeat given task, default is 0
    // By default, task will be execute once and won't be repeated as repeat count is 0
    @SerializedName("REPEAT_COUNT")
    private int mRepeatCount;

    // If task needs scrolling, provide Scroll Config
    @SerializedName("SCROLL_CONFIG")
    private ScrollConfig mScrollConfig;

    // If task sets text, provide SetText Config
    @SerializedName("SET_TEXT_CONFIG")
    private SetTextConfig mSetTextConfig;

    // If task needs swiping, provide Swipe Config
    @SerializedName("SWIPE_CONFIG")
    private SwipeConfig mSwipeConfig;

    // If task validates a value, provide Validation Config
    @SerializedName("VALIDATION_CONFIG")
    private ValidationConfig mValidationConfig;

    public WorkflowTask(
            String name,
            String type,
            WorkflowTaskConfig taskConfig,
            int repeatCount,
            ScrollConfig scrollConfig,
            SetTextConfig setTextConfig,
            SwipeConfig swipeConfig,
            ValidationConfig validationConfig) {
        mName = name;
        mType = type;
        mTaskConfig = taskConfig;
        mRepeatCount = repeatCount;
        mScrollConfig = scrollConfig;
        mSetTextConfig = setTextConfig;
        mSwipeConfig = swipeConfig;
        mValidationConfig = validationConfig;
    }

    public String getTaskName() {
        return mName;
    }

    public String getTaskType() {
        return mType;
    }

    public WorkflowTaskConfig getTaskConfig() {
        return mTaskConfig;
    }

    public int getRepeatCount() {
        return mRepeatCount;
    }

    public ScrollConfig getScrollConfig() {
        return mScrollConfig;
    }

    public SetTextConfig getSetTextConfig() {
        return mSetTextConfig;
    }

    public SwipeConfig getSwipeConfig() {
        return mSwipeConfig;
    }

    /**
     * Execute this task.
     *
     * @param workflowName The name of the workflow this task is part of
     * @param spectatioUiUtil Spectatio
     * @param fullConfig The full configuration this task is from, for retrieving UI Elements
     *     referenced by UI_ELEMENT_REFERENCE configuration
     */
    public void executeTask(
            String workflowName, SpectatioUiUtil spectatioUiUtil, SpectatioConfig fullConfig) {
        Log.i(
                LOG_TAG,
                String.format(
                        "Executing Task %s with Type %s for Workflow %s",
                        mName, mType, workflowName));

        SupportedWorkFlowTasks taskType =
                validateAndGetEnumValue(
                        workflowName, "Workflow Task Type", SupportedWorkFlowTasks.class, mType);

        int executionCount = 0;
        // Execute Task once by default. Repeat it again based on repeat count i.e. mRepeatCount > 0
        do {
            executeTask(taskType, workflowName, spectatioUiUtil, fullConfig);
            executionCount++;
            Log.i(
                    LOG_TAG,
                    String.format(
                            "Completed executing Task %s, %d time(s).", mName, executionCount));

            // Wait for 1 Second before executing another task
            spectatioUiUtil.wait1Second();
        } while (executionCount < (1 + mRepeatCount));

        Log.i(
                LOG_TAG,
                String.format(
                        "Done Executing Task %s with Type %s for Workflow %s",
                        mName, mType, workflowName));
    }

    private void executeTask(
            SupportedWorkFlowTasks taskType,
            String workflowName,
            SpectatioUiUtil spectatioUiUtil,
            SpectatioConfig fullConfig) {
        switch (taskType) {
            case COMMAND:
                validateAndExecuteCommand(workflowName, spectatioUiUtil);
                break;
            case HAS_PACKAGE_IN_FOREGROUND:
                validateAndVerifyPackage(workflowName, spectatioUiUtil);
                break;
            case HAS_UI_ELEMENT_IN_FOREGROUND:
                validateAndVerifyUiElement(workflowName, spectatioUiUtil, fullConfig);
                break;
            case CLICK:
                validateAndClickUiElement(
                        workflowName,
                        spectatioUiUtil,
                        fullConfig,
                        FindType.NONE,
                        /* isLongClick= */ false,
                        /* isOptional= */ false);
                break;
            case CLICK_IF_EXIST:
                validateAndClickUiElement(
                        workflowName,
                        spectatioUiUtil,
                        fullConfig,
                        FindType.NONE,
                        /* isLongClick= */ false,
                        /* isOptional= */ true);
                break;
            case LONG_CLICK:
                validateAndClickUiElement(
                        workflowName,
                        spectatioUiUtil,
                        fullConfig,
                        FindType.NONE,
                        /* isLongClick= */ true,
                        /* isOptional= */ false);
                break;
            case PRESS:
                validateAndPressKey(workflowName, spectatioUiUtil, /* isLongPress= */ false);
                break;
            case LONG_PRESS:
                validateAndPressKey(workflowName, spectatioUiUtil, /* isLongPress= */ true);
                break;
            case SCROLL_TO_FIND_AND_CLICK:
                validateAndClickUiElement(
                        workflowName,
                        spectatioUiUtil,
                        fullConfig,
                        FindType.SCROLL,
                        /* isLongClick= */ false,
                        /* isOptional= */ false);
                break;
            case SCROLL_TO_FIND_AND_CLICK_IF_EXIST:
                validateAndClickUiElement(
                        workflowName,
                        spectatioUiUtil,
                        fullConfig,
                        FindType.SCROLL,
                        /* isLongClick= */ false,
                        /* isOptional= */ true);
                break;
            case SCROLL_TO_FIND:
                validateUiElement(workflowName, spectatioUiUtil, fullConfig, FindType.SCROLL);
                break;
            case SET_TEXT:
                validateAndSetTextOfUiElement(workflowName, spectatioUiUtil, fullConfig);
                break;
            case SWIPE:
                validateAndSwipe(workflowName, spectatioUiUtil);
                break;
            case SWIPE_TO_FIND_AND_CLICK:
                validateAndClickUiElement(
                        workflowName,
                        spectatioUiUtil,
                        fullConfig,
                        FindType.SWIPE,
                        /* isLongClick= */ false,
                        /* isOptional= */ false);
                break;
            case SWIPE_TO_FIND_AND_CLICK_IF_EXIST:
                validateAndClickUiElement(
                        workflowName,
                        spectatioUiUtil,
                        fullConfig,
                        FindType.SWIPE,
                        /* isLongClick= */ false,
                        /* isOptional= */ true);
                break;
            case SWIPE_TO_FIND:
                validateUiElement(workflowName, spectatioUiUtil, fullConfig, FindType.SWIPE);
                break;
            case VALIDATE_VALUE:
                validateElementHasProperty(workflowName, spectatioUiUtil, fullConfig);
                break;
            case WAIT_MS:
                validateAndWait(workflowName, spectatioUiUtil);
                break;
            default:
                throwRuntimeException("Workflow Task Type", mType, workflowName, "Not Supported");
        }
    }

    private void validateElementHasProperty(
            String workflowName, SpectatioUiUtil spectatioUiUtil, SpectatioConfig fullConfig) {
        UiElement uiElement = validateAndGetTaskConfigUiElement(workflowName, fullConfig);
        BySelector selector = uiElement.getBySelectorForUiElement();
        List<UiObject2> uiObjects = spectatioUiUtil.findUiObjects(selector);
        if (uiObjects == null) {
            throw new IllegalStateException(
                    String.format("Could not find UI element for selector %s", selector));
        }

        ValidationConfig validationConfig = validateAndGetTaskValidationConfig(workflowName);
        String property = validationConfig.getProperty();
        String expected = validationConfig.getExpected();
        if (expected == null) {
            String expectedCommandLineKey = validationConfig.getExpectedCommandLineKey();
            expected = CommandLineParameters.getValue(expectedCommandLineKey, null);
        }

        for (UiObject2 uiObject : uiObjects) {
            if (spectatioUiUtil.validateUiObjectProperty(uiObject, property, expected)) {
                return;
            }
        }
        throwRuntimeException(
                "UI Object Property",
                property,
                workflowName,
                String.format("wrong value (should be %s)", expected));
    }

    private ValidationConfig validateAndGetTaskValidationConfig(String workflowName) {
        if (mValidationConfig == null) {
            throwRuntimeException(
                    "Config", "VALIDATION_CONFIG", workflowName, "Missing or Invalid");
        }
        return mValidationConfig;
    }

    private void validateAndWait(String workflowName, SpectatioUiUtil spectatioUiUtil) {
        String waitTime = validateAndGetTaskConfigText(workflowName);
        if (!isValidInteger(/* action= */ "WAIT_MS Value", waitTime, workflowName)) {
            throwRuntimeException("Wait", waitTime, workflowName, "Invalid");
        }
        spectatioUiUtil.waitNSeconds(Integer.parseInt(waitTime));
    }

    private void validateAndExecuteCommand(String workflowName, SpectatioUiUtil spectatioUiUtil) {
        String command = validateAndGetTaskConfigText(workflowName);
        spectatioUiUtil.executeShellCommand(command);
    }

    private void validateAndVerifyUiElement(
            String workflowName, SpectatioUiUtil spectatioUiUtil, SpectatioConfig fullConfig) {
        UiElement uiElement = validateAndGetTaskConfigUiElement(workflowName, fullConfig);
        BySelector selector = uiElement.getBySelectorForUiElement();
        if (!spectatioUiUtil.hasUiElement(selector)) {
            throwRuntimeException(
                    "UI Element", selector.toString(), workflowName, "Not in Foreground");
        }
    }

    private void validateAndVerifyPackage(String workflowName, SpectatioUiUtil spectatioUiUtil) {
        String pkg = validateAndGetTaskConfigText(workflowName);
        if (!spectatioUiUtil.hasPackageInForeground(pkg)) {
            throwRuntimeException("Package", pkg, workflowName, "Not in Foreground");
        }
    }

    private void validateAndSwipe(String workflowName, SpectatioUiUtil spectatioUiUtil) {
        SwipeConfig swipeConfig = validateAndGetTaskSwipeConfig(workflowName);
        SwipeDirection direction =
                validateAndGetEnumValue(
                        workflowName,
                        "Swipe Direction",
                        SwipeDirection.class,
                        swipeConfig.getSwipeDirection());
        SwipeFraction fraction =
                validateAndGetEnumValue(
                        workflowName,
                        "Swipe Fraction",
                        SwipeFraction.class,
                        swipeConfig.getSwipeFraction());
        spectatioUiUtil.swipe(direction, swipeConfig.getNumberOfSteps(), fraction);
    }

    private UiObject2 findUiElement(
            String workflowName,
            SpectatioUiUtil spectatioUiUtil,
            SpectatioConfig fullConfig,
            FindType howToFind) {
        UiElement uiElement = validateAndGetTaskConfigUiElement(workflowName, fullConfig);
        BySelector selector = uiElement.getBySelectorForUiElement();
        UiObject2 uiObject = spectatioUiUtil.findUiObject(selector);
        if (howToFind == FindType.SCROLL && !isValidUiObject(uiObject)) {
            ScrollConfig scrollConfig = validateAndGetTaskScrollConfig(workflowName);
            ScrollActions scrollAction =
                    validateAndGetEnumValue(
                            workflowName,
                            "Scroll Action",
                            ScrollActions.class,
                            scrollConfig.getScrollAction());
            try {
                switch (scrollAction) {
                    case USE_BUTTON:
                        BySelector forwardButtonSelector =
                                scrollConfig.getScrollForwardButton().getBySelectorForUiElement();
                        BySelector backwardButtonSelector =
                                scrollConfig.getScrollBackwardButton().getBySelectorForUiElement();
                        uiObject =
                                spectatioUiUtil.scrollAndFindUiObject(
                                        forwardButtonSelector, backwardButtonSelector, selector);
                        break;
                    case USE_GESTURE:
                        BySelector scrollElementSelector =
                                scrollConfig.getScrollElement().getBySelectorForUiElement();
                        Integer scrollMargin = Integer.valueOf(scrollConfig.getScrollMargin());
                        Integer scrollWaitTime = Integer.valueOf(scrollConfig.getScrollWaitTime());

                        ScrollDirection scrollDirection =
                                validateAndGetEnumValue(
                                        workflowName,
                                        "Scroll Direction",
                                        ScrollDirection.class,
                                        scrollConfig.getScrollDirection());
                        spectatioUiUtil.addScrollValues(scrollMargin, scrollWaitTime);
                        uiObject =
                                spectatioUiUtil.scrollAndFindUiObject(
                                        scrollElementSelector,
                                        selector,
                                        (scrollDirection == ScrollDirection.VERTICAL));
                        break;
                    default:
                        throwRuntimeException(
                                "Scroll Action",
                                scrollConfig.getScrollAction(),
                                workflowName,
                                "Not Supported");
                }
            } catch (MissingUiElementException ex) {
                throwRuntimeException(
                        "Scroll Button or Element for Scroll Action",
                        scrollConfig.getScrollAction(),
                        workflowName,
                        String.format("Missing. Error: %s", ex.getMessage()));
            }
        }
        if (howToFind == FindType.SWIPE && !isValidUiObject(uiObject)) {
            SwipeConfig swipeConfig = validateAndGetTaskSwipeConfig(workflowName);
            SwipeDirection swipeDirection =
                    validateAndGetEnumValue(
                            workflowName,
                            "Swipe Direction",
                            SwipeDirection.class,
                            swipeConfig.getSwipeDirection());
            SwipeFraction swipeFraction =
                    validateAndGetEnumValue(
                            workflowName,
                            "Swipe Fraction",
                            SwipeFraction.class,
                            swipeConfig.getSwipeFraction());
            uiObject =
                    spectatioUiUtil.swipeAndFindUiObject(
                            swipeDirection,
                            swipeConfig.getNumberOfSteps(),
                            swipeFraction,
                            selector);
        }
        return uiObject;
    }

    private void validateUiElement(
            String workflowName,
            SpectatioUiUtil spectatioUiUtil,
            SpectatioConfig fullConfig,
            FindType howToFind) {
        UiObject2 uiObject = findUiElement(workflowName, spectatioUiUtil, fullConfig, howToFind);
        validateUiObject(uiObject, workflowName);
    }

    private void validateAndClickUiElement(
            String workflowName,
            SpectatioUiUtil spectatioUiUtil,
            SpectatioConfig fullConfig,
            FindType howToFind,
            boolean isLongClick,
            boolean isOptional) {
        UiObject2 uiObject = findUiElement(workflowName, spectatioUiUtil, fullConfig, howToFind);
        if (isOptional && !isValidUiObject(uiObject)) {
            return;
        }
        validateUiObject(uiObject, workflowName);
        if (isLongClick) {
            spectatioUiUtil.longPress(uiObject);
        } else {
            spectatioUiUtil.clickAndWait(uiObject);
        }
    }

    private void validateAndSetTextOfUiElement(
            String workflowName, SpectatioUiUtil spectatioUiUtil, SpectatioConfig fullConfig) {
        UiElement element = validateAndGetTaskConfigUiElement(workflowName, fullConfig);
        BySelector selector = element.getBySelectorForUiElement();
        UiObject2 object = spectatioUiUtil.findUiObject(selector);

        String text = validateAndGetTaskSetText(workflowName);
        object.setText(text);
    }

    private void validateAndPressKey(
            String workflowName, SpectatioUiUtil spectatioUiUtil, boolean isLongPress) {
        String key = validateAndGetTaskConfigText(workflowName);
        // Check if Key is an integer i.e. KeyCode
        if (isValidInteger(/* action= */ "PRESS", key, workflowName)) {
            int keyCode = Integer.parseInt(key);
            if (isLongPress) {
                spectatioUiUtil.longPressKey(keyCode);
            } else {
                spectatioUiUtil.pressKeyCode(keyCode);
            }
            return;
        }
        switch (key) {
            case "POWER":
                if (isLongPress) {
                    spectatioUiUtil.longPressPower();
                } else {
                    spectatioUiUtil.pressPower();
                }
                break;
            case "HOME":
                if (isLongPress) {
                    throwRuntimeException("Long Press", key, workflowName, "Not Supported");
                } else {
                    spectatioUiUtil.pressHome();
                }
                break;
            case "BACK":
                if (isLongPress) {
                    throwRuntimeException("Long Press", key, workflowName, "Not Supported");
                } else {
                    spectatioUiUtil.pressBack();
                }
                break;
            case "SCREEN_CENTER":
                if (isLongPress) {
                    spectatioUiUtil.longPressScreenCenter();
                } else {
                    throwRuntimeException("Press", key, workflowName, "Not Supported");
                }
                break;
            case "WAKE_UP":
                if (isLongPress) {
                    throwRuntimeException("Long Press", key, workflowName, "Not Supported");
                } else {
                    spectatioUiUtil.wakeUp();
                }
                break;
            case "MENU":
                if (isLongPress) {
                    throwRuntimeException("Long Press", key, workflowName, "Not Supported");
                } else {
                    spectatioUiUtil.pressMenu();
                }
                break;
            case "SEARCH":
                if (isLongPress) {
                    throwRuntimeException("Long Press", key, workflowName, "Not Supported");
                } else {
                    spectatioUiUtil.pressSearch();
                }
                break;
            case "DPAD_CENTER":
                if (isLongPress) {
                    throwRuntimeException("Long Press", key, workflowName, "Not Supported");
                } else {
                    spectatioUiUtil.pressDPadCenter();
                }
                break;
            case "DPAD_DOWN":
                if (isLongPress) {
                    throwRuntimeException("Long Press", key, workflowName, "Not Supported");
                } else {
                    spectatioUiUtil.pressDPadDown();
                }
                break;
            case "DPAD_UP":
                if (isLongPress) {
                    throwRuntimeException("Long Press", key, workflowName, "Not Supported");
                } else {
                    spectatioUiUtil.pressDPadUp();
                }
                break;
            case "DPAD_LEFT":
                if (isLongPress) {
                    throwRuntimeException("Long Press", key, workflowName, "Not Supported");
                } else {
                    spectatioUiUtil.pressDPadLeft();
                }
                break;
            case "DPAD_RIGHT":
                if (isLongPress) {
                    throwRuntimeException("Long Press", key, workflowName, "Not Supported");
                } else {
                    spectatioUiUtil.pressDPadRight();
                }
                break;
            case "DELETE":
                if (isLongPress) {
                    throwRuntimeException("Long Press", key, workflowName, "Not Supported");
                } else {
                    spectatioUiUtil.pressDelete();
                }
                break;
            case "ENTER":
                if (isLongPress) {
                    throwRuntimeException("Long Press", key, workflowName, "Not Supported");
                } else {
                    spectatioUiUtil.pressEnter();
                }
                break;
            default:
                throwRuntimeException("Config", key, workflowName, "Not Supported");
        }
    }

    private boolean isValidInteger(String action, String value, String workflowName) {
        try {
            int intValue = Integer.parseInt(value);
            if (intValue < 0) {
                throwRuntimeException(action, value, workflowName, "Invalid");
            }
        } catch (NumberFormatException ex) {
            return false;
        }
        return true;
    }

    private boolean isValidUiObject(UiObject2 uiObject) {
        return uiObject != null;
    }

    private void validateUiObject(UiObject2 uiObject, String workflowName) {
        if (!isValidUiObject(uiObject)) {
            throwRuntimeException(
                    "UI Element for Config", "UI_ELEMENT", workflowName, "Missing on Device UI");
        }
    }

    private String validateAndGetTaskConfigText(String workflowName) {
        String taskConfigText = mTaskConfig.getText();
        if (Strings.isNullOrEmpty(taskConfigText)) {
            throwRuntimeException(
                    "Config Text", taskConfigText, workflowName, "Missing or Invalid");
        }
        return taskConfigText.trim();
    }

    private UiElement validateAndGetTaskConfigUiElement(
            String workflowName, SpectatioConfig fullConfig) {
        UiElement uiElement = mTaskConfig.getUiElement();
        String uiElementReference = mTaskConfig.getUiElementReference();
        if (uiElement == null && uiElementReference == null) {
            throwRuntimeException(
                    "Config",
                    "UI_ELEMENT or UI_ELEMENT_REFERENCE",
                    workflowName,
                    "Missing or Invalid");
        }
        if (uiElementReference != null) {
            uiElement = fullConfig.getUiElementFromConfig(uiElementReference);
        }
        return uiElement;
    }

    private ScrollConfig validateAndGetTaskScrollConfig(String workflowName) {
        if (mScrollConfig == null) {
            throwRuntimeException("Config", "SCROLL_CONFIG", workflowName, "Missing or Invalid");
        }
        return mScrollConfig;
    }

    private String validateAndGetTaskSetText(String workflowName) {
        SetTextConfig setTextConfig = validateAndGetTaskSetTextConfig(workflowName);

        String text = setTextConfig.getText();
        if (text != null) {
            return text;
        }

        String commandLineKey = setTextConfig.getCommandLineKey();
        if (commandLineKey == null) {
            throwRuntimeException(
                    "SET_TEXT_CONFIG",
                    "TEXT or COMMAND_LINE_KEY",
                    workflowName,
                    "Missing or Invalid");
        }
        return CommandLineParameters.getValue(commandLineKey, setTextConfig.getDefaultValue());
    }

    private SwipeConfig validateAndGetTaskSwipeConfig(String workflowName) {
        if (mSwipeConfig == null) {
            throwRuntimeException("Config", "SWIPE_CONFIG", workflowName, "Missing or Invalid");
        }
        return mSwipeConfig;
    }

    private SetTextConfig validateAndGetTaskSetTextConfig(String workflowName) {
        if (mSetTextConfig == null) {
            throwRuntimeException("Config", "SET_TEXT_CONFIG", workflowName, "Missing or Invalid");
        }
        return mSetTextConfig;
    }

    private <E extends Enum<E>> E validateAndGetEnumValue(
            String workflowName, String property, Class<E> enumClass, String value) {
        E enumValue = null;
        try {
            enumValue = Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException ex) {
            throwRuntimeException(property, value, workflowName, "Not Supported");
        }
        return enumValue;
    }

    private void throwRuntimeException(
            String property, String value, String workflowName, String reason) {
        throw new RuntimeException(
                String.format(
                        "%s %s for task %s with type %s in Workflow %s is %s.",
                        property, value, mName, mType, workflowName, reason));
    }
}

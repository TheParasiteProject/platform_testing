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
package android.platform.helpers;

import androidx.test.uiautomator.UiObject2;

/** An App Helper interface for Google Slides. */
public interface IGoogleSlidesHelper extends IAppHelper {
    /**
     * Setup expectation: Google Slides is opened.
     *
     * <p>This method will open slides file from shared with me drawer.
     *
     * @throws UnknownUiException If the slides file cannot be opened.
     */
    void openSlidesFromSharedMenu();

    /**
     * Setup expectation: Google Slides is opened.
     *
     * <p>Check if device is now in the Slides page.
     *
     * @return Returns true if device is in the Slides page, false if not.
     */
    boolean isOnSlidesPage();

    /**
     * Setup expectation: Google Slides is opened and is in the Slides page.
     *
     * <p>This method will get a UiObject2 object for Slides page container
     */
    UiObject2 getScrollView();

    /**
     * Setup expectation: Google Slides is opened and is in the Slides page.
     *
     * <p>This method will close slides file.
     */
    void closeSlides();

    /**
     * Setup expectation: Google Slides is open.
     *
     * <p>This method will open slides file by its title.
     *
     * @param title The title text of the slides file to open.
     * @throws UnknownUiException If the slides file cannot be opened.
     */
    void openSlidesByTitle(String title);
}

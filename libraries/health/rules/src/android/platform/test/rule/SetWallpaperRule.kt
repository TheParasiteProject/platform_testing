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

package android.platform.test.rule

import android.app.WallpaperManager
import android.content.ComponentName
import android.service.wallpaper.WallpaperService
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.Description

/**
 * Sets system wallpaper to the specified WallpaperService and resets it after the test
 */
class SetWallpaperRule<T : WallpaperService>(wallpaperServiceClass: Class<T>) : TestWatcher() {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val wallpaperManager =
        instrumentation.context.getSystemService(WallpaperManager::class.java)
    private val wallpaperComponentName =
        ComponentName(instrumentation.context, wallpaperServiceClass)

    override fun starting(description: Description?) {
        wallpaperManager.setWallpaperComponent(wallpaperComponentName)
    }

    override fun finished(description: Description?) {
        wallpaperManager.clearWallpaper()
    }
}

inline fun <reified T : WallpaperService> setWallpaperRule(): SetWallpaperRule<T> =
    SetWallpaperRule(T::class.java)
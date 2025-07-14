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

#pragma once

#include <aidl/android/hardware/graphics/common/Rect.h>
#include <aidl/android/hardware/graphics/composer3/Color.h>

#include <cstdint>
#include <initializer_list>
#include <string>
#include <vector>

namespace hcct {

// Struct to describe a layer layout for testing HWC composition.
struct HwcTestLayer {
  std::string name = "";  // Optional name for the layer, useful for debugging.
  aidl::android::hardware::graphics::common::Rect
      display_frame;  // The position and size of the layer on the display.
  aidl::android::hardware::graphics::composer3::Color
      color;  // The solid color for the layer.
  int z_order = 0;
};

// Struct holding a list of HwcTestLayers to describe a scene layout used for
// testing HWC composition.
class HwcTestScene {
 public:
  std::vector<HwcTestLayer> layers;  // A collection of layers in this scene.

  HwcTestScene(std::initializer_list<HwcTestLayer> initial_layers)
      : layers(initial_layers) {}
};

}  // namespace hcct

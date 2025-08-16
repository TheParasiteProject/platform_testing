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

#include <gtest/gtest.h>
#include <log/log.h>

#include <chrono>
#include <cstdint>
#include <iostream>

#include "Readback.h"
#include "hwc_test_scene.h"
#include "hwc_tester.h"

using hcct::common::PixelFormat;

// ostream operators for composer3 types. These must be in the same namespace
// for gtest to find them.
namespace aidl::android::hardware::graphics::composer3 {
static std::ostream& operator<<(std::ostream& os, const CommandError& error) {
  os << error.toString();
  return os;
}
static std::ostream& operator<<(std::ostream& os, Composition composition) {
  os << toString(composition);
  return os;
}
}  // namespace aidl::android::hardware::graphics::composer3

// For checking results of Validate.
struct ChangedLayer {
  std::string layer;
  Composition composition;

  bool operator==(const ChangedLayer& other) const = default;
};

static std::ostream& operator<<(std::ostream& os,
                                const ChangedLayer& changed_layer) {
  os << "ChangedLayer{"
     << "layer: " << changed_layer.layer << ", "
     << "composition: " << changed_layer.composition << "}";
  return os;
}

class CompositionTest : public ::testing::Test {
 protected:
  void SetUp() override {
    mHwcTester = std::make_unique<hcct::HwcTester>();
    auto displayIds = mHwcTester->GetAllDisplayIds();
    ASSERT_FALSE(displayIds.empty()) << "No display.";
    // Use the first display.
    mDisplayId = displayIds.front();
    // Get the display size.
    std::tie(mDisplayWidth, mDisplayHeight) =
        mHwcTester->GetActiveDisplaySize(mDisplayId);
    ASSERT_GT(mDisplayWidth, 0) << "Invalid width for display.";
    ASSERT_GT(mDisplayHeight, 0) << "Invalid height for display.";

    ASSERT_TRUE(mHwcTester->GetClientWrapper()
                    .setPowerMode(mDisplayId, PowerMode::ON)
                    .isOk());
  }

  void CreateScene(const hcct::HwcTestScene& scene) {
    ASSERT_TRUE(mScene.empty()) << "Scene already created.";
    for (auto& layer : scene.layers) {
      int width = layer.display_frame.right - layer.display_frame.left;
      int height = layer.display_frame.bottom - layer.display_frame.top;
      ASSERT_GT(width, 0) << "Invalid width for layer: " << layer.name;
      ASSERT_GT(height, 0) << "Invalid height for layer: " << layer.name;

      // Create a TestBufferLayer which will create and own an RGBA_8888 buffer
      // which is used for DEVICE composition.
      auto testBufferLayer =
          mHwcTester->CreateBufferLayer(mDisplayId, width, height);
      testBufferLayer->setZOrder(layer.z_order);
      testBufferLayer->setBuffer(
          {static_cast<size_t>(width * height), layer.color});
      testBufferLayer->setDisplayFrame(layer.display_frame);
      testBufferLayer->setBlendMode(BlendMode::PREMULTIPLIED);

      mLayerNames.emplace(testBufferLayer->getLayer(), layer.name);
      mScene.emplace_back(std::move(testBufferLayer));
    }
  }

  auto GetTestLayers() {
    std::vector<hcct::libhwc_aidl_test::TestLayer*> layers(mScene.size());
    std::transform(
        mScene.begin(), mScene.end(), layers.begin(),
        [](const std::unique_ptr<hcct::libhwc_aidl_test::TestBufferLayer>&
               layer) { return layer.get(); });
    return layers;
  }

  auto TakeChangedLayers(ComposerClientReader& reader) {
    auto changed_layers = reader.takeChangedCompositionTypes(mDisplayId);
    std::vector<ChangedLayer> named_changed_layers(changed_layers.size());
    std::transform(changed_layers.begin(), changed_layers.end(),
                   named_changed_layers.begin(),
                   [this](const ChangedCompositionLayer& changed_layer) {
                     return ChangedLayer{
                         .layer = mLayerNames[changed_layer.layer],
                         .composition = changed_layer.composition};
                   });
    return named_changed_layers;
  }

  auto TakePresentFence(ComposerClientReader& reader) {
    return ::android::sp<::android::Fence>::make(
        reader.takePresentFence(mDisplayId).release());
  }

  int64_t mDisplayId = 0;
  int mDisplayWidth = 0;
  int mDisplayHeight = 0;

  std::unique_ptr<hcct::HwcTester> mHwcTester;
  std::vector<std::unique_ptr<hcct::libhwc_aidl_test::TestBufferLayer>> mScene;
  std::map<int64_t, std::string> mLayerNames;

  // Number of frames to present for tests. Increase this if needed to inspect
  // the output or probe device state during a test.
  static constexpr int kNumFrames = 30;
};

TEST_F(CompositionTest, SingleLayer) {
  hcct::HwcTestScene testScene = {
      // Red background, opaque
      {"Background", Rect(0, 0, mDisplayWidth, mDisplayHeight),
       Color{1.0f, 0.0f, 0.0f, 1.0f}, 0},
  };
  ASSERT_NO_FATAL_FAILURE(CreateScene(testScene));

  for (int i = 0; i < kNumFrames; ++i) {
    {
      auto reader = mHwcTester->Validate(mDisplayId, GetTestLayers());
      ASSERT_TRUE(reader.has_value());
      EXPECT_EQ(reader->takeErrors(), std::vector<CommandError>());
      EXPECT_EQ(TakeChangedLayers(*reader), std::vector<ChangedLayer>());
    }

    {
      auto reader = mHwcTester->Present(mDisplayId);
      ASSERT_TRUE(reader.has_value());
      EXPECT_EQ(reader->takeErrors(), std::vector<CommandError>());
      auto fence = TakePresentFence(*reader);
      EXPECT_EQ(fence->wait(100), ::android::OK);
    }
  }
}

TEST_F(CompositionTest, AppWithStatusBar) {
  hcct::HwcTestScene testScene = {
      // Red background, opaque
      {"Background", Rect(0, 0, mDisplayWidth, mDisplayHeight),
       Color{1.0f, 0.0f, 0.0f, 1.0f}, 0},
      // Blue app UI, opaque, 200x200 size at position 500,500
      {"App", Rect(500, 500, 700, 700), Color{0.0f, 0.0f, 1.0f, 1.0f}, 1},
      // Green status bar, opaque, 50 pixels high from top of screen.
      {"Status Bar", Rect(0, 0, mDisplayWidth, 50),
       Color{0.0f, 1.0f, 0.0f, 1.0f}, 3},
  };
  ASSERT_NO_FATAL_FAILURE(CreateScene(testScene));

  for (int i = 0; i < kNumFrames; ++i) {
    {
      auto reader = mHwcTester->Validate(mDisplayId, GetTestLayers());
      ASSERT_TRUE(reader.has_value());
      EXPECT_EQ(reader->takeErrors(), std::vector<CommandError>());
      EXPECT_EQ(TakeChangedLayers(*reader), std::vector<ChangedLayer>());
    }

    {
      auto reader = mHwcTester->Present(mDisplayId);
      ASSERT_TRUE(reader.has_value());
      EXPECT_EQ(reader->takeErrors(), std::vector<CommandError>());
      auto fence = TakePresentFence(*reader);
      EXPECT_EQ(fence->wait(100), ::android::OK);
    }
  }
}

TEST_F(CompositionTest, OverlapWithAlpha) {
  hcct::HwcTestScene testScene = {
      // Red background, opaque
      {"Background", Rect(0, 0, mDisplayWidth, mDisplayHeight),
       Color{1.0f, 0.0f, 0.0f, 1.0f}, 0},
      // Blue app UI, semi-transparent, 200x200 size at position 500,500
      {"App 1", Rect(500, 500, 700, 700), Color{0.0f, 0.0f, 1.0f, 0.7f}, 1},
      // Cyan app UI, semi-transparent, 200x200 size at position 550,550
      {"App Overlap", Rect(550, 550, 750, 750), Color{0.0f, 1.0f, 1.0f, 0.7f},
       2},
      // Green status bar, opaque, 50 pixels high from top of screen.
      {"Status Bar", Rect(0, 0, mDisplayWidth, 50),
       Color{0.0f, 1.0f, 0.0f, 1.0f}, 3},
  };
  ASSERT_NO_FATAL_FAILURE(CreateScene(testScene));

  for (int i = 0; i < kNumFrames; ++i) {
    {
      auto reader = mHwcTester->Validate(mDisplayId, GetTestLayers());
      ASSERT_TRUE(reader.has_value());
      EXPECT_EQ(reader->takeErrors(), std::vector<CommandError>());
      EXPECT_EQ(TakeChangedLayers(*reader), std::vector<ChangedLayer>());
    }

    {
      auto reader = mHwcTester->Present(mDisplayId);
      ASSERT_TRUE(reader.has_value());
      EXPECT_EQ(reader->takeErrors(), std::vector<CommandError>());
      auto fence = TakePresentFence(*reader);
      EXPECT_EQ(fence->wait(100), ::android::OK);
    }
  }
}
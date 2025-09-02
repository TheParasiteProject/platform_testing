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

#include <ComposerClientWrapper.h>
#include <aidl/android/hardware/graphics/common/DisplayHotplugEvent.h>
#include <unistd.h>

#include <memory>
#include <unordered_map>
#include <vector>

#include "Readback.h"
#include "RenderEngine.h"

namespace hcct {

namespace libhwc_aidl_test =
    ::aidl::android::hardware::graphics::composer3::libhwc_aidl_test;
namespace common = ::aidl::android::hardware::graphics::common;

/*
 * HwcTester is a class that provides an interface to interact with the
 * HWC AIDL through libhwc_aidl_test. It's not just an interface to the HWC
 * AIDL, but also provides some helper functions to make it easier to write
 * tests.
 */

class HwcTester {
public:
  HwcTester();
  ~HwcTester();

  std::vector<int64_t> GetAllDisplayIds() const;
  std::vector<std::pair<int64_t, common::DisplayHotplugEvent>>
  getAndClearLatestHotplugs();
  bool DrawSolidColorToScreen(int64_t display_id, Color color);

  std::pair<int, int> GetActiveDisplaySize(int64_t displayId);

  libhwc_aidl_test::ComposerClientWrapper& GetClientWrapper() {
    return *mComposerClient;
  }

  std::unique_ptr<libhwc_aidl_test::TestBufferLayer> CreateBufferLayer(
      int64_t displayId, uint64_t width, uint64_t height);

  std::optional<ComposerClientReader> Validate(
      int64_t displayId,
      const std::vector<libhwc_aidl_test::TestLayer*>& layers);

  std::optional<ComposerClientReader> Present(int64_t displayId);

 private:
  std::vector<DisplayConfiguration> GetDisplayConfigs(int64_t display_id);
  DisplayConfiguration GetDisplayActiveConfigs(int64_t display_id);
  ComposerClientWriter &GetWriter(int64_t display_id);

  std::unique_ptr<libhwc_aidl_test::ComposerClientWrapper> mComposerClient;
  std::unordered_map<int64_t, libhwc_aidl_test::DisplayWrapper> mDisplays;
  std::unordered_map<int64_t, ComposerClientWriter> mWriters;
  std::unique_ptr<libhwc_aidl_test::TestRenderEngine> mRenderEngine;
};

} // namespace hcct

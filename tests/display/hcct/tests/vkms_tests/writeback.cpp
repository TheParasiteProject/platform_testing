
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
#include "hwc_tester.h"
#include "vkms_tester.h"
#include <gtest/gtest.h>
#include <log/log.h>

namespace {

class VkmsWritebackTest : public ::testing::Test {
protected:
  void SetUp() override {
    auto builder = hcct::VkmsTester::VkmsConnectorBuilder::create();
    builder.withType(hcct::VkmsTester::ConnectorType::keDP)
        .withMonitor(hcct::edid::EdpMonitorName::REDRIX);
    mVkmsTester = hcct::VkmsTester::CreateWithBuilders({std::move(builder)});
    ASSERT_TRUE(mVkmsTester) << "Failed to create VkmsTester";

    // Create HwcTester after VkmsTester is successfully initialized
    mHwcTester = std::make_unique<hcct::HwcTester>();
    if (!mHwcTester) {
      // Clean up VkmsTester if HwcTester creation fails
      mVkmsTester.reset();
      FAIL() << "Failed to create HwcTester";
    }

    // Writeback has a known issue on teardown.
    // Let's prevent it from cleaning up and reboot the DUT after the test
    // (through the XML file)
    // TODO(b/412666164): Remove this when the fix makes its way to the
    // prebuilts.
    mVkmsTester->DisableCleanupOnDestruction();
  }

  void TearDown() override {
    mHwcTester.reset();
    mVkmsTester.reset();
  }

  std::unique_ptr<hcct::VkmsTester> mVkmsTester;
  std::unique_ptr<hcct::HwcTester> mHwcTester;
};

TEST_F(VkmsWritebackTest, SimpleSolidColorReadback) {
  for (const libhwc_aidl_test::DisplayWrapper &display :
       mHwcTester->GetDisplays()) {

    auto readbackBuffer = mHwcTester->SetReadbackBufferToDisplaySize(display);
    ASSERT_TRUE(readbackBuffer.has_value())
        << "Failed to set readback buffer for display "
        << display.getDisplayId();

    std::vector<Color> expectedColors = mHwcTester->CreateColorVector(
        display.getDisplayId(), libhwc_aidl_test::RED);
    mHwcTester->DrawColorVectorToDisplay(display.getDisplayId(),
                                         expectedColors);

    EXPECT_NO_FATAL_FAILURE(
        readbackBuffer.value().checkReadbackBuffer(expectedColors, true));
  }
}
} // namespace

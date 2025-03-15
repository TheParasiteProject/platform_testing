/*
 * Copyright (C) 202kMaxRetries The Android Open Source Project
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
#include <unordered_set>

namespace {

constexpr int kMaxRetries = 5;
constexpr int kInitialSetupConnectors = 2;

class VkmsHotplugTest : public ::testing::Test {
protected:
  void SetUp() override {
    mVkmsTester =
        hcct::VkmsTester::CreateWithGenericConnectors(kInitialSetupConnectors);
    ASSERT_TRUE(mVkmsTester) << "Failed to create VkmsTester";
    mHwcTester = std::make_unique<hcct::HwcTester>();
  }

  void TearDown() override {
    mHwcTester.reset();
    mVkmsTester.reset();
  }

  std::unique_ptr<hcct::VkmsTester> mVkmsTester;
  std::unique_ptr<hcct::HwcTester> mHwcTester;
};

TEST_F(VkmsHotplugTest, DetectSingleHotplugs) {
  std::unordered_set<int64_t> hotpluggedDisplays;

  for (int i = 0; i < kInitialSetupConnectors; ++i) {
    mVkmsTester->ToggleConnector(i, false);
    mHwcTester->getAndClearLatestHotplugs();
    ASSERT_TRUE(mHwcTester->getAndClearLatestHotplugs().empty())
        << "Hotplugs should have been cleared";

    mVkmsTester->ToggleConnector(i, true);

    int retryCount = 0;
    std::vector<std::pair<int64_t, hcct::common::DisplayHotplugEvent>>
        receivedHotplugs;
    while ((receivedHotplugs.empty()) && retryCount++ < kMaxRetries) {
      sleep(1);
      receivedHotplugs = mHwcTester->getAndClearLatestHotplugs();
    }
    EXPECT_EQ(1, receivedHotplugs.size())
        << "Hotplug event not received for connector " << i << " after "
        << retryCount << " seconds";
    EXPECT_EQ(receivedHotplugs[0].second,
              hcct::common::DisplayHotplugEvent::CONNECTED)
        << "Received Disconnect event instead of Connect event for connector "
        << i;

    hotpluggedDisplays.insert(receivedHotplugs[0].first);
  }

  EXPECT_EQ(kInitialSetupConnectors, hotpluggedDisplays.size())
      << "Expected " << kInitialSetupConnectors
      << " unique hotplug events, but got " << hotpluggedDisplays.size();
}

TEST_F(VkmsHotplugTest, DetectSingleDisconnectHotplugs) {
  std::unordered_set<int64_t> hotpluggedDisplays;

  for (int i = 0; i < kInitialSetupConnectors; ++i) {
    mHwcTester->getAndClearLatestHotplugs();
    mVkmsTester->ToggleConnector(i, true);
    // Leave time for the hotplug event to be processed and clear the hotplug
    int retryCount = 0;
    std::vector<std::pair<int64_t, hcct::common::DisplayHotplugEvent>>
        receivedHotplugs;
    while ((receivedHotplugs.empty()) && retryCount++ < kMaxRetries) {
      sleep(1);
      receivedHotplugs = mHwcTester->getAndClearLatestHotplugs();
    }
    ASSERT_EQ(receivedHotplugs.size(), 1)
        << "Hotplug event not received for connector " << i << " after "
        << retryCount << " seconds";
    ASSERT_TRUE(mHwcTester->getAndClearLatestHotplugs().empty())
        << "Hotplugs should have been cleared";

    mVkmsTester->ToggleConnector(i, false);

    retryCount = 0;
    receivedHotplugs.clear();
    while ((receivedHotplugs.empty()) && retryCount++ < kMaxRetries) {
      sleep(1);
      receivedHotplugs = mHwcTester->getAndClearLatestHotplugs();
    }
    EXPECT_EQ(1, receivedHotplugs.size())
        << "Hotplug event not received for connector " << i << " after "
        << retryCount << " seconds";
    if (receivedHotplugs.empty()) {
      continue;
    }

    EXPECT_EQ(receivedHotplugs[0].second,
              hcct::common::DisplayHotplugEvent::DISCONNECTED)
        << "Received Connect event instead of Disconnect event for connector "
        << i;

    hotpluggedDisplays.insert(receivedHotplugs[0].first);
  }

  EXPECT_EQ(kInitialSetupConnectors, hotpluggedDisplays.size())
      << "Expected " << kInitialSetupConnectors
      << " unique hotplug events, but got " << hotpluggedDisplays.size();
}

TEST_F(VkmsHotplugTest, DetectMultipleHotplugs) {
  for (int i = 0; i < kInitialSetupConnectors; ++i) {
    mVkmsTester->ToggleConnector(i, false);
  }
  mHwcTester->getAndClearLatestHotplugs();
  ASSERT_TRUE(mHwcTester->getAndClearLatestHotplugs().empty())
      << "Hotplugs should have been cleared";

  for (int i = 0; i < kInitialSetupConnectors; ++i) {
    mVkmsTester->ToggleConnector(i, true);
  }

  int retryCount = 0;
  std::vector<std::pair<int64_t, hcct::common::DisplayHotplugEvent>>
      receivedHotplugs;
  while ((receivedHotplugs.empty()) && retryCount++ < kMaxRetries) {
    sleep(1);
    receivedHotplugs = mHwcTester->getAndClearLatestHotplugs();
  }
  EXPECT_EQ(kInitialSetupConnectors, receivedHotplugs.size())
      << "Hotplug event not received after kMaxRetries seconds";

  std::unordered_set<int64_t> hotpluggedDisplays;
  for (const auto &hotplug : receivedHotplugs) {
    EXPECT_EQ(hotplug.second, hcct::common::DisplayHotplugEvent::CONNECTED)
        << "Received Disconnect event instead of Connect event for connector "
        << hotplug.first;
    hotpluggedDisplays.insert(hotplug.first);
  }
  EXPECT_EQ(kInitialSetupConnectors, hotpluggedDisplays.size())
      << "Expected " << kInitialSetupConnectors << " unique hotplug"
      << " events, but got " << hotpluggedDisplays.size();
}

} // namespace

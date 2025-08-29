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
#include <inttypes.h>
#include <log/log.h>

namespace hcct {

namespace {
static constexpr uint32_t kBufferSlotCount = 64;
} // namespace

HwcTester::HwcTester() {
  // Add thread pool configuration for Binder IPC
  android::ProcessState::self()->setThreadPoolMaxThreadCount(4);
  android::ProcessState::self()->startThreadPool();

  mComposerClient = std::make_shared<libhwc_aidl_test::ComposerClientWrapper>(
      IComposer::descriptor + std::string("/default"));
  if (!mComposerClient) {
    ALOGE("Failed to create HWC client");
  }

  if (!mComposerClient->createClient().isOk()) {
    ALOGE("Failed to create HWC client connection");
  }

  const auto &[status, displays] = mComposerClient->getDisplays();
  if (!status.isOk() || displays.empty()) {
    ALOGE("Failed to get displays");
    return;
  }

  for (const auto &display : displays) {
    mDisplays.emplace(display.getDisplayId(), std::move(display));
  }

  mRenderEngine = std::unique_ptr<libhwc_aidl_test::TestRenderEngine>(
      new libhwc_aidl_test::TestRenderEngine(
          ::android::renderengine::RenderEngineCreationArgs::Builder()
              .setPixelFormat(static_cast<int>(PixelFormat::RGBA_8888))
              .setImageCacheSize(libhwc_aidl_test::TestRenderEngine::
                                     sMaxFrameBufferAcquireBuffers)
              .setContextPriority(
                  ::android::renderengine::RenderEngine::ContextPriority::High)
              .build()));
}

HwcTester::~HwcTester() {
  std::unordered_map<int64_t, ComposerClientWriter *> displayWriters;
  for (const auto &[id, _] : mDisplays) {
    displayWriters.emplace(id, &GetWriter(id));
  }
  mComposerClient->tearDown(displayWriters);
  mComposerClient.reset();
}

std::vector<libhwc_aidl_test::DisplayWrapper> HwcTester::GetDisplays() const {
  std::vector<libhwc_aidl_test::DisplayWrapper> displays;
  for (const auto &[_, display] : mDisplays) {
    displays.push_back(display);
  }
  return displays;
}

std::vector<int64_t> HwcTester::GetDisplayIds() const {
  std::vector<int64_t> displayIds(mDisplays.size());

  for (const auto &[id, _] : mDisplays) {
    displayIds.push_back(id);
  }

  return displayIds;
}

std::vector<std::pair<int64_t, common::DisplayHotplugEvent>>
HwcTester::getAndClearLatestHotplugs() {
  return mComposerClient->getAndClearLatestHotplugs();
}

std::optional<libhwc_aidl_test::ReadbackBuffer>
HwcTester::SetReadbackBufferToDisplaySize(
    const libhwc_aidl_test::DisplayWrapper &display) {
  auto [status, readbackAttrs] =
      mComposerClient->getReadbackBufferAttributes(display.getDisplayId());
  if (!status.isOk()) {
    ALOGE("Failed to get readback buffer attributes for display %" PRId64,
          display.getDisplayId());
    return std::nullopt;
  }

  common::PixelFormat readbackFormat = readbackAttrs.format;
  common::Dataspace readbackDataspace = readbackAttrs.dataspace;

  if (!libhwc_aidl_test::ReadbackHelper::readbackSupported(readbackFormat,
                                                           readbackDataspace)) {
    ALOGE("Readback buffer format/dataspace not supported for display %" PRId64,
          display.getDisplayId());
    return std::nullopt;
  }

  libhwc_aidl_test::ReadbackBuffer readbackBuffer(
      display.getDisplayId(), mComposerClient, display.getDisplayWidth(),
      display.getDisplayHeight(), readbackFormat, readbackDataspace);
  EXPECT_NO_FATAL_FAILURE(readbackBuffer.setReadbackBuffer());
  return readbackBuffer;
}

std::vector<DisplayConfiguration>
HwcTester::GetDisplayConfigs(int64_t displayId) const {
  const auto &[configStatus, configs] =
      mComposerClient->getDisplayConfigurations(displayId);
  if (!configStatus.isOk() || configs.empty()) {
    ALOGE("Failed to get display configs for display %" PRId64, displayId);
  }

  return configs;
}

DisplayConfiguration
HwcTester::GetDisplayActiveConfigs(int64_t displayId) const {
  auto [activeConfigStatus, activeConfig] =
      mComposerClient->getActiveConfig(displayId);
  if (!activeConfigStatus.isOk()) {
    ALOGE("Failed to get active config for display %" PRId64, displayId);
    return {};
  }

  DisplayConfiguration displayConfig;
  const auto &configs = GetDisplayConfigs(displayId);
  for (const auto &config : configs) {
    if (config.configId == activeConfig) {
      return config;
    }
  }

  ALOGE("Active config was not found in configs for display %" PRId64,
        displayId);
  return {};
}

bool HwcTester::DrawSolidColorToScreen(int64_t displayId, Color color) {
  // Create a layer for solid color
  const auto &[status, layer] =
      mComposerClient->createLayer(displayId, kBufferSlotCount, nullptr);
  if (!status.isOk()) {
    ALOGE("Failed to create layer on display %" PRId64, displayId);
    return false;
  }

  // Create a writer for the display commands
  auto &writer = GetWriter(displayId);

  // Set layer properties
  writer.setLayerCompositionType(displayId, layer, Composition::SOLID_COLOR);
  writer.setLayerPlaneAlpha(displayId, layer, color.a);
  writer.setLayerColor(displayId, layer, color);

  DisplayConfiguration displayConfig = GetDisplayActiveConfigs(displayId);
  writer.setLayerDisplayFrame(
      displayId, layer, Rect{0, 0, displayConfig.width, displayConfig.height});
  writer.setLayerZOrder(displayId, layer, 0);

  // Validate and present display
  writer.validateDisplay(displayId, ComposerClientWriter::kNoTimestamp, 0);
  writer.presentDisplay(displayId);

  // Execute the commands
  auto commands = writer.takePendingCommands();
  std::pair<ScopedAStatus, std::vector<CommandResultPayload>> executeRes =
      mComposerClient->executeCommands(commands);
  return executeRes.first.isOk();
}

std::pair<int, int> HwcTester::GetActiveDisplaySize(int64_t display_id) const {
  DisplayConfiguration displayConfig = GetDisplayActiveConfigs(display_id);
  return {displayConfig.width, displayConfig.height};
}

ComposerClientWriter &HwcTester::GetWriter(int64_t display) {
  auto [it, _] = mWriters.try_emplace(display, display);
  return it->second;
}

std::unique_ptr<libhwc_aidl_test::TestBufferLayer> HwcTester::CreateBufferLayer(
    int64_t displayId, uint64_t width, uint64_t height) {
  return std::make_unique<hcct::libhwc_aidl_test::TestBufferLayer>(
      *mComposerClient, *mRenderEngine, displayId, width, height,
      hcct::common::PixelFormat::RGBA_8888, GetWriter(displayId),
      Composition::DEVICE);
}

std::optional<ComposerClientReader> HwcTester::Validate(
    int64_t displayId,
    const std::vector<libhwc_aidl_test::TestLayer *> &layers) {
  auto &writer = GetWriter(displayId);
  for (auto &layer : layers) {
    layer->write(writer);
  }
  writer.validateDisplay(displayId, ComposerClientWriter::kNoTimestamp, 0);

  // Execute the commands.
  auto commands = writer.takePendingCommands();
  std::pair<ScopedAStatus, std::vector<CommandResultPayload>> executeRes =
      mComposerClient->executeCommands(commands);
  if (!executeRes.first.isOk()) {
    return std::nullopt;
  }

  ComposerClientReader reader(displayId);
  reader.parse(std::move(executeRes.second));
  return reader;
}

std::optional<ComposerClientReader> HwcTester::Present(int64_t displayId) {
  auto &writer = GetWriter(displayId);
  writer.presentDisplay(displayId);

  // Execute the commands
  auto commands = writer.takePendingCommands();
  std::pair<ScopedAStatus, std::vector<CommandResultPayload>> executeRes =
      mComposerClient->executeCommands(commands);
  if (!executeRes.first.isOk()) {
    return std::nullopt;
  }

  ComposerClientReader reader(displayId);
  reader.parse(std::move(executeRes.second));
  return reader;
}

std::vector<Color> HwcTester::CreateColorVector(int64_t displayId,
                                                Color color) const {
  const libhwc_aidl_test::DisplayWrapper &display = mDisplays.at(displayId);

  // Create and fill a solid color buffer
  std::vector<Color> colors(static_cast<size_t>(display.getDisplayWidth() *
                                                display.getDisplayHeight()),
                            color);
  return colors;
}

void HwcTester::DrawColorVectorToDisplay(int64_t displayId,
                                         const std::vector<Color> &colors) {
  libhwc_aidl_test::DisplayWrapper &display = mDisplays.at(displayId);

  // Validate that the color vector size matches the expected display dimensions
  size_t expectedSize = static_cast<size_t>(display.getDisplayWidth() *
                                            display.getDisplayHeight());
  if (colors.size() != expectedSize) {
    ALOGE("Color vector size mismatch for display %" PRId64
          ": expected %zu, got %zu",
          displayId, expectedSize, colors.size());
    return;
  }

  libhwc_aidl_test::DisplayProperties displayProps =
      libhwc_aidl_test::ReadbackHelper::setupDisplayProperty(display,
                                                             mComposerClient);

  // Create a buffer layer with solid color content using DEVICE composition
  // (compatible with drm_hwcomposer)
  auto layer = std::make_shared<libhwc_aidl_test::TestBufferLayer>(
      *mComposerClient, *displayProps.testRenderEngine, display.getDisplayId(),
      display.getDisplayWidth(), display.getDisplayHeight(),
      displayProps.pixelFormat, displayProps.writer, Composition::DEVICE);
  layer->setDisplayFrame(
      {0, 0, display.getDisplayWidth(), display.getDisplayHeight()});
  layer->setSourceCrop({0, 0, static_cast<float>(display.getDisplayWidth()),
                        static_cast<float>(display.getDisplayHeight())});
  layer->setZOrder(10);
  layer->setDataspace(displayProps.dataspace);

  EXPECT_NO_FATAL_FAILURE(layer->setBuffer(colors));

  std::vector<std::shared_ptr<libhwc_aidl_test::TestLayer>> layers = {layer};
  layer->write(displayProps.writer);
  Execute(displayProps);
  // Check for errors before validating.
  auto errors = displayProps.reader.takeErrors();
  for (const auto &error : errors) {
    ALOGE("writeLayers error: %s", error.toString().c_str());
  }
  EXPECT_TRUE(errors.empty());

  displayProps.writer.validateDisplay(
      display.getDisplayId(), ComposerClientWriter::kNoTimestamp,
      libhwc_aidl_test::ComposerClientWrapper::kNoFrameIntervalNs);
  Execute(displayProps);
  auto validateErrors = displayProps.reader.takeErrors();
  for (const auto &error : validateErrors) {
    ALOGE("validateDisplay error: %s", error.toString().c_str());
  }
  EXPECT_TRUE(validateErrors.empty());

  // Verify that the HWC is happy with the composition type
  auto changedCompositionTypes =
      displayProps.reader.takeChangedCompositionTypes(display.getDisplayId());
  EXPECT_TRUE(changedCompositionTypes.empty());

  displayProps.writer.presentDisplay(display.getDisplayId());
  Execute(displayProps);

  EXPECT_TRUE(displayProps.reader.takeErrors().empty());
}

bool HwcTester::Execute(libhwc_aidl_test::DisplayProperties &displayProps) {
  auto commands = displayProps.writer.takePendingCommands();
  if (commands.empty()) {
    return true;
  }

  auto [status, results] = mComposerClient->executeCommands(commands);
  if (!status.isOk()) {
    ALOGE("executeCommands failed: %s", status.getDescription().c_str());
    return false;
  }

  displayProps.reader.parse(std::move(results));

  return true;
}

} // namespace hcct

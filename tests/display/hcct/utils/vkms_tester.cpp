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

#include "vkms_tester.h"

#include <android-base/file.h>
#include <cassert>
#include <cstdio>
#include <cstdlib>
#include <cutils/properties.h>
#include <dirent.h>
#include <errno.h>
#include <log/log.h>
#include <string>
#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>
#include <unordered_map>
#include <vector>
#include <inttypes.h>

namespace hcct {

namespace {
// `/config/vkms` is the base directory for VKMS in ConfigFS. `my-vkms` is the
// chosen name of the VKMS instance which can be anything.
constexpr const char *kVkmsBaseDir = "/config/vkms/my-vkms";
constexpr int kPlaneTypePrimary = 1;

// https://cs.android.com/android/platform/superproject/main/+/main:external/libdrm/xf86drmMode.h;l=190
enum class ConnectorStatus {
  kConnected = 1,
  kDisconnected = 2,
  kUnknown = 3,
};
} // namespace

// static
std::unique_ptr<VkmsTester>
VkmsTester::CreateWithGenericConnectors(int displaysCount) {
  if (displaysCount < 0) {
    ALOGE("Invalid number of displays: %i. At least one connector must be "
          "specified.",
          displaysCount);
    return nullptr;
  }

  auto tester = std::unique_ptr<VkmsTester>(new VkmsTester(displaysCount));

  if (!tester->mInitialized) {
    ALOGE("Failed to initialize VkmsTester with Generic Connectors");
    return nullptr;
  }

  return tester;
}

// static
std::unique_ptr<VkmsTester> VkmsTester::CreateWithBuilders(
    const std::vector<VkmsConnectorBuilder> &builders) {
  if (builders.empty()) {
    ALOGE("Empty configuration provided. At least one connector must be "
          "specified.");
    return nullptr;
  }

  auto tester =
      std::unique_ptr<VkmsTester>(new VkmsTester(builders.size(), builders));

  if (!tester->mInitialized) {
    ALOGE("Failed to initialize VkmsTester with Builder Config");
    return nullptr;
  }

  return tester;
}

// static
void VkmsTester::ForceDeleteVkmsDir() { ShutdownAndCleanUpVkms(); }

VkmsTester::VkmsTester(size_t displaysCount,
                       const std::vector<VkmsConnectorBuilder> &builders) {
  mInitialized = ToggleHwc3(false) && SetVkmsAsDisplayDriver() &&
                 SetupDisplays(displaysCount, builders) && ToggleVkms(true) &&
                 ToggleHwc3(true);
  if (!mInitialized) {
    ALOGE("Failed to set up VKMS");
    ShutdownAndCleanUpVkms();
    return;
  }

  mActiveConnectorsCount = displaysCount;
}

VkmsTester::~VkmsTester() {
  if (mDisableCleanupOnDestruction) {
    ALOGI("Skipping cleanup on destruction. mDisableCleanupOnDestruction is "
          "set to true.");
    return;
  }

  ShutdownAndCleanUpVkms();
}

bool VkmsTester::ToggleConnector(int connectorIndex, bool enable) {
  return SetConnectorStatus(connectorIndex, enable);
}

void VkmsTester::DisableCleanupOnDestruction() {
  mDisableCleanupOnDestruction = true;
}

bool VkmsTester::SetVkmsAsDisplayDriver() {
  // TODO(b/398831713): Setup an official doc for reference.

  // Set HWC to use VKMS as the display driver.
  if (property_set("vendor.hwc.drm.device", "/dev/dri/card1") != 0) {
    ALOGE("Failed to set vendor.hwc.drm.device property");
    return false;
  } else {
    ALOGI("Successfully set vendor.hwc.drm.device property");
  }

  // Create VKMS directory
  if (mkdir(kVkmsBaseDir, 0777) == 0) {
    ALOGI("Successfully created directory %s", kVkmsBaseDir);
    return true;
  }

  return false;
}

bool VkmsTester::SetupDisplays(
    int displaysCount, const std::vector<VkmsConnectorBuilder> &builders) {
  bool isExplicitConfig = !builders.empty();
  if (isExplicitConfig && displaysCount != builders.size()) {
    ALOGE("Mismatch between requested displays count and builder config size");
    return false;
  }

  for (int i = 0; i < displaysCount; ++i) {
    CreateResource(DrmResource::kCrtc, i);
    CreateResource(DrmResource::kEncoder, i);
    LinkToCrtc(DrmResource::kEncoder, i, i);

    CreateResource(DrmResource::kConnector, i);
    // Unless explicitly configured, set all connectors to disconnected.
    SetConnectorStatus(i, isExplicitConfig && builders[i].mEnabledAtStart);
    if (isExplicitConfig) {
      SetConnectorType(i, builders[i].mType);
      if (builders[i].mMonitorName.type != edid::MonitorName::Type::UNSET) {
        SetConnectorEdid(i, builders[i].mMonitorName);
      }
    } else {
      // Set connector type, eDP for first one, DP for the rest
      SetConnectorType(i, i == 0 ? ConnectorType::keDP
                                 : ConnectorType::kDisplayPort);
    }
    LinkConnectorToEncoder(i, i);

    int additionalOverlays =
        isExplicitConfig ? builders[i].mAdditionalOverlayPlanes : 0;
    for (int j = 0; j < 2 + additionalOverlays; ++j) {
      CreateResource(DrmResource::kPlane, mLatestPlaneId);
      // For each connector, create at least 2 planes, a primary and a cursor
      // PLUS any additional overlay planes
      PlaneType type;
      switch (j) {
      case 0:
        type = PlaneType::kCursor;
        break;
      case 1:
        type = PlaneType::kPrimary;
        break;
      default:
        type = PlaneType::kOverlay;
        break;
      }
      SetPlaneType(mLatestPlaneId, type);
      SetPlaneFormat(mLatestPlaneId);
      LinkToCrtc(DrmResource::kPlane, mLatestPlaneId, i);

      mLatestPlaneId++;
    }

    ALOGI("Successfully set up display %i", i);
  }

  return true;
}

// static
bool VkmsTester::ToggleVkms(bool enable) {
  std::string path = std::string(kVkmsBaseDir) + "/enabled";
  std::string value = enable ? "1" : "0";
  if (!android::base::WriteStringToFile(value, path)) {
    ALOGE("Failed to toggle VKMS: %s", strerror(errno));
    return false;
  }

  ALOGI("Successfully toggled VKMS at %s", path.c_str());
  return true;
}

// static
bool VkmsTester::ToggleHwc3(bool enable) {
  const char *serviceName = "vendor.hwcomposer-3";
  const char *propertyName = "ctl.start";
  const char *propertyStopName = "ctl.stop";

  if (property_set(enable ? propertyName : propertyStopName, serviceName) !=
      0) {
    ALOGE("Failed to set property %s to %s",
          enable ? propertyName : propertyStopName, serviceName);
    return false;
  }

  ALOGI("Successfully set property %s to %s",
        enable ? propertyName : propertyStopName, serviceName);
  return true;
}

bool VkmsTester::CreateResource(DrmResource resource, int index) {
  std::string resourceBase = kDrmResourceBase.at(resource);
  std::string resourceDir =
      std::string(kVkmsBaseDir) + "/" + resourceBase + std::to_string(index);
  if (mkdir(resourceDir.c_str(), 0777) != 0) {
    ALOGE("Failed to create directory %s: %s", resourceDir.c_str(),
          strerror(errno));
    return false;
  }

  return true;
}

bool VkmsTester::SetConnectorStatus(int index, bool enable) {
  std::string connectorDir = std::string(kVkmsBaseDir) + "/" +
                             kDrmResourceBase.at(DrmResource::kConnector) +
                             std::to_string(index);
  std::string connectedPath = connectorDir + "/status";
  ConnectorStatus status =
      enable ? ConnectorStatus::kConnected : ConnectorStatus::kDisconnected;
  std::string connectedValue = std::to_string(static_cast<int>(status));

  if (!android::base::WriteStringToFile(connectedValue, connectedPath)) {
    ALOGE("Failed to toggle connector %i: %s", index, strerror(errno));
    return false;
  }

  ALOGI("Successfully toggled connector %i: %s", index,
        enable ? "connected" : "disconnected");
  return true;
}

bool VkmsTester::SetConnectorType(int index, ConnectorType type) {
  std::string connectorDir = std::string(kVkmsBaseDir) + "/" +
                             kDrmResourceBase.at(DrmResource::kConnector) +
                             std::to_string(index);
  std::string typePath = connectorDir + "/type";
  std::string typeValue = std::to_string(static_cast<int>(type));
  if (!android::base::WriteStringToFile(typeValue, typePath)) {
    ALOGE("Failed to write connector type: %s", strerror(errno));
    return false;
  }

  ALOGI("Successfully set connector %i type to %i", index,
        static_cast<int>(type));
  return true;
}

bool VkmsTester::SetConnectorEdid(int index, edid::MonitorName monitorName) {
  std::vector<uint8_t> edidData = edid::getBinaryEdidForMonitor(monitorName);
  if (edidData.empty()) {
    ALOGE("Failed to get EDID data for monitor");
    return false;
  }

  std::string connectorDir = std::string(kVkmsBaseDir) + "/" +
                             kDrmResourceBase.at(DrmResource::kConnector) +
                             std::to_string(index);
  std::string edidPath = connectorDir + "/edid";

  int fd = open(edidPath.c_str(), O_WRONLY | O_CREAT | O_TRUNC, 0644);
  if (fd == -1) {
    ALOGE("Failed to open EDID file for writing: %s", strerror(errno));
    return false;
  }

  bool success =
      android::base::WriteFully(fd, edidData.data(), edidData.size());
  close(fd);

  if (success) {
    ALOGI("Successfully wrote EDID data with size %" PRIu64 " to connector %i",
        edidData.size(), index);
  } else {
    ALOGE("Failed to write complete EDID data: %s", strerror(errno));
  }

  return success;
}

bool VkmsTester::SetPlaneType(int index, PlaneType type) {
  std::string planeDir = std::string(kVkmsBaseDir) + "/" +
                         kDrmResourceBase.at(DrmResource::kPlane) +
                         std::to_string(index);
  std::string typePath = planeDir + "/type";
  std::string typeValue = std::to_string(static_cast<int>(type));
  if (!android::base::WriteStringToFile(typeValue, typePath)) {
    ALOGE("Failed to write plane type: %s", strerror(errno));
    return false;
  }

  ALOGI("Successfully set plane %i type to %i", index, static_cast<int>(type));
  return true;
}

bool VkmsTester::SetPlaneFormat(int index) {
  std::string planeDir = std::string(kVkmsBaseDir) + "/" +
                         kDrmResourceBase.at(DrmResource::kPlane) +
                         std::to_string(index);
  std::string formatPath = planeDir + "/supported_formats";
  // TODO(markyacoub): This is now hardcoded to all formats. Extend this later.
  std::string formatValue = "+*";
  if (!android::base::WriteStringToFile(formatValue, formatPath)) {
    ALOGE("Failed to write plane format: %s", strerror(errno));
    return false;
  }

  ALOGI("Successfully set plane %i format", index);
  return true;
}

bool VkmsTester::LinkToCrtc(DrmResource resource, int resourceIdx,
                            int crtcIdx) {
  std::string crtcName =
      kDrmResourceBase.at(DrmResource::kCrtc) + std::to_string(crtcIdx);
  std::string resourceDir = std::string(kVkmsBaseDir) + "/" +
                            kDrmResourceBase.at(resource) +
                            std::to_string(resourceIdx);
  std::string possibleCrtcPath = resourceDir + "/possible_" + crtcName;
  std::string crtcDir = std::string(kVkmsBaseDir) + "/" + crtcName;

  // Now create the symlink
  if (symlink(crtcDir.c_str(), possibleCrtcPath.c_str()) != 0) {
    ALOGE("Failed to create symlink at %s pointing to %s: %s",
          possibleCrtcPath.c_str(), crtcDir.c_str(), strerror(errno));
    return false;
  }

  ALOGI("Successfully linked %s to %s", possibleCrtcPath.c_str(),
        crtcDir.c_str());
  return true;
}

bool VkmsTester::LinkConnectorToEncoder(int connectorIdx, int encoderIdx) {
  std::string encoderName =
      kDrmResourceBase.at(DrmResource::kEncoder) + std::to_string(encoderIdx);
  std::string connectorDir = std::string(kVkmsBaseDir) + "/" +
                             kDrmResourceBase.at(DrmResource::kConnector) +
                             std::to_string(connectorIdx);
  std::string possibleEncoderPath = connectorDir + "/possible_" + encoderName;
  std::string encoderDir = std::string(kVkmsBaseDir) + "/" + encoderName;

  // Now create the symlink
  if (symlink(encoderDir.c_str(), possibleEncoderPath.c_str()) != 0) {
    ALOGE("Failed to create symlink at %s pointing to %s: %s",
          possibleEncoderPath.c_str(), encoderDir.c_str(), strerror(errno));
    return false;
  }

  ALOGI("Successfully linked %s to %s", possibleEncoderPath.c_str(),
        encoderDir.c_str());
  return true;
}

// static
// ConfigFS has special rules about deletion, so we need to clean up manually
// every layer.
void VkmsTester::ShutdownAndCleanUpVkms() {
  ToggleHwc3(false);
  ToggleVkms(false);
  // Give the kernel a longer time to release resources
  usleep(500000);

  // Clean up manually created relationships first under
  // possible_(crtcs/encoders). This is required before we started cleaning up
  // the directories.
  FindAndCleanupPossibleLinks(kVkmsBaseDir);
  CleanUpDirAndChildren(kVkmsBaseDir);
}

// static
void VkmsTester::FindAndCleanupPossibleLinks(const std::string &dirPath) {
  DIR *dir = opendir(dirPath.c_str());
  if (!dir) {
    return;
  }

  struct dirent *entry;
  while ((entry = readdir(dir)) != nullptr) {
    if (strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0) {
      continue;
    }

    std::string path = dirPath + "/" + entry->d_name;
    struct stat statBuf;

    if (lstat(path.c_str(), &statBuf) != 0) {
      continue;
    }

    if (S_ISDIR(statBuf.st_mode)) {
      // If this is a "possible_*" directory, process it specially
      if (strstr(entry->d_name, "possible_") == entry->d_name) {
        DIR *dir = opendir(path.c_str());
        if (!dir) {
          return;
        }

        // First try to remove any contents
        struct dirent *entry;
        while ((entry = readdir(dir)) != nullptr) {
          if (strcmp(entry->d_name, ".") == 0 ||
              strcmp(entry->d_name, "..") == 0) {
            continue;
          }

          std::string fullPath = path + "/" + entry->d_name;
          unlink(
              fullPath
                  .c_str()); // Try to remove anything inside, ignoring errors
        }

        closedir(dir);

        // Then try to remove the directory itself
        rmdir(path.c_str());
      } else {
        // Otherwise recursively look for more possible_* directories
        FindAndCleanupPossibleLinks(path);
      }
    }
  }

  closedir(dir);
}

// static
void VkmsTester::CleanUpDirAndChildren(const std::string &dirPath) {
  DIR *dir = opendir(dirPath.c_str());
  if (!dir) {
    ALOGW("Failed to open directory %s: %s - skipping", dirPath.c_str(),
          strerror(errno));
    return;
  }

  struct dirent *entry;
  while ((entry = readdir(dir)) != nullptr) {
    // Skip "." and ".."
    if (strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0) {
      continue;
    }

    std::string path = dirPath + "/" + entry->d_name;
    struct stat statBuf;

    if (lstat(path.c_str(), &statBuf) != 0) {
      ALOGW("Failed to stat %s: %s - skipping", path.c_str(), strerror(errno));
      continue;
    }

    if (S_ISDIR(statBuf.st_mode)) {
      CleanUpDirAndChildren(path);
    } else {
    }
  }

  closedir(dir);

  // Remove the directory itself. Do not check for errors as directories that
  // are auto-created can't be manually deleted. It's a no-op otherwise so we
  // can ignore the return value.
  rmdir(dirPath.c_str());
}

} // namespace hcct

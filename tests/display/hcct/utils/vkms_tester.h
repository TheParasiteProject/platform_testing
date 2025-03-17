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

#include <memory>
#include <string>
#include <unordered_map>
#include <vector>

namespace hcct {

/**
 * @class VkmsTester
 * @brief Handles setup and configuration of Virtual KMS (VKMS) for display
 * emulation
 *
 * This class manages the creation of VKMS directory structures and file system
 * entries needed to configure virtual displays through the VKMS driver.
 */
class VkmsTester {
public:
  // https://cs.android.com/android/platform/superproject/main/+/main:external/libdrm/include/drm/drm_mode.h;l=403
  enum class ConnectorType {
    kUnknown = 0,
    kVGA = 1,
    kDisplayPort = 10,
    kHDMIA = 11,
    kHDMIB = 12,
    keDP = 14,
    kVirtual = 15,
    kDSI = 16,
    kDPI = 17,
    kWriteback = 18,
  };

  // VkmsConnectorSetup describes the desired configuration for a VKMS
  // connector. It provides default values, allowing you to specify only the
  // parameters that need customization.
  struct VkmsConnectorSetup {
    ConnectorType type = ConnectorType::kDisplayPort;
    // Indicates whether the connector is enabled upon VKMS initialization.
    // This simulates a device powering on with a display already connected.
    bool enabledAtStart = true;
    // Each connector has a primary and a cursor plane by default. Use this to
    // add additional overlay planes.
    int additionalOverlayPlanes = 0;
  };

  /**
   * Creates a VKMS configuration with a specified number of virtual displays,
   * each with a default setup.
   *
   * Each connector is configured with:
   *   - 1 CRTC
   *   - 1 Encoder
   *   - 2 Planes: 1 Primary and 1 Cursor
   *
   * The first connector is set to eDP, and the remaining connectors are set to
   * DisplayPort.
   *
   * @param displaysCount The number of virtual displays to configure.
   * @return A unique pointer to the created VkmsTester instance, or nullptr if
   * creation failed.
   */
  static std::unique_ptr<VkmsTester>
  CreateWithGenericConnectors(int displaysCount);
  /**
   * Creates a VKMS configuration based on a provided vector of
   * `VkmsConnectorSetup`.
   *
   * This method allows for fine-grained control over the configuration of each
   * virtual display. Each entry in the `config` vector corresponds to a single
   * connector and its associated configurable resources.
   *
   * @param config A vector of `VkmsConnectorSetup` objects, each defining the
   * configuration for a single connector. The size of the vector determines the
   * number of virtual displays to create.
   * @return A unique pointer to the created VkmsTester instance, or nullptr if
   * creation failed.
   */
  static std::unique_ptr<VkmsTester>
  CreateWithConfig(const std::vector<VkmsConnectorSetup> &config);
  static void ForceDeleteVkmsDir();

  ~VkmsTester();

  // Returns the number of connectors that have been successfully created
  // regardless of their connection status.
  int getActiveConnectorsCount() const { return mActiveConnectorsCount; }
  bool ToggleConnector(int connectorIndex, bool enable);

  // Prevent the VkmsTester instance from cleaning up the VKMS directories upon
  // destruction.
  void DisableCleanupOnDestruction();

private:
  enum class DrmResource {
    kConnector,
    kCrtc,
    kEncoder,
    kPlane,
  };

  // https://cs.android.com/android/platform/superproject/main/+/main:external/libdrm/xf86drmMode.h;l=225
  enum class PlaneType {
    kOverlay = 0,
    kPrimary = 1,
    kCursor = 2,
  };

  // Create a map of the base directory for each resource type to maintain
  // string consistency throughout the code.
  const std::unordered_map<DrmResource, std::string> kDrmResourceBase = {
      {DrmResource::kConnector, "connectors/CONNECTOR_"},
      {DrmResource::kCrtc, "crtcs/CRTC_"},
      {DrmResource::kEncoder, "encoders/ENCODER_"},
      {DrmResource::kPlane, "planes/PLANE_"},
  };

  // Private constructor to prevent direct instantiation without the Create
  // functions.
  explicit VkmsTester(
      size_t displaysCount,
      const std::vector<VkmsConnectorSetup> &explicitConfig = {});

  bool SetVkmsAsDisplayDriver();
  bool SetupDisplays(int displaysCount,
                     const std::vector<VkmsConnectorSetup> &explicitConfig);
  static bool ToggleVkms(bool enable);
  static bool ToggleHwc3(bool enable);

  bool CreateResource(DrmResource resource, int index);
  bool SetConnectorStatus(int index, bool enable);
  bool SetConnectorType(int index, ConnectorType type);
  bool SetPlaneType(int index, PlaneType type);
  bool SetPlaneFormat(int index);
  bool LinkToCrtc(DrmResource resource, int resourceIdx, int crtcIdx);
  bool LinkConnectorToEncoder(int connectorIdx, int encoderIdx);

  static void ShutdownAndCleanUpVkms();
  static void FindAndCleanupPossibleLinks(const std::string &dirPath);
  static void CleanUpDirAndChildren(const std::string &rootDir);

  size_t mActiveConnectorsCount = 0;
  // Used to track the most recently created plane ID, as the number of planes
  // can vary per connector. This value is updated whenever a new plane is
  // created.
  int mLatestPlaneId = 0;

  bool mDisableCleanupOnDestruction = false;
  bool mInitialized = false;
};

} // namespace hcct
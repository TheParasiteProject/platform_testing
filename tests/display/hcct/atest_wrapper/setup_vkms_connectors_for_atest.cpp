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

#include "edid_helper.h"
#include "vkms_tester.h"
#include <log/log.h>
#include <sstream>
#include <stdlib.h>
#include <string>
#include <unistd.h>
#include <vector>

// clang-format off
/*
 * A binary that turns on vkms and creates connectors.
 *
 * Usage:
 *   Simple mode:
 *     ./setup_vkms_connectors_for_atest <number_of_connectors>
 *
 *   Advanced mode:
 *     ./setup_vkms_connectors_for_atest --config TYPE,NUMBER_OF_OVERLAY_PLANES[,EDID_NAME]
 * TYPE,NUMBER_OF_OVERLAY_PLANES[,EDID_NAME] ...
 *
 *   Where:
 *     TYPE = connector type (DP, HDMIA, HDMIB, eDP, DSI, VGA, VIRTUAL, etc.)
 *     NUMBER_OF_OVERLAY_PLANES = number of additional overlay planes (integer)
 *     EDID_NAME = optional EDID profile name (e.g., ACI_9713_ASUS_VE258_DP)
 *
 * Examples:
 *   ./setup_vkms_connectors_for_atest 3                      # Creates 3 virtual connectors
 *   ./setup_vkms_connectors_for_atest --config DP,2 HDMIA,1  # Creates 2 connectors with specific configs
 *   ./setup_vkms_connectors_for_atest --config DP,2,ACI_9713_ASUS_VE258_DP HDMIA,1,ACI_9155_ASUS_VH238_HDMI
 *      # Creates connectors with specific EDID profiles
 *
 * The binary will set up the vkms (virtual kernel mode setting) driver
 * with the specified configuration. It disables cleanup on destruction
 * so that the vkms setup persists after program termination.
 */
// clang-format on

bool stringToMonitorName(const std::string &edidName,
                         hcct::edid::MonitorName &monitorName) {
  if (edidName.empty()) {
    return false;
  }

  // Check if it's an eDP
#define CHECK_EDP_MONITOR(monitor)                                             \
  if (edidName == #monitor) {                                                  \
    monitorName =                                                              \
        hcct::edid::MonitorName(hcct::edid::EdpMonitorName::monitor);          \
    return true;                                                               \
  }
  // Generate checks for eDPs
  EDP_MONITOR_LIST(CHECK_EDP_MONITOR)

  // Check if it's a DP monitor
#define CHECK_DP_MONITOR(monitor)                                              \
  if (edidName == #monitor) {                                                  \
    monitorName = hcct::edid::MonitorName(hcct::edid::DpMonitorName::monitor); \
    return true;                                                               \
  }
  // Generate checks for all DP monitors
  DP_MONITOR_LIST(CHECK_DP_MONITOR)

  // Check if it's an HDMI monitor
#define CHECK_HDMI_MONITOR(monitor)                                            \
  if (edidName == #monitor) {                                                  \
    monitorName =                                                              \
        hcct::edid::MonitorName(hcct::edid::HdmiMonitorName::monitor);         \
    return true;                                                               \
  }
  // Generate checks for all HDMI monitors
  HDMI_MONITOR_LIST(CHECK_HDMI_MONITOR)

  return false;
}

hcct::VkmsTester::VkmsConnectorBuilder parseConnectorConfig(
    const std::string &configStr) {
  std::istringstream ss(configStr);
  std::string typeStr, planesStr, edidStr;

  auto builder = hcct::VkmsTester::VkmsConnectorBuilder::create();

  // Parse type
  if (std::getline(ss, typeStr, ',')) {
    builder.withType(typeStr);
  }

  // Parse additional planes
  if (std::getline(ss, planesStr, ',')) {
    char *endptr;
    int planes = strtol(planesStr.c_str(), &endptr, 10);
    if (*endptr != '\0' || planes < 0) {
      ALOGE("Invalid number of planes: %s", planesStr.c_str());
    } else {
      builder.withAdditionalOverlayPlanes(planes);
    }
  }

  // Parse optional EDID name
  if (std::getline(ss, edidStr, ',')) {
    hcct::edid::MonitorName monitorName;
    if (stringToMonitorName(edidStr, monitorName)) {
      builder.withMonitor(monitorName);
    } else {
      ALOGE("Unknown EDID profile name: %s", edidStr.c_str());
    }
  }

  return builder;
}

// Helper function to parse all connector configurations
std::vector<hcct::VkmsTester::VkmsConnectorBuilder> parseConnectorConfigs(
    int argc, char *argv[], int startIndex) {
  std::vector<hcct::VkmsTester::VkmsConnectorBuilder> builders;

  for (int i = startIndex; i < argc; i++) {
    std::string configStr(argv[i]);
    builders.push_back(parseConnectorConfig(configStr));
  }

  return builders;
}

void printUsage(const char *programName) {
  // clang-format off
  ALOGI("Usage:");
  ALOGI("  Simple mode:   %s <number_of_connectors>", programName);
  ALOGI("  Advanced mode: %s --config TYPE,NUMBER_OF_OVERLAY_PLANES[,EDID_NAME] TYPE,NUMBER_OF_OVERLAY_PLANES[,EDID_NAME] ...", programName);
  ALOGI("  Where:");
  ALOGI("    TYPE = connector type (DP, HDMIA, HDMIB, eDP, DSI, VGA, VIRTUAL, "
        "WRITEBACK, DPI)");
  ALOGI("    NUMBER_OF_OVERLAY_PLANES = number of additional overlay planes "
        "(integer)");
  ALOGI("Examples:");
  ALOGI("  %s 3", programName);
  ALOGI("  %s --config DP,2 HDMIA,1", programName);
  ALOGI("  %s --config DP,2,ACI_9713_ASUS_VE258_DP HDMIA,1,ACI_9155_ASUS_VH238_HDMI", programName);
  // clang-format on
}

int main(int argc, char *argv[]) {
  if (argc <= 1) {
    ALOGE("Error: No arguments provided");
    printUsage(argv[0]);
    return -1;
  }

  std::unique_ptr<hcct::VkmsTester> vkmsTester;

  // Check for advanced mode with --config flag
  if (strcmp(argv[1], "--config") == 0) {
    if (argc <= 2) {
      ALOGE("Error: No configuration parameters provided after --config");
      printUsage(argv[0]);
      return -1;
    }

    // Parse configs from multiple arguments starting from index 2
    auto builders = parseConnectorConfigs(argc, argv, 2);
    if (builders.empty()) {
      ALOGE("Failed to parse connector configurations");
      return -1;
    }

    ALOGI("Setting up vkms with %zu custom connectors", builders.size());
    vkmsTester = hcct::VkmsTester::CreateWithBuilders(builders);
  } else {
    // Simple mode - just a number of connectors
    char *endptr;
    int numConnectors = strtol(argv[1], &endptr, 10);

    if (*endptr != '\0' || numConnectors <= 0) {
      ALOGE("Error: Invalid number of connectors: %s. Must be a positive "
            "integer.",
            argv[1]);
      printUsage(argv[0]);
      return -1;
    }

    ALOGI("Setting up vkms with %d generic connectors", numConnectors);
    vkmsTester = hcct::VkmsTester::CreateWithGenericConnectors(numConnectors);
  }

  if (!vkmsTester) {
    ALOGE("Failed to create VkmsTester");
    return -1;
  }

  vkmsTester->DisableCleanupOnDestruction();

  // Enable all connectors to run tests on them
  for (int i = 0; i < vkmsTester->getActiveConnectorsCount(); i++) {
    if (!vkmsTester->ToggleConnector(i, true)) {
      ALOGE("Failed to enable connector %d", i);
      return -1;
    }
  }

  return 0;
}
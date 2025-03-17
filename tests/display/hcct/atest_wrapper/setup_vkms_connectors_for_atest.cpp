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
#include <log/log.h>
#include <sstream>
#include <stdlib.h>
#include <string>
#include <unistd.h>
#include <vector>

/*
 * A binary that turns on vkms and creates connectors.
 *
 * Usage:
 *   Simple mode:
 *     ./setup_vkms_connectors_for_atest <number_of_connectors>
 *
 *   Advanced mode:
 *     ./setup_vkms_connectors_for_atest --config TYPE,NUMBER_OF_OVERLAY_PLANES
 * TYPE,NUMBER_OF_OVERLAY_PLANES ...
 *
 *   Where:
 *     TYPE = connector type (DP, HDMIA, HDMIB, eDP, DSI, VGA, VIRTUAL, etc.)
 *     NUMBER_OF_OVERLAY_PLANES = number of additional overlay planes (integer)
 *
 * Examples:
 *   ./setup_vkms_connectors_for_atest 3                      # Creates 3
 * virtual connectors
 *   ./setup_vkms_connectors_for_atest --config DP,2 HDMIA,1  # Creates 2
 * connectors with specific configs
 *
 * The binary will set up the vkms (virtual kernel mode setting) driver
 * with the specified configuration. It disables cleanup on destruction
 * so that the vkms setup persists after program termination.
 */

// Helper function to convert string to ConnectorType
hcct::VkmsTester::ConnectorType
stringToConnectorType(const std::string &typeStr) {
  if (typeStr == "DP")
    return hcct::VkmsTester::ConnectorType::kDisplayPort;
  if (typeStr == "HDMIA")
    return hcct::VkmsTester::ConnectorType::kHDMIA;
  if (typeStr == "HDMIB")
    return hcct::VkmsTester::ConnectorType::kHDMIB;
  if (typeStr == "eDP")
    return hcct::VkmsTester::ConnectorType::keDP;
  if (typeStr == "VGA")
    return hcct::VkmsTester::ConnectorType::kVGA;
  if (typeStr == "DSI")
    return hcct::VkmsTester::ConnectorType::kDSI;
  if (typeStr == "DPI")
    return hcct::VkmsTester::ConnectorType::kDPI;
  if (typeStr == "VIRTUAL")
    return hcct::VkmsTester::ConnectorType::kVirtual;
  if (typeStr == "WRITEBACK")
    return hcct::VkmsTester::ConnectorType::kWriteback;
  return hcct::VkmsTester::ConnectorType::kUnknown;
}

// Helper function to parse a connector configuration string
bool parseConnectorConfig(const std::string &configStr,
                          hcct::VkmsTester::VkmsConnectorSetup &config) {
  std::istringstream ss(configStr);
  std::string typeStr, planesStr;

  // Parse type
  if (std::getline(ss, typeStr, ',')) {
    config.type = stringToConnectorType(typeStr);
    if (config.type == hcct::VkmsTester::ConnectorType::kUnknown &&
        typeStr != "UNKNOWN") {
      ALOGE("Invalid connector type: %s", typeStr.c_str());
      return false;
    }
  }

  // Parse additional planes
  if (std::getline(ss, planesStr, ',')) {
    char *endptr;
    int planes = strtol(planesStr.c_str(), &endptr, 10);
    if (*endptr != '\0' || planes < 0) {
      ALOGE("Invalid number of planes: %s", planesStr.c_str());
      return false;
    }
    config.additionalOverlayPlanes = planes;
  }

  return true;
}

// Helper function to parse all connector configurations
bool parseConnectorConfigs(
    int argc, char *argv[], int startIndex,
    std::vector<hcct::VkmsTester::VkmsConnectorSetup> &configs) {
  for (int i = startIndex; i < argc; i++) {
    std::string configStr(argv[i]);
    hcct::VkmsTester::VkmsConnectorSetup config;
    if (!parseConnectorConfig(configStr, config)) {
      return false;
    }
    configs.push_back(config);
  }

  return !configs.empty();
}

void printUsage(const char *programName) {
  ALOGI("Usage:");
  ALOGI("  Simple mode:   %s <number_of_connectors>", programName);
  ALOGI("  Advanced mode: %s --config TYPE,NUMBER_OF_OVERLAY_PLANES "
        "TYPE,NUMBER_OF_OVERLAY_PLANES ...",
        programName);
  ALOGI("  Where:");
  ALOGI("    TYPE = connector type (DP, HDMIA, HDMIB, eDP, DSI, VGA, VIRTUAL, "
        "WRITEBACK, DPI)");
  ALOGI("    NUMBER_OF_OVERLAY_PLANES = number of additional overlay planes "
        "(integer)");
  ALOGI("Examples:");
  ALOGI("  %s 3", programName);
  ALOGI("  %s --config DP,2 HDMIA,1 eDP,0", programName);
}

int main(int argc, char *argv[]) {
  if (argc <= 1) {
    ALOGE("Error: No arguments provided");
    printUsage(argv[0]);
    return -1;
  }

  std::unique_ptr<hcct::VkmsTester> vkmsTester;
  std::vector<hcct::VkmsTester::VkmsConnectorSetup> connectorConfigs;

  // Check for advanced mode with --config flag
  if (strcmp(argv[1], "--config") == 0) {
    if (argc <= 2) {
      ALOGE("Error: No configuration parameters provided after --config");
      printUsage(argv[0]);
      return -1;
    }

    // Parse configs from multiple arguments starting from index 2
    if (!parseConnectorConfigs(argc, argv, 2, connectorConfigs)) {
      ALOGE("Failed to parse connector configurations");
      return -1;
    }

    ALOGI("Setting up vkms with %zu custom connectors",
          connectorConfigs.size());
    vkmsTester = hcct::VkmsTester::CreateWithConfig(connectorConfigs);
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
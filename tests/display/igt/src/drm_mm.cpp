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

#include <cstdlib>

#include "include/igt_test_helper.h"

namespace igt {
namespace {

/**
 * TEST: drm mm
 * Description: Basic sanity check of DRM's range manager (struct drm_mm)
 * Category: Core
 * Mega feature: General Core features
 * Sub-category: Memory management tests
 * Functionality: drm_mm
 * Feature: mapping
 * Test category: GEM_Legacy
 */

class DrmMmTests : public ::testing::TestWithParam<IgtSubtestParams>,
                             public IgtTestHelper {
public:
  DrmMmTests() : IgtTestHelper("drm_mm") {}
};

TEST_F(DrmMmTests, TestDrmMm) {
  std::string desc = "Basic sanity check of DRM's range manager";
  std::string rationale = "Test the memory management range manager which manages memory allocation and fragmentation.";
  runTest(desc, rationale);
}

} // namespace
} // namespace igt

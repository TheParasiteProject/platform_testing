/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.compatibility.common.util;

import com.google.auto.value.AutoValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for collecting device information. This is used to enforce
 * consistent property collection host-side and device-side for CTS reports.
 *
 * Note that properties across sources can differ, e.g. {@code android.os.Build}
 * properties sometimes deviate from the read-only properties that they're based
 * on.
 */
@AutoValue
public abstract class DevicePropertyInfo {

    abstract String abi();
    abstract String abi2();
    abstract String abis();
    abstract String abis32();
    abstract String abis64();
    abstract String board();
    abstract String brand();
    abstract String device();
    abstract String fingerprint();
    abstract String vendorFingerprint();
    abstract String bootimageFingerprint();
    abstract String id();
    abstract String manufacturer();
    abstract String model();
    abstract String product();
    abstract String referenceFingerprint();
    abstract String serial();
    abstract String tags();
    abstract String type();
    abstract String versionBaseOs();
    abstract String versionRelease();
    abstract String versionSdk();
    abstract String versionSecurityPatch();
    abstract String versionIncremental();
    abstract String versionSdkFull();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder abi(String abi);
        public abstract Builder abi2(String abi2);
        public abstract Builder abis(String abis);
        public abstract Builder abis32(String abis32);
        public abstract Builder abis64(String abis64);
        public abstract Builder board(String board);
        public abstract Builder brand(String brand);
        public abstract Builder device(String device);
        public abstract Builder fingerprint(String fingerprint);
        public abstract Builder vendorFingerprint(String vendorFingerprint);
        public abstract Builder id(String id);
        public abstract Builder manufacturer(String manufacturer);
        public abstract Builder model(String model);
        public abstract Builder product(String product);
        public abstract Builder referenceFingerprint(String referenceFingerprint);
        public abstract Builder serial(String serial);
        public abstract Builder tags(String tags);
        public abstract Builder type(String type);
        public abstract Builder versionBaseOs(String versionBaseOs);
        public abstract Builder versionRelease(String versionRelease);
        public abstract Builder versionSdk(String versionSdk);
        public abstract Builder versionSecurityPatch(String versionSecurityPatch);
        public abstract Builder versionIncremental(String versionIncremental);
        public abstract Builder bootimageFingerprint(String bootimageFingerprint);
        public abstract Builder versionSdkFull(String versionSdkFull);
        public abstract DevicePropertyInfo build();
    }

    public static Builder newBuilder() {
        return new AutoValue_DevicePropertyInfo.Builder()
                .abi("")
                .abi2("")
                .abis("")
                .abis32("")
                .abis64("")
                .board("")
                .brand("")
                .device("")
                .fingerprint("")
                .vendorFingerprint("")
                .bootimageFingerprint("unknown")
                .id("")
                .manufacturer("")
                .model("")
                .product("")
                .referenceFingerprint("")
                .serial("")
                .tags("")
                .type("")
                .versionBaseOs("")
                .versionRelease("")
                .versionSdk("")
                .versionSecurityPatch("")
                .versionIncremental("")
                .versionSdkFull("");
    }

    /**
     * Return a {@code Map} with property keys prepended with a given prefix
     * string. This is intended to be used to generate entries for
     * {@code} Build tag attributes in CTS test results.
     */
    public Map<String, String> getPropertytMapWithPrefix(String prefix) {
        Map<String, String> propertyMap = new HashMap<>();

        propertyMap.put(prefix + "abi", abi());
        propertyMap.put(prefix + "abi2", abi2());
        propertyMap.put(prefix + "abis", abis());
        propertyMap.put(prefix + "abis_32", abis32());
        propertyMap.put(prefix + "abis_64", abis64());
        propertyMap.put(prefix + "board", board());
        propertyMap.put(prefix + "brand", brand());
        propertyMap.put(prefix + "device", device());
        propertyMap.put(prefix + "fingerprint", fingerprint());
        propertyMap.put(prefix + "vendor_fingerprint", vendorFingerprint());
        propertyMap.put(prefix + "bootimage_fingerprint", bootimageFingerprint());
        propertyMap.put(prefix + "id", id());
        propertyMap.put(prefix + "manufacturer", manufacturer());
        propertyMap.put(prefix + "model", model());
        propertyMap.put(prefix + "product", product());
        propertyMap.put(prefix + "reference_fingerprint", referenceFingerprint());
        propertyMap.put(prefix + "serial", serial());
        propertyMap.put(prefix + "tags", tags());
        propertyMap.put(prefix + "type", type());
        propertyMap.put(prefix + "version_base_os", versionBaseOs());
        propertyMap.put(prefix + "version_release", versionRelease());
        propertyMap.put(prefix + "version_sdk", versionSdk());
        propertyMap.put(prefix + "version_security_patch", versionSecurityPatch());
        propertyMap.put(prefix + "version_incremental", versionIncremental());
        propertyMap.put(prefix + "version_sdk_full", versionSdkFull());

        return propertyMap;
    }

}

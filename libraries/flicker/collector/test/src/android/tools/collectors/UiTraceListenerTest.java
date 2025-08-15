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

package android.tools.collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.device.collectors.PerfettoTracingStrategy;
import android.os.Bundle;
import android.util.Base64;

import androidx.test.runner.AndroidJUnit4;

import com.google.protobuf.InvalidProtocolBufferException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import perfetto.protos.PerfettoConfig;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class UiTraceListenerTest {

    @Test
    public void testBuildDefaultConfig() throws InvalidProtocolBufferException {
        Bundle b = new Bundle();
        UiTraceListener listener = new UiTraceListener(b, List.of());
        listener.setupAdditionalArgs();

        var config = getTraceConfig(b);
        assertCommonConfig(b, config);

        List<String> sourceNames =
                config.getDataSourcesList().stream().map(s -> s.getConfig().getName()).toList();

        assertTrue(sourceNames.isEmpty());
    }

    @Test
    public void testBuildConfig_allOff() throws InvalidProtocolBufferException {
        Bundle b = new Bundle();
        b.putString(UiTraceListener.TRACE_FTRACE_KEY, "false");
        b.putString(UiTraceListener.TRACE_LAYERS_KEY, "false");
        b.putString(UiTraceListener.TRACE_SHELL_TRANSITIONS_KEY, "false");
        b.putString(UiTraceListener.TRACE_INPUT_KEY, "false");
        b.putString(UiTraceListener.TRACE_WM_KEY, "false");
        b.putString(UiTraceListener.PROTOLOG_GROUPS_KEY, "");

        UiTraceListener listener = new UiTraceListener(b, List.of());
        listener.setupAdditionalArgs();

        var config = getTraceConfig(b);
        assertCommonConfig(b, config);

        List<String> sourceNames =
                config.getDataSourcesList().stream().map(s -> s.getConfig().getName()).toList();

        assertTrue(sourceNames.isEmpty());
    }

    @Test
    public void testBuildConfig_allOn() throws InvalidProtocolBufferException {
        Bundle b = new Bundle();
        b.putString(UiTraceListener.TRACE_FTRACE_KEY, "true");
        b.putString(UiTraceListener.TRACE_LAYERS_KEY, "true");
        b.putString(UiTraceListener.TRACE_SHELL_TRANSITIONS_KEY, "true");
        b.putString(UiTraceListener.TRACE_INPUT_KEY, "true");
        b.putString(UiTraceListener.TRACE_WM_KEY, "true");
        b.putString(UiTraceListener.PROTOLOG_GROUPS_KEY, "WM_DEBUG_FOCUS,WM_DEBUG_ADD_REMOVE");

        UiTraceListener listener = new UiTraceListener(b, List.of());
        listener.setupAdditionalArgs();

        var config = getTraceConfig(b);
        assertCommonConfig(b, config);

        List<String> sourceNames =
                config.getDataSourcesList().stream().map(s -> s.getConfig().getName()).toList();

        assertTrue(sourceNames.contains("linux.ftrace"));
        assertTrue(sourceNames.contains("android.surfaceflinger.layers"));
        assertTrue(sourceNames.contains("android.surfaceflinger.transactions"));
        assertTrue(sourceNames.contains("com.android.wm.shell.transition"));
        assertTrue(sourceNames.contains("android.input.inputevent"));
        assertTrue(sourceNames.contains("android.protolog"));
        assertTrue(sourceNames.contains("android.windowmanager"));

        PerfettoConfig.TraceConfig.DataSource protologSource =
                config.getDataSourcesList().stream()
                        .filter(s -> s.getConfig().getName().equals("android.protolog"))
                        .findFirst()
                        .get();
        PerfettoConfig.ProtoLogConfig protologConfig =
                protologSource.getConfig().getProtologConfig();
        assertEquals(2, protologConfig.getGroupOverridesCount());
        assertEquals("WM_DEBUG_FOCUS", protologConfig.getGroupOverrides(0).getGroupName());
        assertEquals("WM_DEBUG_ADD_REMOVE", protologConfig.getGroupOverrides(1).getGroupName());
    }

    @Test
    public void testSetupAdditionalArgs_setsCorrectBinaryProto()
            throws InvalidProtocolBufferException {
        Bundle b = new Bundle();
        b.putString(UiTraceListener.TRACE_FTRACE_KEY, "true");
        b.putString(UiTraceListener.TRACE_LAYERS_KEY, "true");
        b.putString(UiTraceListener.TRACE_SHELL_TRANSITIONS_KEY, "true");
        b.putString(UiTraceListener.TRACE_INPUT_KEY, "true");
        b.putString(UiTraceListener.TRACE_WM_KEY, "true");
        b.putString(UiTraceListener.PROTOLOG_GROUPS_KEY, "WM_DEBUG_FOCUS,WM_DEBUG_ADD_REMOVE");

        var listener = new UiTraceListener(b, List.of());
        PerfettoConfig.TraceConfig expectedConfig = listener.buildConfig(b);

        listener.setupAdditionalArgs();

        PerfettoConfig.TraceConfig actualConfig = getTraceConfig(b);
        assertEquals(expectedConfig, actualConfig);
    }

    @Test
    public void testBuildConfig_canEnableProtoLogWithLogLevel()
            throws InvalidProtocolBufferException {
        Bundle b = new Bundle();
        b.putString(UiTraceListener.PROTOLOG_DEFAULT_LOG_FROM_LEVEL_KEY, "INFO");

        UiTraceListener listener = new UiTraceListener(b, List.of());
        listener.setupAdditionalArgs();

        var config = getTraceConfig(b);
        assertCommonConfig(b, config);

        List<String> sourceNames =
                config.getDataSourcesList().stream().map(s -> s.getConfig().getName()).toList();

        assertTrue(sourceNames.contains("android.protolog"));

        PerfettoConfig.TraceConfig.DataSource protologSource =
                config.getDataSourcesList().stream()
                        .filter(s -> s.getConfig().getName().equals("android.protolog"))
                        .findFirst()
                        .get();
        PerfettoConfig.ProtoLogConfig protologConfig =
                protologSource.getConfig().getProtologConfig();
        assertEquals(
                PerfettoConfig.ProtoLogLevel.PROTOLOG_LEVEL_INFO,
                protologConfig.getDefaultLogFromLevel());
    }

    private void assertCommonConfig(Bundle b, PerfettoConfig.TraceConfig config) {
        assertEquals("false", b.getString(PerfettoTracingStrategy.PERFETTO_CONFIG_TEXT_PROTO));
        assertEquals(
                "uiTrace_",
                b.getString(PerfettoTracingStrategy.PERFETTO_CONFIG_OUTPUT_FILE_PREFIX));

        assertTrue(config.getWriteIntoFile());
        assertEquals(1000, config.getFileWritePeriodMs());
        assertEquals(30000, config.getFlushPeriodMs());
        assertEquals(1, config.getBuffersCount());
        var bufferConfig = config.getBuffers(0);
        assertEquals(63488, bufferConfig.getSizeKb());
        assertEquals(
                PerfettoConfig.TraceConfig.BufferConfig.FillPolicy.RING_BUFFER,
                bufferConfig.getFillPolicy());
    }

    private PerfettoConfig.TraceConfig getTraceConfig(Bundle b)
            throws InvalidProtocolBufferException {
        String configBase64 = b.getString(PerfettoTracingStrategy.PERFETTO_CONFIG_CONTENT);
        Assert.assertNotNull(configBase64);
        byte[] configBinary = Base64.decode(configBase64, Base64.DEFAULT);
        return PerfettoConfig.TraceConfig.parseFrom(configBinary);
    }
}

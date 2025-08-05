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
import static org.mockito.Mockito.spy;

import android.app.UiAutomation;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

import perfetto.protos.PerfettoConfig;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class UiTraceListenerTest {

    private UiTraceListener mListener;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mListener = spy(new UiTraceListener(new Bundle(), List.of()));
    }

    @Test
    public void testBuildDefaultConfig() {
        Bundle b = new Bundle();
        var config = mListener.buildConfig(b);

        List<String> sourceNames =
                config.getDataSourcesList().stream().map(s -> s.getConfig().getName()).toList();

        assertTrue(sourceNames.isEmpty());
    }

    @Test
    public void testBuildConfig_allOff() {
        Bundle b = new Bundle();
        b.putString(UiTraceListener.TRACE_FTRACE_KEY, "false");
        b.putString(UiTraceListener.TRACE_LAYERS_KEY, "false");
        b.putString(UiTraceListener.TRACE_SHELL_TRANSITIONS_KEY, "false");
        b.putString(UiTraceListener.TRACE_INPUT_KEY, "false");
        b.putString(UiTraceListener.PROTOLOG_GROUPS_KEY, "");

        var config = mListener.buildConfig(b);

        List<String> sourceNames =
                config.getDataSourcesList().stream().map(s -> s.getConfig().getName()).toList();

        assertTrue(sourceNames.isEmpty());
    }

    @Test
    public void testBuildConfig_allOn() {
        Bundle b = new Bundle();
        b.putString(UiTraceListener.TRACE_FTRACE_KEY, "true");
        b.putString(UiTraceListener.TRACE_LAYERS_KEY, "true");
        b.putString(UiTraceListener.TRACE_SHELL_TRANSITIONS_KEY, "true");
        b.putString(UiTraceListener.TRACE_INPUT_KEY, "true");
        b.putString(UiTraceListener.PROTOLOG_GROUPS_KEY, "WM_DEBUG_FOCUS,WM_DEBUG_ADD_REMOVE");

        mListener = new UiTraceListener(b, List.of());
        var config = mListener.buildConfig(b);

        List<String> sourceNames =
                config.getDataSourcesList().stream().map(s -> s.getConfig().getName()).toList();

        assertTrue(sourceNames.contains("linux.ftrace"));
        assertTrue(sourceNames.contains("android.surfaceflinger.layers"));
        assertTrue(sourceNames.contains("android.surfaceflinger.transactions"));
        assertTrue(sourceNames.contains("com.android.wm.shell.transition"));
        assertTrue(sourceNames.contains("android.input.inputevent"));
        assertTrue(sourceNames.contains("android.protolog"));

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
    public void executeTestPerfettoCommand() {
        var perfettoCmd =
                "perfetto --background-wait -c - -o"
                        + " /data/misc/perfetto-traces/per_test_trace_output_73.perfetto-trace";

        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        ParcelFileDescriptor[] fds = uiAutomation.executeShellCommandRwe(perfettoCmd);

        assertTrue(fds[0].getFileDescriptor().valid());
        assertTrue(fds[1].getFileDescriptor().valid());
        assertTrue(fds[2].getFileDescriptor().valid());
    }
}

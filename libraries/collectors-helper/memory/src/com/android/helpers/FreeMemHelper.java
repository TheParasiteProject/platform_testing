/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.helpers;

import android.os.SystemClock;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FreeMemHelper is a helper to parse the free memory based on total memory available from
 * proc/meminfo, private dirty and private clean information of the cached processes from dumpsys
 * meminfo.
 *
 * Example Usage:
 * freeMemHelper.startCollecting();
 * freeMemHelper.getMetrics();
 * freeMemHelper.stopCollecting();
 */
public class FreeMemHelper implements ICollectorHelper<Long> {
    private static final String TAG = FreeMemHelper.class.getSimpleName();
    private static final String SEPARATOR = "\\s+";
    private static final String DUMPSYS_MEMIFNO = "dumpsys meminfo";
    private static final String PROC_MEMINFO = "cat /proc/meminfo";
    private static final String LINE_SEPARATOR = "\\n";
    private static final Pattern CACHE_PROC_START_PATTERN = Pattern.compile(".*: Cached\\s*$");
    private static final Pattern PID_PATTERN = Pattern.compile("^.*pid(?<processid> [0-9]*).*$");
    private static final String DUMPSYS_PROCESS = "dumpsys meminfo %s";
    private static final String MEM_TOTAL = "^\\s+TOTAL\\s+.*";
    private static final String PROCESS_ID = "processid";
    public static final String MEM_AVAILABLE_CACHE_PROC_DIRTY = "MemAvailable_CacheProcDirty_bytes";
    public static final String PROC_MEMINFO_KEY_FORMAT = "proc_meminfo_%s_bytes";
    public static final String DUMPSYS_CACHED_PROC_MEMORY= "dumpsys_cached_procs_memory_bytes";
    public static final int DROPCACHE_WAIT_MSECS= 10000; // Wait for 10 secs after dropping cache

    private UiDevice mUiDevice;
    private boolean mDropCache = false;
    private String[] memInfoToCollect = null;

    @Override
    public boolean startCollecting() {
        mUiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        return true;
    }

    @Override
    public boolean stopCollecting() {
        return true;
    }

    @Override
    public Map<String, Long> getMetrics() {
        // Drop the cache and wait for 10 secs before collecting the metrics.
        if (mDropCache) {
            executeDropCachesImpl();
        }
        Map<String, Long> results = getMemInfo(memInfoToCollect);

        long cacheProcDirty =
                results.getOrDefault(
                        String.format(PROC_MEMINFO_KEY_FORMAT, "memavailable"), 0L) / 1024;
        byte[] dumpsysMemInfoBytes = MetricUtility.executeCommandBlocking(DUMPSYS_MEMIFNO,
                InstrumentationRegistry.getInstrumentation());
        List<String> cachedProcList = getCachedProcesses(dumpsysMemInfoBytes);
        Long cachedProcMemory = 0L;

        for (String process : cachedProcList) {
            Log.i(TAG, "Cached Process" + process);
            Matcher match;
            if ((match = matches(PID_PATTERN, process)) != null) {
                String processId = match.group(PROCESS_ID);
                String processDumpSysMemInfo = String.format(DUMPSYS_PROCESS, processId);
                String processInfoStr;
                Log.i(TAG, "Process Id of the cached process" + processId);
                try {
                    processInfoStr = mUiDevice.executeShellCommand(processDumpSysMemInfo);
                } catch (IOException ioe) {
                    Log.e(TAG, "Failed to get " + processDumpSysMemInfo + ".", ioe);
                    return null;
                }

                Pattern memTotalPattern = Pattern.compile(MEM_TOTAL, Pattern.MULTILINE);
                Matcher memTotalMatcher = memTotalPattern.matcher(processInfoStr);

                String[] processInfo = null;
                if (memTotalMatcher.find()) {
                    processInfo = memTotalMatcher.group(0).split(LINE_SEPARATOR);
                }
                if (processInfo != null && processInfo.length > 0) {
                    String[] procDetails = processInfo[0].trim().split(SEPARATOR);
                    int privateDirty = Integer.parseInt(procDetails[2].trim());
                    int privateClean = Integer.parseInt(procDetails[3].trim());
                    cachedProcMemory = cachedProcMemory + privateDirty + privateClean;
                    cacheProcDirty = cacheProcDirty + privateDirty + privateClean;
                    Log.i(TAG, "Cached process: " + process + " Private Dirty: "
                            + (privateDirty * 1024) + " Private Clean: " + (privateClean * 1024));
                }
            }
        }

        // Sum of all the cached process memory.
        results.put(DUMPSYS_CACHED_PROC_MEMORY, (cachedProcMemory * 1024));

        // Mem available cache proc dirty (memavailable + cachedProcMemory)
        results.put(MEM_AVAILABLE_CACHE_PROC_DIRTY, (cacheProcDirty * 1024));
        return results;
    }

    /**
     * Collects the memory information from /proc/meminfo based on a list of patterns.
     *
     * @param patternList An array of regular expression strings to search for in /proc/meminfo.
     * @return A map where keys are formatted metric names (e.g., "proc_meminfo_MemTotal_bytes") and
     *     values are the corresponding memory values in bytes.
     */
    private Map<String, Long> getMemInfo(String[] patternList) {
        Map<String, Long> results = new HashMap<>();

        String memInfoOutput;
        try {
            memInfoOutput = mUiDevice.executeShellCommand(PROC_MEMINFO);
            Log.i(TAG, "cat proc/meminfo :" + memInfoOutput);
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to read " + PROC_MEMINFO + ".", ioe);
            // Return an empty map on error to avoid NullPointerException in calling methods.
            return results;
        }

        for (String memInfoPattern : patternList) {
            Pattern pattern = Pattern.compile(memInfoPattern, Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(memInfoOutput);
            if (matcher.find()) {
                String[] memInfo = matcher.group(0).split(SEPARATOR);
                Log.i(TAG, "MemInfo: " + memInfoPattern + ": " + memInfo[1]);
                // Remove special characters (like ':', '.') and trim spaces for the key.
                String metricKey =
                        memInfoPattern.trim().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                results.put(
                        String.format(PROC_MEMINFO_KEY_FORMAT, metricKey),
                        (Long.parseLong(memInfo[1]) * 1024));
            } else {
                Log.e(TAG, "MemInfo: " + memInfoPattern + " not found.");
                // Continue processing other patterns even if one is not found.
            }
        }
        return results;
    }

    /**
     * Checks whether {@code line} matches the given {@link Pattern}.
     *
     * @return The resulting {@link Matcher} obtained by matching the {@code line} against {@code
     * pattern}, or null if the {@code line} does not match.
     */
    private static Matcher matches(Pattern pattern, String line) {
        Matcher ret = pattern.matcher(line);
        return ret.matches() ? ret : null;
    }

    /**
     * Get cached process information from dumpsys meminfo.
     *
     * @param dumpsysMemInfoBytes
     * @return list of cached processes.
     */
    List<String> getCachedProcesses(byte[] dumpsysMemInfoBytes) {
        List<String> cachedProcessList = new ArrayList<String>();
        InputStream inputStream = new ByteArrayInputStream(dumpsysMemInfoBytes);
        boolean isCacheProcSection = false;
        try (BufferedReader bfReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String currLine = null;
            while ((currLine = bfReader.readLine()) != null) {
                Log.i(TAG, currLine);
                Matcher match;
                if (!isCacheProcSection
                        && (match = matches(CACHE_PROC_START_PATTERN, currLine)) == null) {
                    // Continue until the start of cache proc section.
                    continue;
                } else {
                    if (isCacheProcSection) {
                        // If empty we encountered the end of cached process logging.
                        if (!currLine.isEmpty()) {
                            cachedProcessList.add(currLine.trim());
                        } else {
                            break;
                        }
                    }
                    isCacheProcSection = true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cachedProcessList;
    }

    /**
     * Drop the cache and wait for 10 secs before collecting the memory details.
     */
    private void executeDropCachesImpl() {
        // Create a temporary file which contains the dropCaches command.
        // Do this because we cannot write to /proc/sys/vm/drop_caches directly,
        // as executeShellCommand parses the '>' character as a literal.
        try {
            File outputDir = InstrumentationRegistry.getInstrumentation().
                    getContext().getCacheDir();
            File outputFile = File.createTempFile("drop_cache_script", ".sh", outputDir);
            outputFile.setWritable(true);
            outputFile.setExecutable(true, /*ownersOnly*/false);

            String dropCacheScriptPath = outputFile.toString();

            // If this works correctly, the next log-line will print 'Success'.
            String str = "echo 3 > /proc/sys/vm/drop_caches && echo Success || echo Failure";
            BufferedWriter writer = new BufferedWriter(new FileWriter(dropCacheScriptPath));
            writer.write(str);
            writer.close();

            String result = mUiDevice.executeShellCommand(dropCacheScriptPath);
            Log.v(TAG, "dropCaches output was: " + result);
            outputFile.delete();
            SystemClock.sleep(DROPCACHE_WAIT_MSECS);
        } catch (IOException e) {
            throw new AssertionError (e);
        }
    }

    /** Enables the drop cache before taking the memory measurement. */
    public void setDropCache() {
        mDropCache = true;
    }

    /** Sets the meminfo to be collected. */
    public void setMemInfoToCollect(String[] list) {
        memInfoToCollect = list;
    }
}

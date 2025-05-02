/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static java.util.stream.Collectors.joining;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Closeables;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for backup and restore.
 */
public abstract class BackupUtils {
    private static final String LOCAL_TRANSPORT_NAME =
            "com.android.localtransport/.LocalTransport";
    private static final String LOCAL_TRANSPORT_NAME_PRE_Q =
            "android/com.android.internal.backup.LocalTransport";
    private static final String LOCAL_TRANSPORT_PACKAGE = "com.android.localtransport";
    public static final String LOCAL_TRANSPORT_TOKEN = "1";

    private static final int USER_SYSTEM = 0;

    private static final int BACKUP_PROVISIONING_TIMEOUT_SECONDS = 30;
    private static final int BACKUP_PROVISIONING_POLL_INTERVAL_SECONDS = 1;
    private static final long BACKUP_SERVICE_INIT_TIMEOUT_SECS = TimeUnit.MINUTES.toSeconds(2);

    private static final Pattern BACKUP_MANAGER_CURRENTLY_ENABLE_STATUS_PATTERN =
            Pattern.compile("^Backup Manager currently (enabled|disabled)$");
    private static final String MATCH_LINE_BACKUP_MANAGER_IS_NOT_PENDING_INIT =
            "Backup Manager is .* not pending init.*"; // DOTALL
    private static final String MATCH_LINE_ONLY_GMS_BACKUP_TRANSPORT_PENDING_INIT =
            "(?s)" + ".*Pending init: 1.*com.google.android.gms/.backup.BackupTransportService.*";

    private static final String BACKUP_DUMPSYS_CURRENT_TOKEN_FIELD = "Current:";

    /**
     * Kicks off adb shell {@param command} and return an {@link InputStream} with the command
     * output stream. The return value can be ignored and there might no need to close it.
     */
    @CanIgnoreReturnValue
    protected abstract InputStream executeShellCommand(String command) throws IOException;

    @FormatMethod
    protected final InputStream executeShellCommand(
            @FormatString String commandTemplate, Object... commandArgs) throws IOException {
        return executeShellCommand(String.format(Locale.ENGLISH, commandTemplate, commandArgs));
    }

    public void executeShellCommandSync(String command) throws IOException {
        StreamUtil.drainAndClose(new InputStreamReader(executeShellCommand(command)));
    }

    public String getShellCommandOutput(String command) throws IOException {
        InputStream inputStream = executeShellCommand(command);
        String result = StreamUtil.readInputStream(inputStream);
        Closeables.closeQuietly(inputStream);
        return result;
    }

    @FormatMethod
    protected final String getShellCommandOutput(
            @FormatString String commandTemplate, Object... commandArgs) throws IOException {
        return getShellCommandOutput(String.format(Locale.ENGLISH, commandTemplate, commandArgs));
    }

    /** Executes shell command "bmgr backupnow <package>" and assert success. */
    public void backupNowAndAssertSuccess(String packageName) throws IOException {
        assertBackupIsSuccessful(packageName, backupNow(packageName));
    }

    /** Executes "bmgr --user <id> backupnow <package>" and assert success. */
    public void backupNowAndAssertSuccessForUser(String packageName, int userId)
            throws IOException {
        assertBackupIsSuccessful(packageName, backupNowForUser(packageName, userId));
    }

    public void backupNowAndAssertBackupNotAllowed(String packageName) throws IOException {
        assertBackupNotAllowed(packageName, getBackupNowOutput(packageName));
    }

    /** Executes shell command "bmgr backupnow <package>" and waits for completion. */
    public void backupNowSync(String packageName) throws IOException {
        StreamUtil.drainAndClose(new InputStreamReader(backupNow(packageName)));
    }

    public String getBackupNowOutput(String packageName) throws IOException {
        return StreamUtil.readInputStream(backupNow(packageName));
    }

    /** Executes shell command "bmgr restore <token> <package>" and assert success. */
    public void restoreAndAssertSuccess(String token, String packageName) throws IOException {
        assertRestoreIsSuccessful(restore(token, packageName));
    }

    /** Executes shell command "bmgr --user <id> restore <token> <package>" and assert success. */
    public void restoreAndAssertSuccessForUser(String token, String packageName, int userId)
            throws IOException {
        assertRestoreIsSuccessful(restoreForUser(token, packageName, userId));
    }

    public void restoreSync(String token, String packageName) throws IOException {
        StreamUtil.drainAndClose(new InputStreamReader(restore(token, packageName)));
    }

    public String getRestoreOutput(String token, String packageName) throws IOException {
        return StreamUtil.readInputStream(restore(token, packageName));
    }

    public boolean isLocalTransportSelected() throws IOException {
        return isLocalTransportSelectedForUser(getCurrentUserId());
    }

    /**
     * Executes shell command "bmgr --user <id> list transports" to check the currently selected
     * transport and returns {@code true} if the local transport is the selected one.
     */
    public boolean isLocalTransportSelectedForUser(int userId) throws IOException {
        return getShellCommandOutput("bmgr --user %d list transports", userId)
                .contains("* " + getLocalTransportName());
    }

    public boolean isBackupEnabled() throws IOException {
        return isBackupEnabledForUser(getCurrentUserId());
    }

    /**
     * Executes shell command "bmgr --user <id> enabled" and returns if backup is enabled for the
     * user {@code userId}.
     */
    public boolean isBackupEnabledForUser(int userId) throws IOException {
        return getShellCommandOutput("bmgr --user %d enabled", userId)
                .contains("currently enabled");
    }

    public void wakeAndUnlockDevice() throws IOException {
        executeShellCommandSync("input keyevent KEYCODE_WAKEUP");
        executeShellCommandSync("wm dismiss-keyguard");
    }

    /**
     * Returns {@link #LOCAL_TRANSPORT_NAME} if it's available on the device, or
     * {@link #LOCAL_TRANSPORT_NAME_PRE_Q} otherwise.
     */
    public String getLocalTransportName() throws IOException {
        return getShellCommandOutput("pm list packages").contains(LOCAL_TRANSPORT_PACKAGE)
                ? LOCAL_TRANSPORT_NAME : LOCAL_TRANSPORT_NAME_PRE_Q;
    }

    /** Toggles scheduling enabled or disabled. */
    public String setSchedulingEnabled(boolean enabled) throws IOException {
        return setSchedulingEnabled(enabled, getCurrentUserId());
    }

    /**
     * Toggles scheduling enabled or disabled for given user.
     *
     * <p>Executes "bmgr --user USER_ID scheduling BOOL".
     */
    public String setSchedulingEnabled(boolean enabled, int userId) throws IOException {
        return getShellCommandOutput(
                "bmgr --user %d scheduling %b", userId, enabled);
    }

    /** Cancels backup for current user and returns its output. */
    public String cancelBackups() throws IOException {
        return cancelBackupsForUser(getCurrentUserId());
    }

    /**
     * Cancels backup for given user and returns its output.
     *
     * <p>Executes "bmgr --user USER_ID cancel backups".
     */
    public String cancelBackupsForUser(int userId) throws IOException {
        return getShellCommandOutput("bmgr --user %d cancel backups", userId);
    }

    private InputStream backupNow(String packageName) throws IOException {
        return backupNowForUser(packageName, getCurrentUserId());
    }

    /**
     * Executes "bmgr --user <id> backupnow <package>" and returns an {@link InputStream} for its
     * output.
     */
    private InputStream backupNowForUser(String packageName, int userId) throws IOException {
        return executeShellCommand("bmgr --user %d backupnow %s", userId, packageName);
    }

    /**
     * Parses the output of "bmgr backupnow" command and checks that {@code packageName} wasn't
     * allowed to backup.
     *
     * Expected format: "Package <packageName> with result:  Backup is not allowed"
     *
     * TODO: Read input stream instead of string.
     */
    private void assertBackupNotAllowed(String packageName, String backupNowOutput) {
        Scanner in = new Scanner(backupNowOutput);
        boolean found = false;
        while (in.hasNextLine()) {
            String line = in.nextLine();

            if (line.contains(packageName)) {
                String result = line.split(":")[1].trim();
                if ("Backup is not allowed".equals(result)) {
                    found = true;
                }
            }
        }
        in.close();
        assertTrue("Didn't find \'Backup not allowed\' in the output", found);
    }

    /**
     * Parses the output of "bmgr backupnow" command checking that the package {@code packageName}
     * was backed up successfully. Closes the input stream.
     *
     * Expected format: "Package <package> with result: Success"
     */
    private void assertBackupIsSuccessful(String packageName, InputStream backupNowOutput)
            throws IOException {
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(backupNowOutput, StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(packageName)) {
                    String result = line.split(":")[1].trim().toLowerCase();
                    if ("success".equals(result)) {
                        return;
                    }
                }
            }
            fail("Couldn't find package in output or backup wasn't successful");
        } finally {
            StreamUtil.drainAndClose(reader);
        }
    }

    private InputStream restore(String token, String packageName) throws IOException {
        return restoreForUser(token, packageName, getCurrentUserId());
    }

    /**
     * Executes "bmgr --user <id> restore <token> <packageName>" and returns an {@link InputStream}
     * for its output.
     */
    private InputStream restoreForUser(String token, String packageName, int userId)
            throws IOException {
        return executeShellCommand("bmgr --user %d restore %s %s", userId, token, packageName);
    }

    /**
     * Parses the output of "bmgr restore" command and checks that the package under test
     * was restored successfully. Closes the input stream.
     *
     * Expected format: "restoreFinished: 0"
     */
    private void assertRestoreIsSuccessful(InputStream restoreOutput) throws IOException {
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(restoreOutput, StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("restoreFinished: 0")) {
                    return;
                }
            }
            fail("Restore not successful");
        } finally {
            StreamUtil.drainAndClose(reader);
        }
    }

    /**
     * Execute shell command and return output from this command.
     */
    public String executeShellCommandAndReturnOutput(String command) throws IOException {
        InputStream in = executeShellCommand(command);
        BufferedReader br = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8));
        String str;
        StringBuilder out = new StringBuilder();
        while ((str = br.readLine()) != null) {
            out.append(str).append("\n");
        }
        Closeables.closeQuietly(in);
        return out.toString();
    }

    // Copied over from BackupQuotaTest
    public boolean enableBackup(boolean enable) throws Exception {
        int userId = getCurrentUserId();
        boolean previouslyEnabled;
        String output = getLineString(executeShellCommand("bmgr --user %d enabled", userId));
        Matcher matcher = BACKUP_MANAGER_CURRENTLY_ENABLE_STATUS_PATTERN.matcher(output.trim());
        if (matcher.find()) {
            previouslyEnabled = "enabled".equals(matcher.group(1));
        } else {
            throw new RuntimeException("non-parsable output setting bmgr enabled: " + output);
        }

        Closeables.closeQuietly(executeShellCommand("bmgr --user %d enable %s", userId, enable));
        return previouslyEnabled;
    }

    /**
     * Execute shell command "bmgr --user <id> enable <enable> and return previous enabled state.
     */
    public boolean enableBackupForUser(boolean enable, int userId) throws IOException {
        boolean previouslyEnabled = isBackupEnabledForUser(userId);
        executeShellCommandSync(String.format("bmgr --user %d enable %b", userId, enable));
        return previouslyEnabled;
    }

    /** Execute shell command "bmgr --user <id> activate <activate>." */
    public boolean activateBackupForUser(boolean activate, int userId) throws IOException {
        boolean previouslyActivated = isBackupActivatedForUser(userId);
        executeShellCommandSync(String.format("bmgr --user %d activate %b", userId, activate));
        return previouslyActivated;
    }

    /**
     * Executes shell command "bmgr --user <id> activated" and returns if backup is activated for
     * the user {@code userId}.
     */
    public boolean isBackupActivatedForUser(int userId) throws IOException {
        return getShellCommandOutput("bmgr --user %d activated", userId)
                .contains("currently activated");
    }

    /** Returns the current user ID obtained from "am get-current-user". */
    public int getCurrentUserId() throws IOException {
        String out = getShellCommandOutput("am get-current-user");
        try {
            return Integer.parseInt(out.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Failed to parse user id", e);
        }
    }

    private String getLineString(InputStream inputStream) throws IOException {
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String str;
        try {
            str = reader.readLine();
        } finally {
            StreamUtil.drainAndClose(reader);
        }
        return str;
    }

    private String getAllLinesString(InputStream inputStream) {
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        return reader.lines().collect(joining());
    }

    private String getPatternForBackupManagerIsNotPendingInit(int userId) {
        String userIdPrefix = "";
        if (userId != USER_SYSTEM) {
            userIdPrefix = "User " + userId + ":";
        }
        return "(?s)^" + userIdPrefix + MATCH_LINE_BACKUP_MANAGER_IS_NOT_PENDING_INIT;
    }

    /** Blocks until all backup transports has initialised */
    public void waitForBackupInitialization() throws IOException {
        int userId = getCurrentUserId();
        long tryUntilNanos = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(BACKUP_PROVISIONING_TIMEOUT_SECONDS);
        while (System.nanoTime() < tryUntilNanos) {
            String output = getLineString(executeShellCommand("dumpsys backup"));
            if (output.matches(getPatternForBackupManagerIsNotPendingInit(userId))) {
                return;
            }
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(BACKUP_PROVISIONING_POLL_INTERVAL_SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new IOException("Timed out waiting for backup initialization");
    }

    /**
     * Blocks until all backup transports has initialised, or only GmsBackupTransport has not
     * initialised yet.
     */
    public void waitForNonGmsTransportInitialization() throws IOException {
        int userId = getCurrentUserId();
        long tryUntilNanos =
                System.nanoTime() + TimeUnit.SECONDS.toNanos(BACKUP_PROVISIONING_TIMEOUT_SECONDS);
        while (System.nanoTime() < tryUntilNanos) {
            String output = getAllLinesString(executeShellCommand("dumpsys backup"));
            if (output.matches(getPatternForBackupManagerIsNotPendingInit(userId))) {
                return;
            }
            if (output.matches(MATCH_LINE_ONLY_GMS_BACKUP_TRANSPORT_PENDING_INIT)) {
                return;
            }
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(BACKUP_PROVISIONING_POLL_INTERVAL_SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new IOException("Timed out waiting for backup initialization");
    }

    public void waitUntilBackupServiceIsRunning(int userId)
            throws IOException, InterruptedException {
        waitUntilBackupServiceIsRunning(userId, BACKUP_SERVICE_INIT_TIMEOUT_SECS);
    }

    @VisibleForTesting
    void waitUntilBackupServiceIsRunning(int userId, long timeout)
            throws IOException, InterruptedException {
        CommonTestUtils.waitUntil(
                "Backup Manager init timed out",
                timeout,
                () -> {
                    String output = getLineString(executeShellCommand("dumpsys backup users"));
                    return output.matches(
                            "Backup Manager is running for users:.* " + userId + "( .*)?");
                });
    }

    /**
     * Executes shell command "bmgr --user <id> list transports" and returns {@code true} if the
     * user has the {@code transport} available.
     */
    public boolean userHasBackupTransport(String transport, int userId) throws IOException {
        String output =
                getLineString(executeShellCommand("bmgr --user %d list transports", userId));
        for (String t : output.split("\n")) {
            // Parse out the '*' character used to denote the selected transport.
            t = t.replace("*", "").trim();
            if (transport.equals(t)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Executes shell command "bmgr --user <id> transport <transport>" and returns the old
     * transport.
     */
    public String setBackupTransportForUser(String transport, int userId) throws IOException {
        String output =
                executeShellCommandAndReturnOutput(
                        String.format("bmgr --user %d transport %s", userId, transport));
        Pattern pattern = Pattern.compile("\\(formerly (.*)\\)$");
        Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new RuntimeException("Non-parsable output setting bmgr transport: " + output);
        }
    }
}


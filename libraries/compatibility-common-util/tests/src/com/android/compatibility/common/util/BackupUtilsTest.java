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
 * limitations under the License
 */

package com.android.compatibility.common.util;

import static org.junit.Assert.fail;

import com.android.tradefed.util.RunUtil;

import com.google.common.truth.Expect;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Unit tests for {@link BackupUtils}
 */
@RunWith(JUnit4.class)
public class BackupUtilsTest {
    private static final int BACKUP_SERVICE_INIT_TIMEOUT_SECS = 1;

    private static final Pattern ENABLE_COMMAND_PATTERN =
            Pattern.compile("bmgr --user \\d+ enable (true|false)$");
    private static final Pattern ACTIVATE_COMMAND_PATTERN =
            Pattern.compile("bmgr --user \\d+ activate (true|false)$");

    @Rule public final Expect expect = Expect.create();

    private boolean mIsDumpsysCommandCalled;
    private boolean mIsEnableCommandCalled;
    private boolean mIsActivateCommandCalled;

    private BackupUtils mBackupUtils;

    // Map of command -> mock output.
    private final HashMap<String, String> mMockCommandOutputMap = new HashMap<>();

    // Map of command -> mock exception.
    private final HashMap<String, IOException> mMockCommandExceptionMap = new HashMap<>();

    private void onCommandReturns(String command, String output) {
        mMockCommandOutputMap.put(command, output);
    }

    private void onCommandFails(String command, IOException ex) {
        mMockCommandExceptionMap.put(command, ex);
    }

    @Before
    public void setUp() {
        mIsDumpsysCommandCalled = false;
        mIsEnableCommandCalled = false;
        mIsActivateCommandCalled = false;

        mMockCommandExceptionMap.clear();
        mMockCommandOutputMap.clear();
        // Returns the system user 0 by default.
        mMockCommandOutputMap.put("am get-current-user", "0");

        mBackupUtils =
                new BackupUtils() {
                    @Override
                    protected InputStream executeShellCommand(String command) throws IOException {
                        if (command.equals("dumpsys backup")
                                || command.equals("dumpsys backup users")) {
                            mIsDumpsysCommandCalled = true;
                        } else if (ENABLE_COMMAND_PATTERN.matcher(command).find()) {
                            mIsEnableCommandCalled = true;
                        } else if (ACTIVATE_COMMAND_PATTERN.matcher(command).find()) {
                            mIsActivateCommandCalled = true;
                        }

                        IOException ex = mMockCommandExceptionMap.get(command);
                        if (ex != null) {
                            throw ex;
                        }

                        String output = mMockCommandOutputMap.get(command);
                        if (output == null) {
                            fail("Unexpected shell command: " + command);
                        }
                        return new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8));
                    }
                };
    }

    @Test
    public void testEnableBackup_whenEnableTrueAndEnabled_returnsTrue() throws Exception {
        onCommandReturns("bmgr --user 0 enabled", "Backup Manager currently enabled");
        onCommandReturns("bmgr --user 0 enable true", "Backup Manager now enabled");
        expect.that(mBackupUtils.enableBackup(true)).isTrue();
        expect.that(mIsEnableCommandCalled).isTrue();
    }

    @Test
    public void testEnableBackup_whenEnableTrueAndDisabled_returnsFalse() throws Exception {
        onCommandReturns("bmgr --user 0 enabled", "Backup Manager currently disabled");
        onCommandReturns("bmgr --user 0 enable true", "Backup Manager now enabled");
        expect.that(mBackupUtils.enableBackup(true)).isFalse();
        expect.that(mIsEnableCommandCalled).isTrue();
    }

    @Test
    public void testEnableBackup_whenEnableFalseAndEnabled_returnsTrue() throws Exception {
        onCommandReturns("bmgr --user 0 enabled", "Backup Manager currently enabled");
        onCommandReturns("bmgr --user 0 enable false", "Backup Manager now disabled");
        expect.that(mBackupUtils.enableBackup(false)).isTrue();
        expect.that(mIsEnableCommandCalled).isTrue();
    }

    @Test
    public void testEnableBackup_whenEnableFalseAndDisabled_returnsFalse() throws Exception {
        onCommandReturns("bmgr --user 0 enabled", "Backup Manager currently disabled");
        onCommandReturns("bmgr --user 0 enable false", "Backup Manager now disabled");
        expect.that(mBackupUtils.enableBackup(false)).isFalse();
        expect.that(mIsEnableCommandCalled).isTrue();
    }

    @Test
    public void testEnableBackup_whenEnableTrueAndEnabledAndCommandsReturnMultipleLines()
            throws Exception {
        onCommandReturns("bmgr --user 0 enabled", "Backup Manager currently enabled\n...");
        onCommandReturns("bmgr --user 0 enable true", "Backup Manager now enabled\n...");
        expect.that(mBackupUtils.enableBackup(true)).isTrue();
        expect.that(mIsEnableCommandCalled).isTrue();
    }

    @Test
    public void testEnableBackup_whenQueryCommandThrows_propagatesException() throws Exception {
        onCommandReturns("bmgr --user 0 enable true", "Backup Manager now enabled");
        onCommandFails(
                "bmgr --user 0 enabled",
                new IOException("enableBackup: Failed to run command: bmgr --user 0 enabled"));

        boolean isExceptionHappened = false;
        try {
            mBackupUtils.enableBackup(true);
        } catch (IOException e) {
            // enableBackup: Failed to run command: bmgr enabled
            isExceptionHappened = true;
        }
        expect.that(isExceptionHappened).isTrue();
        expect.that(mIsEnableCommandCalled).isFalse();
    }

    @Test
    public void testEnableBackup_whenSetCommandThrows_propagatesException() throws Exception {
        onCommandReturns("bmgr --user 0 enabled", "Backup Manager currently enabled");
        onCommandFails(
                "bmgr --user 0 enable true",
                new IOException("enableBackup: Failed to run command: bmgr --user 0 enable true"));

        boolean isExceptionHappened = false;
        try {
            mBackupUtils.enableBackup(true);
        } catch (IOException e) {
            // enableBackup: Failed to run command: bmgr enable true
            isExceptionHappened = true;
        }
        expect.that(isExceptionHappened).isTrue();
        expect.that(mIsEnableCommandCalled).isTrue();
    }

    @Test
    public void testEnableBackup_whenQueryCommandReturnsInvalidString_throwsException()
            throws Exception {
        onCommandReturns("bmgr --user 0 enabled", "Backup Manager ???");
        onCommandReturns("bmgr --user 0 enable true", "Backup Manager now enabled");

        boolean isExceptionHappened = false;
        try {
            mBackupUtils.enableBackup(true);
        } catch (RuntimeException e) {
            // non-parsable output setting bmgr enabled: Backup Manager ???
            isExceptionHappened = true;
        }
        expect.that(isExceptionHappened).isTrue();
        expect.that(mIsEnableCommandCalled).isFalse();
    }

    @Test
    public void testEnableBackup_whenQueryCommandReturnsEmptyString_throwsException()
            throws Exception {
        onCommandReturns("bmgr --user 0 enabled", ""); // output is empty
        onCommandReturns("bmgr --user 0 enable true", "Backup Manager now enabled");

        boolean isExceptionHappened = false;
        try {
            mBackupUtils.enableBackup(true);
        } catch (NullPointerException e) {
            // null output by running command, bmgr enabled
            isExceptionHappened = true;
        }
        expect.that(isExceptionHappened).isTrue();
        expect.that(mIsEnableCommandCalled).isFalse();
    }

    @Test
    public void testWaitForBackupInitialization_whenEnabled() throws Exception {
        onCommandReturns(
                "dumpsys backup", "Backup Manager is enabled / provisioned / not pending init");
        mBackupUtils.waitForBackupInitialization();
        expect.that(mIsDumpsysCommandCalled).isTrue();
    }

    @Test
    public void testWaitForBackupInitialization_whenDisabled() throws Exception {
        onCommandReturns(
                "dumpsys backup", "Backup Manager is disabled / provisioned / not pending init");
        mBackupUtils.waitForBackupInitialization();
        expect.that(mIsDumpsysCommandCalled).isTrue();
    }

    @Test
    public void testWaitForBackupInitialization_whenNonSystemUser() throws Exception {
        onCommandReturns("am get-current-user", "10"); // non-system user
        onCommandReturns(
                "dumpsys backup",
                "User 10:Backup Manager is enabled / provisioned / not pending init");
        mBackupUtils.waitForBackupInitialization();
        expect.that(mIsDumpsysCommandCalled).isTrue();
    }

    @Test
    public void waitForNonGmsTransportInitialization_whenEnabled() throws Exception {
        onCommandReturns(
                "dumpsys backup", "Backup Manager is enabled / provisioned / not pending init");
        mBackupUtils.waitForNonGmsTransportInitialization();
        expect.that(mIsDumpsysCommandCalled).isTrue();
    }

    @Test
    public void waitForNonGmsTransportInitialization_whenPendingInitForGmsTransportOnly()
            throws Exception {
        onCommandReturns(
                "dumpsys backup",
                "Backup Manager is enabled / setup complete / pending init\n"
                        + "Pending init: 1\n"
                        + "    com.google.android.gms/.backup.BackupTransportService");
        mBackupUtils.waitForNonGmsTransportInitialization();
        expect.that(mIsDumpsysCommandCalled).isTrue();
    }

    @Test
    public void waitForNonGmsTransportInitialization_whenDisabled() throws Exception {
        onCommandReturns(
                "dumpsys backup", "Backup Manager is disabled / provisioned / not pending init");
        mBackupUtils.waitForNonGmsTransportInitialization();
        expect.that(mIsDumpsysCommandCalled).isTrue();
    }

    @Test
    public void testWaitUntilBackupServiceIsRunning_whenRunning_doesntThrow() throws Exception {
        // User 10.
        onCommandReturns("dumpsys backup users", "Backup Manager is running for users: 10");

        try {
            // User 10.
            mBackupUtils.waitUntilBackupServiceIsRunning(10, BACKUP_SERVICE_INIT_TIMEOUT_SECS);
        } catch (AssertionError e) {
            fail("BackupUtils#waitUntilBackupServiceIsRunning threw an exception");
        }
        expect.that(mIsDumpsysCommandCalled).isTrue();
    }

    @Test
    public void testWaitUntilBackupServiceIsRunning_whenNotRunning_throws() throws Exception {
        // Pass in a different userId (11) to not have the current one among running ids.
        onCommandReturns("dumpsys backup users", "Backup Manager is running for users: 11");

        boolean wasExceptionThrown = false;
        try {
            // User 10.
            mBackupUtils.waitUntilBackupServiceIsRunning(10, BACKUP_SERVICE_INIT_TIMEOUT_SECS);
        } catch (AssertionError e) {
            wasExceptionThrown = true;
        }

        expect.that(mIsDumpsysCommandCalled).isTrue();
        expect.that(wasExceptionThrown).isTrue();
    }

    @Test
    public void testWaitForBackupInitialization_whenEnabledAndCommandReturnsMultipleLines()
            throws Exception {
        onCommandReturns(
                "dumpsys backup",
                "Backup Manager is enabled / provisioned / not pending init\n...");
        mBackupUtils.waitForBackupInitialization();
        expect.that(mIsDumpsysCommandCalled).isTrue();
    }

    @Test
    public void testWaitForBackupInitialization_whenCommandThrows_propagatesException()
            throws Exception {
        onCommandFails(
                "dumpsys backup",
                new IOException(
                        "waitForBackupInitialization: Failed to run command: dumpsys backup"));

        boolean isExceptionHappened = false;
        try {
            mBackupUtils.waitForBackupInitialization();
        } catch (IOException e) {
            // waitForBackupInitialization: Failed to run command: dumpsys backup
            isExceptionHappened = true;
        }
        expect.that(isExceptionHappened).isTrue();
        expect.that(mIsDumpsysCommandCalled).isTrue();
    }

    @Test
    public void testWaitForBackupInitialization_whenCommandReturnsInvalidString()
            throws Exception {
        onCommandReturns("dumpsys backup", "Backup Manager ???");

        class TestRunnable implements Runnable {
            @Override
            public void run() {
                try {
                    mBackupUtils.waitForBackupInitialization();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        TestRunnable testRunnable = new TestRunnable();
        Thread testThread = new Thread(testRunnable);

        try {
            testThread.start();
            RunUtil.getDefault().sleep(100);
            expect.that(mIsDumpsysCommandCalled).isTrue();
            expect.that(testThread.isAlive()).isTrue();
        } catch (Exception e) {
            // ignore
        } finally {
            testThread.interrupt();
        }
    }

    @Test
    public void testWaitForBackupInitialization_whenCommandReturnsEmptyString_throwsException()
            throws Exception {
        onCommandReturns("dumpsys backup", ""); // output is empty

        boolean isExceptionHappened = false;
        try {
            mBackupUtils.waitForBackupInitialization();
        } catch (NullPointerException e) {
            // null output by running command, dumpsys backup
            isExceptionHappened = true;
        }
        expect.that(isExceptionHappened).isTrue();
        expect.that(mIsDumpsysCommandCalled).isTrue();
    }

    @Test
    public void testWaitForBackupInitialization_whenUserIdDoesNotMatch() throws Exception {
        onCommandReturns(
                "dumpsys backup",
                "User 10:Backup Manager is enabled / provisioned / not pending init");
        onCommandReturns("am get-current-user", "11"); // User ID doesn't match.

        class TestRunnable implements Runnable {
            @Override
            public void run() {
                try {
                    mBackupUtils.waitForBackupInitialization();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        TestRunnable testRunnable = new TestRunnable();
        Thread testThread = new Thread(testRunnable);

        try {
            testThread.start();
            RunUtil.getDefault().sleep(100);
            expect.that(mIsDumpsysCommandCalled).isTrue();
            expect.that(testThread.isAlive()).isTrue();
        } catch (Exception e) {
            // ignore
        } finally {
            testThread.interrupt();
        }
    }

    @Test
    public void testActivateBackup_whenEnableTrueAndEnabled_returnsTrue() throws Exception {
        onCommandReturns("bmgr --user 10 activated", "Backup Manager currently activated");
        onCommandReturns("bmgr --user 10 activate true", "Backup Manager now activated");
        expect.that(mBackupUtils.activateBackupForUser(true, 10)).isTrue();
        expect.that(mIsActivateCommandCalled).isTrue();
    }

    @Test
    public void testActivateBackup_whenEnableTrueAndDisabled_returnsFalse() throws Exception {
        onCommandReturns("bmgr --user 10 activated", "Backup Manager currently deactivated");
        onCommandReturns("bmgr --user 10 activate true", "Backup Manager now activated");
        expect.that(mBackupUtils.activateBackupForUser(true, 10)).isFalse();
        expect.that(mIsActivateCommandCalled).isTrue();
    }

    @Test
    public void testActivateBackup_whenEnableFalseAndEnabled_returnsTrue() throws Exception {
        onCommandReturns("bmgr --user 10 activated", "Backup Manager currently activated");
        onCommandReturns("bmgr --user 10 activate false", "Backup Manager now deactivated");
        expect.that(mBackupUtils.activateBackupForUser(false, 10)).isTrue();
        expect.that(mIsActivateCommandCalled).isTrue();
    }

    @Test
    public void testActivateBackup_whenEnableFalseAndDisabled_returnsFalse() throws Exception {
        onCommandReturns("bmgr --user 10 activated", "Backup Manager currently deactivated");
        onCommandReturns("bmgr --user 10 activate false", "Backup Manager now deactivated");
        expect.that(mBackupUtils.activateBackupForUser(false, 10)).isFalse();
        expect.that(mIsActivateCommandCalled).isTrue();
    }
}

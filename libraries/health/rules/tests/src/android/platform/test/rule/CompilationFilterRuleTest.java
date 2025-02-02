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
package android.platform.test.rule;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;

import android.os.Bundle;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.List;

/** Unit test the logic for {@link CompilationFilterRule} */
@RunWith(JUnit4.class)
public class CompilationFilterRuleTest {
    private static final Description TEST_DESC = Description.createTestDescription("clzz", "mthd");

    @After
    public void teardown() {
        CompilationFilterRule.mCompiledTests.clear();
    }

    /** Tests that this rule will do nothing if no package are supplied. */
    @Test
    public void testNoAppToCompile() throws Throwable {
        Bundle filterBundle = new Bundle();
        filterBundle.putString(CompilationFilterRule.COMPILE_PACKAGE_NAMES_OPTION, "");
        TestableCompilationFilterRule rule =
                new TestableCompilationFilterRule(filterBundle) {
                    @Override
                    protected String executeShellCommand(String cmd) {
                        super.executeShellCommand(cmd);
                        return CompilationFilterRule.COMPILE_SUCCESS;
                    }
                };
        rule.apply(rule.getTestStatement(), Description.createTestDescription("clzz", "mthd1"))
                .evaluate();
    }

    /** Tests that this rule will compile the app after the test, if supplied. */
    @Test
    public void testSingleAppToCompile() throws Throwable {
        Bundle filterBundle = new Bundle();
        filterBundle.putString(CompilationFilterRule.COMPILE_FILTER_OPTION, "speed");
        filterBundle.putString(
                CompilationFilterRule.COMPILE_PACKAGE_NAMES_OPTION, "example.package");
        TestableCompilationFilterRule rule =
                new TestableCompilationFilterRule(filterBundle) {
                    @Override
                    protected String executeShellCommand(String cmd) {
                        super.executeShellCommand(cmd);
                        return CompilationFilterRule.COMPILE_SUCCESS;
                    }
                };
        rule.apply(rule.getTestStatement(), Description.createTestDescription("clzz", "mthd2"))
                .evaluate();
        String compileCmd =
                String.format(CompilationFilterRule.COMPILE_CMD_FORMAT, "speed", "example.package");
        assertThat(rule.getOperations()).containsExactly("test", compileCmd).inOrder();
    }

    /** Tests that this rule will fail to run and throw if the option is bad. */
    @Test
    public void testAppToCompile_badFilterThrows() throws Throwable {
        Bundle badFilterBundle = new Bundle();
        badFilterBundle.putString(CompilationFilterRule.COMPILE_FILTER_OPTION, "bad-option");
        TestableCompilationFilterRule rule = new TestableCompilationFilterRule(badFilterBundle,
                "example.package");
        try {
            rule.apply(rule.getTestStatement(), TEST_DESC).evaluate();
            fail("An exception should have been thrown about bad filter, but wasn't.");
        } catch (IllegalArgumentException e) {
        }
    }

    /** Tests that this rule will compile one app after the test, if supplied. */
    @Test
    public void testAppToCompile_failCompilationThrows() throws Throwable {
        Bundle filterBundle = new Bundle();
        filterBundle.putString(CompilationFilterRule.COMPILE_FILTER_OPTION, "speed");
        TestableCompilationFilterRule rule = new TestableCompilationFilterRule(filterBundle,
                "example.package") {
            @Override
            protected String executeShellCommand(String cmd) {
                super.executeShellCommand(cmd);
                return "Error";
            }
        };
        try {
            rule.apply(rule.getTestStatement(), TEST_DESC).evaluate();
            fail("An exception should have been thrown about compilation failure, but wasn't.");
        } catch (RuntimeException e) {
        }
    }

    /** Tests that this rule will compile one app after the test, if supplied. */
    @Test
    public void testOneAppToCompile() throws Throwable {
        Bundle filterBundle = new Bundle();
        filterBundle.putString(CompilationFilterRule.COMPILE_FILTER_OPTION, "speed");
        TestableCompilationFilterRule rule = new TestableCompilationFilterRule(filterBundle,
                "example.package") {
            @Override
            protected String executeShellCommand(String cmd) {
                super.executeShellCommand(cmd);
                return CompilationFilterRule.COMPILE_SUCCESS;
            }
        };
        rule.apply(rule.getTestStatement(), Description.createTestDescription("clzz", "mthd1"))
                .evaluate();
        String compileCmd = String.format(CompilationFilterRule.COMPILE_CMD_FORMAT, "speed",
                "example.package");
        assertThat(rule.getOperations()).containsExactly("test", compileCmd)
                .inOrder();
    }

    /** Tests that this rule will compile a app only after the first iteration of the test. */
    public void testOneAppToCompileMultipleIterations(Description test1, Description test2)
            throws Throwable {
        Bundle filterBundle = new Bundle();
        filterBundle.putString(CompilationFilterRule.COMPILE_FILTER_OPTION, "speed");
        TestableCompilationFilterRule rule = new TestableCompilationFilterRule(filterBundle,
                "example.package") {
            @Override
            protected String executeShellCommand(String cmd) {
                super.executeShellCommand(cmd);
                return CompilationFilterRule.COMPILE_SUCCESS;
            }
        };
        rule.apply(rule.getTestStatement(), test1).evaluate();
        rule.apply(rule.getTestStatement(), test2).evaluate();
        String compileCmd = String.format(CompilationFilterRule.COMPILE_CMD_FORMAT, "speed",
                "example.package");
        assertThat(rule.getOperations()).containsExactly("test", compileCmd,"test")
                .inOrder();
    }

    @Test
    public void testOneAppToCompileMultipleIterations_renameOnClass() throws Throwable {
        testOneAppToCompileMultipleIterations(
                Description.createTestDescription("clzz$1", "mthd1"),
                Description.createTestDescription("clzz$2", "mthd1"));
    }

    @Test
    public void testOneAppToCompileMultipleIterations_renameOnMethod() throws Throwable {
        testOneAppToCompileMultipleIterations(
                Description.createTestDescription("clzz", "mthd1$1"),
                Description.createTestDescription("clzz", "mthd1$2"));
    }

    /** Tests that this rule will compile a app multiple times for different tests. */
    public void testOneAppMultipleCompileMultipleTests(Description test1, Description test2)
            throws Throwable {
        Bundle filterBundle = new Bundle();
        filterBundle.putString(CompilationFilterRule.COMPILE_FILTER_OPTION, "speed");
        TestableCompilationFilterRule rule = new TestableCompilationFilterRule(filterBundle,
                "example.package") {
            @Override
            protected String executeShellCommand(String cmd) {
                super.executeShellCommand(cmd);
                return CompilationFilterRule.COMPILE_SUCCESS;
            }
        };
        rule.apply(rule.getTestStatement(), test1).evaluate();
        rule.apply(rule.getTestStatement(), test2).evaluate();
        String compileCmd = String.format(CompilationFilterRule.COMPILE_CMD_FORMAT, "speed",
                "example.package");
        assertThat(rule.getOperations()).containsExactly("test", compileCmd, "test", compileCmd)
                .inOrder();
    }

    @Test
    public void testOneAppMultipleCompileMultipleTests_renameOnClass() throws Throwable {
        testOneAppMultipleCompileMultipleTests(
                Description.createTestDescription("clzz$1", "mthd1"),
                Description.createTestDescription("clzz$2", "mthd2"));
    }

    @Test
    public void testOneAppMultipleCompileMultipleTests_renameOnMethod() throws Throwable {
        testOneAppMultipleCompileMultipleTests(
                Description.createTestDescription("clzz", "mthd1$1"),
                Description.createTestDescription("clzz", "mthd2$1"));
    }

    /** Tests that this rule will compile a app only once for duplicate tests. */
    @Test
    public void testOneAppToCompileDuplicateTests() throws Throwable {
        Bundle filterBundle = new Bundle();
        filterBundle.putString(CompilationFilterRule.COMPILE_FILTER_OPTION, "speed");
        TestableCompilationFilterRule rule = new TestableCompilationFilterRule(filterBundle,
                "example.package") {
            @Override
            protected String executeShellCommand(String cmd) {
                super.executeShellCommand(cmd);
                return CompilationFilterRule.COMPILE_SUCCESS;
            }
        };
        rule.apply(rule.getTestStatement(), Description.createTestDescription("clzz", "mthd1"))
                .evaluate();
        rule.apply(rule.getTestStatement(), Description.createTestDescription("clzz", "mthd1"))
                .evaluate();
        String compileCmd = String.format(CompilationFilterRule.COMPILE_CMD_FORMAT, "speed",
                "example.package");
        assertThat(rule.getOperations()).containsExactly("test", compileCmd,"test")
                .inOrder();
    }

    /** Tests that this rule will compile multiple apps after the test, if supplied. */
    @Test
    public void testMultipleAppsToCompile() throws Throwable {
        Bundle filterBundle = new Bundle();
        filterBundle.putString(CompilationFilterRule.COMPILE_FILTER_OPTION, "speed");
        TestableCompilationFilterRule rule = new TestableCompilationFilterRule(filterBundle,
                "example.package1", "example.package2") {
            @Override
            protected String executeShellCommand(String cmd) {
                super.executeShellCommand(cmd);
                return CompilationFilterRule.COMPILE_SUCCESS;
            }
        };
        rule.apply(rule.getTestStatement(), Description.createTestDescription("clzz", "mthd2"))
                .evaluate();
        String compileCmd1 = String.format(
                CompilationFilterRule.COMPILE_CMD_FORMAT, "speed", "example.package1");
        String compileCmd2 = String.format(
                CompilationFilterRule.COMPILE_CMD_FORMAT, "speed", "example.package2");
        assertThat(rule.getOperations())
                .containsExactly("test", compileCmd1, compileCmd2)
                .inOrder();
    }

    /** Tests that this rule will speed profile compile multiple apps after the test,
     *  if supplied. */
    @Test
    public void testMultipleAppsToCompileInSpeedProfile() throws Throwable {
        Bundle filterBundle = new Bundle();
        filterBundle.putString(CompilationFilterRule.COMPILE_FILTER_OPTION,
                CompilationFilterRule.SPEED_PROFILE_FILTER);
        TestableCompilationFilterRule rule = new TestableCompilationFilterRule(filterBundle,
                "example.package1", "example.package2") {
            @Override
            protected String executeShellCommand(String cmd) {
                super.executeShellCommand(cmd);
                if (cmd.contains("killall -s SIGUSR1")) {
                    return "";
                }
                return CompilationFilterRule.COMPILE_SUCCESS;
            }
        };
        rule.apply(rule.getTestStatement(), Description.createTestDescription("clzz", "mthd3"))
                .evaluate();
        String dumpCmd1 = String.format(CompilationFilterRule.DUMP_PROFILE_CMD,
                "example.package1");
        String compileCmd1 = String.format(CompilationFilterRule.COMPILE_CMD_FORMAT,
                CompilationFilterRule.SPEED_PROFILE_FILTER, "example.package1");
        String dumpCmd2 = String.format(CompilationFilterRule.DUMP_PROFILE_CMD,
                "example.package2");
        String compileCmd2 = String.format(CompilationFilterRule.COMPILE_CMD_FORMAT,
                CompilationFilterRule.SPEED_PROFILE_FILTER, "example.package2");
        assertThat(rule.getOperations())
                .containsExactly("test", dumpCmd1, compileCmd1, dumpCmd2, compileCmd2)
                .inOrder();
    }

    private static class TestableCompilationFilterRule extends CompilationFilterRule {
        private List<String> mOperations = new ArrayList<>();
        private Bundle mBundle;

        public TestableCompilationFilterRule(Bundle bundle, String... applications) {
            super(applications);
            mBundle = bundle;
        }

        @Override
        protected String executeShellCommand(String cmd) {
            mOperations.add(cmd);
            return "";
        }

        @Override
        protected Bundle getArguments() {
            return mBundle;
        }

        public List<String> getOperations() {
            return mOperations;
        }

        public Statement getTestStatement() {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    mOperations.add("test");
                }
            };
        }
    }
}

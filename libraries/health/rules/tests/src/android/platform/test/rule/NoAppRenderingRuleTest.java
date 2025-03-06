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
package android.platform.test.rule;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.runner.Description.createTestDescription;

import android.os.Bundle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.List;

/** Unit test the logic for {@link NoAppRenderingRule} */
@RunWith(JUnit4.class)
public class NoAppRenderingRuleTest {
    /** Tests the rule will not run any commands by default. */
    @Test
    public void testNoAppRenderingRule_disabledByDefault() throws Throwable {
        Bundle empty = new Bundle();
        TestableNoAppRenderingRule rule = new TestableNoAppRenderingRule(empty);

        rule.apply(rule.getTestStatement(), createTestDescription("clzz", "mthd")).evaluate();
        assertThat(rule.getOperations()).containsExactly("test");
    }

    /** Tests the rule will not run any commands when disabled. */
    @Test
    public void testNoAppRenderingRule_disabledByFlag() throws Throwable {
        Bundle noAppRenderingBundle = new Bundle();
        noAppRenderingBundle.putString(NoAppRenderingRule.STOP_NEW_RENDERING, "false");
        TestableNoAppRenderingRule rule = new TestableNoAppRenderingRule(noAppRenderingBundle);

        rule.apply(rule.getTestStatement(), createTestDescription("clzz", "mthd")).evaluate();
        assertThat(rule.getOperations()).containsExactly("test");
    }

    /** Tests the rule will run the commands when enabled. */
    @Test
    public void testNoAppRenderingRule_enabledByFlag() throws Throwable {
        Bundle noAppRenderingBundle = new Bundle();
        noAppRenderingBundle.putString(NoAppRenderingRule.STOP_NEW_RENDERING, "true");
        TestableNoAppRenderingRule rule = new TestableNoAppRenderingRule(noAppRenderingBundle);

        rule.apply(rule.getTestStatement(), createTestDescription("clzz", "mthd")).evaluate();
        assertThat(rule.getOperations())
                .containsExactly(
                        "setprop debug.hwui.drawing_enabled 0",
                        "test",
                        "setprop debug.hwui.drawing_enabled 1")
                .inOrder();
    }

    private static class TestableNoAppRenderingRule extends NoAppRenderingRule {
        private List<String> mOperations = new ArrayList<>();
        private Bundle mBundle;

        TestableNoAppRenderingRule(Bundle bundle) {
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

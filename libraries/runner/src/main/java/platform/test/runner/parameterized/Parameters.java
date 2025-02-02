/*
 * Copyright (C) 2023 The Android Open Source Project
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

package platform.test.runner.parameterized;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.MessageFormat;

/**
 * Annotation for a method which provides parameters to be injected into the test class constructor
 * by {@code ParameterizedAndroidJunit4}.
 *
 * <p>{@see org.junit.runners.Parameterized.Parameters}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Parameters {

    /**
     * Optional pattern to derive the test's name from the parameters. Use numbers in braces to
     * refer to the parameters or the additional data as follows:
     *
     * <pre>
     * {index} - the current parameter index
     * {0} - the first parameter value
     * {1} - the second parameter value
     * etc...
     * </pre>
     *
     * <p>Default value is "{index}" for compatibility with previous JUnit versions.
     *
     * @return {@link MessageFormat} pattern string, except the index placeholder.
     * @see MessageFormat
     */
    String name() default "{index}";

    /**
     * Specifies the test environment this parameter provider should be used. A test can define
     * multiple methods as Parameters provider. The runner will first try to pick the most specific
     * provider, and fallback to the default provider if there is none.
     */
    TestEnvironment target() default TestEnvironment.ALL;

    enum TestEnvironment {
        ALL,
        DEVICE,
        DEVICE_LESS
    }
}

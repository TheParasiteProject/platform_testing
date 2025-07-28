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

package com.google.android.libraries.automotive.val.api;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import org.jspecify.annotations.Nullable;

/** Class creates objects that either contain an error code or an actual value. */
@AutoValue
public abstract class ErrorOr<T> {
  /** Enum of possible error codes. */
  public enum ErrorCode {
    /** This error means that an AAOS platform API is not behaving as expected. */
    ERROR_CODE_BAD_PLATFORM_IMPL,

    /** This error means that a specific property is not supported on this vehicle. */
    ERROR_CODE_PROPERTY_NOT_SUPPORTED,

    /** This error means that a specific area is not supported for a property on this vehicle. */
    ERROR_CODE_AREA_NOT_SUPPORTED,

    /**
     * This error is used represent an error that should never happen in the VAL code. If it does,
     * then the VAL code needs to be updated.
     */
    ERROR_CODE_BAD_VAL_IMPL,

    /**
     * This error means that a property is currently unavailable. This may be temporary or long
     * term.
     */
    ERROR_CODE_PROPERTY_NOT_AVAILABLE,

    /**
     * This error corresponds to the AAOS platform exception {@link CarInternalErrorException} and
     * when {@link CarPropertyValue#getStatus()} returns {@link CarPropertyValue#STATUS_ERROR}. The
     * {@link CarInternalErrorException} is thrown when something unexpected happens in the VHAL.
     */
    ERROR_CODE_PLATFORM_INTERNAL_ERROR,

    /**
     * This error corresponds to the AAOS platform exception {@link
     * PropertyAccessDeniedSecurityException}. It is thrown when the VHAL denies access to a
     * property.
     */
    ERROR_CODE_PROPERTY_ACCESS_DENIED_SECURITY,

    /** This error means that a specific action is not supported on this vehicle. */
    ERROR_CODE_ACTION_NOT_SUPPORTED,

    /** This error means that a specific element is not defined/unknown for a action. */
    ERROR_CODE_UNDEFINED_ELEMENT,

    /**
     * This error means that the required permission is not granted for a action to be supported.
     */
    ERROR_CODE_MISSING_REQUIRED_PERMISSION,

    /**
     * This error means that the {@link CarPropertyManagerCompat#SetConfirmationCallback} timed out
     * before receiving the expected events after {@link CarPropertyManager#setProperty()} was
     * called.
     */
    ERROR_CODE_SET_PROPERTY_CALLBACK_TIMED_OUT,

    /**
     * This error means that the {@link CarPropertyManagerCompat#SetConfirmationCallback} received
     * an {@link InterruptedException} before receiving the expected events after {@link
     * CarPropertyManager#setProperty()} was called.
     */
    ERROR_CODE_SET_PROPERTY_CALLBACK_INTERRUPT_EXCEPTION,

    /** This error means that a specific element is not supported for an action on this vehicle. */
    ERROR_CODE_ELEMENT_NOT_SUPPORTED,

    /** This error means that the VAL API user passed an invalid argument. */
    ERROR_CODE_INVALID_API_ARGUMENT,

    /**
     * This error means that the property ID + area ID combo's value is already equal to the target
     * set value.
     */
    ERROR_CODE_VALUE_ALREADY_SET,

    /**
     * This error means that the action's underlying property is HVAC power dependent, and
     * currently, the HVAC power is disabled which is preventing the action from executing fully.
     */
    ERROR_CODE_HVAC_POWER_IS_DISABLED,

    /**
     * This error means that the property ID + area ID combo's target set value is not supported for
     * the action or property ID + area ID combo.
     */
    ERROR_CODE_VALUE_NOT_SUPPORTED,

    /**
     * This error means that the property ID + area ID combo's target set value is below the minimum
     * value supported for the action or property ID + area ID combo.
     */
    ERROR_CODE_VALUE_BELOW_MINIMUM,

    /**
     * This error means that the property ID + area ID combo's target set value is above the maximum
     * value supported for the action or property ID + area ID combo.
     */
    ERROR_CODE_VALUE_ABOVE_MAXIMUM,

    /** This error means that the one of the property's areas is not readable. */
    ERROR_CODE_AREA_NOT_READABLE,

    /** This error means that the one of the property's areas is not writable. */
    ERROR_CODE_AREA_NOT_WRITABLE,
  }

  /** Creates an error instance. */
  public static <U> ErrorOr<U> createError(ErrorCode errorCode) {
    Preconditions.checkNotNull(errorCode);
    return new AutoValue_ErrorOr<U>((U) null, errorCode);
  }

  /** Creates an instance with a value. */
  public static <U> ErrorOr<U> createValue(U value) {
    Preconditions.checkNotNull(value);
    Preconditions.checkArgument(!(value instanceof ErrorCode));
    return new AutoValue_ErrorOr<U>(value, null);
  }

  /** Returns {@code true} if {@link #errorCode()} is not {@code null}. Otherwise {@code false}. */
  public boolean isError() {
    return errorCode() != null;
  }

  /** Returns value if no error. Otherwise {@code null}. */
  public abstract @Nullable T value();

  /** Returns error code if an error. Otherwise {@code null}. */
  public abstract @Nullable ErrorCode errorCode();
}

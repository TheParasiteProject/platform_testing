/*
 * Copyright 2025 The Android Open Source Project
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

package platform.test.desktop;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static junit.framework.Assert.assertTrue;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.companion.AssociationInfo;
import android.companion.AssociationRequest;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.filters.SdkSuppress;

import org.junit.rules.ExternalResource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * A test rule that creates a {@link CompanionDeviceManager} association with the instrumented
 * package for the duration of the test.
 *
 * <p>NOTE: This is duplicated from
 * cts/tests/tests/virtualdevice/common/src/android/virtualdevice/cts/common/FakeAssociationRule as
 * a simplified version that only works for API 36 and above, and not pulling extra libs required by
 * VirtualDeviceRule.
 *
 * <p>TODO: b/392534769 - Remove this once DesktopMouseTestRule migrated to use UinputMouse.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
class FakeAssociationRule extends ExternalResource {
    private static final String TAG = "FakeAssociationRule";

    private static final String DEVICE_PROFILE = AssociationRequest.DEVICE_PROFILE_APP_STREAMING;
    private static final String DISPLAY_NAME = "CTS CDM VDM Association";

    private static final int TIMEOUT_MS = 10000;

    private final Context mContext = getInstrumentation().getContext();

    private final Executor mCallbackExecutor = Runnable::run;
    private final CompanionDeviceManager mCompanionDeviceManager =
            mContext.getSystemService(CompanionDeviceManager.class);

    @Mock
    private CompanionDeviceManager.OnAssociationsChangedListener mOnAssociationsChangedListener;

    private AssociationInfo mAssociationInfo;

    private AssociationInfo createManagedAssociation() {
        final AssociationInfo[] managedAssociation = new AssociationInfo[1];
        AssociationRequest request =
                new AssociationRequest.Builder()
                        .setDeviceProfile(DEVICE_PROFILE)
                        .setDisplayName(DISPLAY_NAME)
                        .setSelfManaged(true)
                        .setSkipRoleGrant(true)
                        .build();
        CountDownLatch latch = new CountDownLatch(1);
        CompanionDeviceManager.Callback callback =
                new CompanionDeviceManager.Callback() {
                    @Override
                    public void onAssociationCreated(@NonNull AssociationInfo associationInfo) {
                        managedAssociation[0] = associationInfo;
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(@Nullable CharSequence error) {
                        fail(error == null ? "Failed to create CDM association" : error.toString());
                    }
                };
        reset(mOnAssociationsChangedListener);
        mCompanionDeviceManager.associate(request, Runnable::run, callback);

        try {
            latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail("Interrupted while waiting for CDM association: " + e);
        }

        verifyAssociationsChanged();
        return managedAssociation[0];
    }

    @Override
    protected void before() throws Throwable {
        super.before();
        MockitoAnnotations.initMocks(this);
        assumeTrue(hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP));
        mCompanionDeviceManager.addOnAssociationsChangedListener(
                mCallbackExecutor, mOnAssociationsChangedListener);
        clearExistingAssociations();
        mAssociationInfo = createManagedAssociation();
    }

    @Override
    protected void after() {
        super.after();
        clearExistingAssociations();
        mCompanionDeviceManager.removeOnAssociationsChangedListener(mOnAssociationsChangedListener);
    }

    private void clearExistingAssociations() {
        List<AssociationInfo> associations = mCompanionDeviceManager.getMyAssociations();
        for (AssociationInfo association : associations) {
            disassociate(association.getId());
        }
        assertTrue(mCompanionDeviceManager.getMyAssociations().isEmpty());
        mAssociationInfo = null;
    }

    AssociationInfo getAssociationInfo() {
        return mAssociationInfo;
    }

    private void disassociate(int associationId) {
        reset(mOnAssociationsChangedListener);
        mCompanionDeviceManager.disassociate(associationId);
        verifyAssociationsChanged();
    }

    private void verifyAssociationsChanged() {
        verify(
                        mOnAssociationsChangedListener,
                        timeout(TIMEOUT_MS)
                                .description(
                                        TAG
                                                + ": onAssociationChanged not called, total"
                                                + " associations: "
                                                + mCompanionDeviceManager
                                                        .getMyAssociations()
                                                        .size()))
                .onAssociationsChanged(any());
    }

    /** Returns true if the device has a given system feature */
    private boolean hasSystemFeature(String feature) {
        return mContext.getPackageManager().hasSystemFeature(feature);
    }
}

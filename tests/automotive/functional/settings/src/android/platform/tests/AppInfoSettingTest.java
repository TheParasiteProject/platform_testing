package android.platform.tests;

import static junit.framework.Assert.assertTrue;

import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoAppInfoSettingsHelper;
import android.platform.helpers.IAutoAppInfoSettingsHelper.State;
import android.platform.helpers.IAutoSettingHelper;
import android.platform.helpers.SettingsConstants;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class AppInfoSettingTest {
    private HelperAccessor<IAutoAppInfoSettingsHelper> mAppInfoSettingsHelper;
    private HelperAccessor<IAutoSettingHelper> mSettingHelper;
    private static final String LOG_TAG = AppInfoSettingTest.class.getSimpleName();

    private static final String CONTACTS_APP = "Contacts";
    private static final String PHONE_PERMISSION = "Phone";
    private static final String CONTACT_PACKAGE = "com.android.contacts";

    public AppInfoSettingTest() throws Exception {
        mAppInfoSettingsHelper = new HelperAccessor<>(IAutoAppInfoSettingsHelper.class);
        mSettingHelper = new HelperAccessor<>(IAutoSettingHelper.class);
    }
    @Before
    public void openAppInfoFacet() {
        mSettingHelper.get().openSetting(SettingsConstants.APPS_SETTINGS);
        boolean hasAppsInfoUiElement =
                mSettingHelper.get().checkMenuExists("Reset app grid to A-Z order")
                        || mSettingHelper.get().checkMenuExists("Recently opened");
        assertTrue("Apps setting did not open.", hasAppsInfoUiElement);
        mAppInfoSettingsHelper.get().showAllApps();
    }

    @After
    public void goBackToSettingsScreen() {
        mSettingHelper.get().exit();
    }

    @Test
    public void testDisableEnableApplication() {
        Log.i(LOG_TAG, "Act: Open the Contacts App");
        mAppInfoSettingsHelper.get().selectApp(CONTACTS_APP);
        Log.i(LOG_TAG, "Act: Disable the contacts app");
        mAppInfoSettingsHelper.get().enableDisableApplication(State.DISABLE);
        Log.i(LOG_TAG, "Assert: Application is disabled");
        assertTrue(
                "Application is not disabled",
                mAppInfoSettingsHelper.get().isApplicationDisabled(CONTACT_PACKAGE));
        Log.i(LOG_TAG, "Act: Enable the contacts app");
        mAppInfoSettingsHelper.get().enableDisableApplication(State.ENABLE);
        Log.i(LOG_TAG, "Assert: Application is enabled");
        assertTrue(
                "Application is not enabled",
                !mAppInfoSettingsHelper.get().isApplicationDisabled(CONTACT_PACKAGE));
    }

    @Test
    public void testApplicationPermissions() {
        Log.i(LOG_TAG, "Act: Open the Contacts App");
        mAppInfoSettingsHelper.get().selectApp(CONTACTS_APP);
        Log.i(LOG_TAG, "Act: Enable the phone app permission");
        mAppInfoSettingsHelper.get().setAppPermission(PHONE_PERMISSION, State.ENABLE);
        Log.i(LOG_TAG, "Assert: Permission is enabled");
        assertTrue(
                "Permission is disabled",
                mAppInfoSettingsHelper.get().getCurrentPermissions().contains(PHONE_PERMISSION));
        Log.i(LOG_TAG, "Act: Disable the phone app permission");
        mAppInfoSettingsHelper.get().setAppPermission(PHONE_PERMISSION, State.DISABLE);
        Log.i(LOG_TAG, "Assert: Permission is disabled");
        assertTrue(
                "Permission is not disabled",
                !mAppInfoSettingsHelper.get().getCurrentPermissions().contains(PHONE_PERMISSION));
    }

    @Test
    public void testAllowedAppNumber() {
        Log.i(LOG_TAG, "Act: Open the App Settings");
        // Navigate to the app permission manager.
        mSettingHelper.get().openSetting(SettingsConstants.APPS_SETTINGS);
        Log.i(LOG_TAG, "Act: Open the Permission manager");
        mAppInfoSettingsHelper.get().openPermissionManager();

        // Get one specific Permission UI element (that we have not looked at before).
        // Check whether its displayed allowed apps matches its (internal) listed apps.
        List<Integer> results =
                mAppInfoSettingsHelper.get().validateAppsPermissionManager(CONTACTS_APP);
        int summaryAllowed = results.get(0);
        int summaryTotal = results.get(1);
        int listedAllowed = results.get(2);
        int listedTotal = results.get(3);
        Log.i(LOG_TAG, "Assert: Number of listed allowed apps matches the display");
        assertTrue(
                String.format(
                        "Number of listed apps allowed does not match display."
                                + "\nSummary Value: %d \tListed: %d \t"
                                + results.toString(),
                        summaryAllowed,
                        listedAllowed),
                summaryAllowed == listedAllowed);
        Log.i(LOG_TAG, "Assert: Number of listed not allowed apps matches the display");
        assertTrue(
                String.format(
                        "Number of listed apps not allowed does not match display."
                                + "\nSummary Value: %d \tListed: %d",
                        summaryTotal, listedTotal),
                summaryTotal == listedTotal);
    }
}

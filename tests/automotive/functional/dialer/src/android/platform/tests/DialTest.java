/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.platform.tests;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import android.platform.helpers.HelperAccessor;
import android.platform.helpers.IAutoDialContactDetailsHelper;
import android.platform.helpers.IAutoDialContactDetailsHelper.ContactType;
import android.platform.helpers.IAutoDialHelper;
import android.platform.helpers.IAutoDialHelper.AudioSource;
import android.platform.helpers.IAutoDialHelper.OrderType;
import android.platform.helpers.IAutoVehicleHardKeysHelper;
import android.platform.test.option.StringOption;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DialTest {
    public static final String DIAL_A_NUMBER = "Dial a number";
    public static final String DIALED_CONTACT = "Aaron";
    public static final String DETAILED_CONTACT = "Aaron";
    public static final String CONTACT_TYPE = "Work";

    private static final String SMALL_NUMBER_PARAM = "small-phone-number";
    private static final String LARGE_NUMBER_PARAM = "large-phone-number";
    private static final String SEARCH_CONTACT_NUMBER_PARAM = "search-contact-number";
    private static final String SEARCH_CONTACT_NAME_PARAM = "search-contact-name";
    private static final String LOG_TAG = DialTest.class.getSimpleName();

    @ClassRule
    public static StringOption mSmallPhoneNumber =
            new StringOption(SMALL_NUMBER_PARAM).setRequired(true);

    @ClassRule
    public static StringOption mLargePhoneNumber =
            new StringOption(LARGE_NUMBER_PARAM).setRequired(true);

    @ClassRule
    public static StringOption mSearchContactNumber =
            new StringOption(SEARCH_CONTACT_NUMBER_PARAM).setRequired(true);

    @ClassRule
    public static StringOption mSearchContactName =
            new StringOption(SEARCH_CONTACT_NAME_PARAM).setRequired(true);

    private HelperAccessor<IAutoDialHelper> mDialerHelper;
    private HelperAccessor<IAutoVehicleHardKeysHelper> mVehicleHardKeysHelper;
    private HelperAccessor<IAutoDialContactDetailsHelper> mContactDetailsHelper;

    public DialTest() throws Exception {
        mDialerHelper = new HelperAccessor<>(IAutoDialHelper.class);
        mContactDetailsHelper = new HelperAccessor<>(IAutoDialContactDetailsHelper.class);
        mVehicleHardKeysHelper = new HelperAccessor<>(IAutoVehicleHardKeysHelper.class);
    }

    @After
    public void endCall() {
        Log.i(LOG_TAG, "Act: Press End Call");
        mVehicleHardKeysHelper.get().pressEndCallKey();
    }

    @Test
    public void testDialSmallNumber() {
        Log.i(LOG_TAG, "Act: Dial small phone number");
        mDialerHelper.get().dialANumber(mSmallPhoneNumber.get());
        Log.i(LOG_TAG, "Act: Make a call ");
        mDialerHelper.get().makeCall();
        Log.i(LOG_TAG, "Act: Get actual dialed number");
        String actualDialedNumber = mDialerHelper.get().getDialedNumber();
        Log.i(LOG_TAG, "Assert: Small Phone number is same as dialed number");
        assertEquals(mSmallPhoneNumber.get(), actualDialedNumber.replaceAll("[-()\\s]", ""));
        Log.i(LOG_TAG, "Act: End Call");
        mDialerHelper.get().endCall();
    }

    @Test
    public void testDialLargeNumber() {
        Log.i(LOG_TAG, "Act: Dial large phone number");
        mDialerHelper.get().dialANumber(mLargePhoneNumber.get());
        Log.i(LOG_TAG, "Act: Make a call ");
        mDialerHelper.get().makeCall();
        Log.i(LOG_TAG, "Act: Get actual dialed number");
        String actualDialedNumber = mDialerHelper.get().getDialedNumber();
        Log.i(LOG_TAG, "Assert: Large Phone number is same as dialed number");
        assertEquals(mLargePhoneNumber.get(), actualDialedNumber.replaceAll("[-()\\s]", ""));
        Log.i(LOG_TAG, "Act: End Call");
        mDialerHelper.get().endCall();
    }

    @Test
    public void testHistoryUpdatesCalledNumber() {
        Log.i(LOG_TAG, "Act: Dial small phone number");
        mDialerHelper.get().dialANumber(mSmallPhoneNumber.get());
        Log.i(LOG_TAG, "Act: Make a call ");
        mDialerHelper.get().makeCall();
        Log.i(LOG_TAG, "Act: End Call");
        mDialerHelper.get().endCall();
        Log.i(LOG_TAG, "Act: Open Call History");
        mDialerHelper.get().openCallHistory();
        Log.i(LOG_TAG, "Act: Call History is updated");
        assertTrue(
                "Call History did not update",
                mDialerHelper.get().getRecentCallHistory().equals(mSmallPhoneNumber.get()));
    }

    @Test
    public void testHistoryUpdatesCalledContactName() {
        Log.i(LOG_TAG, "Act: Open Dialer app");
        mDialerHelper.get().open();
        Log.i(LOG_TAG, "Act: Get Contact name");
        String dialContactByName = mSearchContactName.get();
        Log.i(LOG_TAG, "Act: Make a call using contact name");
        mDialerHelper.get().callContact(dialContactByName);
        Log.i(LOG_TAG, "Act: End Call");
        mDialerHelper.get().endCall();
        Log.i(LOG_TAG, "Act: Open Call History");
        mDialerHelper.get().openCallHistory();
        Log.i(LOG_TAG, "Act: Call History is updated");
        assertTrue(
                "Call History did not update",
                mDialerHelper.get().getRecentCallHistory().equals(dialContactByName));
    }

    @Test
    public void testDeleteDialedNumber() {
        Log.i(LOG_TAG, "Act: Dial small phone number");
        mDialerHelper.get().dialANumber(mSmallPhoneNumber.get());
        Log.i(LOG_TAG, "Act: Delete dialed number");
        mDialerHelper.get().deleteDialedNumber();
        Log.i(LOG_TAG, "Act: Get the number in dial pad");
        String numberAfterDelete = mDialerHelper.get().getNumberInDialPad();
        Log.i(LOG_TAG, "Assert: Dialed number is same as Deleted number");
        assertTrue(DIAL_A_NUMBER.equals(numberAfterDelete));
    }

    @Test
    public void testMuteUnmuteCall() {
        Log.i(LOG_TAG, "Act: Dial small phone number");
        mDialerHelper.get().dialANumber(mSmallPhoneNumber.get());
        Log.i(LOG_TAG, "Act: Make a call ");
        mDialerHelper.get().makeCall();
        Log.i(LOG_TAG, "Act: Mute the call ");
        try {
            Log.i(LOG_TAG, "Act: Mute the call ");
            mDialerHelper.get().muteCall();
            Log.i(LOG_TAG, "Act: Unmute the call ");
            mDialerHelper.get().unmuteCall();
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        } finally {
            Log.i(LOG_TAG, "Act: End call ");
            mDialerHelper.get().endCall();
        }
    }

    @Test
    public void testEndCallHardkey() {
        Log.i(LOG_TAG, "Act: Dial large phone number");
        mDialerHelper.get().dialANumber(mLargePhoneNumber.get());
        Log.i(LOG_TAG, "Act: Make a call");
        mDialerHelper.get().makeCall();
        Log.i(LOG_TAG, "Act: Get dialed number");
        String actualDialedNumber = mDialerHelper.get().getDialedNumber();
        Log.i(LOG_TAG, "Assert: Large Phone number is same as dialed number");
        assertEquals(mLargePhoneNumber.get(), actualDialedNumber.replaceAll("[-()\\s]", ""));
        Log.i(LOG_TAG, "Act: End call using hardkey");
        mVehicleHardKeysHelper.get().pressEndCallKey();
    }

    @Test
    public void testCallAudioSourceTransfer() {
        Log.i(LOG_TAG, "Act: Dial small phone number");
        mDialerHelper.get().dialANumber(mSmallPhoneNumber.get());
        Log.i(LOG_TAG, "Act: Make a call");
        mDialerHelper.get().makeCall();
        Log.i(LOG_TAG, "Act: Change audio source to phone");
        mDialerHelper.get().changeAudioSource(AudioSource.PHONE);
        Log.i(LOG_TAG, "Act: Change audio source to Car speakers");
        mDialerHelper.get().changeAudioSource(AudioSource.CAR_SPEAKERS);
        Log.i(LOG_TAG, "Act: End call ");
        mDialerHelper.get().endCall();
    }

    @Test
    public void testCallFromHistory() {
        Log.i(LOG_TAG, "Act: Dial small phone number");
        mDialerHelper.get().dialANumber(mSmallPhoneNumber.get());
        Log.i(LOG_TAG, "Act: Make a call");
        mDialerHelper.get().makeCall();
        Log.i(LOG_TAG, "Act: End call ");
        mDialerHelper.get().endCall();
        Log.i(LOG_TAG, "Act: Open call history");
        mDialerHelper.get().openCallHistory();
        Log.i(LOG_TAG, "Act: Call most recent number from call history");
        mDialerHelper.get().callMostRecentHistory();
        Log.i(LOG_TAG, "Assert: History is same as dialed number");
        assertTrue(
                "History is not same as dialed number.",
                mDialerHelper.get().getDialedContactName().equals(mSmallPhoneNumber.get()));
        Log.i(LOG_TAG, "Act: End call ");
        mDialerHelper.get().endCall();
    }

    @Test
    public void testDisplayedNameMatchesCalledContactName() {
        Log.i(LOG_TAG, "Act: Open Dialer");
        mDialerHelper.get().open();
        Log.i(LOG_TAG, "Act: Get some contact name");
        String dialContactByName = mSearchContactName.get();
        Log.i(LOG_TAG, "Act: Make a call using contact name");
        mDialerHelper.get().callContact(dialContactByName);
        Log.i(LOG_TAG, "Assert: Contact name is same as dialed contact");
        assertTrue(
                "Contact name is not the same",
                mDialerHelper.get().getContactName().contains(dialContactByName));
        Log.i(LOG_TAG, "Act: End call ");
        mDialerHelper.get().endCall();
    }

    @Test
    public void testDisplayedContactTypeMatchesCalledContactType() {
        Log.i(LOG_TAG, "Act: Open Dialer");
        mDialerHelper.get().open();
        Log.i(LOG_TAG, "Act: Make a call using contact name");
        mDialerHelper.get().callContact(mSearchContactName.get());
        Log.i(LOG_TAG, "Assert: Contact type is same as dialed contact type");
        assertTrue(
                "Contact detail is not the same",
                mDialerHelper.get().getContactType().equalsIgnoreCase(CONTACT_TYPE));
        mDialerHelper.get().endCall();
    }

    @Test
    public void testSearchContactByName() {
        Log.i(LOG_TAG, "Act: Open Dialer");
        mDialerHelper.get().open();
        Log.i(LOG_TAG, "Act: Search contact by name");
        mDialerHelper.get().searchContacts();
    }

    @Test
    public void testSearchContactByNumber() {
        Log.i(LOG_TAG, "Act: Open Dialer");
        mDialerHelper.get().open();
        Log.i(LOG_TAG, "Act: Search contact by phone number");
        mDialerHelper.get().searchContactsByNumber(mSearchContactNumber.get());
        Log.i(LOG_TAG, "Assert: Search contact by phone number is found");
        assertEquals(
                "Cannot find contact",
                mSearchContactName.get(),
                mDialerHelper.get().getFirstSearchResult());
    }

    @Test
    public void testSortContacts() {
        Log.i(LOG_TAG, "Act: Open Dialer");
        mDialerHelper.get().open();
        Log.i(LOG_TAG, "Act: Sort contacts by last name");
        mDialerHelper.get().sortContactListBy(OrderType.LAST_NAME);
        Log.i(LOG_TAG, "Assert: Sorted contacts by last name is correct");
        assertEquals(
                "Order by last name is not correct.",
                mDialerHelper.get().getFirstContactFromContactList(),
                DIALED_CONTACT);
        Log.i(LOG_TAG, "Act: Sort contacts by first name");
        mDialerHelper.get().sortContactListBy(OrderType.FIRST_NAME);
        Log.i(LOG_TAG, "Assert: Sorted contacts by first name is correct");
        assertEquals(
                "Order is not correct.",
                mDialerHelper.get().getFirstContactFromContactList(),
                DIALED_CONTACT);
    }

    @Test
    public void testAddRemoveFavoriteContact() {
        Log.i(LOG_TAG, "Act: Open Dialer");
        mDialerHelper.get().open();
        Log.i(LOG_TAG, "Act: Get a contact name");
        String favoritesContact = mSearchContactName.get();
        Log.i(LOG_TAG, "Act: Open contact details of the contact");
        mDialerHelper.get().openDetailsPage(favoritesContact);
        Log.i(LOG_TAG, "Act: Add contacts to favourate list");
        mContactDetailsHelper.get().addRemoveFavoriteContact();
        Log.i(LOG_TAG, "Act: Close Contact details screen");
        mContactDetailsHelper.get().closeDetailsPage();
        Log.i(LOG_TAG, "Assert: Contact is added t favourate list");
        assertTrue(
                "Contact is not added to favorites.",
                mDialerHelper.get().isContactInFavorites(favoritesContact));
        Log.i(LOG_TAG, "Act: Open contact details of the favourate contact");
        mDialerHelper.get().openDetailsPage(favoritesContact);
        Log.i(LOG_TAG, "Act: Remove contact from favourate list");
        mContactDetailsHelper.get().addRemoveFavoriteContact();
        Log.i(LOG_TAG, "Act: Close Contact details screen");
        mContactDetailsHelper.get().closeDetailsPage();
        Log.i(LOG_TAG, "Assert: Contact is removed from favourate list");
        assertFalse(
                "Contact is not removed from favorites.",
                mDialerHelper.get().isContactInFavorites(favoritesContact));
    }

    @Test
    public void testMakeCallFromContactDetailsPage() {
        Log.i(LOG_TAG, "Act: Open Dialer");
        mDialerHelper.get().open();
        Log.i(LOG_TAG, "Act: Open Details page of a contact");
        mDialerHelper.get().openDetailsPage(DETAILED_CONTACT);
        Log.i(LOG_TAG, "Act: Make a call to the contact of type MOBILE");
        mContactDetailsHelper.get().makeCallFromDetailsPageByType(ContactType.MOBILE);
        Log.i(LOG_TAG, "Assert: Contact name is same as dialed contact ");
        assertTrue(
                "Contact name is not the same",
                mDialerHelper.get().getContactName().contains(DETAILED_CONTACT));
        Log.i(LOG_TAG, "Act: End call");
        mDialerHelper.get().endCall();
        Log.i(LOG_TAG, "Act: Close Contact Details Page");
        mContactDetailsHelper.get().closeDetailsPage();
    }
}

/*
 * Copyright 2010-2016 Frans-Willem Hardijzer
 *
 * This file is part of BitonSync.
 *
 * BitonSync is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BitonSync is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BitonSync.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.hardijzer.bitonsync.platform;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.text.TextUtils;
import android.util.Log;

import nl.hardijzer.bitonsync.Constants;
import nl.hardijzer.bitonsync.R;

/**
 * Helper class for storing data in the platform content providers.
 */
public class ContactOperations {

    private final ContentValues mValues;
    private ContentProviderOperation.Builder mBuilder;
    private final BatchOperation mBatchOperation;
    private final Context mContext;
    private boolean mYield;
    private long mRawContactId;
    private int mBackReference;
    private boolean mIsNewContact;

    /**
     * Returns an instance of ContactOperations instance for adding new contact
     * to the platform contacts provider.
     * 
     * @param context the Authenticator Activity context
     * @param userId the userId of the sample SyncAdapter user object
     * @param accountName the username of the current login
     * @return instance of ContactOperations
     */
    public static ContactOperations createNewContact(Context context,
        String naam, String accountName, BatchOperation batchOperation) {
        return new ContactOperations(context, naam, accountName,
            batchOperation);
    }

    /**
     * Returns an instance of ContactOperations for updating existing contact in
     * the platform contacts provider.
     * 
     * @param context the Authenticator Activity context
     * @param rawContactId the unique Id of the existing rawContact
     * @return instance of ContactOperations
     */
    public static ContactOperations updateExistingContact(Context context,
        long rawContactId, BatchOperation batchOperation) {
        return new ContactOperations(context, rawContactId, batchOperation);
    }

    public ContactOperations(Context context, BatchOperation batchOperation) {
        mValues = new ContentValues();
        mYield = true;
        mContext = context;
        mBatchOperation = batchOperation;
    }

    public ContactOperations(Context context, String naam, String accountName,
        BatchOperation batchOperation) {
        this(context, batchOperation);
        mBackReference = mBatchOperation.size();
        mIsNewContact = true;
        mValues.put(RawContacts.SOURCE_ID, naam);
        mValues.put(RawContacts.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
        mValues.put(RawContacts.ACCOUNT_NAME, accountName);
        mBuilder =
            newInsertCpo(RawContacts.CONTENT_URI, true).withValues(mValues);
        mBatchOperation.add(mBuilder.build());
        
        //Nickname veranderd nooit, dus gewoon meteen toevoegen
        mValues.clear();
        mValues.put(Nickname.MIMETYPE, Nickname.CONTENT_ITEM_TYPE);
        mValues.put(Nickname.NAME, naam);
        addInsertOp();
    }

    public ContactOperations(Context context, long rawContactId,
        BatchOperation batchOperation) {
        this(context, batchOperation);
        mIsNewContact = false;
        mRawContactId = rawContactId;
    }
    //mValues.put(StructuredName.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
    
    public ContactOperations addNaam(String naam, String voornaam, String achternaam) {
    	mValues.clear();
    	mValues.put(StructuredName.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
    	if (!TextUtils.isEmpty(voornaam))
    		mValues.put(StructuredName.GIVEN_NAME, voornaam);
    	if (!TextUtils.isEmpty(achternaam))
    		mValues.put(StructuredName.FAMILY_NAME, achternaam);
    	String displayname;
    	if (naam!=voornaam)
    		displayname=voornaam+" \""+naam+"\" "+achternaam;
    	else
    		displayname=voornaam+" "+achternaam;
    	mValues.put(StructuredName.DISPLAY_NAME, displayname);
    	addInsertOp();
    	return this;
    }
    
    public ContactOperations updateNaam(String naam, String huidigeVoornaam, String huidigeAchternaam, String voornaam, String achternaam, Uri uri) {
    	if (TextUtils.equals(huidigeVoornaam,voornaam) && TextUtils.equals(huidigeAchternaam,achternaam))
    		return this;
    	mValues.clear();
    	mValues.put(StructuredName.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
    	mValues.put(StructuredName.GIVEN_NAME, voornaam);
    	mValues.put(StructuredName.FAMILY_NAME, achternaam);
    	String displayname;
    	if (naam!=voornaam)
    		displayname=voornaam+" \""+naam+"\" "+achternaam;
    	else
    		displayname=voornaam+" "+achternaam;
    	mValues.put(StructuredName.DISPLAY_NAME, displayname);
    	addUpdateOp(uri);
    	return this;
    }

    public ContactOperations addEmail(String email) {
    	if (TextUtils.isEmpty(email))
    		return this;
        mValues.clear();
        mValues.put(Email.DATA, email);
        mValues.put(Email.TYPE, Email.TYPE_OTHER);
        mValues.put(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE);
        addInsertOp();
        return this;
    }

    public ContactOperations updateEmail(String huidigeEmail, String email,
        Uri uri) {
    	if (TextUtils.equals(huidigeEmail,email))
    		return this;
    	if (TextUtils.isEmpty(email)) {
    		addDeleteOp(uri);
    		return this;
    	}
        mValues.clear();
        mValues.put(Email.DATA, email);
        addUpdateOp(uri);
        return this;
    }

    public ContactOperations addTelefoon(String telefoon) {
    	if (TextUtils.isEmpty(telefoon))
    		return this;
        mValues.clear();
        mValues.put(Phone.NUMBER, telefoon);
        mValues.put(Phone.TYPE, Phone.TYPE_HOME);
        mValues.put(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
        addInsertOp();
        return this;
    }
    
    public ContactOperations updateTelefoon(String huidigeTelefoon, String telefoon,
        Uri uri) {
        if (TextUtils.equals(huidigeTelefoon, telefoon))
        	return this;
    	if (TextUtils.isEmpty(telefoon)) {
    		addDeleteOp(uri);
    		return this;
    	}
        mValues.clear();
        mValues.put(Phone.NUMBER, telefoon);
        addUpdateOp(uri);
        return this;
    }
    
    public ContactOperations addMobiel(String mobiel) {
    	if (TextUtils.isEmpty(mobiel))
    		return this;
        mValues.clear();
        mValues.put(Phone.NUMBER, mobiel);
        mValues.put(Phone.TYPE, Phone.TYPE_MOBILE);
        mValues.put(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
        addInsertOp();
        return this;
    }
    
    public ContactOperations updateMobiel(String huidigeMobiel, String mobiel,
        Uri uri) {
        if (TextUtils.equals(huidigeMobiel, mobiel))
        	return this;
        if (TextUtils.isEmpty(mobiel)) {
    		addDeleteOp(uri);
    		return this;
    	}
        mValues.clear();
        mValues.put(Phone.NUMBER, mobiel);
        addUpdateOp(uri);
        return this;
    }
    
    public ContactOperations addAdres(String adres) {
    	if (TextUtils.isEmpty(adres))
    		return this;
        mValues.clear();
        mValues.put(StructuredPostal.FORMATTED_ADDRESS, adres);
        mValues.put(StructuredPostal.TYPE, StructuredPostal.TYPE_HOME);
        mValues.put(StructuredPostal.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE);
        addInsertOp();
        return this;
    }
    
    public ContactOperations updateAdres(String huidigAdres, String adres,
        Uri uri) {
        if (TextUtils.equals(huidigAdres, adres))
        	return this;
        if (TextUtils.isEmpty(adres)) {
    		addDeleteOp(uri);
    		return this;
    	}
        mValues.clear();
        mValues.put(StructuredPostal.FORMATTED_ADDRESS, adres);
        addUpdateOp(uri);
        return this;
    }
    
    public ContactOperations addGeboorte(String geboorte) {
    	if (TextUtils.isEmpty(geboorte))
    		return this;
        mValues.clear();
        mValues.put(Event.START_DATE, geboorte);
        mValues.put(Event.TYPE, Event.TYPE_BIRTHDAY);
        mValues.put(Event.MIMETYPE, Event.CONTENT_ITEM_TYPE);
        addInsertOp();
        return this;
    }
    
    public ContactOperations updateGeboorte(String huidigeGeboorte, String geboorte,
        Uri uri) {
        if (TextUtils.equals(huidigeGeboorte, geboorte))
        	return this;
        if (TextUtils.isEmpty(geboorte)) {
    		addDeleteOp(uri);
    		return this;
    	}
        mValues.clear();
        mValues.put(Event.START_DATE, geboorte);
        addUpdateOp(uri);
        return this;
    }

    /**
     * Adds an insert operation into the batch
     */
    private void addInsertOp() {
        if (!mIsNewContact) {
            mValues.put(Phone.RAW_CONTACT_ID, mRawContactId);
        }
        mBuilder =
            newInsertCpo(addCallerIsSyncAdapterParameter(Data.CONTENT_URI),
                mYield);
        mBuilder.withValues(mValues);
        if (mIsNewContact) {
            mBuilder
                .withValueBackReference(Data.RAW_CONTACT_ID, mBackReference);
        }
        mYield = false;
        mBatchOperation.add(mBuilder.build());
    }

    /**
     * Adds an update operation into the batch
     */
    private void addUpdateOp(Uri uri) {
        mBuilder = newUpdateCpo(uri, mYield).withValues(mValues);
        mYield = false;
        mBatchOperation.add(mBuilder.build());
    }
    
    private void addDeleteOp(Uri uri) {
    	mBuilder = newDeleteCpo(uri,mYield);
    	mYield=false;
    	mBatchOperation.add(mBuilder.build());
    }

    public static ContentProviderOperation.Builder newInsertCpo(Uri uri,
        boolean yield) {
        return ContentProviderOperation.newInsert(
            addCallerIsSyncAdapterParameter(uri)).withYieldAllowed(yield);
    }

    public static ContentProviderOperation.Builder newUpdateCpo(Uri uri,
        boolean yield) {
        return ContentProviderOperation.newUpdate(
            addCallerIsSyncAdapterParameter(uri)).withYieldAllowed(yield);
    }

    public static ContentProviderOperation.Builder newDeleteCpo(Uri uri,
        boolean yield) {
        return ContentProviderOperation.newDelete(
            addCallerIsSyncAdapterParameter(uri)).withYieldAllowed(yield);

    }

    private static Uri addCallerIsSyncAdapterParameter(Uri uri) {
        return uri.buildUpon().appendQueryParameter(
            ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
    }

}

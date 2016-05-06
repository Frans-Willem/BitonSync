/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package nl.hardijzer.bitonsync.platform;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.StatusUpdates;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.util.Log;

import nl.hardijzer.bitonsync.Constants;
import nl.hardijzer.bitonsync.R;
import nl.hardijzer.bitonsync.client.User;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

/**
 * Class for managing contacts sync related mOperations
 */
public class ContactManager {
    /**
     * Custom IM protocol used when storing status messages.
     */
    private static final String TAG = "ContactManager";

    /**
     * Synchronize raw contacts
     * 
     * @param context The context of Authenticator Activity
     * @param account The username for the account
     * @param users The list of users
     */
    public static synchronized void syncContacts(Context context,
        String account, List<User> users) {
        String naam;
        long rawContactId = 0;
        final ContentResolver resolver = context.getContentResolver();
        /*
         * Make a list of existing contacts, so we can remove any non-resynched ones
         */
        HashMap<String,Long> leftoverMap=new HashMap();
        HashSet<String> doublecheckSet=new HashSet();
        final Cursor c =
            resolver.query(RawContacts.CONTENT_URI, GetAllQuery.PROJECTION,
            		GetAllQuery.SELECTION,
                new String[] {account}, null);
        while (c.moveToNext()) {
        	long id=c.getLong(GetAllQuery.COLUMN_ID);
        	naam=c.getString(GetAllQuery.COLUMN_NAAM);
        	leftoverMap.put(naam, id);
        }
        
        final BatchOperation batchOperation =
            new BatchOperation(context, resolver);
        for (final User user : users) {
            naam = user.getNaam();
            if (doublecheckSet.contains(naam))
            	continue; //Ignore it if there was already a contact by that name.
            doublecheckSet.add(naam);
            // Check to see if the contact needs to be inserted or updated
            Long preContactId=leftoverMap.remove(naam);
            rawContactId = (preContactId==null)?0:preContactId.longValue();//lookupRawContact(resolver, naam);
            if (rawContactId != 0) {
                // update contact
                updateContact(context, resolver, account, user,
                    rawContactId, batchOperation);
            } else {
                // add new contact
                addContact(context, account, user, batchOperation);
            }
            // A sync adapter should batch operations on multiple contacts,
            // because it will make a dramatic performance difference.
            if (batchOperation.size() >= 50) {
                batchOperation.execute();
            }
        }
        //Delete any contacts not in the friends list
        for (final Long del : leftoverMap.values()) {
        	deleteContact(context,del.longValue(),batchOperation);
        	if (batchOperation.size() >= 50) {
                batchOperation.execute();
            }
        }
        batchOperation.execute();
    }

    /**
     * Adds a single contact to the platform contacts provider.
     * 
     * @param context the Authenticator Activity context
     * @param accountName the account the contact belongs to
     * @param user the sample SyncAdapter User object
     */
    private static void addContact(Context context, String accountName,
        User user, BatchOperation batchOperation) {
        // Put the data in the contacts provider
        final ContactOperations contactOp =
            ContactOperations.createNewContact(context, user.getNaam(),
                accountName, batchOperation);
        contactOp.addNaam(user.getNaam(),user.getVoornaam(), user.getAchternaam()).addEmail(
            user.getEmail()).addMobiel(user.getMobiel())
            .addTelefoon(user.getTelefoon()).addAdres(user.getAdres()).addGeboorte(user.getGeboorteDatum());
    }

    /**
     * Updates a single contact to the platform contacts provider.
     * 
     * @param context the Authenticator Activity context
     * @param resolver the ContentResolver to use
     * @param accountName the account the contact belongs to
     * @param user the sample SyncAdapter contact object.
     * @param rawContactId the unique Id for this rawContact in contacts
     *        provider
     */
    private static void updateContact(Context context,
        ContentResolver resolver, String accountName, User user,
        long rawContactId, BatchOperation batchOperation) {
        Uri uri;
        String mobiel = null;
        String telefoon = null;
        String email = null;
        String adres = null;
        String geboorte = null;

        final Cursor c =
            resolver.query(Data.CONTENT_URI, DataQuery.PROJECTION,
                DataQuery.SELECTION,
                new String[] {String.valueOf(rawContactId)}, null);
        final ContactOperations contactOp =
            ContactOperations.updateExistingContact(context, rawContactId,
                batchOperation);

        try {
            while (c.moveToNext()) {
                final long id = c.getLong(DataQuery.COLUMN_ID);
                final String mimeType = c.getString(DataQuery.COLUMN_MIMETYPE);
                uri = ContentUris.withAppendedId(Data.CONTENT_URI, id);

                if (mimeType.equals(StructuredName.CONTENT_ITEM_TYPE)) {
                    final String lastName =
                        c.getString(DataQuery.COLUMN_FAMILY_NAME);
                    final String firstName =
                        c.getString(DataQuery.COLUMN_GIVEN_NAME);
                    contactOp.updateNaam(user.getNaam(), firstName, lastName, user
                        .getVoornaam(), user.getAchternaam(), uri);
                }

                else if (mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                    final int type = c.getInt(DataQuery.COLUMN_PHONE_TYPE);

                    if (type == Phone.TYPE_MOBILE) {
                        mobiel = c.getString(DataQuery.COLUMN_PHONE_NUMBER);
                        contactOp.updateMobiel(mobiel, user.getMobiel(),
                            uri);
                    } else if (type == Phone.TYPE_HOME) {
                        telefoon = c.getString(DataQuery.COLUMN_PHONE_NUMBER);
                        contactOp.updateTelefoon(telefoon, user.getTelefoon(),
                            uri);
                    }
                }

                else if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
                    email = c.getString(DataQuery.COLUMN_EMAIL_ADDRESS);
                    contactOp.updateEmail(email, user.getEmail(), uri);

                }
                else if (mimeType.equals(StructuredPostal.CONTENT_ITEM_TYPE)) {
                	adres = c.getString(DataQuery.COLUMN_FORMATTED_ADDRESS);
                	contactOp.updateAdres(adres,user.getAdres(),uri);
                }
                else if (mimeType.equals(Event.CONTENT_ITEM_TYPE)) {
                	final int type = c.getInt(DataQuery.COLUMN_EVENT_TYPE);
                	if (type == Event.TYPE_BIRTHDAY) {
                		geboorte = c.getString(DataQuery.COLUMN_EVENT_START_DATE);
                		contactOp.updateGeboorte(geboorte, user.getGeboorteDatum(), uri);
                	}
                }
            } // while
        } finally {
            c.close();
        }

        // Add the cell phone, if present and not updated above
        if (mobiel == null) {
            contactOp.addMobiel(user.getMobiel());
        }

        // Add the other phone, if present and not updated above
        if (telefoon == null) {
            contactOp.addTelefoon(user.getTelefoon());
        }

        // Add the email address, if present and not updated above
        if (email == null) {
            contactOp.addEmail(user.getEmail());
        }
        
        if (adres==null) {
        	contactOp.addAdres(user.getAdres());
        }
        
        if (geboorte == null) {
        	contactOp.addGeboorte(user.getGeboorteDatum());
        }

    }

    /**
     * Deletes a contact from the platform contacts provider.
     * 
     * @param context the Authenticator Activity context
     * @param rawContactId the unique Id for this rawContact in contacts
     *        provider
     */
    private static void deleteContact(Context context, long rawContactId,
        BatchOperation batchOperation) {
        batchOperation.add(ContactOperations.newDeleteCpo(
            ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
            true).build());
    }
    
    private interface GetAllQuery {
    	public final static String[] PROJECTION =
    		new String[] {RawContacts._ID, RawContacts.SOURCE_ID};
    	public final static int COLUMN_ID = 0;
    	public final static int COLUMN_NAAM = 1;
    	public static final String SELECTION =
            RawContacts.ACCOUNT_TYPE + "='" + Constants.ACCOUNT_TYPE + "' AND "+RawContacts.ACCOUNT_NAME+"=?";
    }

    /**
     * Constants for a query to get contact data for a given rawContactId
     */
    private interface DataQuery {
        public static final String[] PROJECTION =
            new String[] {Data._ID, Data.MIMETYPE, Data.DATA1, Data.DATA2,
                Data.DATA3,};

        public static final int COLUMN_ID = 0;
        public static final int COLUMN_MIMETYPE = 1;
        public static final int COLUMN_DATA1 = 2;
        public static final int COLUMN_DATA2 = 3;
        public static final int COLUMN_DATA3 = 4;
        public static final int COLUMN_PHONE_NUMBER = COLUMN_DATA1;
        public static final int COLUMN_PHONE_TYPE = COLUMN_DATA2;
        public static final int COLUMN_EMAIL_ADDRESS = COLUMN_DATA1;
        public static final int COLUMN_EMAIL_TYPE = COLUMN_DATA2;
        public static final int COLUMN_GIVEN_NAME = COLUMN_DATA2;
        public static final int COLUMN_FAMILY_NAME = COLUMN_DATA3;
        public static final int COLUMN_FORMATTED_ADDRESS = COLUMN_DATA1;
        public static final int COLUMN_EVENT_TYPE = COLUMN_DATA2;
        public static final int COLUMN_EVENT_START_DATE = COLUMN_DATA1;

        public static final String SELECTION = Data.RAW_CONTACT_ID + "=?";
    }
}

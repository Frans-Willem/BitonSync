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

import android.provider.ContactsContract.Data;

/*
 * The standard columns representing contact's info from social apps.
 */
public interface SampleSyncAdapterColumns {
    /**
     * MIME-type used when storing a profile {@link Data} entry.
     */
    public static final String MIME_PROFILE =
        "vnd.android.cursor.item/vnd.bitonsyncadapter.profile";

    public static final String DATA_PID = Data.DATA1;
    public static final String DATA_SUMMARY = Data.DATA2;
    public static final String DATA_DETAIL = Data.DATA3;

}

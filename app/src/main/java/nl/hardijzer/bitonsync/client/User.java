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

package nl.hardijzer.bitonsync.client;

import java.lang.reflect.Type;

import android.util.Log;

import org.json.JSONObject;

import com.google.gson.*;

/**
 * Represents a sample SyncAdapter user
 */
public class User {
	public static JsonDeserializer<User> deserializer=new UserDeserializer();
	private final String mNaam; //= username, immers binnen Biton geen dubbelen :)
    private final String mVoornaam;
    private final String mAchternaam;
    private final String mTelefoon;
    private final String mMobiel;
    private final String mEmail;
    private final String mAdres;
    private final String mGeboorteDatum;

    public String getNaam() {
        return mNaam;
    }

    public String getVoornaam() {
        return mVoornaam;
    }

    public String getAchternaam() {
        return mAchternaam;
    }

    public String getTelefoon() {
        return mTelefoon;
    }

    public String getMobiel() {
        return mMobiel;
    }

    public String getEmail() {
        return mEmail;
    }

    public String getAdres() {
        return mAdres;
    }
    
    public String getGeboorteDatum() {
        return mGeboorteDatum;
    }
    
    public User(String naam, String voornaam, String achternaam, String telefoon, String mobiel, String email, String adres, String geboortedatum) {
    	mNaam=naam; //= username, immers binnen Biton geen dubbelen :)
        mVoornaam=voornaam;
        mAchternaam=achternaam;
        mTelefoon=telefoon;
        mMobiel=mobiel;
        mEmail=email;
        mAdres=adres;
        mGeboorteDatum=geboortedatum;
    }
}

class UserDeserializer implements JsonDeserializer<User> {
	@Override
	public User deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {
		if (json instanceof JsonObject) {
			JsonObject obj=(JsonObject)json;
			return new User(
					(String)context.deserialize(obj.get("naam"),String.class),
					(String)context.deserialize(obj.get("voornaam"),String.class),
					(String)context.deserialize(obj.get("achternaam"),String.class),
					(String)context.deserialize(obj.get("vaste telefoon"),String.class),
					(String)context.deserialize(obj.get("mobiel"),String.class),
					(String)context.deserialize(obj.get("e-mail"),String.class),
					(String)context.deserialize(obj.get("adres"),String.class),
					(String)context.deserialize(obj.get("geboortedatum"),String.class)
					);
		}
		return null;
	}
}


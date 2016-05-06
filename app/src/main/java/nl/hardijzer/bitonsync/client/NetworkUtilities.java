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

package nl.hardijzer.bitonsync.client;

import android.accounts.Account;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import nl.hardijzer.bitonsync.authenticator.AuthenticatorActivity;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import nl.hardijzer.bitonsync.Entities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides utility methods for communicating with the server.
 */
public class NetworkUtilities {
    private static final String TAG = "NetworkUtilities";
    public static final String PARAM_USERNAME = "log";
    public static final String PARAM_PASSWORD = "pwd";
    public static final String PARAM_METHOD = "m";
    public static final String USER_AGENT = "BitonSync/1.0";
    public static final int REGISTRATION_TIMEOUT = 30 * 1000; // ms
    public static final String BASE_URL =
        "http://www.biton.nl";
    public static final String SYNC_URI = BASE_URL + "/bitonsync.php";
    public static final String AUTH_METHOD = "auth";
    public static final String DATA_METHOD = "data";
    private static HttpClient mHttpClient;

    /**
     * Configures the httpClient to connect to the URL provided.
     */
    public static void maybeCreateHttpClient() {
        if (mHttpClient == null) {
            mHttpClient = new DefaultHttpClient();
            final HttpParams params = mHttpClient.getParams();
            HttpConnectionParams.setConnectionTimeout(params,
                REGISTRATION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(params, REGISTRATION_TIMEOUT);
            ConnManagerParams.setTimeout(params, REGISTRATION_TIMEOUT);
            params.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
        }
    }

    /**
     * Executes the network requests on a separate thread.
     * 
     * @param runnable The runnable instance containing network mOperations to
     *        be executed.
     */
    public static Thread performOnBackgroundThread(final Runnable runnable) {
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {

                }
            }
        };
        t.start();
        return t;
    }
    
    /**
     * Connects to the Voiper server, authenticates the provided username and
     * password.
     * 
     * @param username The user's username
     * @param password The user's password
     * @param handler The hander instance from the calling UI thread.
     * @param context The context of the calling Activity.
     * @return boolean The boolean result indicating whether the user was
     *         successfully authenticated.
     */
    public static boolean authenticate(String username, String password,
        Handler handler, final Context context) {
        final HttpResponse resp;

        final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_USERNAME, username));
        params.add(new BasicNameValuePair(PARAM_PASSWORD, password));
        params.add(new BasicNameValuePair(PARAM_METHOD, AUTH_METHOD));
        //params.add(new BasicNameValuePair("op","Inloggen"));
        HttpEntity entity = null;
        try {
            entity = new UrlEncodedFormEntity(params);
        } catch (final UnsupportedEncodingException e) {
            // this should never happen.
            throw new AssertionError(e);
        }
        final HttpPost post = new HttpPost(SYNC_URI);
        post.addHeader(entity.getContentType());
        post.setEntity(entity);
        maybeCreateHttpClient();

        try {
            resp = mHttpClient.execute(post);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Successful authentication");
                }
                String strResponse=EntityUtils.toString(resp.getEntity());
                boolean loggedin=strResponse.startsWith("OK ");
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG,loggedin?"Auth: true":"Auth: false");
                }
                sendResult(loggedin, handler, context);
                return loggedin;
            } else {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Error authenticating" + resp.getStatusLine());
                }
                sendResult(false, handler, context);
                return false;
            }
        } catch (final IOException e) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "IOException when getting authtoken", e);
            }
            sendResult(false, handler, context);
            return false;
        } finally {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "getAuthtoken completing");
            }
        }
    }

    /**
     * Sends the authentication response from server back to the caller main UI
     * thread through its handler.
     * 
     * @param result The boolean holding authentication result
     * @param handler The main UI thread's handler instance.
     * @param context The caller Activity's context.
     */
    private static void sendResult(final Boolean result, final Handler handler,
        final Context context) {
        if (handler == null || context == null) {
            return;
        }
        handler.post(new Runnable() {
            public void run() {
                ((AuthenticatorActivity) context).onAuthenticationResult(result);
            }
        });
    }

    /**
     * Attempts to authenticate the user credentials on the server.
     * 
     * @param username The user's username
     * @param password The user's password to be authenticated
     * @param handler The main UI thread's handler instance.
     * @param context The caller Activity's context
     * @return Thread The thread on which the network mOperations are executed.
     */
    public static Thread attemptAuth(final String username,
        final String password, final Handler handler, final Context context) {
        final Runnable runnable = new Runnable() {
            public void run() {
                authenticate(username, password, handler, context);
            }
        };
        // run on background thread.
        return NetworkUtilities.performOnBackgroundThread(runnable);
    }
    
    private static String unhtmlify(String input) {
    	input=input.replaceAll("<[^>]*>","");
    	return Entities.HTML40.unescape(input);
    }

    /**
     * Fetches the list of friend data updates from the server
     * 
     * @param account The account being synced.
     * @param authtoken The authtoken stored in AccountManager for this account
     * @param lastUpdated The last time that sync was performed
     * @return list The list of updates received from the server.
     */
    public static List<User> fetchFriendUpdates(Account account,
        String authtoken)
        throws ParseException, IOException, AuthenticationException {
    	
        ArrayList<User> friendList = new ArrayList<User>();
        final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_USERNAME, account.name));
        params.add(new BasicNameValuePair(PARAM_PASSWORD, authtoken));
        params.add(new BasicNameValuePair(PARAM_METHOD,DATA_METHOD));
        Log.i(TAG, params.toString());
        
        HttpEntity entity = null;
        entity = new UrlEncodedFormEntity(params);
        final HttpPost post = new HttpPost(SYNC_URI);//FETCH_FRIEND_UPDATES_URI
        post.addHeader(entity.getContentType());
        post.setEntity(entity);
        maybeCreateHttpClient();

        final HttpResponse resp = mHttpClient.execute(post);
        
        //final HttpResponse resp = mHttpClient.execute(new HttpGet(FETCH_FRIEND_UPDATES_URI));

        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            // Succesfully connected to the samplesyncadapter server and
            // authenticated.
            // Extract friends data in json format.
            final String response = EntityUtils.toString(resp.getEntity());
            boolean loggedin=response.startsWith("OK ");
            if (loggedin) {
            	String data=response.substring(3);
            	GsonBuilder bld=new GsonBuilder();
            	bld.registerTypeAdapter(User.class, User.deserializer);
            	Gson gson=bld.create();
            	friendList=gson.fromJson(data, new TypeToken<ArrayList<User>>(){}.getType());
            	
            	/*
            	
            	
	            String[] parts=response.split("<td class=''>");
	            for (int i=1; i<parts.length; i++) {
	            	int end=parts[i].indexOf("</td>");
	            	if (end>=0)
	            		parts[i]=parts[i].substring(0,end);
	            	parts[i]=unhtmlify(parts[i]);
	            }
	        	
	            for (int i=1; i+6<=parts.length; i+=6) {
	            	Matcher naammatcher=Pattern.compile("(^.+) \\((.+?)\\) (.+$)").matcher(parts[i]);
	            	String naam,voornaam,achternaam;
	            	if (naammatcher.find()) {
	            		voornaam=parts[i].substring(naammatcher.start(2),naammatcher.end(2));
	            		naam=parts[i].substring(naammatcher.start(1),naammatcher.end(1));
	            		achternaam=parts[i].substring(naammatcher.start(3),naammatcher.end(3));
	            	} else {
	            		voornaam=parts[i];
	            		achternaam="";
	            		int split=voornaam.indexOf(" ");
	            		if (split>=0) {
	            			achternaam=voornaam.substring(split+1);
	            			voornaam=voornaam.substring(0,split);
	            		}
	            		naam=voornaam;
	            	}
	            	String telefoon=parts[i+1];
	            	String mobiel=parts[i+2];
	            	if (telefoon.startsWith("0")) {
	            		telefoon="+31"+telefoon.substring(1);
	            	}
	            	if (mobiel.startsWith("0")) {
	            		mobiel="+31"+mobiel.substring(1);
	            	}
	            	String geboorte=parts[i+5];
	            	String[] geboorte_parts=geboorte.split(" ");
	            	if (geboorte_parts.length==3) {
	            		String dag=geboorte_parts[0];
	            		String jaar=geboorte_parts[2];
	            		String maand=geboorte_parts[1];
	            		if (dag.length()<2)
	            			dag="00".substring(dag.length())+dag;
	            		if (jaar.length()==2)
	            			jaar="19"+jaar;
	            		//januari 1 ja
	            		//februari 2 f
	            		//maart 3 ma
	            		//april 4 ap
	            		//mei 5 me
	            		//juni 6 jun
	            		//juli 7 jul
	            		//aug 8 au
	            		//sep 9 s
	            		//okt 12 o
	            		//nov 11 n
	            		//dec 12 d
	            		if (maand.startsWith("ja")) geboorte=jaar+"-01-"+dag;
	            		else if (maand.startsWith("f")) geboorte=jaar+"-02-"+dag;
	            		else if (maand.startsWith("ma")) geboorte=jaar+"-03-"+dag;
	            		else if (maand.startsWith("ap")) geboorte=jaar+"-04-"+dag;
	            		else if (maand.startsWith("me")) geboorte=jaar+"-05-"+dag;
	            		else if (maand.startsWith("jun")) geboorte=jaar+"-06-"+dag;
	            		else if (maand.startsWith("jul")) geboorte=jaar+"-07-"+dag;
	            		else if (maand.startsWith("au")) geboorte=jaar+"-08-"+dag;
	            		else if (maand.startsWith("s")) geboorte=jaar+"-09-"+dag;
	            		else if (maand.startsWith("o")) geboorte=jaar+"-10-"+dag;
	            		else if (maand.startsWith("n")) geboorte=jaar+"-11-"+dag;
	            		else if (maand.startsWith("d")) geboorte=jaar+"-12-"+dag;
	            	}
	            	friendList.add(new User(naam, voornaam, achternaam, telefoon, mobiel, parts[i+3], parts[i+4], geboorte));
	            }*/
            } else {
            	 Log.e(TAG,
                 "Authentication exception in fetching remote contacts (content)");
            	 throw new AuthenticationException();
            }
        } else {
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                Log.e(TAG,
                    "Authentication exception in fetching remote contacts");
                throw new AuthenticationException();
            } else {
                Log.e(TAG, "Server error in fetching remote contacts: "
                    + resp.getStatusLine());
                throw new IOException();
            }
        }
        if (friendList.isEmpty()) {
        	Log.e(TAG, "No friends found, auth error?: "
                    + resp.getStatusLine());
                throw new AuthenticationException();
        }
        return friendList;
    }
}

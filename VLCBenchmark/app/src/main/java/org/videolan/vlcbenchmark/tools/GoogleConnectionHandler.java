/*
 *****************************************************************************
 * GoogleConnectionHandler.java
 *****************************************************************************
 * Copyright © 2017 - 2018 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/

package org.videolan.vlcbenchmark.tools;

import android.content.Context;
import android.content.Intent;
import androidx.fragment.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import org.videolan.vlcbenchmark.Constants;


/**
 * GoogleConnectionHandler is a singleton class that handles the android google connexion
 * It has to be accessible from several activities and fragments hence the singleton
 */
public class GoogleConnectionHandler {

    final static private String TAG = GoogleConnectionHandler.class.getName();

    static private GoogleConnectionHandler instance;
    private GoogleSignInClient mGoogleSignInClient;
    private FragmentActivity mFragmentActivity;

    private GoogleConnectionHandler() {

    }

    /** Getter for the singleton instance */
    public static GoogleConnectionHandler getInstance() {
        if (instance == null) {
            instance = new GoogleConnectionHandler();
        }
        return instance;
    }

    /**
     * Sets the calling context and instanciates the GoogleSignInClient
     * To be called when resuming an activity
     * @param context calling context
     * @param fragmentActivity calling fragment
     */
    /* */
    public void setGoogleSignInClient(Context context, FragmentActivity fragmentActivity) {
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        if (mGoogleSignInClient == null) {
            mFragmentActivity = fragmentActivity;
            mGoogleSignInClient = GoogleSignIn.getClient(context, googleSignInOptions);
        }
    }

    /**
     * Destroys the GoogleApiClient and resets calling context
     * To be called when pausing a fragment or activity
     */
    public void unsetGoogleSignInClient() {
        mFragmentActivity = null;
        mGoogleSignInClient = null;
    }

    public void signIn() {
        if (mGoogleSignInClient != null) {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(mFragmentActivity);
            if (account == null) {
                Intent intent = mGoogleSignInClient.getSignInIntent();
                mFragmentActivity.startActivityForResult(intent, Constants.RequestCodes.GOOGLE_CONNECTION);
            } else {
                Log.i(TAG, "signIn: Already signed in");
            }
        } else {
            Log.e(TAG, "signIn: mGoogleSignInClient is null");
        }
    }

    public void signOut() {
        mGoogleSignInClient.signOut();
    }

    public boolean isConnected() {
        if (mFragmentActivity == null) {
            Log.e(TAG, "isConnected: mFragmentActivity is null");
            return false;
        }
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(mFragmentActivity);
        if (account == null)
            return false;
        return true;
    }

    /**
     * Handles the data generated by the google account choice activity
     * @param data Intent from google account choice activity
     */
    public boolean handleSignInResult(Intent data) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        boolean success = task.isSuccessful();
        try {
            task.getResult(ApiException.class);
        } catch (ApiException e) {
            Log.d(TAG, "handleSignInResult: " + e.toString());
        }
        return success;
    }

    /**
     * Google account getter (once connected)
     * @return Google account
     */
    public GoogleSignInAccount getAccount() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(mFragmentActivity);
        if ( account == null) {
            Log.e(TAG, "getAccount: account is null");
        }
        return account;
    }
}

package org.videolan.vlcbenchmark.tools;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.videolan.vlcbenchmark.BenchGLActivity;
import org.videolan.vlcbenchmark.R;
import org.videolan.vlcbenchmark.RequestCodes;


/**
 * GoogleConnectionHandler is a singleton class that handles the android google connexion
 * It has to be accessible from several activities and fragments hence the singleton
 * It is based on the use of GoogleApiClient wich can't be instanciated more than once
 * and is specific to a context. Because of that it has to be created with the activity / fragment
 * that needs to connect to google and destroyed with it
 */
public class GoogleConnectionHandler {

    final static private String TAG = GoogleConnectionHandler.class.getName();

    static private GoogleConnectionHandler instance;

    /* google related variables */
    private GoogleSignInOptions mGoogleSignInOptions;
    private GoogleApiClient.OnConnectionFailedListener mFailedListener;
    private GoogleApiClient mGoogleApiClient;
    private FragmentActivity mFragmentActivity;
    private GoogleSignInAccount mAccount;

    private GoogleConnectionHandler() {
        mGoogleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                Log.e(TAG, "Google authentification failed");
            }
        };
    }

    /** Getter for the singleton instance */
    public static GoogleConnectionHandler getInstance() {
        if (instance == null) {
            instance = new GoogleConnectionHandler();
        }
        return instance;
    }

    /**
     * Sets the calling context and instanciates the GoogleApiClient
     * To be called when resuming an activity
     * @param context calling context
     * @param fragmentActivity calling fragment
     */
    /* */
    public void setGoogleApiClient(Context context, FragmentActivity fragmentActivity) {
        if (mGoogleApiClient == null) {
            mFragmentActivity = fragmentActivity;
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .enableAutoManage(fragmentActivity, mFailedListener)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, mGoogleSignInOptions)
                    .build();
        }
    }

    /**
     * Destroys the GoogleApiClient and resets calling context
     * To be called when pausing a fragment or activity
     */
    public void unsetGoogleApiClient() {
        if (mGoogleApiClient != null && mFragmentActivity != null) {
            mGoogleApiClient.stopAutoManage(mFragmentActivity);
            mFragmentActivity = null;
            mGoogleApiClient = null;
        }
    }

    /**
     * If silentSignIn has failed then asks the user what account he wants to connect
     * else just gets the user account
     * @param googleSignInResult results from silentSignIn
     */
    private void getUser(GoogleSignInResult googleSignInResult) {
        if (!googleSignInResult.isSuccess()) {
            Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
            mFragmentActivity.startActivityForResult(signInIntent, RequestCodes.GOOGLE_CONNECTION);
        } else {
            handleSignInResult(googleSignInResult);
            if (mFragmentActivity instanceof org.videolan.vlcbenchmark.ResultPage) {
                mFragmentActivity.startActivityForResult(new Intent(mFragmentActivity, BenchGLActivity.class), RequestCodes.OPENGL);
            }
        }
    }

    /**
     * Tries to connect through silentSignIn
     * Sets a callback if the result is pending
     */
    public void signIn() {
        if (mGoogleApiClient != null) {
            OptionalPendingResult<GoogleSignInResult> pendingResult = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
            if (pendingResult.isDone()) {
                getUser(pendingResult.get());
            } else {
                pendingResult.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                    @Override
                    public void onResult(@NonNull GoogleSignInResult googleSignInResult) {
                        getUser(googleSignInResult);
                    }
                });
            }
        } else {
            Log.e(TAG, "signIn: GoogleClientApi null");
        }
    }

    /**
     * Signs out from google
     */
    public void signOut() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                    new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            Log.e(TAG, "Google signOut status: " + status.toString());
                        }
                    }
            );
        }
    }

    /**
     * Updates the connect and disconnect button from SettingsFragment
     * according to the connection success status
     * @param fragment SettingsFragment
     * @param googleSignInResult Sign in results
     */
    private void updateGoogleButton(PreferenceFragmentCompat fragment, GoogleSignInResult googleSignInResult) {
        if (!googleSignInResult.isSuccess()) {
            if (fragment.findPreference("connect_key") == null) {
                Preference preference = fragment.findPreference("disconnect_key");
                preference.setTitle(fragment.getResources().getString(R.string.connect_pref));
                preference.setKey("connect_key");
            }
        } else {
            if (fragment.findPreference("disconnect_key") == null) {
                Preference preference = fragment.findPreference("connect_key");
                preference.setTitle(fragment.getResources().getString(R.string.disconnect_pref));
                preference.setKey("disconnect_key");
            }
        }
    }

    /**
     * Uses silentSignIn to check if the user is connected to Google
     * Then called updateGoogleButton to update the UI accordingly
     * @param fragment SettingsFragment instance
     */
    public void checkConnection(final PreferenceFragmentCompat fragment) {
        if (mGoogleApiClient != null) {
            OptionalPendingResult<GoogleSignInResult> pendingResult = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
            if (pendingResult.isDone()) {
                updateGoogleButton(fragment, pendingResult.get());
            } else {
                pendingResult.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                    @Override
                    public void onResult(@NonNull GoogleSignInResult googleSignInResult) {
                        updateGoogleButton(fragment, googleSignInResult);
                    }
                });
            }
        }
    }

    /**
     * Handles the data generated by the google account choice activity
     * @param data Intent from google account choice activity
     */
    public void handleSignInResult(Intent data) {
        GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
        mAccount = result.getSignInAccount();
    }

    /**
     * Handles the data from a connection
     * @param result date from google connection activity
     */
    private void handleSignInResult(GoogleSignInResult result) {
        mAccount = result.getSignInAccount();
    }

    /**
     * Google account getter (once connected)
     * @return Google account
     */
    public GoogleSignInAccount getAccount() {
        return mAccount;
    }
}

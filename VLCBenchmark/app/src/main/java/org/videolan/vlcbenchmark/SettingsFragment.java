/*****************************************************************************
 * SettingsFragment.java
 *****************************************************************************
 * Copyright © 2017 VLC authors and VideoLAN
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

package org.videolan.vlcbenchmark;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;

import org.videolan.vlcbenchmark.tools.DialogInstance;
import org.videolan.vlcbenchmark.tools.FileHandler;
import org.videolan.vlcbenchmark.tools.GoogleConnectionHandler;
import org.videolan.vlcbenchmark.tools.JsonHandler;

import java.io.File;

public class SettingsFragment extends PreferenceFragmentCompat {

    private static String TAG = SettingsFragment.class.getName();

    GoogleConnectionHandler mGoogleConnectionHandler;

    ISettingsFragment mListener;

    @Override
    public void onResume() {
        super.onResume();
        mGoogleConnectionHandler = GoogleConnectionHandler.getInstance();
        mGoogleConnectionHandler.setGoogleSignInClient(getContext(), getActivity());
        this.updateGoogleButton();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.app_preferences);
    }

    private void deleteSamples() {
        DialogInstance dialog = new DialogInstance(
                R.string.dialog_title_sample_deletion, R.string.dialog_text_file_deletion_success);
        File dir = new File(FileHandler.getFolderStr("media_folder"));
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.delete()) {
                    Log.e("VLCBench", "Failed to delete sample " + file.getName());
                    dialog.setMessage(R.string.dialog_text_sample_deletion_failure);
                    break;
                }
            }
        }
        dialog.display(getActivity());
        mListener.resetDownload();
    }

    private void deleteResults() {
        boolean ret = JsonHandler.deleteFiles();
        DialogInstance dialog;
        if (ret) {
            dialog = new DialogInstance(R.string.dialog_title_file_deletion, R.string.dialog_text_file_deletion_success);
        } else {
            dialog = new DialogInstance(R.string.dialog_title_file_deletion, R.string.dialog_text_file_deletion_failure);
        }
        dialog.display(getActivity());
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        AlertDialog.Builder dialog;
        dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.dialog_title_warning)
                .setMessage(R.string.dialog_text_deletion_confirmation)
                .setNeutralButton(R.string.dialog_btn_cancel, null);
        switch (preference.getKey()) {
            case "delete_saves_key":
                dialog.setNegativeButton(R.string.dialog_btn_continue, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteResults();
                    }
                });
                dialog.show();
                break;
            case "connect_key":
                mGoogleConnectionHandler.signIn();
                break;
            case "disconnect_key":
                mGoogleConnectionHandler.signOut();
                this.updateGoogleButton();
                break;
            case "delete_samples_key":
                dialog.setNegativeButton(R.string.dialog_btn_continue, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteSamples();
                    }
                });
                dialog.show();
                break;
            case "about_key":
                Log.e("VLCBench", "about_key selected");
                startActivity(new Intent(getActivity(), AboutActivity.class));
                break;
            default:
                Log.e("VLCBench", "Unknown preference selected");
                break;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.w(TAG, "onActivityResult: " + requestCode );
        if (requestCode == Constants.RequestCodes.GOOGLE_CONNECTION) {
            mGoogleConnectionHandler.setGoogleSignInClient(getContext(), getActivity());
            if (mGoogleConnectionHandler.handleSignInResult(data)) {
                updateGoogleButton();
            } else {
                DialogInstance dialogInstance = new DialogInstance(R.string.dialog_title_error, R.string.dialog_text_err_google);
                dialogInstance.display(getContext());
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateGoogleButton() {
        if (!mGoogleConnectionHandler.isConnected()) {
            if (this.findPreference("connect_key") == null) {
                Preference preference = this.findPreference("disconnect_key");
                preference.setTitle(this.getResources().getString(R.string.connect_pref));
                preference.setKey("connect_key");
            }
        } else {
            if (this.findPreference("disconnect_key") == null) {
                Preference preference = this.findPreference("connect_key");
                preference.setTitle(this.getResources().getString(R.string.disconnect_pref));
                preference.setKey("disconnect_key");
            }
        }
    }

    @Override
    public void onPause() {
        mGoogleConnectionHandler.unsetGoogleSignInClient();
        super.onPause();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ISettingsFragment) {
            mListener = (ISettingsFragment) context;
        } else {
            throw new RuntimeException(context.toString());
        }
    }

    public interface ISettingsFragment {
        void resetDownload();
    }

}

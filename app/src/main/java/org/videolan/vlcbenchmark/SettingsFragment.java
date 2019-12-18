/*
 *****************************************************************************
 * SettingsFragment.java
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

package org.videolan.vlcbenchmark;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.videolan.vlcbenchmark.tools.CopyFilesTask;
import org.videolan.vlcbenchmark.tools.DialogInstance;
import org.videolan.vlcbenchmark.tools.FormatStr;
import org.videolan.vlcbenchmark.tools.GoogleConnectionHandler;
import org.videolan.vlcbenchmark.tools.JsonHandler;
import org.videolan.vlcbenchmark.tools.StorageManager;
import org.videolan.vlcbenchmark.tools.Util;

import java.io.File;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import kotlin.Unit;

public class SettingsFragment extends PreferenceFragmentCompat implements CopyFilesTask.IOnFilesCopied {

    private static String TAG = SettingsFragment.class.getName();

    private GoogleConnectionHandler mGoogleConnectionHandler;
    private ProgressDialog progressDialog = null;
    private Long mCopySize = -1L;
    private CopyFilesTask mTask = null;

    @Override
    public void onResume() {
        super.onResume();

        mGoogleConnectionHandler = GoogleConnectionHandler.getInstance();
        mGoogleConnectionHandler.setGoogleSignInClient(getContext(), getActivity());
        this.updateGoogleButton();

        ListPreference preference = this.findPreference("storage_key");
        if (getActivity() != null &&  preference != null) {
            preference.setTitle(getActivity().getString(R.string.storage_pref));
            setStoragePreference(preference, null);
            ArrayList<String> entries = new ArrayList<String>();
            entries.add(getActivity().getString(R.string.internal_memory));
            for (String val : StorageManager.INSTANCE.getExternalStorageDirectories()) {
                entries.add(String.format(getActivity().getString(R.string.sdcard), val));
            }
            preference.setEntries(entries.toArray(new CharSequence[entries.size()]));
            ArrayList<String> entryValues = new ArrayList<String>();
            entryValues.add(StorageManager.INSTANCE.getEXTERNAL_PUBLIC_DIRECTORY());
            entryValues.addAll(StorageManager.INSTANCE.getExternalStorageDirectories());
            preference.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));
            SettingsFragment context = this;
            preference.setOnPreferenceChangeListener((Preference changedPreference, Object newValue) ->{
                String oldValue = StorageManager.INSTANCE.getDirectory();
                if (oldValue != null && StorageManager.INSTANCE.checkNewMountpointFreeSpace(oldValue, (String)newValue)) {
                    mCopySize = StorageManager.INSTANCE.getDirectoryMemoryUsage(oldValue);
                    mTask = new CopyFilesTask(context);
                    mTask.execute(oldValue, newValue + StorageManager.INSTANCE.getBaseDir());
                    createDialog();
                    return true;
                } else {
                    Toast.makeText(getActivity(), R.string.toast_error_mountpoint_no_space, Toast.LENGTH_LONG).show();
                    return false;
                }
            });
        }
    }

    private void createDialog() {
        if (getActivity() != null && getFragmentManager() != null) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            progressDialog = new ProgressDialog();
            progressDialog.setTitle(R.string.dialog_title_file_copy);
            progressDialog.setCancelCallback(this::onDialogCancel);
            progressDialog.setCancelable(false);
            progressDialog.show(getFragmentManager(), "Copy dialog");
        }
    }

    private Unit onDialogCancel() {
        if (getActivity() != null)
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
        if (mTask != null)
            mTask.cancel(true);
        return Unit.INSTANCE;
    }

    @Override
    public void onFileCopied(boolean success, @NonNull String newValue) {
        newValue = newValue.replace(StorageManager.INSTANCE.getBaseDir(), "");
        if (getActivity() != null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
            ListPreference preference = findPreference("storage_key");
            if (preference != null) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("storage_dir", newValue);
                editor.commit();
                setStoragePreference(preference, newValue);
            }
            if (!success) {
                Toast.makeText(getActivity(), R.string.toast_error_file_transfert, Toast.LENGTH_LONG).show();
            }
        }
        mTask = null;
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
        mCopySize = -1L;
    }

    @Override
    public void updateProgress(long downloadSize, long downloadSpeed) {
        Util.runInUiThread(() -> {
            if (getContext() != null && progressDialog != null) {
                double percent = (double)downloadSize / (double)mCopySize * 100d;
                String progressString = String.format(
                        getContext().getString(R.string.dialog_text_download_progress),
                        FormatStr.INSTANCE.format2Dec(percent),
                        FormatStr.INSTANCE.byteRateToString(getContext(), downloadSpeed),
                        FormatStr.INSTANCE.byteSizeToString(getContext(), downloadSize),
                        FormatStr.INSTANCE.byteSizeToString(getContext(), mCopySize));
                progressDialog.updateProgress(percent, progressString, "");
            }
        });
    }

    private void setStoragePreference(ListPreference preference, String location) {
        String subtitle;
        if (location == null)
            location = StorageManager.INSTANCE.getMountpoint();
        preference.setValue(location);
        if (StorageManager.INSTANCE.getEXTERNAL_PUBLIC_DIRECTORY().equals(location)) {
            subtitle = getString(R.string.internal_memory);
        } else {
            if (getActivity() != null) {
                subtitle = String.format(getActivity().getString(R.string.sdcard),
                        StorageManager.INSTANCE.getFilenameFromPath(location));
            } else {
                subtitle = StorageManager.INSTANCE.getFilenameFromPath(location);
            }
        }
        preference.setSummary(subtitle);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.app_preferences);
    }

    private void deleteSamples() {
        DialogInstance dialog = new DialogInstance(
                R.string.dialog_title_sample_deletion, R.string.dialog_text_file_deletion_success);
        String dirPath = StorageManager.INSTANCE.getInternalDirStr("media_folder");
        if (dirPath == null) {
            Log.e(TAG, "deleteSamples: media forlder path is null");
            return;
        }
        File dir = new File(dirPath);
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
                Intent intent = new Intent(getActivity(), AboutActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                break;
            case "storage_key":
                //Handled in the preference onchange listener
                break;
            default:
                Log.e("VLCBench", "Unknown preference selected");
                break;
        }
        return super.onPreferenceTreeClick(preference);
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
                preference.setTitle(this.getResources().getString(R.string.disconnect_pref) + " " + mGoogleConnectionHandler.getAccount().getEmail());
                preference.setKey("disconnect_key");
            }
        }
    }

    @Override
    public void onPause() {
        mGoogleConnectionHandler.unsetGoogleSignInClient();
        if (mTask != null) {
            mTask.cancel(true);
        }
        super.onPause();
    }

}

package org.videolan.vlcbenchmark;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;

import org.videolan.vlcbenchmark.tools.FileHandler;
import org.videolan.vlcbenchmark.tools.GoogleConnectionHandler;
import org.videolan.vlcbenchmark.tools.JsonHandler;

import java.io.File;

public class SettingsFragment extends PreferenceFragmentCompat {

    GoogleConnectionHandler mGoogleConnectionHandler;

    ISettingsFragment mListener;

    @Override
    public void onResume() {
        super.onResume();
        mGoogleConnectionHandler = GoogleConnectionHandler.getInstance();
        mGoogleConnectionHandler.setGoogleApiClient(getContext(), getActivity());
        mGoogleConnectionHandler.checkConnection(this);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.app_preferences);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        boolean ret;

        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setNeutralButton(android.R.string.ok, null);

        switch (preference.getKey()) {
            case "delete_saves_key":
                ret = JsonHandler.deleteFiles();
                dialog.setTitle(getResources().getString(R.string.dialog_title_file_deletion));
                if (ret) {
                    dialog.setMessage(getResources().getString(R.string.dialog_text_file_deletion_success));
                } else {
                    dialog.setMessage(getResources().getString(R.string.dialog_text_file_deletion_failure));
                }
                dialog.show();
                break;
            case "connect_key":
                mGoogleConnectionHandler.signIn();
                mGoogleConnectionHandler.checkConnection(this);
                break;
            case "disconnect_key":
                mGoogleConnectionHandler.signOut();
                mGoogleConnectionHandler.checkConnection(this);
                break;
            case "delete_samples_key":
                dialog.setTitle(getResources().getString(R.string.dialog_title_sample_deletion));
                dialog.setMessage(getResources().getString(R.string.dialog_text_sample_deletion_success));
                File dir = new File(FileHandler.getFolderStr("media_folder"));
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.delete()) {
                            Log.e("VLCBench", "Failed to delete sample " + file.getName());
                            dialog.setMessage(getResources().getString(R.string.dialog_text_sample_deletion_failure));
                            break;
                        }
                    }
                }
                dialog.show();
                mListener.resetDownload();
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
    public void onPause() {
        mGoogleConnectionHandler.unsetGoogleApiClient();
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

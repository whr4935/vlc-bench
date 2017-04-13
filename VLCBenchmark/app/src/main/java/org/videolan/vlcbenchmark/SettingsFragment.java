package org.videolan.vlcbenchmark;

import android.content.Context;
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

    final String FILE_DELETION = "File deletion";
    final String FILE_DELETION_MESSAGE_FAILURE = "Failed to delete all test results";
    final String FILE_DELETION_MESSAGE_SUCCESS = "Deleted all test results";

    final String SAMPLE_DELETION = "Sample deletion";
    final String SAMPLE_DELETION_MESSAGE_FAILURE = "Failed to delete all samples";
    final String SAMPLE_DELETION_MESSAGE_SUCCESS = "Deleted all samples";

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
                dialog.setTitle(FILE_DELETION);
                if (ret) {
                    dialog.setMessage(FILE_DELETION_MESSAGE_SUCCESS);
                } else {
                    dialog.setMessage(FILE_DELETION_MESSAGE_FAILURE);
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
                dialog.setTitle(SAMPLE_DELETION);
                dialog.setMessage(SAMPLE_DELETION_MESSAGE_SUCCESS);
                File dir = new File(FileHandler.getFolderStr("media_folder"));
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.delete()) {
                            Log.e("VLCBench", "Failed to delete sample " + file.getName());
                            dialog.setMessage(SAMPLE_DELETION_MESSAGE_FAILURE);
                            break;
                        }
                    }
                }
                dialog.show();
                mListener.resetDownload();
                break;
            case "about_key":
                Log.e("VLCBench", "about_key selected");
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

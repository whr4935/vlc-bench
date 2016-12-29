package org.videolan.vlcbenchmark;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.videolan.vlcbenchmark.tools.JsonHandler;

import java.util.ArrayList;

public class SettingsFragment extends PreferenceFragmentCompat {

    final String FILE_DELETION = "File deletion";
    final String FILE_DELETION_MESSAGE_FAILURE = "Failed to delete all test results";
    final String FILE_DELETION_MESSAGE_SUCCESS = "Deleted all test results";

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
            case "delete_key":
                ret = JsonHandler.deleteFiles();
                dialog.setTitle(FILE_DELETION);
                if (ret) {
                    dialog.setMessage(FILE_DELETION_MESSAGE_SUCCESS);
                } else {
                    dialog.setMessage(FILE_DELETION_MESSAGE_FAILURE);
                }
                dialog.show();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }
}

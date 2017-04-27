package org.videolan.vlcbenchmark.tools;

import android.app.AlertDialog;
import android.content.Context;

import org.videolan.vlcbenchmark.R;

public class DialogInstance {

    private int titleResId;
    private int textResId;

    public DialogInstance(int titleResId, int textResId) {
        this.titleResId = titleResId;
        this.textResId = textResId;
    }

    public void display(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(context.getResources().getString(titleResId))
                .setMessage(context.getResources().getString(textResId))
                .setNegativeButton(context.getResources().getString(R.string.dialog_btn_ok), null)
                .show();
    }

    public void setMessage(int textResId) {
        this.textResId = textResId;
    }
}

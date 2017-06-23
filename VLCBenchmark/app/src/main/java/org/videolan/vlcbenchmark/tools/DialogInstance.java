/*****************************************************************************
 * DialogInstance.java
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

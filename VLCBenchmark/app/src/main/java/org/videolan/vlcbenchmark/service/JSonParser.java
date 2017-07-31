/*****************************************************************************
 * JSonParser.java
 *****************************************************************************
 * Copyright © 2016-2017 VLC authors and VideoLAN
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

package org.videolan.vlcbenchmark.service;

import android.content.Context;
import android.util.JsonReader;
import android.util.Pair;

import org.videolan.vlcbenchmark.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by penava_b on 12/07/16.
 */
public class JSonParser {

    private static String encoding = null;

    public static String getEncoding() {
        return encoding;
    }

    public static List<MediaInfo> getMediaInfos(Context context) throws IOException {
        URL url = new URL(context.getString(R.string.config_file_location_url));
        URLConnection connection = url.openConnection();
        InputStream in = connection.getInputStream();
        encoding = connection.getContentEncoding();
        encoding = (encoding == null ? "UTF-8" : encoding);
        JsonReader reader = null;
        try {
            reader = new JsonReader(new InputStreamReader(in, encoding));
            return readMessagesArray(reader);
        } finally {
            reader.close();
        }
    }

    static List<MediaInfo> readMessagesArray(JsonReader reader) throws IOException {
        List<MediaInfo> messages = new ArrayList<MediaInfo>();

        reader.beginArray();
        while (reader.hasNext()) {
            messages.add(readMediaInfo(reader));
        }
        reader.endArray();
        return messages;
    }

    static MediaInfo readMediaInfo(JsonReader reader) throws IOException {
        String url = null, name = null, checksum = null;
        Pair<ArrayList<Long>, ArrayList<int[]>> snapshot = null;

        try {
            reader.beginObject();
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case "url":
                        url = reader.nextString();
                        break;
                    case "name":
                        name = reader.nextString();
                        break;
                    case "checksum":
                        checksum = reader.nextString();
                        break;
                    case "snapshot":
                        snapshot = readLongArray(reader);
                        break;
                    default:
                        reader.skipValue();
                        break;
                }
            }
            reader.endObject();
        } catch (IllegalStateException e) {
            throw new IOException("VLCBenchmark is using a too old version. Update the application to fix: " + e.toString());
        }
        return new MediaInfo(url, name, checksum, snapshot.first, snapshot.second);
    }

    static Pair<ArrayList<Long>, ArrayList<int[]>> readLongArray(JsonReader reader) throws IOException {
        Pair<ArrayList<Long>, ArrayList<int[]>> result = new Pair<>(new ArrayList<Long>(), new ArrayList<int[]>());
        int[] colorValues;
        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginArray();
            result.first.add(reader.hasNext() ? reader.nextLong() : 0L);
            reader.beginArray();
            colorValues = new int[30];
            int i = 0;
            while (reader.hasNext()) {
                colorValues[i] = reader.nextInt();
                i += 1;
            }
            result.second.add(colorValues);
            reader.endArray();
//            result.second.add(reader.hasNext() ? reader.nextInt() : 0);
            reader.endArray();
        }
        reader.endArray();
        return result;
    }
}
package org.videolan.vlcbenchmark.service;

import android.util.JsonReader;
import android.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by penava_b on 12/07/16.
 */
public class JSonParser {

    private static final String JSON_FILE_URL = "https://raw.githubusercontent.com/DaemonSnake/FileDump/master/test.json";
    private static String encoding = null;

    public static String getEncoding() {
        return encoding;
    }

    public static List<MediaInfo> getMediaInfos() throws IOException {
        URL url = new URL(JSON_FILE_URL);
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
        Pair<ArrayList<Long>, ArrayList<Integer>> snapshot = null;

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
            throw new IOException("VLCBenchmark is using a too old version. Update the application to fix");
        }
        return new MediaInfo(url, name, checksum, snapshot.first, snapshot.second);
    }

    static Pair<ArrayList<Long>, ArrayList<Integer>> readLongArray(JsonReader reader) throws IOException {
        Pair<ArrayList<Long>, ArrayList<Integer>> result = new Pair<>(new ArrayList<Long>(), new ArrayList<Integer>());

        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginArray();
            result.first.add(reader.hasNext() ? reader.nextLong() : 0L);
            result.second.add(reader.hasNext() ? reader.nextInt() : 0);
            reader.endArray();
        }
        reader.endArray();
        return result;
    }
}
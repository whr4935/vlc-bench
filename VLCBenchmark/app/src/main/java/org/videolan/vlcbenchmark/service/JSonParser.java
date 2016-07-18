package org.videolan.vlcbenchmark.service;

import android.util.JsonReader;

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

    private static final String JSON_FILE_URL = "https://raw.githubusercontent.com/DaemonSnake/FileDump/master/test.json";
    private static String encoding = null;

    public static String getEncoding()
    {
        return encoding;
    }

    public static List<MediaInfo> getMediaInfos() throws IOException {
        URL url = new URL(JSON_FILE_URL);
        URLConnection connection = url.openConnection();
        InputStream in = connection.getInputStream();
        encoding = connection.getContentEncoding();
        encoding = (encoding == null ? "UTF-8" : encoding);
        JsonReader reader = null;
        try
        {
            reader = new JsonReader(new InputStreamReader(in, encoding));
            return readMessagesArray(reader);
        }
        finally {
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
        List<Double> snapshot = null;

        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName())
            {
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
                    snapshot = readDoublesArray(reader);
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        return new MediaInfo(url, name, checksum, snapshot);
    }

    static List<Double> readDoublesArray(JsonReader reader) throws IOException {
        List<Double> doubles = new ArrayList<Double>();

        reader.beginArray();
        while (reader.hasNext())
            doubles.add(reader.nextDouble());
        reader.endArray();
        return doubles;
    }
}
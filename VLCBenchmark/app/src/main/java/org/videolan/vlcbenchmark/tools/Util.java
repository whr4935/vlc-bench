package org.videolan.vlcbenchmark.tools;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Util {

    private final static String TAG = Util.class.getName();

    public static String readAsset(String assetName, AssetManager assetManager) {
        InputStream is = null;
        BufferedReader r = null;
        try {
            is = assetManager.open(assetName);
            r = new BufferedReader(new InputStreamReader(is, "UTF8"));
            StringBuilder sb = new StringBuilder();
            String line = r.readLine();
            if(line != null) {
                sb.append(line);
                line = r.readLine();
                while(line != null) {
                    sb.append('\n');
                    sb.append(line);
                    line = r.readLine();
                }
            }
            return sb.toString();
        } catch (IOException e) {
            return null;
        } finally {
            close(is);
            close(r);
        }
    }

    private static boolean close(Closeable closeable) {
        if (closeable != null)
            try {
                closeable.close();
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Failed to close: " + e.toString());
            }
        return false;
    }
}

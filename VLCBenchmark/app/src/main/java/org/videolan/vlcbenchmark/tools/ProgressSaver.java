package org.videolan.vlcbenchmark.tools;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.videolan.vlcbenchmark.BuildConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProgressSaver {

    private final static String TAG = "ProgressSaver";
    private final static String mFilename = "save.json";

    public static List<TestInfo>[] load(Context context) {
        File jsonFile = new File(context.getFilesDir(), mFilename);
        List<TestInfo>[] loopList;
        try {
            StringBuilder text = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(jsonFile));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
            JSONArray jsonArray = new JSONArray(text.toString());
            Log.w(TAG, jsonArray.toString());
            if (jsonArray.length() == 1) {
                loopList = new ArrayList[]{new ArrayList<TestInfo>()};
            } else {
                loopList = new ArrayList[]{new ArrayList<TestInfo>(), new ArrayList<TestInfo>(), new ArrayList()};
            }
            for (int i = 0 ; i < jsonArray.length() ; ++i) {
                String jsonArrayString = jsonArray.getJSONArray(i).toString();
                JSONArray resultList = new JSONArray(jsonArrayString);
                ArrayList<TestInfo> testInfoList = new ArrayList<>();
                for (int inc = 0 ; inc < resultList.length() ; inc++ ) {
                    testInfoList.add(new TestInfo(resultList.getJSONObject(inc)));
                }
                loopList[i] = testInfoList;
            }
        } catch (FileNotFoundException e) {
            Log.i(TAG, "Json file not found: " + e.toString());
            return null;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to load json file : " + e.toString());
            return null;
        } catch (IOException e) {
            Log.e(TAG, "Failed to read jsonFile : " + e.toString());
            return null;
        }
        return loopList;
    }

    public static String save(Context context, List<TestInfo>[] testInfoList) {
        File jsonFile = new File(context.getFilesDir(), mFilename);
        try {
            JSONArray jsonTestInformation = new JSONArray();
            for (List<TestInfo> testInfo : testInfoList) {
                JSONArray jsonTestResults = new JSONArray();
                for (TestInfo result : testInfo) {
                    JSONObject jsonResult = result.jsonDump();
                    jsonTestResults.put(jsonResult);
                }
                jsonTestInformation.put(jsonTestResults);
            }
            FileOutputStream jsonFileOutputStream;

            jsonFileOutputStream = new FileOutputStream(jsonFile, false);
            if (BuildConfig.DEBUG) {
                jsonFileOutputStream.write(jsonTestInformation.toString().getBytes());
            } else {
                jsonFileOutputStream.write(jsonTestInformation.toString().getBytes());
            }

        } catch (IOException | JSONException e) {
            Log.e(TAG, "Failed to save json test results");
            FileHandler.delete(jsonFile);
            return null;
        }
        return mFilename;
    }

    public static void discard(Context context) {
        File jsonFile = new File(context.getFilesDir(), mFilename);
        jsonFile.delete();
    }
}

package org.videolan.vlcbenchmark;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.util.Log;
import android.widget.TabHost;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import values.GridFragment;

public class ResultPage extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_page);

        ArrayList<TestInfo> r1 = (ArrayList<TestInfo>)getIntent().getSerializableExtra("resultsTestOne");
        ArrayList<TestInfo> r2 = (ArrayList<TestInfo>)getIntent().getSerializableExtra("resultsTestTwo");
        ArrayList<TestInfo> r3 = (ArrayList<TestInfo>)getIntent().getSerializableExtra("resultsTestThree");

        double soft = getIntent().getDoubleExtra("soft", 0);
        double hard = getIntent().getDoubleExtra("hard", 0);

        final FragmentTabHost mTabHost = (FragmentTabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);

        Bundle args = new Bundle();
        args.putSerializable("results", r1);
        mTabHost.addTab(mTabHost.newTabSpec("tab1").setIndicator("Test 1"), GridFragment.class, args);
        if (r2.size() > 0) {
            args = new Bundle();
            args.putSerializable("results", r2);
            mTabHost.addTab(mTabHost.newTabSpec("tab2").setIndicator("Test 2"), GridFragment.class, args);
        }
        if (r3.size() > 0) {
            args = new Bundle();
            args.putSerializable("results", r3);
            mTabHost.addTab(mTabHost.newTabSpec("tab3").setIndicator("Test 3"), GridFragment.class, args);
        }

        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener(){
            @Override
            public void onTabChanged(String tabId) {
            }});

        TextView softView = (TextView)findViewById(R.id.softAvg);
        String softText = "Software score : " + soft;
        softView.setText(softText);

        TextView hardView = (TextView)findViewById(R.id.hardAvg);
        String hardText = "Hardware score : " + hard;
        hardView.setText(hardText);

    }

    /**
     * Returns the JSON array to send to the server.
     * @param testInfoList list of all test results.
     * @return null in case of failure.
     */
    public JSONObject dumpResults(ArrayList<TestInfo> testInfoList) {
        JSONObject results = new JSONObject();
        JSONObject deviceInformation;
        JSONArray testInformation;

        deviceInformation = getDeviceInformation();
        testInformation = getTestInformation(testInfoList);

        if (deviceInformation == null || testInformation == null) {
            return null;
        }

        try {
            results.put("device_information", deviceInformation);
            results.put("test_information", testInformation);
        } catch (JSONException e) {
            Log.e("VLC - Benchmark", "Failed to build final result json");
            return null;
        }

        return results;
    }

    /**
     * Returns test information in a JSONArray.
     * @param testInfoList list of all test results.
     * @return null in case of failure.
     */
    private JSONArray getTestInformation(ArrayList<TestInfo> testInfoList) {
        JSONArray testInfoArray = new JSONArray();

        for (TestInfo element : testInfoList) {
            JSONObject testInfo = new JSONObject();
            try {
                testInfo.put("name", element.name);
                testInfo.put("hardware_score", element.hardware);
                testInfo.put("software_score", element.software);
                testInfo.put("loop_number", element.loopNumber);
                testInfo.put("frame_dropped", element.frameDropped);
                testInfo.put("percent_of_bad_screenshot", element.percentOfBadScreenshots);
                testInfo.put("percent_of_bad_seek", element.percentOfBadSeek);
                testInfo.put("number_of_warning", element.numberOfWarnings);
                testInfoArray.put(testInfo);
            } catch (JSONException e) {
                Log.e("VLC - Benchmark", "Failed to build test information json object");
                return null;
            }
        }
        return testInfoArray;
    }

    /**
     * Returns device information in a JSONObject.
     * @return null in case of failure.
     */
    private JSONObject getDeviceInformation() {
        JSONObject properties = new JSONObject();

        try {
            properties.put("board", Build.BOARD);
            properties.put("bootloader", Build.BOOTLOADER);
            properties.put("brand", Build.BRAND);
            properties.put("device", Build.DEVICE);
            properties.put("display", Build.DISPLAY);
            properties.put("fingerprint", Build.FINGERPRINT);
            properties.put("host", Build.HOST);
            properties.put("id", Build.ID);
            properties.put("manufacturer", Build.MANUFACTURER);
            properties.put("model", Build.MODEL);
            properties.put("product", Build.PRODUCT);
            properties.put("serial", Build.SERIAL);

        /* Min version API 21 */
//        properties.put("supported_32_bit_abi", Build.SUPPORTED_32_BIT_ABIS);
//        properties.put("supported_64_bit_abi", Build.SUPPORTED_64_BIT_ABIS);
//        properties.put("supported_abi", Build.SUPPORTED_ABIS);

            properties.put("tags", Build.TAGS);
            properties.put("time", Build.TIME);
            properties.put("type", Build.TYPE);
            properties.put("user", Build.USER);

            properties.put("os_arch", System.getProperty("os.arch"));


        } catch (JSONException e) {
            Log.e("VLC - Benchmark", "Failed to build device information json object");
            return null;
        }
        return properties;
    }
}

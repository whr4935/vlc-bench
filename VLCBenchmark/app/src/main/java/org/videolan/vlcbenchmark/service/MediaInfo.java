package org.videolan.vlcbenchmark.service;

import java.util.List;

/**
 * Created by penava_b on 12/07/16.
 */
public class MediaInfo {
        String url;
        String name;
        List<Double> snapshot;
        String checksum;
        String localUrl;

    public MediaInfo(String url, String name, String checksum, List<Double> snapshot) {
        this.url = url;
        this.name = name;
        this.checksum = checksum;
        this.snapshot = snapshot;
        this.localUrl = null;
    }

    @Override
    public String toString()
    {
        return "MediaInfo: " + name + " " + url;
    }
}

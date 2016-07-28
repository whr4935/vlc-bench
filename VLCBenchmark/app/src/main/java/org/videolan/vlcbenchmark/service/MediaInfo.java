package org.videolan.vlcbenchmark.service;

import java.io.Serializable;
import java.util.List;

/**
 * Created by penava_b on 12/07/16.
 */
public class MediaInfo implements Serializable {

    String url;
        String name;
        List<Long> snapshot;
        String checksum;
        String localUrl;

    public MediaInfo(String url, String name, String checksum, List<Long> snapshot) {
        this.url = url;
        this.name = name;
        this.checksum = checksum;
        this.snapshot = snapshot;
        this.localUrl = null;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public List<Long> getSnapshot() {
        return snapshot;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getLocalUrl() {
        return localUrl;
    }

    @Override
    public String toString()
    {
        return "MediaInfo: " + name + " " + url;
    }
}

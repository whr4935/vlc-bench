/*
 *****************************************************************************
 * MediaInfo.java
 *****************************************************************************
 * Copyright © 2016-2018 VLC authors and VideoLAN
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

import java.io.Serializable;
import java.util.List;

public class MediaInfo implements Serializable {

    String url;
    String name;
    List<Long> timestamps;
    List<int[]> colors;
    String checksum;
    String localUrl;
    int size;

    public MediaInfo(String url, String name, String checksum, List<Long> timestamps, List<int[]> colors, int size) {
        this.url = url;
        this.name = name;
        this.checksum = checksum;
        this.localUrl = null;
        this.timestamps = timestamps;
        this.colors = colors;
        this.size = size;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public List<Long> getTimestamps() { return timestamps; }

    public List<int[]> getColors() {
        return colors;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getLocalUrl() {
        return localUrl;
    }

    public void setLocalUrl(String localUrl ) { this.localUrl = localUrl; }

    public int getSize() { return size; }

    @Override
    public String toString() {
        return "MediaInfo: " + name + " " + url;
    }
}

/*
 *****************************************************************************
 * CrashHandler.java
 *****************************************************************************
 * Copyright © 2017 - 2018 VLC authors and VideoLAN
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

package org.videolan.vlcbenchmark;

public class Constants {

    public static class RequestCodes {
        public static int RESULTS = 1;
        public static int VLC = 2;
        public static int GOOGLE_CONNECTION = 3;
        public static int OPENGL = 4;
    }

    public static class ResultCodes {
        public static final int RESULT_OK = -1;
        public static final int RESULT_CANCELED = 0;
        public static final int RESULT_NO_HW = 1;
        public static final int RESULT_CONNECTION_FAILED = 2;
        public static final int RESULT_PLAYBACK_ERROR = 3;
        public static final int RESULT_HARDWARE_ACCELERATION_ERROR = 4;
        public static final int RESULT_VIDEO_TRACK_LOST = 5;
        public static final int RESULT_VLC_CRASH = 6;
    }

}

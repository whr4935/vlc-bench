/*
 *****************************************************************************
 * VLCWorkerModel.java
 *****************************************************************************
 * Copyright © 2018 VLC authors and VideoLAN
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

package org.videolan.vlcbenchmark.benchmark

enum class TestTypes {
    SOFTWARE_SCREENSHOT,
    SOFTWARE_PLAYBACK,
    HARDWARE_SCREENSHOT,
    HARDWARE_PLAYBACK;

    /**
     * Allows to use this enum as an incrementing type that loops once it reached its last value.
     * @return the next enum in ordinal order after the current one. If none are after it will return the first.
     */
    operator fun next(): TestTypes {
        return values()[(ordinal + 1) % values().size]
    }

    /**
     * @return true if the ordinal of the current enum represents a test software
     */
    fun isSoftware(): Boolean {
        return ordinal / 2 % 2 == 0
    }

    /**
     * @return true if the ordinal of the current enum represned a screenshot test
     */
    fun isScreenshot(): Boolean {
        return ordinal % 2 == 0
    }
}
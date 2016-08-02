package org.videolan.vlcbenchmark;

import java.io.Serializable;

/**
 * Created by penava_b on 19/07/16.
 */
public class TestInfo implements Serializable {
    String name;
    double software;
    double hardware;
    int loopNumber;
    double percentOfFrameDrop;
    double percentOfBadScreenshots;
    double percentOfBadSeek;
    int numberOfWarnings;
}

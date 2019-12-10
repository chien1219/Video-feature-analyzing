package com.mycompany.videoquerying;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * A struct for containing the results of OpenCV's dominant color analysis.
 * @author stermark
 */
public class OpenCVColorResults implements Serializable
{
    ArrayList<FrameData> frames;
    public OpenCVColorResults()
    {
        frames = new ArrayList<>();
    }
}

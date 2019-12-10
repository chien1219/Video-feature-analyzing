package com.mycompany.videoquerying;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by ivanchen on 4/30/18.
 */
public class FrameData implements Serializable
{
    ArrayList<ColorData> frameColors;
    public FrameData()
    {
        frameColors = new ArrayList<>();
    }
}

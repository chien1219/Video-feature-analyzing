package com.mycompany.videoquerying;

import java.io.Serializable;

/**
 * A struct containing all data relating to a single segment within a video or shot label from Google Cloud results. 
 * @author stermark
 */
public class SegmentData implements Serializable
{
    public double startTime;
    public double endTime;
    public double confidence;
    
    public SegmentData()
    {
        startTime = 0;
        endTime = 0;
        confidence = 0;
    }
}

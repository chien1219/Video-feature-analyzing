package com.mycompany.videoquerying;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * A struct for containing all data relating to a single video label from Google Cloud Video Intelligence.
 * @author stermark
 */
public class VideoLabelData implements Serializable
{
    public String videoLabel;
    public ArrayList<String> videoLabelCategories;
    public SegmentData segmentData;
    
    public VideoLabelData()
    {
        videoLabel = "";
        videoLabelCategories = new ArrayList();
        segmentData = new SegmentData();
    }
}

package com.mycompany.videoquerying;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * A struct for containing the data associated with a single shot label obtained from Google Cloud Video Intelligence API.
 * @author stermark
 */
public class ShotLabelData implements Serializable
{
    public String shotLabel;
    public ArrayList<String> shotLabelCategories;
    public ArrayList<SegmentData> segments;
    
    public ShotLabelData()
    {
        shotLabel = "";
        shotLabelCategories = new ArrayList();
        segments = new ArrayList();
    }
}

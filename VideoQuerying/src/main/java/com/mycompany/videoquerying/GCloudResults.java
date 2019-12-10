package com.mycompany.videoquerying;

import java.io.Serializable;
import java.util.HashMap;

/**
 * A struct for containing the label detection results from Google Cloud Video Intelligence API for a single video.
 * @author stermark
 */
public class GCloudResults implements Serializable
{
    public HashMap<String, VideoLabelData> videoLabels;
    public HashMap<String, ShotLabelData> shotLabels;
    
    public GCloudResults()
    {
        videoLabels = new HashMap();
        shotLabels = new HashMap();
    }
}

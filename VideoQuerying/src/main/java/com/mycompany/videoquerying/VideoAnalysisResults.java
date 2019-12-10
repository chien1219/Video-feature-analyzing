package com.mycompany.videoquerying;

import java.io.Serializable;

/**
 * A struct containing the results of all descriptors for a single video.
 * @author stermark
 */
public class VideoAnalysisResults implements Serializable
{
    public String filename;
    public GCloudResults objectResults;
    public OpenCVColorResults colorResults;
    public OpenCVMotionResults motionResults;
    
    public VideoAnalysisResults()
    {
        filename = "";
        objectResults = new GCloudResults();
        colorResults = new OpenCVColorResults();
        motionResults = new OpenCVMotionResults();
    }
}

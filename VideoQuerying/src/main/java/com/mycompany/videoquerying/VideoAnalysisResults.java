package com.mycompany.videoquerying;

import java.awt.*;
import java.io.Serializable;

/**
 * A struct containing the results of all descriptors for a single video.
 * @author stermark
 */
public class VideoAnalysisResults implements Serializable
{
    public String filename;
    public GCloudResults objectResults;
    public OpenCVMotionResults motionResults;
    public OpenCVColorResults OpenCVColorResults;
    public ColorResults colorResults;

    public VideoAnalysisResults()
    {
        filename = "";
        objectResults = new GCloudResults();
        OpenCVColorResults = new OpenCVColorResults();
        motionResults = new OpenCVMotionResults();
        colorResults = new ColorResults();
    }
}

package com.mycompany.videoquerying;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * A struct for containing the results of OpenCV's motion vector analysis.
 * @author stermark
 */
public class OpenCVMotionResults implements Serializable
{
    public ArrayList<Double> frameMotion;
    public double totalMotion;
    public double averageMotion;
    
    public OpenCVMotionResults()
    {
        frameMotion = new ArrayList();
        totalMotion = 0;
        averageMotion = 0;
    }
}

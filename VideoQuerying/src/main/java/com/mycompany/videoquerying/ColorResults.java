package com.mycompany.videoquerying;

import java.io.Serializable;
import java.util.*;

/**
 * A struct for containing the results of OpenCV's dominant color analysis.
 */
public class ColorResults implements Serializable
{
    public Map<String, Integer> frameMap;
    public Map<String, Double> resultMap;
    public Map<String, double[]> resultScoreMap;
    //public OpenCVColorResults()    {     frames = new ArrayList<>();      }
    public ColorResults()    { }
}

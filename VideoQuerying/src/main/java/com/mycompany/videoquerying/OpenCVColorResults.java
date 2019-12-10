package com.mycompany.videoquerying;

import java.io.Serializable;
import java.util.ArrayList;

public class OpenCVColorResults implements Serializable{
    ArrayList<FrameData> frames;
    public OpenCVColorResults()    {       frames = new ArrayList<>();    }
}

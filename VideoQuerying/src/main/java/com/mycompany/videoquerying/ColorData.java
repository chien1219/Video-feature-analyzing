package com.mycompany.videoquerying;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by ivanchen on 4/30/18.
 */
class ColorData implements Serializable
{
    public int r;
    public int g;
    public int b;
    public double percentage;
    public ColorData(int r, int g, int b, double percentage)
    {
        this.r = r;
        this.g = g;
        this.b = b;
        this.percentage = percentage;
    }
}


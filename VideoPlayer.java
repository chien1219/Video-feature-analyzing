import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.lang.*;
import java.util.*;

import javax.swing.*;

public class VideoPlayer {
  public static final int WIDTH = 352;
  public static final int HEIGHT = 288;
  public static final String QUERY_FOLDER = "query";
  public static final String DB_FOLDER = "db";

  public static void main(String[] args) {

    ArrayList<BufferedImage> images = new ArrayList<BufferedImage>();

    BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
      for (int y = 0; y < HEIGHT; y++) {
        for (int x = 0; x < WIDTH; x++) {
   			  byte r = (byte)128;
   				byte g = (byte)128;
   				byte b = (byte)128; 
   				int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
    			image.setRGB(x,y,pix);
    		}
      }
    images.add(image);
	
	  VideoQueryUI ui = new VideoQueryUI(images);
	  ui.showUI();
  }
}
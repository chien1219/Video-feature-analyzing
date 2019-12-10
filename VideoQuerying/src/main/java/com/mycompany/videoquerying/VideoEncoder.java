package com.mycompany.videoquerying;

import java.awt.image.*;

import javax.imageio.ImageIO;

import java.io.*;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * Encodes videos to standard formats.
 * @author stermark
 */
public class VideoEncoder {
    
    private static int IMAGE_WIDTH = 352;
    private static int IMAGE_HEIGHT = 288;

    public VideoEncoder()
    {
        
    }

    /**
     * Returns a three digit string of the given frameNum
     * @param frameNum
     * @return 
     */
    private static String getFrameNumString(int frameNum)
    {
        if (frameNum < 10)
            return "00" + Integer.toString(frameNum);
        else if (frameNum < 100)
            return "0" + Integer.toString(frameNum);
        else
            return Integer.toString(frameNum);
    }
    
    /**
     * Encodes the frames and .wav file of the specified video to .mp4.
     * @param queryVideoDir
     * @return - Whether the encoding was successful or not.
     */
    public static boolean encodeMp4(String queryVideoDir)
    {
        File file = new File(queryVideoDir);
        String videoName = file.getName();
        String videosDir = file.getParent();
        
        // Create .png's of all the .rgb frames
        BufferedImage outputImage = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        String ext = ".rgb";
        String framePath = "";
        int frameNum = 1;
        boolean continueReadingFrames = true;
        while (continueReadingFrames)
        {
            // Get the framepath of the next video frame
            String formattedFrameNum = getFrameNumString(frameNum);
            framePath = videosDir + "/" + videoName + "/" + videoName + formattedFrameNum + ext;

            // Load in the next video frame
            boolean nextFrameAvailable = readImageRGB(IMAGE_WIDTH, IMAGE_HEIGHT, framePath, outputImage);

            if (nextFrameAvailable)
            {
                // Write out the frame to jpeg format
                String outputPath = videosDir + "/" + videoName + "/" + videoName + formattedFrameNum + ".png";
                writeImageToPng(outputImage, outputPath);

                // Prepare for next frame
                frameNum++;
            }
            else 
            {
                System.out.println("Could not read frame with index " + Integer.toString(frameNum) + ".");
                continueReadingFrames = false;
            }   
        }
        
        // Run ffmpeg with a new process to convert the .png's and .wav file to an .mp4 video
        // ffmpeg -framerate 30 -start_number 1 -i sports%03d.png -i sports.wav -pix_fmt yuv420p -c:v libx264 -vf scale=1056x864 -acodec aac sportsTest.mp4
        // ffmpeg -framerate 30 -start_number 1 -i sports%03d.png -i sports.wav -pix_fmt yuv420p -c:v libx264 -qp 1 -acodec aac NEWTEST2.mp4
        try
        {
//            String ffmpegCommand = "/usr/local/bin/ffmpeg -framerate 30 -start_number 1 -i " + videosDir+"/"+videoName+"/"+videoName + "%03d.png -i "
//                                    + videosDir+"/"+videoName+"/"+videoName + ".wav -pix_fmt yuv420p -c:v libx264 -qp 1 -acodec aac " + videosDir+"/"+videoName+"/"+videoName + ".mp4";
            
            //
             String ffmpegCommand = "/usr/local/bin/ffmpeg -framerate 30 -start_number 1 -i " + videosDir+"/"+videoName+"/"+videoName + "%03d.png -i "
                                    + videosDir+"/"+videoName+"/"+videoName + ".wav -pix_fmt yuv420p -c:v libx264 -vf scale=1056x864 -acodec aac " + videosDir+"/"+videoName+"/"+videoName + ".mp4";
            
            Runtime.getRuntime().exec(ffmpegCommand);
        }
        catch (Exception e)
        {
            System.out.println("Unable to use ffmpeg to process the files.");
//            e.printStackTrace();
            return false;
        }
        return true;
    }
    
    /**
     * Writes a single BufferedImage to the given fileName location as a .png
     * @param img
     * @param fileName 
     */
    private static void writeImageToPng(BufferedImage img, String fileName)
    {
        try
        {
            File outputfile = new File(fileName);
            ImageIO.write(img, "png", outputfile);
        }
        catch (IOException e)
        {
            System.out.println("Unable to write out image.");
        }
    }
     
    /**
     * Reads the image of given width and height at the given imgPath into the provided BufferedImage.
     * @param width
     * @param height
     * @param imgPath
     * @param img 
     */
    private static boolean readImageRGB(int width, int height, String imgPath, BufferedImage img)
    {
        try 
        {
            File file = new File(imgPath);
            InputStream is = new FileInputStream(file);

            long len = file.length();
            byte[] bytes = new byte[(int)len];

            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) 
            {
                offset += numRead;
            }

            int ind = 0;
            for(int y = 0; y < height; y++)
            {
                for(int x = 0; x < width; x++)
                {
                    byte a = 0;
                    byte r = bytes[ind];
                    byte g = bytes[ind+height*width];
                    byte b = bytes[ind+height*width*2]; 

                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    //int pix = ((a << 24) + (r << 16) + (g << 8) + b);
                    img.setRGB(x,y,pix);
                    ind++;
                }
            }
        } 
        catch (FileNotFoundException e) 
        {
//            e.printStackTrace();
            return false;
        } 
        catch (IOException e) 
        {
//            e.printStackTrace();
            return false;
        }
        return true;
    }
}

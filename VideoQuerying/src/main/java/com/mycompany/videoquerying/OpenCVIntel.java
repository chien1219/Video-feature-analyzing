package com.mycompany.videoquerying;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;


/**
 * Created by ivanchen on 4/22/18.
 */
public class OpenCVIntel {

    public static void CVInit()
    {
        nu.pattern.OpenCV.loadShared();
    }

    /*
    This should be called when trying to find the primary colors of the images
    Input: string directory
    output: Mat
     */
    public static OpenCVColorResults ClusterVideoCV(String directory)
    {
        /*
         *This is a defined to find how many kcluster colors we want to use
         */
        int k = 5;

        OpenCVColorResults ocvcr = new OpenCVColorResults();
        Mat frame = new Mat();
        VideoCapture camera = new VideoCapture(directory);

        //set the video size to 1056x864
        camera.set(3, 1056);
        camera.set(4, 864);

        /*
         * for each frame, populate the framedata with ColorData; It then adds the framesdata into the OpenCVColorResult
         */
        int j = 0;
        while(camera.read(frame))
        {
            FrameData framedata = new FrameData();
            cluster(frame, k, framedata, true);
            ocvcr.frames.add(framedata);

            System.out.println("Color frame: " + j);
            j++;
        }
        return ocvcr;
    }

    public static OpenCVColorResults ClusterQueryVideos(String directory)
    {
        int k = 5;

        OpenCVColorResults ocvcr = new OpenCVColorResults();
        Mat totalColors = new Mat();
//        Mat returnedColors = new Mat();
        Mat frame = new Mat();

        VideoCapture camera = new VideoCapture(directory);

        //set the video size to 1056x864
        camera.set(3, 1056);
        camera.set(4, 864);

        int j = 0;
        while(camera.read(frame))
        {
            if(j % 10 == 0)
            {
                totalColors.push_back(cluster(frame, k, null, false));
            }
            System.out.println(j++);
        }

//        System.out.println("Finished total colors pushback");
        Mat combinedTotalColors = new Mat();

        ArrayList<Mat> singleColorMat = new ArrayList();
        singleColorMat.add(totalColors.col(0));
        singleColorMat.add(totalColors.col(1));
        singleColorMat.add(totalColors.col(2));
        Core.merge(singleColorMat, combinedTotalColors);

        FrameData frameData = new FrameData();
//        System.out.println("CombinedTotalColors dump = " + combinedTotalColors.dump());
        cluster(combinedTotalColors, k, frameData, true);
        ocvcr.frames.add(frameData);
        return ocvcr;

//        System.out.println(returnedColors.dump());
//        return returnedColors;
    }

    /*
     * for each frame, populate the framedata with ColorData; It then adds the framesdata into the OpenCVColorResult
     * Input: Mat cutout, int k, FrameData, boolean
     * Input: Boolean populateFrameCluster determines if we want to find the percentage of colors used
     */
    public static Mat cluster(Mat cutout, int k, FrameData framedata, boolean populateFrameCluster) {

        Mat samples = cutout.reshape(1, cutout.cols() * cutout.rows());
        Mat samples32f = new Mat();
        samples.convertTo(samples32f, CvType.CV_32F, 1.0 / 255.0);

        Mat labels = new Mat();
        TermCriteria criteria = new TermCriteria(TermCriteria.COUNT, 100, 1);
        Mat centers = new Mat();
        Core.kmeans(samples32f, k, labels, criteria, 1, Core.KMEANS_PP_CENTERS, centers);

        centers.convertTo(centers, CvType.CV_8UC1, 255.0);
        centers.reshape(3);
//        System.out.println("This is before Centers dump");
//        System.out.println(centers.dump());

        if(populateFrameCluster)
        {
            countClusters(cutout, labels, centers, framedata);
        }

        return centers;
    }

    /*
     * Input: Mat cutout, Mat labels, FrameData, Mat Centers, FrameData
     * Results: framedata is updated with new color data based on k.
     */
    private static void countClusters (Mat cutout, Mat labels, Mat centers, FrameData frameData) {

        Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
        for (int i = 0; i < centers.rows(); i++) counts.put(i, 0);

        int rows = 0;
        int totalcutout = cutout.rows() * cutout.cols();
        for (int y = 0; y < cutout.rows(); y++) {
            for (int x = 0; x < cutout.cols(); x++) {
                int label = (int) labels.get(rows, 0)[0];
                counts.put(label, counts.get(label) + 1);
                rows++;
            }
        }

//        System.out.println("Counts: " + counts);

        for(int row = 0; row < centers.rows(); row++)
        {
//            System.out.println("R = " + (int)centers.get(row, 2)[0]);
//            System.out.println("G = " + (int)centers.get(row, 1)[0]);
//            System.out.println("B = " + (int)centers.get(row, 0)[0]);
            frameData.frameColors.add(
                new ColorData((int)centers.get(row, 2)[0], (int)centers.get(row, 1)[0], (int)centers.get(row, 0)[0], (float)counts.get(row)/totalcutout));
        }

    }

    // Processes the motion data of the given filepath to an .mp4
    public static OpenCVMotionResults MotionCV(String videoFilepath) {
        //load library
        nu.pattern.OpenCV.loadShared();

        Mat frame = new Mat();
        Mat firstFrame = new Mat();
        Mat gray = new Mat();
        Mat frameDelta = new Mat();
        Mat thresh = new Mat();

        /*
        Sample directory is "./database_videos/sports/sports.mp4"
         */
//        VideoCapture camera = new VideoCapture("./database_videos/sports/sports.mp4");
        VideoCapture camera = new VideoCapture(videoFilepath);

        //set the video size to 1056x864
        camera.set(3, 1056);
        camera.set(4, 864);

        camera.read(frame);
        
        //convert to grayscale and set the first frame
        Imgproc.cvtColor(frame, firstFrame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(firstFrame, firstFrame, new Size(21, 21), 0);

        OpenCVMotionResults finalResults = new OpenCVMotionResults();
        
        while(camera.read(frame)) {
            //convert to grayscale
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.GaussianBlur(gray, gray, new Size(21, 21), 0);

            //compute difference between first frame and current frame
            Core.absdiff(firstFrame, gray, frameDelta);
            Imgproc.threshold(frameDelta, thresh, 25, 255, Imgproc.THRESH_BINARY);

             // Write the difference (delta) frames out to a folder
//            Imgcodecs.imwrite("/Users/ivanchen/Desktop/delta/delta" + j++ + ".jpg", frameDelta);

            Imgproc.dilate(thresh, thresh, new Mat(), new Point(-1, -1), 2);
            
            List<MatOfPoint> cnts = new ArrayList<MatOfPoint>();
            Imgproc.findContours(thresh, cnts, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            // This resets the frame each time
            Imgproc.cvtColor(frame, firstFrame, Imgproc.COLOR_BGR2GRAY);
            Imgproc.GaussianBlur(firstFrame, firstFrame, new Size(21, 21), 0);

            // Calculate the total current motion in the current frame
            double currentFrameMotionAmount = 0;
            for (int i = 0; i < cnts.size(); i++)
            {
                currentFrameMotionAmount += Imgproc.contourArea(cnts.get(i));
            }
            
            // Add this amount to the finalResults
            finalResults.frameMotion.add(currentFrameMotionAmount);
            finalResults.totalMotion += currentFrameMotionAmount;
        }
        
        finalResults.averageMotion = finalResults.totalMotion / (double) finalResults.frameMotion.size();

        // Uncomment the following to print out the motion results
//        for (int i = 0; i < finalResults.frameMotion.size(); i++)
//        {
//            System.out.println("Frame " + i + ": " + finalResults.frameMotion.get(i));
//        }
//        System.out.println("Total motion: " + finalResults.totalMotion);
//        System.out.println("Average motion: " + finalResults.averageMotion);

        return finalResults;
    }


}

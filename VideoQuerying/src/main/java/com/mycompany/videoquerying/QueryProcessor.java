package com.mycompany.videoquerying;

import static com.mycompany.videoquerying.GcloudVideoIntel.analyzeLabels;
import static com.mycompany.videoquerying.OpenCVIntel.*;
import static com.mycompany.videoquerying.VideoEncoder.encodeMp4;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map.Entry;

/**
 * A wrapper class for handling all video query processing.
 * @author stermark
 */
public class QueryProcessor {

    private static ColorSearch searchClass;
    
    // Returns a sorted ArrayList of the best database matches for the current query
    public static ArrayList<MatchResult> findDatabaseMatch(VideoAnalysisResults queryResults,
                                                            String databaseDirectory,
                                                            boolean useObjectDescriptor, 
                                                            boolean useColorDescriptor, 
                                                            boolean useMotionDescriptor)
    {
        System.out.println("Searching database for similar videos...");
        
        // Initialize the match results arraylist
        ArrayList<MatchResult> results = new ArrayList();
        
        // Intialize descriptor weight parameters
        int numDescriptorsUsed = 0;
        if (useObjectDescriptor) numDescriptorsUsed++;
        if (useColorDescriptor) numDescriptorsUsed++;
        if (useMotionDescriptor) numDescriptorsUsed++;
        
        /**********************************************************************/        
        /*   Load in the pre-processed database meta files for comparison
        /**********************************************************************/
        // Get the names of the videos in the database and read their meta files
        File[] directories = new File(databaseDirectory).listFiles(File::isDirectory);
        ArrayList<VideoAnalysisResults> databaseVideoMeta = new ArrayList();
        for (int i = 0; i < directories.length; i++)
        {
            VideoAnalysisResults nextResults = readDatabaseMetadataFile(directories[i].getAbsolutePath());
            if (nextResults != null)
            {
                databaseVideoMeta.add(nextResults);
                System.out.println("Read in meta file for database video: " + directories[i].getName());
            }
            else
            {
                System.out.println("No meta file found for database video: " + directories[i].getName());
            }
        }
        
        System.out.println("Finished reading database meta files.");
        
        // foreach video that has a meta file in the database
        for (int i = 0; i < databaseVideoMeta.size(); i++)
        {
            double timePerFrame = 1.0 / 30.0;
            
            /**********************************************************************/        
            /*                  OBJECT RECOGNITION DESCRIPTOR
            /**********************************************************************/
            double overallObjectScore = 0;
            double[] objectFrameScore = new double[600];
            if (useObjectDescriptor && queryResults.objectResults != null)
            {   
                System.out.println("Processing object results...");
                
                // Get the GCloud object results for the current database video
                GCloudResults databaseObjectResults = databaseVideoMeta.get(i).objectResults;
                
                // Keep track of how many labels are considered when matching
                int numLabelsUsed = 0;
                
                // for each frame in the database video
                for (int frame = 0; frame < 600; frame++)
                {   
                    int numFrameLabels = 0;
                    
                    // for each video label in the query video
                    for (Entry<String, VideoLabelData> queryEntry : queryResults.objectResults.videoLabels.entrySet())
                    {
                        // Calculate the start and end time of the current frame
                        double frameStartTime = frame * timePerFrame;
                        double frameEndTime = frameStartTime + timePerFrame;
                        
                        // check if query video label is present in the current frame (using shot labels)
                        if (databaseObjectResults.shotLabels.containsKey(queryEntry.getKey()))                               
                        {
                            for (int seg = 0; seg < databaseObjectResults.shotLabels.get(queryEntry.getKey()).segments.size(); seg++)
                            {
                                if (frameEndTime > databaseObjectResults.shotLabels.get(queryEntry.getKey()).segments.get(seg).startTime  
                                     && frameStartTime < databaseObjectResults.shotLabels.get(queryEntry.getKey()).segments.get(seg).endTime)
                                {
                                    objectFrameScore[frame] += queryEntry.getValue().segmentData.confidence * 
                                                databaseObjectResults.shotLabels.get(queryEntry.getKey()).segments.get(seg).confidence;
                                    
                                     numLabelsUsed++;
                                     numFrameLabels++;
                                }
                            }
                        }
                    }
                    
                    overallObjectScore += objectFrameScore[frame];
                    
                    if (numFrameLabels > 0)
                        objectFrameScore[frame] /= numFrameLabels;
                }
                
                // Calculate the overall object score as a percentage
                if (numLabelsUsed > 0)
                {
                    overallObjectScore = (overallObjectScore / (double) numLabelsUsed);
                }             
            }
            
            /**********************************************************************/        
            /*                      DOMINANT COLOR DESCRIPTOR
            /**********************************************************************/
            double overallColorScore = 0;
            double[] colorFrameScore = new double[600];
            if (useColorDescriptor && queryResults.colorResults != null)
            {
                System.out.println("Processing color results...");

                overallColorScore = queryResults.colorResults.resultMap.get(directories[i].getName());

                for (int frame = 0; frame < 600; frame++)
                {
                    colorFrameScore[frame] = queryResults.colorResults.resultScoreMap.get(directories[i].getName())[frame];
                }
            }
            
            /**********************************************************************/        
            /*                          MOTION DESCRIPTOR
            /**********************************************************************/
            double overallMotionScore = 0;
			double[] motionFrameScore = new double[600];

			if (useMotionDescriptor && queryResults.motionResults != null)
			{
				System.out.println("Processing motion results...");

				// Get the GCloud object results for the current database video
				OpenCVMotionResults databaseMotionResults = databaseVideoMeta.get(i).motionResults;

				// Calculate the area of the frame
				double frameArea = 1056 * 864;

				// for each frame in the current database, find a score based on the average motion in the query video clip
				for (int frame = 0; frame < databaseMotionResults.frameMotion.size(); frame++)
				{
					double absDiff = Math.abs(queryResults.motionResults.averageMotion - databaseMotionResults.frameMotion.get(frame));
					motionFrameScore[frame] = (1 - (absDiff / frameArea));
				}

				// Calculate the overall video motion match score
				// Since the query video is shorter, apply a sliding window on database videos to compare motion
				int queryResultsFrameCount = queryResults.motionResults.frameMotion.size();
				double totalMotion = 0;
				double minDiff = Double.MAX_VALUE;

				// For each frame in the current database, calculate the average motion of the last (150) frames
				for (int frame = 0; frame < databaseMotionResults.frameMotion.size(); frame++)
				{
					totalMotion +=  databaseMotionResults.frameMotion.get(frame);

					// Are we far into the video enough to calculate the window average?
					if (frame >= queryResultsFrameCount)
					{
						double averageMotion = totalMotion / queryResultsFrameCount;
						double diff = Math.abs(queryResults.motionResults.averageMotion - averageMotion);

						if (diff < minDiff)
						{
							minDiff = diff;
						}

						totalMotion -= databaseMotionResults.frameMotion.get(frame - queryResultsFrameCount);
					}
				}

				overallMotionScore = (1 - (minDiff / frameArea));
			}
            
            /**********************************************************************/        
            /*        CREATE MATCH RESULT FOR THIS QUERY/DATABASE VIDEO PAIR
            /**********************************************************************/
            // Combine all the scores and weight them evenly based on how many descriptors were used
            System.out.println("Score Color - " + overallColorScore);
            System.out.println(" Score Object - " + overallObjectScore);
            System.out.println(" Motion Object - " + overallMotionScore);
            double finalScore = overallObjectScore + overallColorScore + overallMotionScore;
            if (numDescriptorsUsed > 0)
            {
                finalScore = finalScore / (double) numDescriptorsUsed;
            }
            
            double[] finalFrameScores = new double[600];
            for (int j = 0; j < finalFrameScores.length; j++)
            {
                finalFrameScores[j] = objectFrameScore[j] + colorFrameScore[j] + motionFrameScore[j];
                if (numDescriptorsUsed > 0)
                {
                    finalFrameScores[j] = finalFrameScores[j] / (double) numDescriptorsUsed;
                }
            }
            
            // Create a new MatchResult and add it to the results to return after all searching is finished.
            MatchResult newResult = new MatchResult(queryResults.filename, databaseVideoMeta.get(i).filename, finalScore, finalFrameScores);
            results.add(newResult);
        }

        // Sort the list of files from highest to lowest score before returning
        Collections.sort(results);
        
        return results;
    }

    private static double scoreColor(FrameData query, FrameData db)
    {
        double mag = magnitude (new ColorData(255, 255, 255, 0));
        double score = 0;
        for(int dIt = 0; dIt < db.frameColors.size(); dIt++)
        {
            for(int qIt = 0; qIt < query.frameColors.size(); qIt++)
            {
                score += ((distance(query.frameColors.get(qIt), db.frameColors.get(dIt)) / mag)
                            * db.frameColors.get(dIt).percentage
                            * query.frameColors.get(qIt).percentage);
            }
        }
        return score;
    }

    private static double distance(ColorData c1, ColorData c2)
    {
        return Math.sqrt(Math.pow((c1.r - c2.r), 2) + Math.pow((c1.g - c2.g), 2) + Math.pow((c1.b - c2.b), 2));
    }

    private static double magnitude(ColorData c)
    {
        return Math.sqrt(Math.pow(c.r, 2) + Math.pow(c.g, 2) + Math.pow(c.b, 2));
    }

    // Performs analysis of the frames at the given filename
    public static ColorResults processColor(String filename, boolean isQuery)
    {
        ColorResults colorResults = new ColorResults();
        if (isQuery)
        {
            colorResults.resultMap = searchClass.search(filename);
            colorResults.frameMap = searchClass.frameMap;
            colorResults.resultScoreMap = searchClass.resultsScoreMap;
        }
        else
        {

        }
        return colorResults;
    }

    // Performs analysis of the query video using Google Cloud object recognition
    public static GCloudResults processGoogleCloudObjects(String filePath)
    {
        try
        {
            System.out.println("Calling the cloud analysis.");
            GCloudResults results = analyzeLabels(filePath);
            return results;
        }
        catch (Exception e)
        {
            System.out.println("Error when calling Google Cloud's analyzeLabels()");
            e.printStackTrace();
        }
        return null;
    }

    // Performs analysis of the frames at the given filepath using OpenCV
    public static OpenCVColorResults processOpenCVColor(String filepath, boolean isQuery)
    {
        OpenCVColorResults opcv = new OpenCVColorResults();
        if (isQuery)
        {
             opcv =  ClusterQueryVideos(filepath);
        }
        else
        {
            opcv = ClusterVideoCV(filepath);
        }

        return opcv;
    }

    // Performs analysis of the frames at the given filepath using OpenCV
    public static OpenCVMotionResults processOpenCVMotion(String filepath)
    {
        return MotionCV(filepath);
    }
    
    // Reads in the metadata in the provided file directory.
    public static VideoAnalysisResults readDatabaseMetadataFile(String videoDirectory)
    {
        // Ensure that the directory to read from exists
        File videoFileDirectory = new File (videoDirectory);
        if (!videoFileDirectory.exists())
        {
            System.out.println("Video directory not found. Please enter a valid video location.");
            return null;
        }
        
        String metadataFilepath = videoFileDirectory.getAbsolutePath() + "/" + videoFileDirectory.getName() + ".meta";
        System.out.println(metadataFilepath);
        
        VideoAnalysisResults metadata = null;
        
        // Deserialization
        try
        {   
            // Reading the object from a file
            FileInputStream file = new FileInputStream(metadataFilepath);
            ObjectInputStream in = new ObjectInputStream(file);
             
            // Method for deserialization of object
            metadata = (VideoAnalysisResults) in.readObject();
             
            in.close();
            file.close();
        }
         
        catch(IOException ex)
        {
            System.out.println("IOException is caught. Unable to read metadata." + ex.getLocalizedMessage());
        }
         
        catch(ClassNotFoundException ex)
        {
            System.out.println("ClassNotFoundException is caught. Unable to read metadata.");
        }
        
        return metadata;
    }
    
    // Used to initialize metadata file for a video in the database.
    public static void writeDatabaseMetadataFile(VideoAnalysisResults results, String videoDirectory)
    {
        // Ensure that the directory to write to exists
        File videoFileDirectory = new File (videoDirectory);
        if (!videoFileDirectory.exists())
        {
            System.out.println("Video directory not found. Please enter a valid video location.");
            return;
        }
        
        String videoFilepath = videoFileDirectory.getAbsolutePath() + "/" + results.filename + ".meta";
        System.out.println(videoFilepath);
        
        // Serialize and write out the object
        try
        {   
            //Saving of object in a file
            FileOutputStream file = new FileOutputStream(videoFilepath);
            ObjectOutputStream out = new ObjectOutputStream(file);
             
            // Method for serialization of object
            out.writeObject(results);
             
            out.close();
            file.close();
             
            System.out.println("Object has been serialized.");
 
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
            System.out.println("IOException is caught. Unable to write out results object.");
        }
    }
    
    // Creates a full metadata file for each of the videos in the database.
    // NOTE: Should not be used in production code as all videos should be pre-processed offline.
    public static void processAllDatabaseVideos(String databaseDirectory)
    {
        // for each video in the database
        File[] directories = new File(databaseDirectory).listFiles(File::isDirectory);

        InitSearchClass();
        //ProcessDBVideos(directories);

        System.out.println("Finished processing all videos in the database.");
    }

    private static void InitSearchClass()
    {
        searchClass = new ColorSearch();

        try {
            searchClass.init();
        } catch (IOException e) {
            System.out.println("Search Class Initialize Error - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void ProcessDBVideos(File[] directories)
    {
        for (int i = 0; i < directories.length; i++)
        {
            // Get video directory
            String videoDirectory = directories[i].getAbsolutePath();

            System.out.println("Encoding DB Video: " + videoDirectory);

            // Encode the video (to get .png's and .mp4)
            //encodeMp4(videoDirectory);

            // Get the newly created .mp4 video filepath
            String databaseVideoFilepath = directories[i].getAbsolutePath() + "/" + directories[i].getName() + ".mp4";

            // Setup VideoAnalysisResults for the current video
            VideoAnalysisResults dbVideoResults = new VideoAnalysisResults();

            // Get video name
            dbVideoResults.filename = directories[i].getName();

            System.out.println("Processing DB Video: " + videoDirectory);
            // Process objects
            dbVideoResults.objectResults = processGoogleCloudObjects(databaseVideoFilepath);
            // Process color (Not in used anymore)
            dbVideoResults.OpenCVColorResults = processOpenCVColor(databaseVideoFilepath, false);
            // Process motion
            dbVideoResults.motionResults = processOpenCVMotion(databaseVideoFilepath);

            // Write out VideoAnalysisResults
            writeDatabaseMetadataFile(dbVideoResults, videoDirectory);

            // Report success or failure
            System.out.println("Finished writing " + dbVideoResults.filename + ".meta");
        }
    }
}

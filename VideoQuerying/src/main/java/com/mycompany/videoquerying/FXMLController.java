package com.mycompany.videoquerying;

import static com.mycompany.videoquerying.OpenCVIntel.*;
import static com.mycompany.videoquerying.QueryProcessor.findDatabaseMatch;
import static com.mycompany.videoquerying.QueryProcessor.processGoogleCloudObjects;
import static com.mycompany.videoquerying.QueryProcessor.processOpenCVColor;
import static com.mycompany.videoquerying.QueryProcessor.processOpenCVMotion;
import static com.mycompany.videoquerying.QueryProcessor.processAllDatabaseVideos;

import static com.mycompany.videoquerying.GcloudVideoIntel.analyzeLabels;
import static com.mycompany.videoquerying.GcloudVideoIntel.analyzeLabelsFromCloud;
import static com.mycompany.videoquerying.GcloudVideoIntel.uploadFiles;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

public class FXMLController implements Initializable {
    
    /* Video Querying Variables */
    private VideoEncoder encoder;
    
    private Duration databaseVideoDuration;
    private Duration queryVideoDuration;
    
    private final int FRAME_RATE = 30;
    
    private final String DATABASE_DIR = "./database_videos/";
    
    private HashMap<String, MediaPlayer> loadedVideos;
    
    private boolean useObjectDescriptor = false;
    private boolean useColorDescriptor = false;
    private boolean useMotionDescriptor = false;
    
    private ArrayList<MatchResult> currentMatches = new ArrayList();
    private String currentDatabaseVideoName = "";
    
    /* FXML Variables */
    @FXML
    private MediaView mvDatabaseVideo;
    @FXML
    private MediaView mvQueryVideo;
    @FXML
    private TextField txtQueryVideo;
    @FXML
    private ListView lstviewResultsList;
    @FXML
    private Label lblQueryStatus;
    @FXML
    private ToggleGroup descriptorGroup;
    @FXML
    private Slider querySlider;
    @FXML
    private Slider databaseSlider;
    @FXML
    private CheckBox chkColor;
    @FXML
    private CheckBox chkMotion;
    @FXML
    private CheckBox chkObjects;    
    @FXML
    private AreaChart chtVisualMatch;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Set up some default form values
        txtQueryVideo.setText("./query_videos/first");

        // Initialize other variables
        encoder = new VideoEncoder();
        
        // Initialize the MediaPlayer HashMap
        loadedVideos = new HashMap<>();

        // Add listener to the database list view
        initializeDatabaseListView();
        
        // Initialize OpenCV
        CVInit();
              
        // OPTIONAL: Re-process all database video meta files
//        processAllDatabaseVideos(DATABASE_DIR);
        
    } 
    
    @FXML
    private void handleSearchAction(ActionEvent event) {        
        /**********************************************************************/        
        /*   Get the query video input and ensure that the files exist
        /**********************************************************************/
        String queryDirectory = txtQueryVideo.getText();
        File queryFileDirectory = new File (queryDirectory);
        if (!queryFileDirectory.exists())
        {
            lblQueryStatus.setText("Directory not found. Please enter a valid query location.");
            return;
        }
        lblQueryStatus.setText("Processing query...");
        
        /**********************************************************************/
        /* Set the descriptor processing flags based on checkbox selections
        /**********************************************************************/
        useColorDescriptor = chkColor.isSelected();
        useMotionDescriptor = chkMotion.isSelected();
        useObjectDescriptor = chkObjects.isSelected();
        
        /**********************************************************************/
        /*       Write query video frames to pngs and then encode to mp4
        /**********************************************************************/
        System.out.println("Query Status: Processing query video...");
        encoder.encodeMp4(queryDirectory);
        
        /**********************************************************************/        
        /*                  Load the query video into the GUI
        /**********************************************************************/
        String queryVideoFilepath = queryFileDirectory.getAbsolutePath() + "/" + queryFileDirectory.getName() + ".mp4";
        System.out.println(queryVideoFilepath);
        loadQueryVideo(queryVideoFilepath);

        /**********************************************************************/
        /*                   Perform descriptor analysis
        /**********************************************************************/
        System.out.println("Query Status: Processing video descriptors...");
        
        // Create the container for the query results
        VideoAnalysisResults queryResults = new VideoAnalysisResults();
        queryResults.filename = queryFileDirectory.getName();
        
        // Google Cloud object detection
        queryResults.objectResults = processGoogleCloudObjects(queryVideoFilepath);            
        // Opencv color
        queryResults.colorResults = processOpenCVColor(queryVideoFilepath, true);
        // Opencv motion
        queryResults.motionResults = processOpenCVMotion(queryVideoFilepath);
        
        // Print out the filename, color, and motion results (the object results are already printed during processing)
        printAdditionalVideoAnalysisResultsData(queryResults);

        /**********************************************************************/
        /* Calculate scores and rank the videos based on descriptor selection
        /**********************************************************************/
        System.out.println("Query Status: Finding closest matches among database videos...");
        ArrayList<MatchResult> matches = findDatabaseMatch(queryResults, DATABASE_DIR, useObjectDescriptor, useColorDescriptor, useMotionDescriptor);
        currentMatches = matches;
        updateListView(currentMatches);
        
        /**********************************************************************/
        /*  Display select the top view in the list view and the line graph
        /**********************************************************************/
        if (!currentMatches.isEmpty())
        {
            // Select the first video as a default to show after the results are found
            lstviewResultsList.getSelectionModel().select(0);
        } 
        
        lblQueryStatus.setText("Finished processing query.");
        System.out.println("Finished processing query.");
    }
    
    /**
     * Load in the h.264 encoded .mp4 at the given filepath into the given MediaView.
     * @param filepath - File path to the h.264 encoded .mp4 video.
     * @param viewer - The MediaViwer in the GUI that this generated MediaPlayer will be attached to.
     * @return 
     */
    private MediaPlayer loadVideo(String filepath, MediaView viewer)
    {        
        // Load in an mp4 video file to the MediaPlayer
        File file = new File(filepath);
        Media media = new Media(file.toURI().toString());
        
        // Create the media player with this media in it
        MediaPlayer player = new MediaPlayer(media);
        
        // Add the media player into the viewer
        viewer.setMediaPlayer(player);

        // Change width and height to fit video
        viewer.setPreserveRatio(true);

        return player;
    }
    
    // Loads in the given query video and updates the GUI accordingly
    private MediaPlayer loadQueryVideo(String filepath)
    {
        MediaPlayer player = loadVideo(filepath, mvQueryVideo);
        
        // Initalize some values once the media player is ready
        mvQueryVideo.getMediaPlayer().setOnReady(new Runnable() 
        {
            @Override
            public void run() 
            {
                queryVideoDuration = mvQueryVideo.getMediaPlayer().getMedia().getDuration();
                querySlider.setMax(100);
                updateSliderValue(mvQueryVideo.getMediaPlayer(), querySlider, queryVideoDuration);
            }
        });
        
        // When stopping the player, set it's time back to 0 and add a listener to update the slider values again.
        mvQueryVideo.getMediaPlayer().setOnStopped(new Runnable() 
        {
            @Override
            public void run() 
            {
                mvQueryVideo.getMediaPlayer().seek(Duration.ZERO);
                mvQueryVideo.getMediaPlayer().currentTimeProperty().addListener(new InvalidationListener()
                {
                    public void invalidated(javafx.beans.Observable ov)
                    {
                        updateSliderValue(mvQueryVideo.getMediaPlayer(), querySlider, queryVideoDuration);
                    }
                });
            }
        });
        
        // Reset the mediaplayer once it is done playing
        mvQueryVideo.getMediaPlayer().setOnEndOfMedia(new Runnable() 
        {
            @Override
            public void run() 
            {
                mvQueryVideo.getMediaPlayer().seek(Duration.ZERO);
                mvQueryVideo.getMediaPlayer().currentTimeProperty().addListener(new InvalidationListener()
                {
                    public void invalidated(javafx.beans.Observable ov)
                    {
                        updateSliderValue(mvQueryVideo.getMediaPlayer(), querySlider, queryVideoDuration);
                    }
                });
            }
        });
        
        // Set up listeners so that the slider bar reflects the current values of the video being displayed
        mvQueryVideo.getMediaPlayer().currentTimeProperty().addListener(new InvalidationListener()
        {
            public void invalidated(javafx.beans.Observable ov)
            {
                updateSliderValue(mvQueryVideo.getMediaPlayer(), querySlider, queryVideoDuration);
            }
        });
        
        // Enables very rough scrubbing (as long as the video is paused/playing and not stopped).
        querySlider.valueProperty().addListener(new InvalidationListener()
        {
            public void invalidated(javafx.beans.Observable ov)
            {
                if (querySlider.isValueChanging())
                {
                    // multiply duration by percentage calculated by slider position
                    mvQueryVideo.getMediaPlayer().seek(queryVideoDuration.multiply(querySlider.getValue() / 100.0));
                }
            }
        });
        
        return player;
    }
    
    // Loads in the given database video and updates GUI values accordingly
    private MediaPlayer loadDatabaseVideo(String filepath)
    {
        MediaPlayer player = loadVideo(filepath, mvDatabaseVideo);
        
        // Initalize some values once the media player is ready
        mvDatabaseVideo.getMediaPlayer().setOnReady(new Runnable() 
        {
            @Override
            public void run() 
            {
                databaseVideoDuration = mvDatabaseVideo.getMediaPlayer().getMedia().getDuration();
                databaseSlider.setMax(100);
                updateSliderValue(mvDatabaseVideo.getMediaPlayer(), databaseSlider, databaseVideoDuration);
            }
        });
        
        // When stopping the player, set it's time back to 0 and add a listener to update the slider values again.
        mvDatabaseVideo.getMediaPlayer().setOnStopped(new Runnable() 
        {
            @Override
            public void run() 
            {
                mvDatabaseVideo.getMediaPlayer().seek(Duration.ZERO);
                mvDatabaseVideo.getMediaPlayer().currentTimeProperty().addListener(new InvalidationListener()
                {
                    public void invalidated(javafx.beans.Observable ov)
                    {
                        updateSliderValue(mvDatabaseVideo.getMediaPlayer(), databaseSlider, databaseVideoDuration);
                    }
                });
            }
        });
        
        // Reset the mediaplayer once it is done playing
        mvDatabaseVideo.getMediaPlayer().setOnEndOfMedia(new Runnable() 
        {
            @Override
            public void run() 
            {
                mvDatabaseVideo.getMediaPlayer().seek(Duration.ZERO);
                mvDatabaseVideo.getMediaPlayer().currentTimeProperty().addListener(new InvalidationListener()
                {
                    public void invalidated(javafx.beans.Observable ov)
                    {
                        updateSliderValue(mvDatabaseVideo.getMediaPlayer(), databaseSlider, databaseVideoDuration);
                    }
                });
            }
        });
        
        // Set up listeners so that the slider bar reflects the current values of the video being displayed
        mvDatabaseVideo.getMediaPlayer().currentTimeProperty().addListener(new InvalidationListener()
        {
            public void invalidated(javafx.beans.Observable ov)
            {
                updateSliderValue(mvDatabaseVideo.getMediaPlayer(), databaseSlider, databaseVideoDuration);
            }
        });
        
        // Enables very rough scrubbing (as long as the video is paused/playing and not stopped).
        databaseSlider.valueProperty().addListener(new InvalidationListener()
        {
            public void invalidated(javafx.beans.Observable ov)
            {
                if (databaseSlider.isValueChanging())
                {
                    // multiply duration by percentage calculated by slider position
                    mvDatabaseVideo.getMediaPlayer().seek(databaseVideoDuration.multiply(databaseSlider.getValue() / 100.0));
                }
            }
        });
        
        return player;
    }

    // Updates the slider values so that the current video position is reflected in the slider's current value
    protected void updateSliderValue(final MediaPlayer player, final Slider slider, final Duration duration) 
    {
        if (slider != null) 
        {
            Platform.runLater(new Runnable() 
            {
                public void run() 
                {
                    Duration currentTime = player.getCurrentTime();
                    slider.setDisable(duration.isUnknown());
                    if (!slider.isDisabled() && duration.greaterThan(Duration.ZERO) && !slider.isValueChanging()) 
                    {
                        slider.setValue(currentTime.divide(duration).toMillis() * 100.0);
                    }
                }
            });
        }
    }
    
    @FXML
    private void playDatabaseVideo()
    {
        if (mvDatabaseVideo.getMediaPlayer() != null)
        {
            mvDatabaseVideo.getMediaPlayer().play();
        }
    }
    
    @FXML
    private void pauseDatabaseVideo()
    {
        if (mvDatabaseVideo.getMediaPlayer() != null)
        {
            mvDatabaseVideo.getMediaPlayer().pause();
        }
    }
    
    @FXML
    private void stopDatabaseVideo()
    {
        if (mvDatabaseVideo.getMediaPlayer() != null)
        {
            mvDatabaseVideo.getMediaPlayer().stop();
        }
    }
    
    @FXML
    private void playQueryVideo()
    {
        if (mvQueryVideo.getMediaPlayer() != null)
        {
            mvQueryVideo.getMediaPlayer().play();
        }
    }
    
    @FXML
    private void pauseQueryVideo()
    {
        if (mvQueryVideo.getMediaPlayer() != null)
        {
            mvQueryVideo.getMediaPlayer().pause();
        }
    }
    
    @FXML
    private void stopQueryVideo()
    {
        if (mvQueryVideo.getMediaPlayer() != null)
        {
            mvQueryVideo.getMediaPlayer().stop();
        }
    }
    
    @FXML
    private void selectColorCheckBox(ActionEvent event)
    {
        chkColor.setSelected(true);
    }
    
    @FXML
    private void selectMotionCheckBox(ActionEvent event)
    {
        chkMotion.setSelected(true);
    }
    
    @FXML
    private void selectObjectCheckBox(ActionEvent event)
    {
        chkObjects.setSelected(true);
    }
    
    // Adds a set of items to a ListView GUI item.
    private void initializeDatabaseListView()
    {
        // Add a listener to results list view to handle selection changes and load the new video the user selected.
        lstviewResultsList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() 
        {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) 
            {   
                if (newValue != null)
                {
                    // Extract the name of the new database video
                    String[] ss = newValue.split("\\|");
                    String newVideoString = ss[0].trim();
                    System.out.println(newVideoString + ".");

                    // Load the match result results into the visual indicator graph
                    for (int i = 0; i < currentMatches.size(); i++)
                    {
                        if (Objects.equals(currentMatches.get(i).databaseFileName, newVideoString))
                        {
                            updateLineChart(currentMatches.get(i).frameScores);
                        }
                    }

                    // Load database video into player in the GUI
                    if (loadedVideos.containsKey(newVideoString))
                    {
                        // Stop the previous video
                        loadedVideos.get(currentDatabaseVideoName).stop();

                        // Load in the new video that the user selected
                        mvDatabaseVideo.setMediaPlayer(loadedVideos.get(newVideoString));
                    }
                    else
                    {
                        // Load in the video and add it to the loaded videos hashmap
                        MediaPlayer newPlayer = loadDatabaseVideo(DATABASE_DIR + newVideoString + "/" + newVideoString + ".mp4");
                        loadedVideos.put(newVideoString, newPlayer);
                    }

                    // Mark the new video name as the current video name
                    currentDatabaseVideoName = newVideoString;
                }
            }
        });
    }
    
    // Adds the new frame score data to the line chart in the GUI.
    private void updateLineChart(double[] frameScores)
    {
        final XYChart.Series newSeries = new XYChart.Series();
        
        for (int i = 0; i < frameScores.length; i++)
        {
            newSeries.getData().add(new XYChart.Data(i, frameScores[i]));
        }

        // Get rid of any old data before adding the new data
         chtVisualMatch.getData().clear(); 
         chtVisualMatch.getData().add(newSeries);
    }
    
    // Updates the list view in the GUI with results of the given matches.
    // Note: The given ArrayList is assumed to be sorted in descending order already.
    private void updateListView(ArrayList<MatchResult> matches)
    {
        ArrayList<String> directoryNames = new ArrayList();
        for (int i = 0; i < matches.size(); i++)
        {
            String displayString = matches.get(i).databaseFileName;
            displayString += " | ";
            displayString += String.format("%,.3f", matches.get(i).matchScore);
            directoryNames.add(displayString);
        }
        lstviewResultsList.getItems().clear();
        lstviewResultsList.getItems().addAll(directoryNames);
    }
    
    public void printAdditionalVideoAnalysisResultsData(VideoAnalysisResults results)
    {
        System.out.println("VideoAnalysisResults for " + results.filename);
        System.out.println("***** Color Results *****");
        for (int i = 0; i < results.colorResults.frames.size(); i++)
        {
            for (int j = 0; j < results.colorResults.frames.get(i).frameColors.size(); j++)
            {
                ColorData c = results.colorResults.frames.get(i).frameColors.get(j);
                System.out.println("color: (" + c.r + ", " + c.g + ", " + c.b + ")" + " | percentage: " + c.percentage);
            }
        }
        System.out.println("***************");
       
        System.out.println("***** Motion Results *****");
        System.out.println("Average motion: " + results.motionResults.averageMotion);
        System.out.println("Total motion: " + results.motionResults.totalMotion);
        System.out.println("***************");
    }
}

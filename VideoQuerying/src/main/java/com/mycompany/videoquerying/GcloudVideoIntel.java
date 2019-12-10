package com.mycompany.videoquerying;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.videointelligence.v1.AnnotateVideoProgress;
import com.google.cloud.videointelligence.v1.AnnotateVideoRequest;
import com.google.cloud.videointelligence.v1.AnnotateVideoResponse;
import com.google.cloud.videointelligence.v1.Entity;
import com.google.cloud.videointelligence.v1.ExplicitContentFrame;
import com.google.cloud.videointelligence.v1.Feature;
import com.google.cloud.videointelligence.v1.LabelAnnotation;
import com.google.cloud.videointelligence.v1.LabelSegment;
import com.google.cloud.videointelligence.v1.VideoAnnotationResults;
import com.google.cloud.videointelligence.v1.VideoIntelligenceServiceClient;
import com.google.cloud.videointelligence.v1.VideoSegment;
import com.google.protobuf.ByteString;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.auth.oauth2.GoogleCredentials;
import io.grpc.Context;
import org.apache.commons.codec.binary.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.StorageOptions;
import java.io.File;
import java.util.List;


/**
 * Created by ivanchen on 4/13/18.
 */
public class GcloudVideoIntel {

    // Uploads the .mp4 video at the given file path and returns the object results.
    public static GCloudResults analyzeLabels(String filePath){
        
        // Instantiate a com.google.cloud.videointelligence.v1.VideoIntelligenceServiceClient
        try (VideoIntelligenceServiceClient client = VideoIntelligenceServiceClient.create()) {
            // Read file and encode into bytes
            Path path = Paths.get(filePath);
            byte[] data = Files.readAllBytes(path);

            AnnotateVideoRequest request = AnnotateVideoRequest.newBuilder()
                    .setInputContent(ByteString.copyFrom(data))
                    .addFeatures(Feature.LABEL_DETECTION)
                    .build();

            // Create an operation that will contain the response when the operation completes.
            OperationFuture<AnnotateVideoResponse, AnnotateVideoProgress> response =
                    client.annotateVideoAsync(request);

            // Read the results of the request in a GCloudResults struct
            GCloudResults finalResults = new GCloudResults();
            
            
            System.out.println("Waiting for operation to complete...");
            List<VideoAnnotationResults> responseResults = response.get().getAnnotationResultsList();
            
            // For each video response
            for (int i = 0; i < responseResults.size(); i++)
            {
                // For each video label
                for (int videoLabelIndex = 0; videoLabelIndex < responseResults.get(i).getSegmentLabelAnnotationsList().size(); videoLabelIndex++)
                {
                    // Get the raw video label data
                    LabelAnnotation videoLabelAnnotation = responseResults.get(i).getSegmentLabelAnnotations(videoLabelIndex);
                    
                    // Set up the new video label data structure
                    VideoLabelData videoLabel = new VideoLabelData();
                    
                    // Extract video label information
                    videoLabel.videoLabel = videoLabelAnnotation.getEntity().getDescription();
                    
                    // For each video label category
                    for (Entity categoryEntity : videoLabelAnnotation.getCategoryEntitiesList())
                    {
                        videoLabel.videoLabelCategories.add(categoryEntity.getDescription());
                    }
                    
                    // For each video label segment (includes start time, end time, and confidence)
                    for (LabelSegment segment : videoLabelAnnotation.getSegmentsList())
                    {
                        videoLabel.segmentData.startTime = segment.getSegment().getStartTimeOffset().getSeconds()
                                + segment.getSegment().getStartTimeOffset().getNanos() / 1e9;
                        
                        videoLabel.segmentData.endTime = segment.getSegment().getEndTimeOffset().getSeconds()
                                + segment.getSegment().getEndTimeOffset().getNanos() / 1e9;
                        
                        videoLabel.segmentData.confidence = segment.getConfidence();   
                    }
                    
                    // Add the final video label into the list of videoLabels to be returned
                    finalResults.videoLabels.put(videoLabel.videoLabel, videoLabel); // Note: Duplicate videoLabel key values are possible
                }
                
                // For each shot label
                for (int shotLabelIndex = 0; shotLabelIndex < responseResults.get(i).getShotLabelAnnotationsList().size(); shotLabelIndex++)
                {
                    // Get the raw shot label data
                    LabelAnnotation shotLabelAnnotation = responseResults.get(i).getShotLabelAnnotationsList().get(shotLabelIndex);
                    
                    // Set up the new shot label data structure
                    ShotLabelData shotLabel = new ShotLabelData();
                    
                    // Extract shot label information
                    shotLabel.shotLabel = shotLabelAnnotation.getEntity().getDescription();
                    
                    // For each shot label category
                    for (Entity categoryEntity : shotLabelAnnotation.getCategoryEntitiesList()) 
                    {
                        shotLabel.shotLabelCategories.add(categoryEntity.getDescription());
                    }
                                        
                    // For each shot label segment (includes start time, end time, and confidence)
                    for (LabelSegment segment : shotLabelAnnotation.getSegmentsList()) 
                    {
                        SegmentData newSegmentData = new SegmentData();
                        
                        newSegmentData.startTime = segment.getSegment().getStartTimeOffset().getSeconds()
                                + segment.getSegment().getStartTimeOffset().getNanos() / 1e9;
                        
                        newSegmentData.endTime = segment.getSegment().getEndTimeOffset().getSeconds()
                                + segment.getSegment().getEndTimeOffset().getNanos() / 1e9;
                        
                        newSegmentData.confidence = segment.getConfidence();
                        
                        shotLabel.segments.add(newSegmentData);
                    }
                    
                    // Add the final shot label into the list of shot labels to be returned
                    finalResults.shotLabels.put(shotLabel.shotLabel, shotLabel);
                }
                
            }
            
            int index = 0;
//            for (VideoAnnotationResults results : response.get().getAnnotationResultsList()) 
            for (VideoAnnotationResults results : responseResults)
            {
                System.out.println("********************* Results " + index + " *********************");
                // process video / segment level label annotations
                System.out.println("********************* Video level label annotations: *********************");
                System.out.println("Locations: ");
                for (LabelAnnotation labelAnnotation : results.getSegmentLabelAnnotationsList()) 
                {
                    System.out.println("Video label: " + labelAnnotation.getEntity().getDescription());
                    // categories
                    for (Entity categoryEntity : labelAnnotation.getCategoryEntitiesList()) 
                    {
                        System.out.println("Video label category: " + categoryEntity.getDescription());
                    }
                    // segments
                    for (LabelSegment segment : labelAnnotation.getSegmentsList()) 
                    {
                        double startTime = segment.getSegment().getStartTimeOffset().getSeconds()
                                + segment.getSegment().getStartTimeOffset().getNanos() / 1e9;
                        double endTime = segment.getSegment().getEndTimeOffset().getSeconds()
                                + segment.getSegment().getEndTimeOffset().getNanos() / 1e9;
                        System.out.printf("Segment location: %.3f:%.2f\n", startTime, endTime);
                        System.out.println("Confidence: " + segment.getConfidence());
                    }
                    System.out.println("------------------------");
                }

                System.out.println("********************* Shot level label annotations: *********************");
                // process shot label annotations
                for (LabelAnnotation labelAnnotation : results.getShotLabelAnnotationsList()) 
                {
                    System.out.println("Shot label: " + labelAnnotation.getEntity().getDescription());
                    // categories
                    for (Entity categoryEntity : labelAnnotation.getCategoryEntitiesList()) 
                    {
                        System.out.println("Shot label category: " + categoryEntity.getDescription());
                    }
                    // segments
                    for (LabelSegment segment : labelAnnotation.getSegmentsList()) 
                    {
                        double startTime = segment.getSegment().getStartTimeOffset().getSeconds()
                                + segment.getSegment().getStartTimeOffset().getNanos() / 1e9;
                        double endTime = segment.getSegment().getEndTimeOffset().getSeconds()
                                + segment.getSegment().getEndTimeOffset().getNanos() / 1e9;
                        System.out.printf("Segment location: %.3f:%.2f\n", startTime, endTime);
                        System.out.println("Confidence: " + segment.getConfidence());
                    }
                    System.out.println("------------------------");
                }

//                System.out.println("********************* Frame label annotations: *********************");
//                // process frame label annotations
//                for (LabelAnnotation labelAnnotation : results.getFrameLabelAnnotationsList()) 
//                {
//                    System.out
//                            .println("Frame label: " + labelAnnotation.getEntity().getDescription());
//                    // categories
//                    for (Entity categoryEntity : labelAnnotation.getCategoryEntitiesList()) 
//                    {
//                        System.out.println("Frame label category: " + categoryEntity.getDescription());
//                    }
//                    // segments
//                    for (LabelSegment segment : labelAnnotation.getSegmentsList()) 
//                    {
//                        double startTime = segment.getSegment().getStartTimeOffset().getSeconds()
//                                + segment.getSegment().getStartTimeOffset().getNanos() / 1e9;
//                        double endTime = segment.getSegment().getEndTimeOffset().getSeconds()
//                                + segment.getSegment().getEndTimeOffset().getNanos() / 1e9;
//                        System.out.printf("Segment location: %.3f:%.2f\n", startTime, endTime);
//                        System.out.println("Confidence: " + segment.getConfidence());
//                    }
//                }
                index++;
            }
            
            // Return the results of the request to Google Cloud
            return finalResults; 
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
        return null;
    }

    // Analyzes a file that has already been uploaded to Google Cloud Storage
    // DO NOT USE
    public static void analyzeLabelsFromCloud() throws IOException, Exception
    {
        // Instantiate a com.google.cloud.videointelligence.v1.VideoIntelligenceServiceClient
        try (VideoIntelligenceServiceClient client = VideoIntelligenceServiceClient.create()) {
            
//          String gcsUri = "gs://demomaker/cat.mp4";
            String gcsUri = "gs://videointelligencebucket/sports.mp4";
          // Provide path to file hosted on GCS as "gs://bucket-name/..."
          AnnotateVideoRequest request = AnnotateVideoRequest.newBuilder()
              .setInputUri(gcsUri)
              .addFeatures(Feature.LABEL_DETECTION)
              .build();
          // Create an operation that will contain the response when the operation completes.
          OperationFuture<AnnotateVideoResponse, AnnotateVideoProgress> response =
              client.annotateVideoAsync(request);

          System.out.println("Waiting for operation to complete...");
          for (VideoAnnotationResults results : response.get().getAnnotationResultsList()) {
            // process video / segment level label annotations
            System.out.println("Locations: ");
            for (LabelAnnotation labelAnnotation : results.getSegmentLabelAnnotationsList()) {
              System.out
                  .println("Video label: " + labelAnnotation.getEntity().getDescription());
              // categories
              for (Entity categoryEntity : labelAnnotation.getCategoryEntitiesList()) {
                System.out.println("Video label category: " + categoryEntity.getDescription());
              }
              // segments
              for (LabelSegment segment : labelAnnotation.getSegmentsList()) {
                double startTime = segment.getSegment().getStartTimeOffset().getSeconds()
                    + segment.getSegment().getStartTimeOffset().getNanos() / 1e9;
                double endTime = segment.getSegment().getEndTimeOffset().getSeconds()
                    + segment.getSegment().getEndTimeOffset().getNanos() / 1e9;
                System.out.printf("Segment location: %.3f:%.3f\n", startTime, endTime);
                System.out.println("Confidence: " + segment.getConfidence());
              }
            }

            // process shot label annotations
            for (LabelAnnotation labelAnnotation : results.getShotLabelAnnotationsList()) {
              System.out
                  .println("Shot label: " + labelAnnotation.getEntity().getDescription());
              // categories
              for (Entity categoryEntity : labelAnnotation.getCategoryEntitiesList()) {
                System.out.println("Shot label category: " + categoryEntity.getDescription());
              }
              // segments
              for (LabelSegment segment : labelAnnotation.getSegmentsList()) {
                double startTime = segment.getSegment().getStartTimeOffset().getSeconds()
                    + segment.getSegment().getStartTimeOffset().getNanos() / 1e9;
                double endTime = segment.getSegment().getEndTimeOffset().getSeconds()
                    + segment.getSegment().getEndTimeOffset().getNanos() / 1e9;
                System.out.printf("Segment location: %.3f:%.3f\n", startTime, endTime);
                System.out.println("Confidence: " + segment.getConfidence());
              }
            }

            // process frame label annotations
            for (LabelAnnotation labelAnnotation : results.getFrameLabelAnnotationsList()) {
              System.out
                  .println("Frame label: " + labelAnnotation.getEntity().getDescription());
              // categories
              for (Entity categoryEntity : labelAnnotation.getCategoryEntitiesList()) {
                System.out.println("Frame label category: " + categoryEntity.getDescription());
              }
              // segments
              for (LabelSegment segment : labelAnnotation.getSegmentsList()) {
                double startTime = segment.getSegment().getStartTimeOffset().getSeconds()
                    + segment.getSegment().getStartTimeOffset().getNanos() / 1e9;
                double endTime = segment.getSegment().getEndTimeOffset().getSeconds()
                    + segment.getSegment().getEndTimeOffset().getNanos() / 1e9;
                System.out.printf("Segment location: %.3f:%.2f\n", startTime, endTime);
                System.out.println("Confidence: " + segment.getConfidence());
              }
            }
          }
        }
    }
    
    // Uploads a file to Google Cloud Storage
    // DO NOT USE
    public static void uploadFiles() throws IOException
    {
        // Create a service object
        // Credentials are inferred from the environment.
        Storage storage = StorageOptions.getDefaultInstance().getService();

        // Create a bucket
        String bucketName = "videointelligencebucket"; // Change this to something unique
        Bucket bucket = storage.get(bucketName); //, bgos)storage.create(BucketInfo.of(bucketName));

        // Upload a blob to the newly created bucket
//        Blob blob = bucket.create("my_blob_name", "a simple blob text here...".getBytes(UTF_8), "text/plain");
        // Read file and encode into Base64
        Path path = Paths.get("./database_videos/sports/sports.mp4");
        byte[] data = Files.readAllBytes(path);
//        byte[] encodedBytes = Base64.encodeBase64(data);
        
        Blob blob = bucket.create("newSportsVideo3.mp4", data, "video/mp4");

        // Read the blob content from the server
//        String blobContent = new String(blob.getContent(), UTF_8);

        // List all your buckets
        System.out.println("My buckets:");
        for (Bucket currentBucket : storage.list().iterateAll()) {
          System.out.println(currentBucket);
        }

        // List the blobs in a particular bucket
        System.out.println("My blobs:");
        for (Blob currentBlob : bucket.list().iterateAll()) {
          System.out.println(currentBlob);
        }
    }
}

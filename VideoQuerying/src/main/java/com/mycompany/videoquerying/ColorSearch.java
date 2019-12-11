package com.mycompany.videoquerying;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ColorSearch {
    public VideoStruc[] vs;
    public Map<String, Integer> frameMap;
    private int database_videos;
    public Map<String, double[]> resultsScoreMap;

    public void init() throws IOException{

        File[] directories = new File(FXMLController.DATABASE_DIR).listFiles(File::isDirectory);
        database_videos = directories.length;

        frameMap = new HashMap<String, Integer>();
        resultsScoreMap = new HashMap<String, double[]>();

        vs = new VideoStruc[database_videos];

        for (int i = 0; i < database_videos; i++)
        {
            vs[i] = new VideoStruc(600, 0);
            vs[i].videoname = directories[i].getName();
        }

        File cachedFile = new File("video6.txt");
        // Use Cached txt file
        if(cachedFile.exists())
            ReadCached();
        // Read and Extract
        else
            ExtractVideoAudio();
    }

    public Map<String, Double> search(String queryname) {
        //initialize query

        VideoStruc queryV = new VideoStruc(150, 1);
        queryV.videoname = queryname;
        try {
            queryV.readAndextractSound();
        }catch(IOException e) {
            e.printStackTrace();
        }
        System.out.println("Audio Data Parse Success!");

        queryV.readAndextractVideo();
        System.out.println("Image Data Analysis Success!");

        queryV.extractColor();
        System.out.println("Color Extraction Success!");
        System.out.println("");

        Map<String, Double> simiMap = new HashMap<String, Double>();
        for(int i = 0;i < 7;i++) {
            simiMap.put(vs[i].videoname, 0.0);
        }

        //compare the most similar videos
        for(int i = 0; i < 7; i++) {
            //array for recording the start frame of the similar clips
            int[] resembleClips = new int[250];
            double[] resembleValues = new double[250];
            double[] imgBytesScores = new double[600];
            for(int j = 0;j < 250;j++) {
                resembleClips[j] = -1;
                resembleValues[j] = 0.0;
            }

            for (int x = 0; x < 600; x++) {
                double frameHashCompareScore = hashCompare(queryV.imgbytes[0], vs[i].imgbytes[x]);
                imgBytesScores[x] = frameHashCompareScore;
            }

            //get the start frames of the most similar clips
            int k = 0;
            for(int j = 0;j < vs[i].framenum - queryV.framenum;j++) {
                double simi = hashCompare(queryV.imgbytes[0], vs[i].imgbytes[j]);
                //System.out.println(simi);
                if(k >= 250) {
                    break;
                }
                if(simi >= 0.70) {
                    resembleClips[k] = j;
                    resembleValues[k] = simi;
                    k++;

                }
            }

            //From the latter frames compute the eligible clips
            for(int r = 30;r < queryV.framenum;r+= 30) {
                for(int j = 0;j < 250;j++) {
                    if(resembleValues[j] != 0.0) {
                        double simi = hashCompare(queryV.imgbytes[r], vs[i].imgbytes[resembleClips[j] + r]);
                        if(simi < 0.7) {
                            resembleClips[j] = -1;
                            resembleValues[j] = 0.0;
                        }
                    }
                }
            }

            int clip = 0;
            double maxvalue = 0.0;
            for(int j = 0;j < 250;j++) {
                if(resembleValues[j] != 0.0) {
                    if(resembleValues[j] > maxvalue) {
                        clip = resembleClips[j];
                        maxvalue = resembleValues[j];
                    }
                }
            }
            frameMap.put(vs[i].videoname, clip);
            System.out.println(vs[i].videoname + " " + frameMap.get(vs[i].videoname));

            double simisum = 0.0;
            k = 0;
            for(int j = clip;j < clip + 150 && k < queryV.framenum;j++, k++) {
                double frameHashCompareScore2 = hashCompare(queryV.imgbytes[k], vs[i].imgbytes[j]);
                imgBytesScores[j] = frameHashCompareScore2;
                simisum += frameHashCompareScore2;
            }
            //System.out.println(simisum);


            simiMap.put(vs[i].videoname, simisum / 150.0);
            //System.out.println("For video" + i + ": " + simisum / 150.0);
            resultsScoreMap.put(vs[i].videoname, imgBytesScores);

        }

        System.out.println("Image Structure Comparison Complete!");

        //Compare the colors
        for(int i = 0;i < 7;i++) {
            int[] resembleClips = new int[250];
            double[] resembleValues = new double[250];
            double[] colorDistScores = new double[600];
            for(int j = 0;j < 250;j++) {
                resembleClips[j] = -1;
                resembleValues[j] = 0.0;
            }

            for (int x = 0; x < 600; x++) {
                double frameDistCompareScore = 1 - distCompare(queryV.rgbCount[0], vs[i].rgbCount[x]);
                colorDistScores[x] = frameDistCompareScore;
            }

            //get the start frames of the most similar clips
            int k = 0;
            for(int j = 0;j < vs[i].framenum - queryV.framenum;j++) {
                double simi = distCompare(queryV.rgbCount[0], vs[i].rgbCount[j]);
                //System.out.println(simi);
                if(k >= 250) {
                    break;
                }
                if(simi < 0.2) {
                    resembleClips[k] = j;
                    resembleValues[k] = simi;
                    k++;

                }
            }

            //From the latter frames compute the eligible clips
            for(int r = 30;r < queryV.framenum;r+= 30) {
                for(int j = 0;j < 250;j++) {
                    if(resembleValues[j] != 0.0) {
                        double simi = distCompare(queryV.rgbCount[r], vs[i].rgbCount[resembleClips[j] + r]);
                        if(simi >= 0.2) {
                            resembleClips[j] = -1;
                            resembleValues[j] = 0.0;
                        }
                    }
                }
            }

            int clip = 0;
            double minvalue = 0.0;
            for(int j = 0;j < 250;j++) {
                if(resembleValues[j] != 0.0) {
                    if(resembleValues[j] < minvalue) {
                        clip = resembleClips[j];
                        minvalue = resembleValues[j];
                    }
                }
            }

            double simisum = 0.0;
            k = 0;
            for(int j = clip;j < clip + 150 && k < queryV.framenum;j++, k++) {
                double frameDistCompareScore2 = distCompare(queryV.rgbCount[k], vs[i].rgbCount[j]);
                colorDistScores[j] = frameDistCompareScore2;
                simisum += frameDistCompareScore2;
            }

            simiMap.put(vs[i].videoname, simiMap.get(vs[i].videoname) * 0.8 + (1 - simisum / 150.0) * 0.2);
            //System.out.println(simiMap.get(vs[i].videoname));

            double[] tmpImgBytesScores = resultsScoreMap.get(vs[i].videoname);
            for (int x = 0; x < 600; x++) {
                tmpImgBytesScores[x] = 0.8 * tmpImgBytesScores[x] + 0.2 * colorDistScores[x];
            }
            resultsScoreMap.put(vs[i].videoname, tmpImgBytesScores);
        }
        System.out.println("Color Comparison Complete!");
        System.out.println("Audio Comparison Complete!");
        System.out.println("Compare and Match Complete!");

        return simiMap;
    }

    private void ReadCached() throws IOException{
        for(int i = 0; i < 7;i++) {
            BufferedReader in = new BufferedReader(new FileReader("video" + i + ".txt"));
            String line = in.readLine();
            vs[i].sound = Integer.parseInt(line);
            //System.out.println(vs[i].sound);
            for(int j = 0;j < vs[i].framenum;j++) {
                line = in.readLine();
                for(int k = 0;k < 99;k++) {
                    vs[i].imgbytes[j][k] = line.charAt(k) - '0';
                    //System.out.print(vs[i].imgbytes[j][k]);
                }
                //System.out.println("");
            }
            for(int j = 0;j < vs[i].framenum;j++) {
                line = in.readLine();
                int t = 0;
                for(int k = 0;k < 3;k++) {
                    String dvalue = "";
                    for(;line.charAt(t) != ' ';t++) {
                        dvalue += line.charAt(t);

                    }
                    t++;
                    vs[i].rgbCount[j][k] = Double.parseDouble(dvalue);
                }
            }
            in.close();
        }
    }

    private void ExtractVideoAudio() throws IOException{
        for(int i = 0;i < 7;i++) {
            try {
                vs[i].readAndextractSound();
            }catch(IOException e) {
                e.printStackTrace();
            }

            vs[i].readAndextractVideo();

            vs[i].extractColor();

            File fout = new File("video" + i + ".txt");
            FileOutputStream fos = new FileOutputStream(fout);

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write(Integer.toString(vs[i].sound));
            bw.newLine();
            for(int j = 0;j < vs[i].framenum;j++) {
                for(int k = 0;k < 99;k++) {
                    bw.write(Integer.toString(vs[i].imgbytes[j][k]));
                }
                bw.newLine();
            }
            for(int j = 0;j < vs[i].framenum;j++) {
                for(int k = 0;k < 3;k++) {
                    bw.write(Double.toString(vs[i].rgbCount[j][k]));
                    bw.write(" ");
                }
                bw.newLine();
            }
            bw.close();
        }
    }

    private double hashCompare(int[] q, int [] db) {
        double sim = 0.0;

        for(int i = 0;i < 99;i++) {
            if(q[i] == db[i]) {
                sim += 1.0;
            }
        }
        //System.out.println(sim);

        sim = sim / 99.0;

        return sim;
    }

    private double distCompare(double[] q, double[] db) {
        double d = 0.0;

        for(int i = 0;i < q.length;i++) {
            d += Math.pow(q[i] - db[i], 2.0);
        }
        d = Math.sqrt(d);

        return d;
    }
}

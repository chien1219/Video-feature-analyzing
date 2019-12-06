import java.io.*;

public final class VideoPath{

    public static String queryPath;
    public static String dbPath;
  
    public static String getQueryPath(){
        return VideoPath.queryPath == null ? "fuck" : VideoPath.queryPath;
    }
  
    public static String getDBPath(){
        return VideoPath.dbPath == null ? "fuckdb" : VideoPath.dbPath;
    }
  
    public static void setQueryPath(String p){
        VideoPath.queryPath = p;
    }
  
    public static void setDBPath(String p){
        VideoPath.dbPath = p;
    }
  }
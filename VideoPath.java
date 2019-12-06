import java.io.*;

public final class VideoPath{

    public static String queryPath;
    public static String dbPath;
  
    public static String getQueryPath(){
        return VideoPath.queryPath == null ? "query" : VideoPath.queryPath;
    }
  
    public static String getDBPath(){
        return VideoPath.dbPath == null ? "db" : VideoPath.dbPath;
    }
  
    public static void setQueryPath(String p){
        VideoPath.queryPath = p;
    }
  
    public static void setDBPath(String p){
        VideoPath.dbPath = p;
    }
  }
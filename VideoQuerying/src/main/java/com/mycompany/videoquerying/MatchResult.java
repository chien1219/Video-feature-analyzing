package com.mycompany.videoquerying;

/**
 *
 * @author stermark
 */
public class MatchResult implements Comparable<MatchResult> 
{
    public String queryFileName;
    public String databaseFileName;
    public double matchScore;
    public double[] frameScores;
    
    public MatchResult(String _queryFileName, String _databaseFileName, double _matchScore, double[] _frameScores)
    {
        queryFileName = _queryFileName;
        databaseFileName = _databaseFileName;
        matchScore = _matchScore;
        frameScores = _frameScores;
    }

    @Override
    public int compareTo(MatchResult other) {
        return Double.compare(other.matchScore, this.matchScore); // sorts in descending order
    }    
}

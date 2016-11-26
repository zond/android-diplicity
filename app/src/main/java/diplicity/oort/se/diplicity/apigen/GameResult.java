import retrofit2.http.*;
	
public class GameResult {
  public String GameID;
  public String SoloWinnerMember;
  public String SoloWinnerUser;
  public java.util.List<String> DIASMembers;
  public java.util.List<String> DIASUsers;
  public java.util.List<String> NMRMembers;
  public java.util.List<String> NMRUsers;
  public java.util.List<String> EliminatedMembers;
  public java.util.List<String> EliminatedUsers;
  public java.util.List<String> AllUsers;
  public java.util.List<GameScore> Scores;
  public Boolean Rated;
  public java.util.Date CreatedAt;
}
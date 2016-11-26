import retrofit2.http.*;
	
public class PhaseResult {
  public String GameID;
  public Long PhaseOrdinal;
  public java.util.List<String> NMRUsers;
  public java.util.List<String> ActiveUsers;
  public java.util.List<String> ReadyUsers;
  public java.util.List<String> AllUsers;
}
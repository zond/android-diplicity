import retrofit2.http.*;
	
public class FCMToken {
  public String Value;
  public Boolean Disabled;
  public String Note;
  public String App;
  public FCMNotificationConfig MessageConfig;
  public FCMNotificationConfig PhaseConfig;
}
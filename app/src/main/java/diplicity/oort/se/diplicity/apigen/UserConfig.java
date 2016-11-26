import retrofit2.http.*;
	
public class UserConfig {
  public String UserId;
  public java.util.List<FCMToken> FCMTokens;
  public MailConfig MailConfig;
}
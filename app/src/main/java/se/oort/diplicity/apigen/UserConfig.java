package se.oort.diplicity.apigen;

import retrofit2.http.*;
	
public class UserConfig implements java.io.Serializable {
  public String UserId;
  public java.util.List<FCMToken> FCMTokens;
  public MailConfig MailConfig;
  public java.util.List<String> Colors;
  public Long PhaseDeadlineWarningMinutesAhead;
}
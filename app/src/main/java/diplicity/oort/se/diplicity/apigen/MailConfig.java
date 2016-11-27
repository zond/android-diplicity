package se.oort.diplicity.apigen;

import retrofit2.http.*;
	
public class MailConfig {
  public Boolean Enabled;
  public UnsubscribeConfig UnsubscribeConfig;
  public MailNotificationConfig MessageConfig;
  public MailNotificationConfig PhaseConfig;
}
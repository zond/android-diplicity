package se.oort.diplicity.apigen;

import retrofit2.http.*;
import java.util.*;
	
public class MailConfig implements java.io.Serializable {
  public Boolean Enabled;
  public UnsubscribeConfig UnsubscribeConfig;
  public MailNotificationConfig MessageConfig;
  public MailNotificationConfig PhaseConfig;
}
package se.oort.diplicity.apigen;

import retrofit2.http.*;
import java.util.*;
	
public class MailNotificationConfig implements java.io.Serializable {
  public String SubjectTemplate;
  public String TextBodyTemplate;
  public String HTMLBodyTemplate;
}
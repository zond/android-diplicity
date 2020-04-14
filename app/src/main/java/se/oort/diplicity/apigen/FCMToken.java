package se.oort.diplicity.apigen;

import retrofit2.http.*;
import java.util.*;
	
public class FCMToken implements java.io.Serializable {
  public String Value;
  public Boolean Disabled;
  public String Note;
  public String App;
  public FCMNotificationConfig MessageConfig;
  public FCMNotificationConfig PhaseConfig;
  public String ReplaceToken;
}
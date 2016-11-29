package se.oort.diplicity.apigen;

import retrofit2.http.*;
	
public class FlaggedMessages {
  public String GameID;
  public String UserId;
  public java.util.List<FlaggedMessage> Messages;
  public java.util.Date CreatedAt;
}
package se.oort.diplicity.apigen;

import retrofit2.http.*;
	
public class FlaggedMessage implements java.io.Serializable {
  public String GameID;
  public String ChannelMembers;
  public String Sender;
  public String Body;
  public java.util.Date CreatedAt;
  public String AuthorId;
}
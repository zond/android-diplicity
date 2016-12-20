package se.oort.diplicity.apigen;

import retrofit2.http.*;
	
public class Message implements java.io.Serializable {
  public String ID;
  public String GameID;
  public java.util.List<String> ChannelMembers;
  public String Sender;
  public String Body;
  public java.util.Date CreatedAt;
  public Ticker Age;
}
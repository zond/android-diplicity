package se.oort.diplicity.apigen;

import retrofit2.http.*;
	
public class MessageFlag implements java.io.Serializable {
  public String GameID;
  public java.util.List<String> ChannelMembers;
  public java.util.Date From;
  public java.util.Date To;
}
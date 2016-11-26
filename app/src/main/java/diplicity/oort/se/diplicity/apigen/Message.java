import retrofit2.http.*;
	
public class Message {
  public String ID;
  public String GameID;
  public java.util.List<String> ChannelMembers;
  public String Sender;
  public String Body;
  public java.util.Date CreatedAt;
}
import retrofit2.http.*;
	
public class MessageContainer {
  public Message Properties;
  public java.util.List<Link> Links;
  public String name;
  public java.util.List<java.util.List<String>> Desc;
}
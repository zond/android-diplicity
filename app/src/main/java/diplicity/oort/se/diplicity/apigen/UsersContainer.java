package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
	
public class UsersContainer {
  public java.util.List<User> Properties;
  public java.util.List<Link> Links;
  public String name;
  public java.util.List<java.util.List<String>> Desc;
}
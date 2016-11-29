package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
	
public class MembersContainer {
  public java.util.List<MemberContainer> Properties;
  public java.util.List<Link> Links;
  public String name;
  public java.util.List<java.util.List<String>> Desc;
}
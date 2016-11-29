package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
	
public class ResolutionsContainer {
  public java.util.List<ResolutionContainer> Properties;
  public java.util.List<Link> Links;
  public String name;
  public java.util.List<java.util.List<String>> Desc;
}
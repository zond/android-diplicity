package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
	
public class GameResultsContainer {
  public java.util.List<GameResult> Properties;
  public java.util.List<Link> Links;
  public String name;
  public java.util.List<java.util.List<String>> Desc;
}
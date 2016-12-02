package se.oort.diplicity.apigen;

import retrofit2.http.*;
	
public class GameState implements java.io.Serializable {
  public String GameID;
  public String Nation;
  public java.util.List<String> Muted;
}
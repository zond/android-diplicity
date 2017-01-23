package se.oort.diplicity.apigen;

import retrofit2.http.*;
	
public class Member implements java.io.Serializable {
  public User User;
  public String Nation;
  public String GameAlias;
  public PhaseState NewestPhaseState;
}
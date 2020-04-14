package se.oort.diplicity.apigen;

import retrofit2.http.*;
import java.util.*;
	
public class Member implements java.io.Serializable {
  public User User;
  public String Nation;
  public String GameAlias;
  public String NationPreferences;
  public PhaseState NewestPhaseState;
  public Long UnreadMessages;
}
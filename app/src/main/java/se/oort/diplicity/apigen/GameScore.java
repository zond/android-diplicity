package se.oort.diplicity.apigen;

import retrofit2.http.*;
import java.util.*;
	
public class GameScore implements java.io.Serializable {
  public String UserId;
  public String Member;
  public Long SCs;
  public Double Score;
  public String Explanation;
}
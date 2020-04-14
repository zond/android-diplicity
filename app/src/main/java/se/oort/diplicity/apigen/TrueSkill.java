package se.oort.diplicity.apigen;

import retrofit2.http.*;
	
public class TrueSkill implements java.io.Serializable {
  public String GameID;
  public String UserId;
  public java.util.Date CreatedAt;
  public String Member;
  public Double Mu;
  public Double Sigma;
  public Double Rating;
}
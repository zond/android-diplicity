package se.oort.diplicity.apigen;

import retrofit2.http.*;
	
public class Game implements java.io.Serializable {
  public String ID;
  public Boolean Started;
  public Boolean Closed;
  public Boolean Finished;
  public String Desc;
  public String Variant;
  public Long PhaseLengthMinutes;
  public Double MaxHated;
  public Double MaxHater;
  public Double MinRating;
  public Double MaxRating;
  public Double MinReliability;
  public Double MinQuickness;
  public Long NMembers;
  public java.util.List<Member> Members;
  public java.util.List<PhaseMeta> NewestPhaseMeta;
  public java.util.List<Ban> ActiveBans;
  public java.util.List<String> FailedRequirements;
  public java.util.Date CreatedAt;
  public Ticker CreatedAgo;
  public java.util.Date StartedAt;
  public Ticker StartedAgo;
  public java.util.Date FinishedAt;
  public Ticker FinishedAgo;
}
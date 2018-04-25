package se.oort.diplicity.apigen;

import retrofit2.http.*;
	
public class UserStatsNumbers implements java.io.Serializable {
  public Long StartedGames;
  public Long FinishedGames;
  public Long SoloGames;
  public Long DIASGames;
  public Long EliminatedGames;
  public Long DroppedGames;
  public Long NMRPhases;
  public Long ActivePhases;
  public Long ReadyPhases;
  public Double Reliability;
  public Double Quickness;
  public Long OwnedBans;
  public Long SharedBans;
  public Double Hated;
  public Double Hater;
}
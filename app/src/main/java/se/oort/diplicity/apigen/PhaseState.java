package se.oort.diplicity.apigen;

import retrofit2.http.*;
	
public class PhaseState implements java.io.Serializable {
  public String GameID;
  public Long PhaseOrdinal;
  public String Nation;
  public Boolean ReadyToResolve;
  public Boolean WantsDIAS;
  public Boolean OnProbation;
  public String Note;
}
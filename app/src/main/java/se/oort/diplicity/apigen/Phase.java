package se.oort.diplicity.apigen;

import retrofit2.http.*;
import java.util.*;
	
public class Phase implements java.io.Serializable {
  public Long PhaseOrdinal;
  public String Season;
  public Long Year;
  public String Type;
  public Boolean Resolved;
  public java.util.Date CreatedAt;
  public Ticker CreatedAgo;
  public java.util.Date ResolvedAt;
  public Ticker ResolvedAgo;
  public java.util.Date DeadlineAt;
  public Ticker NextDeadlineIn;
  public String UnitsJSON;
  public String SCsJSON;
  public String GameID;
  public java.util.List<UnitWrapper> Units;
  public java.util.List<SC> SCs;
  public java.util.List<Dislodged> Dislodgeds;
  public java.util.List<Dislodger> Dislodgers;
  public java.util.List<Bounce> Bounces;
  public java.util.List<Resolution> Resolutions;
  public String Host;
}
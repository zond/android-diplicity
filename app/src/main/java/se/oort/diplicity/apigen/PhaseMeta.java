package se.oort.diplicity.apigen;

import retrofit2.http.*;
	
public class PhaseMeta {
  public Long PhaseOrdinal;
  public String Season;
  public Long Year;
  public String Type;
  public Boolean Resolved;
  public java.util.Date DeadlineAt;
  public String UnitsJSON;
  public String SCsJSON;
}
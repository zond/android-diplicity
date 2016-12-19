package se.oort.diplicity.apigen;

import retrofit2.http.*;
	
public class PhaseMeta implements java.io.Serializable {
  public Long PhaseOrdinal;
  public String Season;
  public Long Year;
  public String Type;
  public Boolean Resolved;
  public java.util.Date DeadlineAt;
  public Long NextDeadlineIn;
  public String UnitsJSON;
  public String SCsJSON;
}
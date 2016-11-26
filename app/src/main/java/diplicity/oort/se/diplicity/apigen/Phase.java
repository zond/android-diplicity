import retrofit2.http.*;
	
public class Phase {
  public String GameID;
  public Long PhaseOrdinal;
  public String Season;
  public Long Year;
  public String Type;
  public java.util.List<Unit> Units;
  public java.util.List<SC> SCs;
  public java.util.List<Dislodged> Dislodgeds;
  public java.util.List<Dislodger> Dislodgers;
  public java.util.List<Bounce> Bounces;
  public java.util.List<Resolution> Resolutions;
  public Boolean Resolved;
  public java.util.Date DeadlineAt;
  public String Host;
  public String Scheme;
}
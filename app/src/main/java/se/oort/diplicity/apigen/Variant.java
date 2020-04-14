package se.oort.diplicity.apigen;

import retrofit2.http.*;
	
public class Variant implements java.io.Serializable {
  public String Name;
  public java.util.List<String> Nations;
  public java.util.List<String> PhaseTypes;
  public java.util.List<String> Seasons;
  public java.util.List<String> UnitTypes;
  public String SVGVersion;
  public String CreatedBy;
  public String Version;
  public String Description;
  public String Rules;
  public java.util.List<String> OrderTypes;
  public RenderPhase Start;
}
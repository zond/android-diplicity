package se.oort.diplicity.apigen;

import retrofit2.http.*;
import java.util.*;
	
public class RenderPhase implements java.io.Serializable {
  public Long Year;
  public String Season;
  public String Type;
  public Map<String,String> SCs;
  public Map<String,Unit> Units;
  public String Map;
}
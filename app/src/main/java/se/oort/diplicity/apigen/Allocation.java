package se.oort.diplicity.apigen;

import retrofit2.http.*;
import java.util.*;
	
public class Allocation implements java.io.Serializable {
  public java.util.List<AllocationMember> Members;
  public String Variant;
}
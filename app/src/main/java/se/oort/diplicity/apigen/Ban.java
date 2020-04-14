package se.oort.diplicity.apigen;

import retrofit2.http.*;
import java.util.*;
	
public class Ban implements java.io.Serializable {
  public java.util.List<String> UserIds;
  public java.util.List<String> OwnerIds;
  public java.util.List<User> Users;
}
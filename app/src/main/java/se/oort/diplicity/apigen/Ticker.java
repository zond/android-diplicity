package se.oort.diplicity.apigen;
	
import java.util.*;
		
public class Ticker implements java.io.Serializable {
  public Long nanos;
  public Date unserializedAt;
  public Ticker(Date unserializedAt, Long nanos) {
    this.unserializedAt = unserializedAt;
    this.nanos = nanos;
  }
  public Date createdAt() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(unserializedAt);
		cal.add(Calendar.SECOND, (int) (nanos / (long) -1000000000));
		return cal.getTime();
	}
  public Date deadlineAt() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(unserializedAt);
		cal.add(Calendar.SECOND, (int) (nanos / (long) 1000000000));
		return cal.getTime();
	}
	public Long millisLeft() {
		return (long) (deadlineAt().getTime() - unserializedAt.getTime());
	}
}
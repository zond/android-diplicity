package se.oort.diplicity.apigen;
		
public class MultiContainer<T> implements java.io.Serializable {
  public MultiContainer() {
  }
	public java.util.List<SingleContainer<T>> Properties;
  public java.util.List<Link> Links;
  public String name;
  public java.util.List<java.util.List<String>> Desc;
}
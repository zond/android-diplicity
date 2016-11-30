package se.oort.diplicity.apigen;
		
public class MultiContainer<T> {
  public MultiContainer() {
  }
	public java.util.List<SingleContainer<T>> Properties;
  public java.util.List<Link> Links;
  public String name;
  public java.util.List<java.util.List<String>> Desc;
}
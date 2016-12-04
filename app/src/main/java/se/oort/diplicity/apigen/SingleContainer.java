package se.oort.diplicity.apigen;
		
public class SingleContainer<T> implements java.io.Serializable {
  public SingleContainer() {
  }
  public T Properties;
  public java.util.List<Link> Links;
  public String name;
  public java.util.List<java.util.List<String>> Desc;
}
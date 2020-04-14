package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
import rx.*;
	
public interface VariantService {
  @GET("/Variant/{variant_name}")
  Observable<SingleContainer<Variant>> VariantLoad(@Path("variant_name") String variant_name);

  @GET("/Variants")
  Observable<MultiContainer<Variant>> ListVariants();

}
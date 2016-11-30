package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
import rx.*;
	
public interface RedirectURLService {
  @DELETE("/RedirectURL/{id}")
  Observable<SingleContainer<RedirectURL>> RedirectURLDelete(@Path("id") String id);

  @GET("/User/{user_id}/RedirectURLs")
  Observable<MultiContainer<RedirectURL>> ListRedirectURLs(@Path("user_id") String user_id);

}
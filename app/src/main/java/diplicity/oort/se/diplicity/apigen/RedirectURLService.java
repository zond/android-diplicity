import retrofit2.http.*;
import rx.*;
	
public interface RedirectURLService {
  @DELETE("/RedirectURL/{id}")
  Observable<RedirectURLContainer> RedirectURLDelete(@Path("id") String id);

  @GET("/User/{user_id}/RedirectURLs")
  Observable<RedirectURLsContainer> ListRedirectURLs(@Path("user_id") String user_id);

}
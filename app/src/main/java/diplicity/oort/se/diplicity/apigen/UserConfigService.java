import retrofit2.http.*;
import rx.*;
	
public interface UserConfigService {
  @GET("/User/{user_id}/UserConfig")
  Observable<UserConfigContainer> UserConfigLoad(@Path("user_id") String user_id);

  @PUT("/User/{user_id}/UserConfig")
  Observable<UserConfigContainer> UserConfigUpdate(@Path("user_id") String user_id);

}
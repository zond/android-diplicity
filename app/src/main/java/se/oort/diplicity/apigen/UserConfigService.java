package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
import rx.*;
	
public interface UserConfigService {
  @GET("/User/{user_id}/UserConfig")
  Observable<SingleContainer<UserConfig>> UserConfigLoad(@Path("user_id") String user_id);

  @PUT("/User/{user_id}/UserConfig")
  Observable<SingleContainer<UserConfig>> UserConfigUpdate(@Path("user_id") String user_id);

}
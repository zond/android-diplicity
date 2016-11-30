package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
import rx.*;
	
public interface BanService {
  @POST("/User/{user_id}/Ban")
  Observable<SingleContainer<Ban>> BanCreate(@Body Ban ban, @Path("user_id") String user_id);

  @DELETE("/User/{user_id}/Ban/{banned_id}")
  Observable<SingleContainer<Ban>> BanDelete(@Path("user_id") String user_id, @Path("banned_id") String banned_id);

  @GET("/User/{user_id}/Bans")
  Observable<MultiContainer<Ban>> ListBans(@Path("user_id") String user_id);

}
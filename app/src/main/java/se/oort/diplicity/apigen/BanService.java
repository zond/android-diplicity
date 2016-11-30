package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
import rx.*;
	
public interface BanService {
  @POST("/User/{user_id}/Ban")
  Observable<BanContainer> BanCreate(@Body Ban ban, @Path("user_id") String user_id);

  @DELETE("/User/{user_id}/Ban/{banned_id}")
  Observable<BanContainer> BanDelete(@Path("user_id") String user_id, @Path("banned_id") String banned_id);

  @GET("/User/{user_id}/Bans")
  Observable<BansContainer> ListBans(@Path("user_id") String user_id);

}
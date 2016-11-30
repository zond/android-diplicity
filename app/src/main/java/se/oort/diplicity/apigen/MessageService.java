package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
import rx.*;
	
public interface MessageService {
  @POST("/Game/{game_id}/Messages")
  Observable<SingleContainer<Message>> MessageCreate(@Body Message message, @Path("game_id") String game_id);

  @GET("/Game/{game_id}/Channel/{channel_members}/Messages")
  Observable<MultiContainer<Message>> ListMessages(@Path("game_id") String game_id, @Path("channel_members") String channel_members);

}
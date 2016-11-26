import retrofit2.http.*;
import rx.*;
	
public interface MessageService {
  @POST("/Game/{game_id}/Messages")
  Observable<MessageContainer> MessageCreate(@Path("game_id") String game_id, @Body Message message);

  @GET("/Game/{game_id}/Channel/{channel_members}/Messages")
  Observable<MessagesContainer> ListMessages(@Path("game_id") String game_id, @Path("channel_members") String channel_members);

}
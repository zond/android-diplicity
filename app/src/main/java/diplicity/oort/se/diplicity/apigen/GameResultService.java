import retrofit2.http.*;
import rx.*;
	
public interface GameResultService {
  @GET("/Game/{game_id}/GameResult")
  Observable<GameResultContainer> GameResultLoad(@Path("game_id") String game_id);

}
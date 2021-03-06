package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
import rx.*;
	
public interface GameResultService {
  @GET("/Game/{game_id}/GameResult")
  Observable<SingleContainer<GameResult>> GameResultLoad(@Path("game_id") String game_id);

}
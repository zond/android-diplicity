package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
import rx.*;
	
public interface GameStateService {
  @PUT("/Game/{game_id}/GameState/{nation}")
  Observable<GameStateContainer> GameStateUpdate(@Path("game_id") String game_id, @Path("nation") String nation);

  @GET("/Game/{game_id}/GameStates")
  Observable<GameStatesContainer> ListGameStates(@Path("game_id") String game_id);

}
package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
import rx.*;
	
public interface GameService {
  @POST("/Game")
  Observable<GameContainer> GameCreate(@Body Game game);

  @GET("/Game/{id}")
  Observable<GameContainer> GameLoad(@Path("id") String id);

  @GET("/Games/Open")
  Observable<GamesContainer> OpenGames();

  @GET("/Games/Started")
  Observable<GamesContainer> StartedGames();

  @GET("/Games/Finished")
  Observable<GamesContainer> FinishedGames();

  @GET("/Games/My/Staging")
  Observable<GamesContainer> MyStagingGames();

  @GET("/Games/My/Started")
  Observable<GamesContainer> MyStartedGames();

  @GET("/Games/My/Finished")
  Observable<GamesContainer> MyFinishedGames();

}
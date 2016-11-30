package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
import rx.*;
	
public interface GameService {
  @POST("/Game")
  Observable<GameContainer> GameCreate(@Body Game game);

  @GET("/Game/{id}")
  Observable<GameContainer> GameLoad(@Path("id") String id);

  @GET("/Games/Open")
  Observable<GamesContainer> OpenGames(@Query("variant") String variant, @Query("min-reliability") String min_reliability, @Query("min-quickness") String min_quickness, @Query("max-hater") String max_hater, @Query("max-hated") String max_hated, @Query("min-rating") String min_rating, @Query("max-rating") String max_rating, @Query("limit") String limit, @Query("cursor") String cursor);

  @GET("/Games/Started")
  Observable<GamesContainer> StartedGames(@Query("variant") String variant, @Query("min-reliability") String min_reliability, @Query("min-quickness") String min_quickness, @Query("max-hater") String max_hater, @Query("max-hated") String max_hated, @Query("min-rating") String min_rating, @Query("max-rating") String max_rating, @Query("limit") String limit, @Query("cursor") String cursor);

  @GET("/Games/Finished")
  Observable<GamesContainer> FinishedGames(@Query("variant") String variant, @Query("min-reliability") String min_reliability, @Query("min-quickness") String min_quickness, @Query("max-hater") String max_hater, @Query("max-hated") String max_hated, @Query("min-rating") String min_rating, @Query("max-rating") String max_rating, @Query("limit") String limit, @Query("cursor") String cursor);

  @GET("/Games/My/Staging")
  Observable<GamesContainer> MyStagingGames(@Query("variant") String variant, @Query("min-reliability") String min_reliability, @Query("min-quickness") String min_quickness, @Query("max-hater") String max_hater, @Query("max-hated") String max_hated, @Query("min-rating") String min_rating, @Query("max-rating") String max_rating, @Query("limit") String limit, @Query("cursor") String cursor);

  @GET("/Games/My/Started")
  Observable<GamesContainer> MyStartedGames(@Query("variant") String variant, @Query("min-reliability") String min_reliability, @Query("min-quickness") String min_quickness, @Query("max-hater") String max_hater, @Query("max-hated") String max_hated, @Query("min-rating") String min_rating, @Query("max-rating") String max_rating, @Query("limit") String limit, @Query("cursor") String cursor);

  @GET("/Games/My/Finished")
  Observable<GamesContainer> MyFinishedGames(@Query("variant") String variant, @Query("min-reliability") String min_reliability, @Query("min-quickness") String min_quickness, @Query("max-hater") String max_hater, @Query("max-hated") String max_hated, @Query("min-rating") String min_rating, @Query("max-rating") String max_rating, @Query("limit") String limit, @Query("cursor") String cursor);

}
package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
import rx.*;
	
public interface GameService {
  @POST("/Game")
  Observable<SingleContainer<Game>> GameCreate(@Body Game game);

  @GET("/Game/{id}")
  Observable<SingleContainer<Game>> GameLoad(@Path("id") String id);

  @GET("/Games/Open")
  Observable<MultiContainer<Game>> ListOpenGames(@Query("variant") String variant, @Query("min-reliability") String min_reliability, @Query("min-quickness") String min_quickness, @Query("max-hater") String max_hater, @Query("max-hated") String max_hated, @Query("min-rating") String min_rating, @Query("max-rating") String max_rating, @Query("limit") String limit, @Query("cursor") String cursor);

  @GET("/Games/Started")
  Observable<MultiContainer<Game>> ListStartedGames(@Query("variant") String variant, @Query("min-reliability") String min_reliability, @Query("min-quickness") String min_quickness, @Query("max-hater") String max_hater, @Query("max-hated") String max_hated, @Query("min-rating") String min_rating, @Query("max-rating") String max_rating, @Query("limit") String limit, @Query("cursor") String cursor);

  @GET("/Games/Finished")
  Observable<MultiContainer<Game>> ListFinishedGames(@Query("variant") String variant, @Query("min-reliability") String min_reliability, @Query("min-quickness") String min_quickness, @Query("max-hater") String max_hater, @Query("max-hated") String max_hated, @Query("min-rating") String min_rating, @Query("max-rating") String max_rating, @Query("limit") String limit, @Query("cursor") String cursor);

  @GET("/Games/My/Staging")
  Observable<MultiContainer<Game>> ListMyStagingGames(@Query("variant") String variant, @Query("min-reliability") String min_reliability, @Query("min-quickness") String min_quickness, @Query("max-hater") String max_hater, @Query("max-hated") String max_hated, @Query("min-rating") String min_rating, @Query("max-rating") String max_rating, @Query("limit") String limit, @Query("cursor") String cursor);

  @GET("/Games/My/Started")
  Observable<MultiContainer<Game>> ListMyStartedGames(@Query("variant") String variant, @Query("min-reliability") String min_reliability, @Query("min-quickness") String min_quickness, @Query("max-hater") String max_hater, @Query("max-hated") String max_hated, @Query("min-rating") String min_rating, @Query("max-rating") String max_rating, @Query("limit") String limit, @Query("cursor") String cursor);

  @GET("/Games/My/Finished")
  Observable<MultiContainer<Game>> ListMyFinishedGames(@Query("variant") String variant, @Query("min-reliability") String min_reliability, @Query("min-quickness") String min_quickness, @Query("max-hater") String max_hater, @Query("max-hated") String max_hated, @Query("min-rating") String min_rating, @Query("max-rating") String max_rating, @Query("limit") String limit, @Query("cursor") String cursor);

}
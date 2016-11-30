package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
import rx.*;
	
public interface UserStatsService {
  @GET("/User/{user_id}/Stats")
  Observable<SingleContainer<UserStats>> UserStatsLoad(@Path("user_id") String user_id);

  @GET("/Users/TopRated")
  Observable<MultiContainer<UserStats>> ListTopRatedPlayers(@Query("limit") String limit, @Query("cursor") String cursor);

  @GET("/Users/TopReliable")
  Observable<MultiContainer<UserStats>> ListTopReliablePlayers(@Query("limit") String limit, @Query("cursor") String cursor);

  @GET("/Users/TopHated")
  Observable<MultiContainer<UserStats>> ListTopHatedPlayers(@Query("limit") String limit, @Query("cursor") String cursor);

  @GET("/Users/TopHater")
  Observable<MultiContainer<UserStats>> ListTopHaterPlayers(@Query("limit") String limit, @Query("cursor") String cursor);

  @GET("/Users/TopQuick")
  Observable<MultiContainer<UserStats>> ListTopQuickPlayers(@Query("limit") String limit, @Query("cursor") String cursor);

}
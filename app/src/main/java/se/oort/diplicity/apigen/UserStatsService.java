package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
import rx.*;
	
public interface UserStatsService {
  @GET("/User/{user_id}/Stats")
  Observable<UserStatsContainer> UserStatsLoad(@Path("user_id") String user_id);

  @GET("/Users/TopRated")
  Observable<UserStatssContainer> ListTopRatedPlayers(@Query("limit") String limit, @Query("cursor") String cursor);

  @GET("/Users/TopReliable")
  Observable<UserStatssContainer> ListTopReliablePlayers(@Query("limit") String limit, @Query("cursor") String cursor);

  @GET("/Users/TopHated")
  Observable<UserStatssContainer> ListTopHatedPlayers(@Query("limit") String limit, @Query("cursor") String cursor);

  @GET("/Users/TopHater")
  Observable<UserStatssContainer> ListTopHaterPlayers(@Query("limit") String limit, @Query("cursor") String cursor);

  @GET("/Users/TopQuick")
  Observable<UserStatssContainer> ListTopQuickPlayers(@Query("limit") String limit, @Query("cursor") String cursor);

}
import retrofit2.http.*;
import rx.*;
	
public interface UserStatsService {
  @GET("/User/{user_id}/Stats")
  Observable<UserStatsContainer> UserStatsLoad(@Path("user_id") String user_id);

  @GET("/Users/TopRated")
  Observable<UserStatssContainer> ListTopRatedPlayers();

  @GET("/Users/TopReliable")
  Observable<UserStatssContainer> ListTopReliablePlayers();

  @GET("/Users/TopHated")
  Observable<UserStatssContainer> ListTopHatedPlayers();

  @GET("/Users/TopHater")
  Observable<UserStatssContainer> ListTopHaterPlayers();

  @GET("/Users/TopQuick")
  Observable<UserStatssContainer> ListTopQuickPlayers();

}
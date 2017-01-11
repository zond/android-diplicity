package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
import rx.*;
	
public interface MemberService {
  @POST("/Game/{game_id}/Member")
  Observable<SingleContainer<Member>> MemberCreate(@Body Member member, @Path("game_id") String game_id);

  @PUT("/Game/{game_id}/Member/{user_id}")
  Observable<SingleContainer<Member>> MemberUpdate(@Body Member member, @Path("game_id") String game_id, @Path("user_id") String user_id);

  @DELETE("/Game/{game_id}/Member/{user_id}")
  Observable<SingleContainer<Member>> MemberDelete(@Path("game_id") String game_id, @Path("user_id") String user_id);

}
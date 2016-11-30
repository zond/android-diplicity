package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
import rx.*;
	
public interface MemberService {
  @POST("/Game/{game_id}/Member")
  Observable<MemberContainer> MemberCreate(@Body Member member, @Path("game_id") String game_id);

  @DELETE("/Game/{game_id}/Member/{user_id}")
  Observable<MemberContainer> MemberDelete(@Path("game_id") String game_id, @Path("user_id") String user_id);

}
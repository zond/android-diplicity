import retrofit2.http.*;
import rx.*;
	
public interface PhaseResultService {
  @GET("/Game/{game_id}/Phase/{phase_ordinal}/Result")
  Observable<PhaseResultContainer> PhaseResultLoad(@Path("game_id") String game_id, @Path("phase_ordinal") String phase_ordinal);

}
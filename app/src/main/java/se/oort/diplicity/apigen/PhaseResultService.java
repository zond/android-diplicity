package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
import rx.*;
	
public interface PhaseResultService {
  @GET("/Game/{game_id}/Phase/{phase_ordinal}/Result")
  Observable<SingleContainer<PhaseResult>> PhaseResultLoad(@Path("game_id") String game_id, @Path("phase_ordinal") String phase_ordinal);

}
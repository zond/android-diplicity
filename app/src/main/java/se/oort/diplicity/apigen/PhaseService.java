package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
import rx.*;
	
public interface PhaseService {
  @GET("/Game/{game_id}/Phase/{phase_ordinal}")
  Observable<SingleContainer<Phase>> PhaseLoad(@Path("game_id") String game_id, @Path("phase_ordinal") String phase_ordinal);

  @GET("/Game/{game_id}/Phases")
  Observable<MultiContainer<Phase>> ListPhases(@Path("game_id") String game_id);

}
package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
import rx.*;
	
public interface PhaseStateService {
  @PUT("/Game/{game_id}/Phase/{phase_ordinal}/PhaseState/{nation}")
  Observable<SingleContainer<PhaseState>> PhaseStateUpdate(@Path("game_id") String game_id, @Path("phase_ordinal") String phase_ordinal, @Path("nation") String nation);

  @GET("/Game/{game_id}/Phase/{phase_ordinal}/PhaseStates")
  Observable<MultiContainer<PhaseState>> ListPhaseStates(@Path("game_id") String game_id, @Path("phase_ordinal") String phase_ordinal);

}
package se.oort.diplicity;

import java.util.Map;

import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Observable;
import rx.Single;
import se.oort.diplicity.apigen.MultiContainer;
import se.oort.diplicity.apigen.SingleContainer;

public interface OptionsService {
    class Option {
        public Map<String, Option> Next;
        public String Type;
    }

    @GET("/Game/{game_id}/Phase/{phase_ordinal}/Options")
    Observable<SingleContainer<Map<String, Option>>> GetOptions(@Path("game_id") String gameID, @Path("phase_ordinal") String phaseOrdinal);
}

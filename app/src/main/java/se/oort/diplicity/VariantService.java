package se.oort.diplicity;

import java.io.Serializable;
import java.util.List;

import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Observable;
import se.oort.diplicity.apigen.MultiContainer;
import se.oort.diplicity.apigen.SingleContainer;

public interface VariantService {
    class Variant implements Serializable {
        String Name;
        List<String> Nations;
    }
    class Phase implements Serializable {
    }
    @GET("/Variants")
    Observable<MultiContainer<Variant>> GetVariants();

    @GET("/Variant/{name}/Start")
    Observable<SingleContainer<Phase>> GetStartPhase(@Path("name") String name);
}

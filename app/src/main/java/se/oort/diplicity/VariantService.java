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
        /** Who the version was created by (or the empty string if no creator information is known). */
        String CreatedBy;
        /** Version of the variant (or the empty string if no version information is known). */
        String Version;
        /** A short description summarising the variant. */
        String Description;
        /** The rules of the variant (in particular where they differ from classical). */
        String Rules;
    }
    class Phase implements Serializable {
    }
    @GET("/Variants")
    Observable<MultiContainer<Variant>> GetVariants();

    @GET("/Variant/{name}/Start")
    Observable<SingleContainer<Phase>> GetStartPhase(@Path("name") String name);
}

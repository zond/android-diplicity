package se.oort.diplicity;

import java.io.Serializable;
import java.util.List;

import okhttp3.internal.framed.Variant;
import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Observable;
import se.oort.diplicity.apigen.Link;
import se.oort.diplicity.apigen.MultiContainer;
import se.oort.diplicity.apigen.SingleContainer;

public interface VariantService {
    public static class Variant implements Serializable {
        String Name;
        List<Link> Links;
    }
    public static class Phase implements Serializable {
        List<Link> Links;
    }
    @GET("/Variants")
    Observable<MultiContainer<Variant>> GetVariants();

    @GET("/Variant/{name}/Start")
    Observable<SingleContainer<Phase>> GetStartPhase(@Path("name") String name);
}

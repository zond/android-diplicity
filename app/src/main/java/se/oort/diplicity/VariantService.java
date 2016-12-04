package se.oort.diplicity;

import java.io.Serializable;

import okhttp3.internal.framed.Variant;
import retrofit2.http.GET;
import rx.Observable;
import se.oort.diplicity.apigen.MultiContainer;

public interface VariantService {
    public static class Variant implements Serializable {
        String Name;
    }
    @GET("/Variants")
    Observable<MultiContainer<Variant>> GetVariants();
}

package se.oort.diplicity;

import retrofit2.http.GET;
import rx.Observable;
import se.oort.diplicity.apigen.User;

public interface RootService {
    public static class Properties {
        public User User;
    }
    public static class Root {
        public Properties Properties;
    }
    @GET("/")
    Observable<Root> GetRoot();
}

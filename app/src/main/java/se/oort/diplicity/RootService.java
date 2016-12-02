package se.oort.diplicity;

import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import rx.Observable;
import se.oort.diplicity.apigen.Member;
import se.oort.diplicity.apigen.SingleContainer;
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

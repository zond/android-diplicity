package se.oort.diplicity;

import java.io.Serializable;
import java.util.List;

import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Observable;
import se.oort.diplicity.apigen.MultiContainer;

public interface ChannelService {
    class NMessagesSince implements Serializable {
        public java.util.Date Since;
        public int NMessages;
    }
    class Channel implements Serializable {
        public String GameID;
        public List<String> Members;
        public int NMessages;
        public NMessagesSince NMessagesSince;
    }

    @GET("/Game/{game_id}/Channels")
    Observable<MultiContainer<Channel>> ListChannels(@Path("game_id") String gameID);
}

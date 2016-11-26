import retrofit2.http.*;
import rx.*;
	
public interface FlaggedMessagesService {
  @GET("/FlaggedMessages")
  Observable<FlaggedMessagessContainer> ListFlaggedMessages();

}
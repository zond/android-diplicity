package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
import rx.*;
	
public interface AllocationService {
  @POST("/Allocation")
  Observable<SingleContainer<Allocation>> AllocationCreate(@Body Allocation allocation);

}
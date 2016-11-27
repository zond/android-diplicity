package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
import rx.*;
	
public interface OrderService {
  @POST("/Game/{game_id}/Phase/{phase_ordinal}/Order")
  Observable<OrderContainer> OrderCreate(@Path("game_id") String game_id, @Path("phase_ordinal") String phase_ordinal, @Body Order order);

  @PUT("/Game/{game_id}/Phase/{phase_ordinal}/Order/{src_province}")
  Observable<OrderContainer> OrderUpdate(@Path("game_id") String game_id, @Path("phase_ordinal") String phase_ordinal, @Path("src_province") String src_province);

  @DELETE("/Game/{game_id}/Phase/{phase_ordinal}/Order/{src_province}")
  Observable<OrderContainer> OrderDelete(@Path("game_id") String game_id, @Path("phase_ordinal") String phase_ordinal, @Path("src_province") String src_province);

  @GET("/Game/{game_id}/Phase/{phase_ordinal}/Orders")
  Observable<OrdersContainer> ListOrders(@Path("game_id") String game_id, @Path("phase_ordinal") String phase_ordinal);

}
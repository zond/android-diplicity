package se.oort.diplicity.apigen;
	
import retrofit2.http.*;
import rx.*;
	
public interface GameService {
  @POST("/Game")
  Observable<SingleContainer<Game>> GameCreate(@Body Game game);

  @GET("/Game/{id}")
  Observable<SingleContainer<Game>> GameLoad(@Path("id") String id);

  @GET("/Games/Open")
  Observable<MultiContainer<Game>> ListOpenGames(@Query("cursor") String cursor, @Query("limit") String limit, @Query("variant") String variant, @Query("min-reliability") String min_reliability, @Query("min-quickness") String min_quickness, @Query("max-hater") String max_hater, @Query("max-hated") String max_hated, @Query("min-rating") String min_rating, @Query("max-rating") String max_rating, @Query("only-private") String only_private, @Query("nation-allocation") String nation_allocation, @Query("phase-length-minutes") String phase_length_minutes, @Query("conference-chat-disabled") String conference_chat_disabled, @Query("group-chat-disabled") String group_chat_disabled, @Query("private-chat-disabled") String private_chat_disabled);

  @GET("/Games/Started")
  Observable<MultiContainer<Game>> ListStartedGames(@Query("cursor") String cursor, @Query("limit") String limit, @Query("variant") String variant, @Query("min-reliability") String min_reliability, @Query("min-quickness") String min_quickness, @Query("max-hater") String max_hater, @Query("max-hated") String max_hated, @Query("min-rating") String min_rating, @Query("max-rating") String max_rating, @Query("only-private") String only_private, @Query("nation-allocation") String nation_allocation, @Query("phase-length-minutes") String phase_length_minutes, @Query("conference-chat-disabled") String conference_chat_disabled, @Query("group-chat-disabled") String group_chat_disabled, @Query("private-chat-disabled") String private_chat_disabled);

  @GET("/Games/Finished")
  Observable<MultiContainer<Game>> ListFinishedGames(@Query("cursor") String cursor, @Query("limit") String limit, @Query("variant") String variant, @Query("min-reliability") String min_reliability, @Query("min-quickness") String min_quickness, @Query("max-hater") String max_hater, @Query("max-hated") String max_hated, @Query("min-rating") String min_rating, @Query("max-rating") String max_rating, @Query("only-private") String only_private, @Query("nation-allocation") String nation_allocation, @Query("phase-length-minutes") String phase_length_minutes, @Query("conference-chat-disabled") String conference_chat_disabled, @Query("group-chat-disabled") String group_chat_disabled, @Query("private-chat-disabled") String private_chat_disabled);

  @GET("/Games/My/Staging")
  Observable<MultiContainer<Game>> ListMyStagingGames(@Query("cursor") String cursor, @Query("limit") String limit, @Query("variant") String variant, @Query("min-reliability") String min_reliability, @Query("min-quickness") String min_quickness, @Query("max-hater") String max_hater, @Query("max-hated") String max_hated, @Query("min-rating") String min_rating, @Query("max-rating") String max_rating, @Query("only-private") String only_private, @Query("nation-allocation") String nation_allocation, @Query("phase-length-minutes") String phase_length_minutes, @Query("conference-chat-disabled") String conference_chat_disabled, @Query("group-chat-disabled") String group_chat_disabled, @Query("private-chat-disabled") String private_chat_disabled);

  @GET("/Games/My/Started")
  Observable<MultiContainer<Game>> ListMyStartedGames(@Query("cursor") String cursor, @Query("limit") String limit, @Query("variant") String variant, @Query("min-reliability") String min_reliability, @Query("min-quickness") String min_quickness, @Query("max-hater") String max_hater, @Query("max-hated") String max_hated, @Query("min-rating") String min_rating, @Query("max-rating") String max_rating, @Query("only-private") String only_private, @Query("nation-allocation") String nation_allocation, @Query("phase-length-minutes") String phase_length_minutes, @Query("conference-chat-disabled") String conference_chat_disabled, @Query("group-chat-disabled") String group_chat_disabled, @Query("private-chat-disabled") String private_chat_disabled);

  @GET("/Games/My/Finished")
  Observable<MultiContainer<Game>> ListMyFinishedGames(@Query("cursor") String cursor, @Query("limit") String limit, @Query("variant") String variant, @Query("min-reliability") String min_reliability, @Query("min-quickness") String min_quickness, @Query("max-hater") String max_hater, @Query("max-hated") String max_hated, @Query("min-rating") String min_rating, @Query("max-rating") String max_rating, @Query("only-private") String only_private, @Query("nation-allocation") String nation_allocation, @Query("phase-length-minutes") String phase_length_minutes, @Query("conference-chat-disabled") String conference_chat_disabled, @Query("group-chat-disabled") String group_chat_disabled, @Query("private-chat-disabled") String private_chat_disabled);

  @GET("/Games/{user_id}/Staging")
  Observable<MultiContainer<Game>> ListOtherStagingGames(@Path("user_id") String user_id, @Query("cursor") String cursor, @Query("limit") String limit, @Query("variant") String variant, @Query("min-reliability") String min_reliability, @Query("min-quickness") String min_quickness, @Query("max-hater") String max_hater, @Query("max-hated") String max_hated, @Query("min-rating") String min_rating, @Query("max-rating") String max_rating, @Query("only-private") String only_private, @Query("nation-allocation") String nation_allocation, @Query("phase-length-minutes") String phase_length_minutes, @Query("conference-chat-disabled") String conference_chat_disabled, @Query("group-chat-disabled") String group_chat_disabled, @Query("private-chat-disabled") String private_chat_disabled);

  @GET("/Games/{user_id}/Started")
  Observable<MultiContainer<Game>> ListOtherStartedGames(@Path("user_id") String user_id, @Query("cursor") String cursor, @Query("limit") String limit, @Query("variant") String variant, @Query("min-reliability") String min_reliability, @Query("min-quickness") String min_quickness, @Query("max-hater") String max_hater, @Query("max-hated") String max_hated, @Query("min-rating") String min_rating, @Query("max-rating") String max_rating, @Query("only-private") String only_private, @Query("nation-allocation") String nation_allocation, @Query("phase-length-minutes") String phase_length_minutes, @Query("conference-chat-disabled") String conference_chat_disabled, @Query("group-chat-disabled") String group_chat_disabled, @Query("private-chat-disabled") String private_chat_disabled);

  @GET("/Games/{user_id}/Finished")
  Observable<MultiContainer<Game>> ListOtherFinishedGames(@Path("user_id") String user_id, @Query("cursor") String cursor, @Query("limit") String limit, @Query("variant") String variant, @Query("min-reliability") String min_reliability, @Query("min-quickness") String min_quickness, @Query("max-hater") String max_hater, @Query("max-hated") String max_hated, @Query("min-rating") String min_rating, @Query("max-rating") String max_rating, @Query("only-private") String only_private, @Query("nation-allocation") String nation_allocation, @Query("phase-length-minutes") String phase_length_minutes, @Query("conference-chat-disabled") String conference_chat_disabled, @Query("group-chat-disabled") String group_chat_disabled, @Query("private-chat-disabled") String private_chat_disabled);

}
package tech.id.runappsandroid;


import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DirectionsApiService {
    @GET("maps/api/directions/json")
    Call<DirectionsResponse> getDirection(
            @Query("origin") String origin,
            @Query("destination") String destination,
            @Query("key") String apiKey,
            @Query("alternatives") boolean alternatives
    );
}


//import retrofit2.Call;
//import retrofit2.http.GET;
//import retrofit2.http.Query;
//
//public interface DirectionsApiService {
//    @GET("maps/api/directions/json")
//    Call<DirectionsResponse> getDirection(
//            @Query("origin") String origin,
//            @Query("destination") String destination,
//            @Query("key") String apiKey
//    );
//}

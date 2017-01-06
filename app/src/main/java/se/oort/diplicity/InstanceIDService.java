package se.oort.diplicity;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class InstanceIDService extends FirebaseInstanceIdService {

    private interface FCMValueService {
        public static class FCMValue {
            String Value;
        }
        @PUT("/User/{user_id}/FCMToken/{replace_token}/Replace")
        Observable<Object> FCMValueUpdate(@Body FCMValue fcmValue, @Path("user_id") String user_id, @Path("replace_token") String replace_token);
    }

    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d("Diplicity", "Got new FCM token " + refreshedToken);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String replaceToken = prefs.getString(RetrofitActivity.FCM_REPLACE_TOKEN_PREF_KEY, "");
        if (replaceToken.equals("")) {
            Log.d("Diplicity", "No replace token configured, ignoring");
            return;
        }

        String baseURL = prefs.getString(RetrofitActivity.API_URL_PREF_KEY, "");
        if (baseURL.equals("")) {
            Log.d("Diplicity", "No base URL configured, ignoring");
            return;
        }

        String userId = prefs.getString(RetrofitActivity.USER_ID_PREF_KEY, "");
        if (userId.equals("")) {
            Log.d("Diplicity", "No user id configured, ignoring");
            return;
        }

        FCMValueService.FCMValue fcmValue = new FCMValueService.FCMValue();
        fcmValue.Value = refreshedToken;
        RxJavaCallAdapterFactory rxAdapter = RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io());
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(rxAdapter)
                .build();
        FCMValueService fcmValueService = retrofit.create(FCMValueService.class);
        fcmValueService.FCMValueUpdate(fcmValue, userId, replaceToken).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<Object>() {
                    @Override
                    public void onCompleted() {
                        Log.d("Diplicity", "Successfully updated FCM token on server");
                    }

                    @Override
                    public void onError(Throwable e) {
                        App.firebaseCrashReport("Failed updating FCM token on server", e);
                    }

                    @Override
                    public void onNext(Object o) {
                    }
                });
    }
}

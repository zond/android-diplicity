package se.oort.diplicity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.HttpException;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import se.oort.diplicity.apigen.GameService;
import se.oort.diplicity.apigen.MemberService;
import se.oort.diplicity.apigen.MultiContainer;
import se.oort.diplicity.apigen.User;
import se.oort.diplicity.apigen.UserStatsService;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.T;

public abstract class RetrofitActivity extends AppCompatActivity {

    static final int LOGIN_REQUEST = 1;
    static final String API_URL_KEY = "api_url";
    static final String DEFAULT_URL = "https://diplicity-engine.appspot.com";
    static final String LOGGED_IN_USER_KEY = "logged_in_user";

    AuthenticatingCallAdapterFactory adapterFactory;
    Retrofit retrofit;

    public User loggedInUser;

    public GameService gameService;
    public UserStatsService userStatsService;
    public MemberService memberService;
    public RootService rootService;

    private SharedPreferences prefs;
    private SharedPreferences.OnSharedPreferenceChangeListener prefsListener;

    private class LoginSubscriber<R> {
        private Subscriber<? super R> subscriber;
        private Observable.OnSubscribe<R> onSubscribe;
        public LoginSubscriber(Subscriber<? super R> subscriber, Observable.OnSubscribe<R> onSubscribe) {
            this.subscriber = subscriber;
            this.onSubscribe = onSubscribe;
        }
        public void retry() {
            this.onSubscribe.call(this.subscriber);
        }
    }
    private static List<LoginSubscriber<?>> loginSubscribers = Collections.synchronizedList(new ArrayList<LoginSubscriber<?>>());

    public static Serializable unserialize(byte[] b) {
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        Serializable o;
        try {
            bis = new ByteArrayInputStream(b);
            ois = new ObjectInputStream(bis);
            o = (Serializable) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                ois.close();
                bis.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return o;
    }

    public static byte[] serialize(Serializable o) {
        ObjectOutputStream objectOutputStream = null;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            objectOutputStream = new ObjectOutputStream(bout);
            objectOutputStream.writeObject(o);
            objectOutputStream.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                bout.close();
                objectOutputStream.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return bout.toByteArray();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LOGIN_REQUEST) {
            if (resultCode == RESULT_OK) {
                final ProgressDialog progress = new ProgressDialog(this);
                progress.setTitle(getResources().getString(R.string.logging_in));
                progress.setCancelable(true);
                progress.show();

                rootService.GetRoot()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Subscriber<RootService.Root>() {
                            @Override
                            public void onCompleted() {
                                progress.dismiss();
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.e("Diplicity", "Error loading user: " + e);
                                Toast.makeText(RetrofitActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();
                                progress.dismiss();
                            }

                            @Override
                            public void onNext(RootService.Root root) {
                                loggedInUser = root.Properties.User;
                                List<LoginSubscriber<?>> subscribersCopy;
                                synchronized (loginSubscribers) {
                                    subscribersCopy = new ArrayList<LoginSubscriber<?>>(loginSubscribers);
                                    loginSubscribers.clear();
                                }
                                for (LoginSubscriber<?> subscriber : subscribersCopy) {
                                    subscriber.retry();
                                }
                            }
                        });
            }
        }
    }

    protected void setBaseURL(String baseURL) {
        ((App) getApplication()).baseURL = baseURL;
        adapterFactory = new AuthenticatingCallAdapterFactory();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request toIssue = chain.request().newBuilder()
                        .addHeader("Accept", "application/json; charset=UTF-8").build();
                String authToken = ((App) getApplication()).authToken;
                if (authToken != null) {
                    toIssue = toIssue.newBuilder()
                            .addHeader("Authorization", "bearer " + authToken)
                            .build();
                }
                Log.d("Diplicity", "" + chain.request().method() + "ing " + chain.request().url());
                return chain.proceed(toIssue);
            }
        });
        retrofit = new Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(adapterFactory)
                .client(builder.build())
                .build();
        gameService = retrofit.create(GameService.class);
        userStatsService = retrofit.create(UserStatsService.class);
        memberService = retrofit.create(MemberService.class);
        rootService = retrofit.create(RootService.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefsListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                if (s.equals(API_URL_KEY)) {
                    setBaseURL(sharedPreferences.getString(API_URL_KEY, ""));
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(prefsListener);

        String baseURL = prefs.getString(API_URL_KEY, "");
        HttpUrl httpUrl = HttpUrl.parse(baseURL);
        if (httpUrl != null) {
            if (baseURL.lastIndexOf("/") != baseURL.length() - 1) {
                baseURL = baseURL + "/";
            }
            setBaseURL(baseURL);
        } else {
            Log.d("Diplicity", "Malformed URL " + baseURL + ", resetting to default URL " + DEFAULT_URL);
            setBaseURL(DEFAULT_URL);
            prefs.edit().putString(API_URL_KEY, DEFAULT_URL).commit();
        }
    }

    @Override
    public void onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener);
        super.onDestroy();
    }

    private class AuthenticatingCallAdapterFactory extends CallAdapter.Factory {
        RxJavaCallAdapterFactory delegate;
        public AuthenticatingCallAdapterFactory() {
            delegate = RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io());
        }
        @Override
        public CallAdapter<?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
            return new AuthenticatingCallAdapter(delegate.get(returnType, annotations, retrofit));
        }
    }

    private class AuthenticatingCallAdapter implements CallAdapter<Observable<?>> {
        private CallAdapter<Observable<?>> delegate;
        private AuthenticatingCallAdapter(CallAdapter delegate) {
            this.delegate = delegate;
        }
        @Override
        public Type responseType() {
            return delegate.responseType();
        }
        @Override
        public <R> Observable<R> adapt(final Call<R> call) {
            final Observable<R> resultObservable = Observable.create(new Observable.OnSubscribe<R>() {
                @Override
                @SuppressWarnings("unchecked")
                public void call(final Subscriber<? super R> subscriber) {
                    final Observable.OnSubscribe<R> thisOnSubscribe = this;
                    Observable<R> delegateObservable = (Observable<R>) delegate.adapt(call);
                    delegateObservable
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Subscriber<R>() {
                                @Override
                                public void onCompleted() {
                                    subscriber.onCompleted();
                                }

                                @Override
                                public void onError(Throwable e) {
                                    if (e instanceof HttpException) {
                                        HttpException he = (HttpException) e;
                                        if (he.code() == 401) {
                                            synchronized (loginSubscribers) {
                                                loginSubscribers.add(new LoginSubscriber<R>(subscriber, thisOnSubscribe));
                                                if (loginSubscribers.size() == 1) {
                                                    startActivityForResult(new Intent(RetrofitActivity.this, LoginActivity.class), LOGIN_REQUEST);
                                                }
                                            }
                                            return;
                                        }
                                    }
                                    subscriber.onError(e);
                                }

                                @Override
                                public void onNext(R r) {
                                    subscriber.onNext(r);
                                }
                            });
                }
            });
            return resultObservable;
        }
    }
}

package se.oort.diplicity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
import rx.functions.Func2;
import rx.observables.JoinObservable;
import rx.schedulers.Schedulers;
import se.oort.diplicity.apigen.BanService;
import se.oort.diplicity.apigen.GameResultService;
import se.oort.diplicity.apigen.GameService;
import se.oort.diplicity.apigen.GameStateService;
import se.oort.diplicity.apigen.MemberService;
import se.oort.diplicity.apigen.MessageService;
import se.oort.diplicity.apigen.MultiContainer;
import se.oort.diplicity.apigen.OrderService;
import se.oort.diplicity.apigen.PhaseResultService;
import se.oort.diplicity.apigen.PhaseService;
import se.oort.diplicity.apigen.PhaseStateService;
import se.oort.diplicity.apigen.Ticker;
import se.oort.diplicity.apigen.TickerUnserializer;
import se.oort.diplicity.apigen.User;
import se.oort.diplicity.apigen.UserConfigService;
import se.oort.diplicity.apigen.UserStatsService;

public abstract class RetrofitActivity extends AppCompatActivity {

    // start intent for result stuff
    static final int LOGIN_REQUEST = 1;

    // prefs stuff
    static final String API_URL_PREF_KEY = "api_url_pref_key";
    static final String USER_ID_PREF_KEY = "user_id_pref_key";
    static final String LOGGED_IN_USER_PREF_KEY = "logged_in_user_pref_key";
    static final String AUTH_TOKEN_PREF_KEY = "auth_token_pref_key";
    static final String VARIANTS_PREF_KEY = "variants_pref_key";

    // default urls
    static final String DEFAULT_URL = "https://diplicity-engine.appspot.com/";
    static final String LOCAL_DEVELOPMENT_URL = "http://localhost:8080/";

    // services
    public GameService gameService;
    public UserStatsService userStatsService;
    public MemberService memberService;
    public RootService rootService;
    public VariantService variantService;
    public OptionsService optionsService;
    public OrderService orderService;
    public PhaseService phaseService;
    public ChannelService channelService;
    public MessageService messageService;
    public PhaseResultService phaseResultService;
    public GameResultService gameResultService;
    public PhaseStateService phaseStateService;
    public GameStateService gameStateService;
    public UserConfigService userConfigService;
    public BanService banService;

    // prefs listening (to recreate services when the base URL is updated)
    private SharedPreferences prefs;
    private SharedPreferences.OnSharedPreferenceChangeListener prefsListener;

    // progress dialogs (to not dismiss already removed dialogs)
    private Set<ProgressDialog> progressDialogs = new HashSet<>();

    // picture cache
    private static LRUCache<String,Bitmap> pictureCache = new LRUCache<>(128);

    // async handler for stuff not covered by RXJava (JavaScript in WebView, for example)
    public Handler handler = new Handler();

    // cache for serialized prefs
    private static User loggedInUser = null;
    private static MultiContainer<VariantService.Variant> variants = null;

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
    private final static List<LoginSubscriber<?>> loginSubscribers = Collections.synchronizedList(new ArrayList<LoginSubscriber<?>>());

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

    public Sendable<Throwable> newProgressAndToastHandler(final ErrorHandler onError, final String progressMessage) {
        final ProgressDialog progress = new ProgressDialog(this);
        if (progressMessage != null) {
            if (!progressMessage.equals("")) {
                progress.setTitle(progressMessage);
            }
            progress.setCancelable(true);
            progress.show();
            this.progressDialogs.add(progress);
        }

        return new Sendable<Throwable>() {
            @Override
            public void send(Throwable e) {
                try {
                    if (e != null) {
                        String msg = "Error " + progressMessage;
                        if (e instanceof HttpException) {
                            HttpException he = (HttpException) e;
                            if (onError != null && onError.code == he.code()) {
                                onError.handler.send(he);
                            } else {
                                if (he.code() == 412) {
                                    Toast.makeText(RetrofitActivity.this, R.string.update_your_state, Toast.LENGTH_LONG).show();
                                } else if (he.code() > 399 && he.code() < 500) {
                                    App.firebaseCrashReport(msg, e);
                                    Toast.makeText(RetrofitActivity.this, R.string.client_misbehaved, Toast.LENGTH_SHORT).show();
                                } else if (he.code() > 499) {
                                    App.firebaseCrashReport(msg, e);
                                    Toast.makeText(RetrofitActivity.this, R.string.server_error, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(RetrofitActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            App.firebaseCrashReport(msg, e);
                            Toast.makeText(RetrofitActivity.this, R.string.unknown_error, Toast.LENGTH_SHORT).show();
                        }
                    }
                } finally {
                    if (progressMessage != null) {
                        RetrofitActivity.this.progressDialogs.remove(progress);
                        progress.dismiss();
                    }
                }
            }
        };
    }

    public <T> void observe(Observable<T> observable, @Nullable final Sendable<T> onResult, @Nullable final Sendable<Throwable> onDone) {
        observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<T>() {
                    @Override
                    public void onCompleted() {
                        if (onDone != null) {
                            onDone.send(null);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (onDone != null)
                            onDone.send(e);
                    }

                    @Override
                    public void onNext(T t) {
                        if (onResult != null)
                            onResult.send(t);
                    }
                });
    }

    protected boolean consumeDiplicityJSON(MessagingService.DiplicityJSON diplicityJSON) {
        return false;
    }

    protected void performLogin() {
        observe(JoinObservable.when(JoinObservable
                .from(rootService.GetRoot())
                .and(variantService.GetVariants())
                .then(new Func2<RootService.Root, MultiContainer<VariantService.Variant>, Object>() {
                    @Override
                    public Object call(RootService.Root root, MultiContainer<VariantService.Variant> variants) {
                        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(RetrofitActivity.this).edit();
                        Gson gson = new Gson();
                        prefs.putString(LOGGED_IN_USER_PREF_KEY, gson.toJson(root.Properties.User));
                        loggedInUser = root.Properties.User;
                        prefs.putString(VARIANTS_PREF_KEY, gson.toJson(variants));
                        variants = variants;
                        prefs.apply();
                        List<LoginSubscriber<?>> subscribersCopy;
                        synchronized (loginSubscribers) {
                            subscribersCopy = new ArrayList<LoginSubscriber<?>>(loginSubscribers);
                            loginSubscribers.clear();
                        }
                        for (LoginSubscriber<?> subscriber : subscribersCopy) {
                            subscriber.retry();
                        }
                        return null;
                    }
                })).toObservable(), null, newProgressAndToastHandler(null, getResources().getString(R.string.logging_in)));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LOGIN_REQUEST) {
            if (resultCode == RESULT_OK) {
                performLogin();
            }
        }
    }

    public static class ErrorHandler {
        public int code;
        public Sendable<HttpException> handler;
        public ErrorHandler(int code, Sendable<HttpException> handler) {
            this.code = code;
            this.handler = handler;
        }
    }

    public <T> void handleReq(Observable<T> req, final Sendable<T> handler, final String progressMessage) {
        observe(req, handler, newProgressAndToastHandler(null, progressMessage));
    }

    public <T> void handleReq(Observable<T> req, final Sendable<T> handler, final ErrorHandler onError, final String progressMessage) {
        observe(req, handler, newProgressAndToastHandler(onError, progressMessage));
    }

    public String toString(Double d) {
        return String.format(
                Locale.getDefault(),
                getResources().getString(R.string.float_format),
                d);
    }

    protected void recreateServices() {
        AuthenticatingCallAdapterFactory adapterFactory = new AuthenticatingCallAdapterFactory();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request toIssue = chain.request().newBuilder()
                        .addHeader("Accept", "application/json; charset=UTF-8").build();
                if (getLocalDevelopmentMode() && !getLocalDevelopmentModeFakeID().equals("")) {
                    HttpUrl url = toIssue.url().newBuilder().addQueryParameter("fake-id", getLocalDevelopmentModeFakeID()).build();
                    toIssue = toIssue.newBuilder().url(url).build();
                } else if (!getAuthToken().equals("")){
                    toIssue = toIssue.newBuilder()
                            .addHeader("Authorization", "bearer " + getAuthToken())
                            .build();
                }
                Log.d("Diplicity", "" + toIssue.method() + "ing " + toIssue.url());
                return chain.proceed(toIssue);
            }
        });
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Ticker.class, new TickerUnserializer())
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getBaseURL())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(adapterFactory)
                .client(builder.build())
                .build();

        gameService = retrofit.create(GameService.class);
        userStatsService = retrofit.create(UserStatsService.class);
        memberService = retrofit.create(MemberService.class);
        rootService = retrofit.create(RootService.class);
        variantService = retrofit.create(VariantService.class);
        optionsService = retrofit.create(OptionsService.class);
        orderService = retrofit.create(OrderService.class);
        phaseService = retrofit.create(PhaseService.class);
        channelService = retrofit.create(ChannelService.class);
        messageService = retrofit.create(MessageService.class);
        phaseResultService = retrofit.create(PhaseResultService.class);
        gameResultService = retrofit.create(GameResultService.class);
        phaseStateService = retrofit.create(PhaseStateService.class);
        gameStateService = retrofit.create(GameStateService.class);
        userConfigService = retrofit.create(UserConfigService.class);
        banService = retrofit.create(BanService.class);
    }

    public String getLocalDevelopmentModeFakeID() {
        return prefs.getString(getResources().getString(R.string.local_development_mode_fake_id_pref_key), "");
    }

    public boolean getLocalDevelopmentMode() {
        return prefs.getBoolean(getResources().getString(R.string.local_development_mode_pref_key), false);
    }

    public User getLoggedInUser() {
        if (loggedInUser == null) {
            loggedInUser = new Gson().fromJson(prefs.getString(LOGGED_IN_USER_PREF_KEY, "{}"), User.class);
        }
        return loggedInUser;
    }

    public MultiContainer<VariantService.Variant> getVariants() {
        if (variants == null) {
            variants = new Gson().fromJson(prefs.getString(VARIANTS_PREF_KEY, "[]"), new TypeToken<MultiContainer<VariantService.Variant>>(){}.getType());
        }
        return variants;
    }

    public String getBaseURL() {
        return prefs.getString(API_URL_PREF_KEY, DEFAULT_URL);
    }

    public String getAuthToken() {
        return prefs.getString(AUTH_TOKEN_PREF_KEY, "");
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefsListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                if (s.equals(API_URL_PREF_KEY)) {
                    recreateServices();
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(prefsListener);

        recreateServices();
    }

    @Override
    public void onDestroy() {
        for (ProgressDialog dialog : progressDialogs) {
            dialog.dismiss();
        }
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener);
        super.onDestroy();
    }

    public void populateImage(final ImageView view, final String u, final int width, final int height) {
        Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(final Subscriber<? super Bitmap> subscriber) {
                Bitmap bmp = pictureCache.get(u);
                if (bmp == null) {
                    try {
                        URL url = new URL(u);
                        bmp = ThumbnailUtils.extractThumbnail(
                                BitmapFactory.decodeStream(url.openConnection().getInputStream()),
                                width, height);
                        pictureCache.put(u, bmp);
                    } catch(IOException e) {
                        subscriber.onError(e);
                        return;
                    }
                }
                subscriber.onNext(bmp);
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Bitmap>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d("Diplicity", "Error loading " + u, e);
                        view.setImageDrawable(getResources().getDrawable(R.drawable.broken_image));
                    }

                    @Override
                    public void onNext(Bitmap bitmap) {
                        view.setImageBitmap(bitmap);
                    }
                });

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
            return Observable.create(new Observable.OnSubscribe<R>() {
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
                                                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RetrofitActivity.this);
                                                    if (getLocalDevelopmentMode() &&
                                                            !getLocalDevelopmentModeFakeID().equals("")) {
                                                        Log.d("Diplicity", "Performing fake login as " + getLocalDevelopmentModeFakeID());
                                                        performLogin();
                                                    } else {
                                                        startActivityForResult(new Intent(RetrofitActivity.this, LoginActivity.class), LOGIN_REQUEST);
                                                    }
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
        }
    }
}

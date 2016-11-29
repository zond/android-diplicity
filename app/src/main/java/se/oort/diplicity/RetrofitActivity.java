package se.oort.diplicity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import retrofit2.http.HTTP;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import se.oort.diplicity.apigen.GameService;

public abstract class RetrofitActivity extends AppCompatActivity {

    static final int LOGIN_REQUEST = 1;

    AuthenticatingCallAdapterFactory adapterFactory;
    Retrofit retrofit;
    GameService gameService;

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
    private List<LoginSubscriber<?>> loginSubscribers = Collections.synchronizedList(new ArrayList<LoginSubscriber<?>>());

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LOGIN_REQUEST) {
            if (resultCode == RESULT_OK) {
                List<LoginSubscriber<?>> subscribersCopy;
                synchronized (loginSubscribers) {
                    subscribersCopy = new ArrayList<LoginSubscriber<?>>(loginSubscribers);
                    loginSubscribers.clear();
                }
                for (LoginSubscriber<?> subscriber : subscribersCopy) {
                    subscriber.retry();
                }
            }
        }
    }

    public RetrofitActivity() {
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
                return chain.proceed(toIssue);
            }
        });
        retrofit = new Retrofit.Builder()
                .baseUrl("https://diplicity-engine.appspot.com")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(adapterFactory)
                .client(builder.build())
                .build();
        gameService = retrofit.create(GameService.class);
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
                                            loginSubscribers.add(new LoginSubscriber<R>(subscriber, thisOnSubscribe));
                                            startActivityForResult(new Intent(RetrofitActivity.this, LoginActivity.class), LOGIN_REQUEST);
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

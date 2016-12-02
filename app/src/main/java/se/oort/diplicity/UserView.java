package se.oort.diplicity;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.net.URL;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import se.oort.diplicity.apigen.User;

public class UserView extends FrameLayout {
    public UserView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        addView(inflater.inflate(R.layout.user_view, null));
    }
    public void setUser(final User user) {
        Log.d("Diplicity", "setting user to " + user.Name);
        ((TextView) findViewById(R.id.name)).setText(user.Name);
        final ImageView avatar = (ImageView) findViewById(R.id.avatar);
        Log.d("Diplicity", "Subscribing to " + user.Picture);
        Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(final Subscriber<? super Bitmap> subscriber) {
                try {
                    URL url = new URL(user.Picture);
                    subscriber.onNext(ThumbnailUtils.extractThumbnail(
                            BitmapFactory.decodeStream(url.openConnection().getInputStream()),
                            avatar.getWidth(), avatar.getHeight()));
                    subscriber.onCompleted();
                } catch(IOException e) {
                    subscriber.onError(e);
                }

            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Bitmap>() {
                    @Override
                    public void onCompleted() {
                        Log.d("Diplicity", "Finished loading " + user.Picture);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d("Diplicity", "Error loading " + user.Picture + ": "  + e);
                        avatar.setImageDrawable(getContext().getResources().getDrawable(R.drawable.broken_image));
                    }

                    @Override
                    public void onNext(Bitmap bitmap) {
                        avatar.setImageBitmap(bitmap);
                    }
                });
    }
}

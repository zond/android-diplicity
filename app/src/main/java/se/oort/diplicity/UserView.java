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
    private void inflate() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        addView(inflater.inflate(R.layout.user_view, null));
    }
    public UserView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate();
    }
    public void setUser(RetrofitActivity retrofitActivity, User user) {
        ((TextView) findViewById(R.id.name)).setText(user.Name);
        retrofitActivity.populateImage((ImageView) findViewById(R.id.avatar), user.Picture);
    }
}

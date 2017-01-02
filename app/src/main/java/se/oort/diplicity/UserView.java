package se.oort.diplicity;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.net.URL;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import se.oort.diplicity.apigen.SingleContainer;
import se.oort.diplicity.apigen.User;
import se.oort.diplicity.apigen.UserStats;

public class UserView extends FrameLayout {
    private void inflate() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        addView(inflater.inflate(R.layout.user_view, null));
    }
    public UserView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate();
    }
    public static OnClickListener getAvatarClickListener(final RetrofitActivity retrofitActivity, final User user) {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                retrofitActivity.handleReq(
                        retrofitActivity.userStatsService.UserStatsLoad(user.Id),
                        new Sendable<SingleContainer<UserStats>>() {
                            @Override
                            public void send(SingleContainer<UserStats> userStatsSingleContainer) {
                                final AlertDialog dialog = new AlertDialog.Builder(retrofitActivity).setView(R.layout.user_dialog).show();
                                ((UserStatsTable) dialog.findViewById(R.id.user_stats)).setUserStats(retrofitActivity, userStatsSingleContainer.Properties);
                                ((UserView) dialog.findViewById(R.id.user)).setUser(retrofitActivity, user);
                            }
                        }, retrofitActivity.getResources().getString(R.string.loading_user_stats));
            }
        };
    }
    public void setUser(RetrofitActivity retrofitActivity, User user) {
        ((TextView) findViewById(R.id.name)).setText(user.Name);
        ImageView avatar = (ImageView) findViewById(R.id.avatar);
        retrofitActivity.populateImage(avatar, user.Picture);
        avatar.setOnClickListener(getAvatarClickListener(retrofitActivity, user));
    }
}

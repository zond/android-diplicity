package se.oort.diplicity;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func2;
import rx.joins.JoinObserver;
import rx.observables.JoinObservable;
import rx.schedulers.Schedulers;
import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.GameState;
import se.oort.diplicity.apigen.Member;
import se.oort.diplicity.apigen.MultiContainer;
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

    private static void setupUserStats(RetrofitActivity retrofitActivity, AlertDialog dialog, UserStats userStats) {
        ((UserStatsTable) dialog.findViewById(R.id.user_stats)).setUserStats(retrofitActivity, userStats);
        ((UserView) dialog.findViewById(R.id.user)).setUser(retrofitActivity, userStats.User);
        dialog.findViewById(R.id.muted).setVisibility(GONE);
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
                                AlertDialog dialog = new AlertDialog.Builder(retrofitActivity).setView(R.layout.user_dialog).show();
                                setupUserStats(retrofitActivity, dialog, userStatsSingleContainer.Properties);
                            }
                        }, retrofitActivity.getResources().getString(R.string.loading_user_stats));
            }
        };
    }

    public static OnClickListener getAvatarClickListener(final RetrofitActivity retrofitActivity, final Game game, final Member member, final User user) {
        if (game == null) {
            return getAvatarClickListener(retrofitActivity, user);
        }
        Member me = null;
        for (Member m : game.Members) {
            if (m.User.Id.equals(retrofitActivity.getLoggedInUser().Id)) {
                me = m;
                break;
            }
        }
        if (me == null || me.Nation.equals(member.Nation)) {
            return getAvatarClickListener(retrofitActivity, user);
        }
        final Member finalMe = me;
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                retrofitActivity.handleReq(
                        JoinObservable.when(JoinObservable
                                .from(retrofitActivity.userStatsService.UserStatsLoad(user.Id))
                                .and(retrofitActivity.gameStateService.GameStateLoad(game.ID, finalMe.Nation))
                                .then(new Func2<SingleContainer<UserStats>, SingleContainer<GameState>, Object>() {
                                    @Override
                                    public Object call(SingleContainer<UserStats> userStatsSingleContainer, final SingleContainer<GameState> gameStateSingleContainer) {
                                        AlertDialog dialog = new AlertDialog.Builder(retrofitActivity).setView(R.layout.user_dialog).show();
                                        setupUserStats(retrofitActivity, dialog, userStatsSingleContainer.Properties);
                                        final CheckBox mutedCheckBox = (CheckBox) dialog.findViewById(R.id.muted);
                                        mutedCheckBox.setVisibility(VISIBLE);
                                        mutedCheckBox.setChecked(gameStateSingleContainer.Properties.Muted != null && gameStateSingleContainer.Properties.Muted.contains(member.Nation));
                                        mutedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                            @Override
                                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                if (isChecked && (gameStateSingleContainer.Properties.Muted == null || !gameStateSingleContainer.Properties.Muted.contains(member.Nation))) {
                                                    if (gameStateSingleContainer.Properties.Muted == null) {
                                                        gameStateSingleContainer.Properties.Muted = new ArrayList<String>();
                                                    }
                                                    gameStateSingleContainer.Properties.Muted.add(member.Nation);
                                                } else if (!isChecked && gameStateSingleContainer.Properties.Muted != null && gameStateSingleContainer.Properties.Muted.contains(member.Nation)) {
                                                    gameStateSingleContainer.Properties.Muted.remove(member.Nation);
                                                }
                                                retrofitActivity.handleReq(
                                                        retrofitActivity.gameStateService.GameStateUpdate(gameStateSingleContainer.Properties, game.ID, finalMe.Nation),
                                                        new Sendable<SingleContainer<GameState>>() {
                                                            @Override
                                                            public void send(SingleContainer<GameState> gameStateSingleContainer) {

                                                            }
                                                        }, retrofitActivity.getResources().getString(R.string.updating));
                                            }
                                        });
                                        return null;
                                    }
                                })).toObservable(),
                        new Sendable<Object>() {
                            @Override
                            public void send(Object o) {}
                        }, retrofitActivity.getResources().getString(R.string.loading_user_stats));
            }
        };
    }

    public void setUser(RetrofitActivity retrofitActivity, User user) {
        setMember(retrofitActivity, null, null, user);
    }

    public void setMember(RetrofitActivity retrofitActivity, Game game, Member member, User user) {
        ((TextView) findViewById(R.id.name)).setText(user.Name);
        ImageView avatar = (ImageView) findViewById(R.id.avatar);
        retrofitActivity.populateImage(avatar, user.Picture);
        avatar.setOnClickListener(getAvatarClickListener(retrofitActivity, game, member, user));
    }
}
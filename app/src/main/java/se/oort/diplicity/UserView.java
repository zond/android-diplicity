package se.oort.diplicity;


import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.HttpException;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.observables.JoinObservable;
import se.oort.diplicity.apigen.Ban;
import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.GameState;
import se.oort.diplicity.apigen.Member;
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

    private static void setupUserDialog(final RetrofitActivity retrofitActivity, AlertDialog dialog, final SingleContainer<UserStats> userStats, final SingleContainer<Ban> ban) {
        ((UserStatsTable) dialog.findViewById(R.id.user_stats)).setUserStats(retrofitActivity, userStats.Properties);
        ((UserView) dialog.findViewById(R.id.user)).setUser(retrofitActivity, userStats.Properties.User, true);
        final CheckBox bannedCheckBox = (CheckBox) dialog.findViewById(R.id.banned);
        if (userStats.Properties.UserId.equals(retrofitActivity.getLoggedInUser().Id)) {
            bannedCheckBox.setVisibility(GONE);
        } else {
            bannedCheckBox.setVisibility(VISIBLE);
            bannedCheckBox.setChecked(ban != null);
            bannedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        Ban ban = new Ban();
                        ban.UserIds = new ArrayList<String>();
                        ban.UserIds.add(userStats.Properties.UserId);
                        ban.UserIds.add(retrofitActivity.getLoggedInUser().Id);
                        retrofitActivity.handleReq(
                                retrofitActivity.banService.BanCreate(ban, retrofitActivity.getLoggedInUser().Id),
                                new Sendable<SingleContainer<Ban>>() {
                                    @Override
                                    public void send(SingleContainer<Ban> banSingleContainer) {
                                    }
                                }, retrofitActivity.getResources().getString(R.string.updating));
                    } else {
                        retrofitActivity.handleReq(
                                retrofitActivity.banService.BanDelete(retrofitActivity.getLoggedInUser().Id, userStats.Properties.UserId),
                                new Sendable<SingleContainer<Ban>>() {
                                    @Override
                                    public void send(SingleContainer<Ban> banSingleContainer) {

                                    }
                                }, retrofitActivity.getResources().getString(R.string.updating));
                    }
                }
            });
        }
        dialog.findViewById(R.id.muted).setVisibility(GONE);
        ((Button) dialog.findViewById(R.id.other_finished_game_button)).setOnClickListener(getOtherGamesClickListener(retrofitActivity, userStats.Properties.User, MainActivity.FINISHED));
        ((Button) dialog.findViewById(R.id.other_staging_game_button)).setOnClickListener(getOtherGamesClickListener(retrofitActivity, userStats.Properties.User, MainActivity.STAGING));
        ((Button) dialog.findViewById(R.id.other_started_game_button)).setOnClickListener(getOtherGamesClickListener(retrofitActivity, userStats.Properties.User, MainActivity.STARTED));
    }

    private static OnClickListener getOtherGamesClickListener(final RetrofitActivity retrofitActivity, final User user, final String state) {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(retrofitActivity, MainActivity.class);
                intent.setAction(MainActivity.ACTION_VIEW_USER_GAMES);
                intent.putExtra(MainActivity.GAME_STATE_KEY, state);
                intent.putExtra(MainActivity.SERIALIZED_USER_KEY, RetrofitActivity.serialize(user));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                retrofitActivity.startActivity(intent);
            }
        };
    }

    public static OnClickListener getAvatarClickListener(final RetrofitActivity retrofitActivity, final User user) {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                retrofitActivity.handleReq(
                        JoinObservable.when(JoinObservable
                                .from(retrofitActivity.userStatsService.UserStatsLoad(user.Id))
                                .and(retrofitActivity.banService.BanLoad(retrofitActivity.getLoggedInUser().Id, user.Id).onErrorReturn(new Func1<Throwable, SingleContainer<Ban>>() {
                                    @Override
                                    public SingleContainer<Ban> call(Throwable throwable) {
                                        if (throwable instanceof HttpException) {
                                            HttpException he = (HttpException) throwable;
                                            if (he.code() == 404) {
                                                return null;
                                            }
                                        }
                                        throw new RuntimeException(throwable);
                                    }
                                }))
                                .then(new Func2<SingleContainer<UserStats>, SingleContainer<Ban>, Object>() {
                                    @Override
                                    public Object call(SingleContainer<UserStats> userStatsSingleContainer, SingleContainer<Ban> banSingleContainer) {
                                        AlertDialog dialog = new AlertDialog.Builder(retrofitActivity).setView(R.layout.user_dialog).show();
                                        setupUserDialog(retrofitActivity, dialog, userStatsSingleContainer, banSingleContainer);
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

    public static OnClickListener getAvatarClickListener(final RetrofitActivity retrofitActivity, final Game game, final Member member, final User user) {
        if (game == null) {
            return getAvatarClickListener(retrofitActivity, user);
        }
        Member me = retrofitActivity.getLoggedInMember(game);
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
                                .and(retrofitActivity.banService.BanLoad(retrofitActivity.getLoggedInUser().Id, user.Id).onErrorReturn(new Func1<Throwable, SingleContainer<Ban>>() {
                                    @Override
                                    public SingleContainer<Ban> call(Throwable throwable) {
                                        if (throwable instanceof HttpException) {
                                            HttpException he = (HttpException) throwable;
                                            if (he.code() == 404) {
                                                return null;
                                            }
                                        }
                                        throw new RuntimeException(throwable);
                                    }
                                }))
                                .then(new Func3<SingleContainer<UserStats>, SingleContainer<GameState>, SingleContainer<Ban>, Object>() {
                                    @Override
                                    public Object call(final SingleContainer<UserStats> userStatsSingleContainer, final SingleContainer<GameState> gameStateSingleContainer, final SingleContainer<Ban> banSingleContainer) {
                                        AlertDialog dialog = new AlertDialog.Builder(retrofitActivity).setView(R.layout.user_dialog).show();
                                        setupUserDialog(retrofitActivity, dialog, userStatsSingleContainer, banSingleContainer);
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

    public void setUser(RetrofitActivity retrofitActivity, User user, boolean withName) {
        setMember(retrofitActivity, null, null, user, withName);
    }

    public void setMember(RetrofitActivity retrofitActivity, Game game, Member member, User user, boolean withName) {
        if (withName) {
            ((TextView) findViewById(R.id.name)).setText(user.Name);
            findViewById(R.id.name).setVisibility(VISIBLE);
        } else {
            findViewById(R.id.name).setVisibility(GONE);
        }
        ImageView avatar = (ImageView) findViewById(R.id.avatar);
        retrofitActivity.populateImage(avatar, user.Picture, 36, 36);
        avatar.setOnClickListener(getAvatarClickListener(retrofitActivity, game, member, user));
    }
}
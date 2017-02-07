package se.oort.diplicity;

import android.app.TaskStackBuilder;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.crash.FirebaseCrash;

import rx.joins.JoinObserver;
import se.oort.diplicity.apigen.FCMNotificationConfig;
import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.Member;
import se.oort.diplicity.apigen.SingleContainer;
import se.oort.diplicity.game.GameActivity;
import se.oort.diplicity.game.PressActivity;

public class FCMReceiver extends RetrofitActivity {

    public static final String DIPLICITY_JSON_EXTRA = "DiplicityJSON";

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        final MessagingService.DiplicityJSON message = MessagingService.decodeDataPayload(getIntent().getExtras().getString(DIPLICITY_JSON_EXTRA));
        if (message.type.equals("message")) {
            handleReq(
                    gameService.GameLoad(message.message.GameID),
                    new Sendable<SingleContainer<Game>>() {
                        @Override
                        public void send(SingleContainer<Game> gameSingleContainer) {
                                Member member = null;
                                for (Member m: gameSingleContainer.Properties.Members) {
                                    if (m.User.Id.equals(getLoggedInUser().Id)) {
                                        member = m;
                                        break;
                                    }
                                }
                                if (member != null) {
                                    Intent mainIntent = new Intent(FCMReceiver.this, MainActivity.class);
                                    Intent gameIntent = GameActivity.startGameIntent(FCMReceiver.this, gameSingleContainer.Properties, gameSingleContainer.Properties.NewestPhaseMeta.get(0));
                                    ChannelService.Channel channel = new ChannelService.Channel();
                                    channel.GameID = message.message.GameID;
                                    channel.Members = message.message.ChannelMembers;
                                    Intent pressIntent = PressActivity.startPressIntent(FCMReceiver.this, gameSingleContainer.Properties, channel, member);
                                    TaskStackBuilder.create(FCMReceiver.this)
                                            .addNextIntent(mainIntent)
                                            .addNextIntent(gameIntent)
                                            .addNextIntent(pressIntent).startActivities();
                                }
                                finish();

                        }
                    }, getResources().getString(R.string.loading_state));
        } else if (message.type.equals("phase")) {
            handleReq(
                    gameService.GameLoad(message.gameID),
                    new Sendable<SingleContainer<Game>>() {
                        @Override
                        public void send(SingleContainer<Game> gameSingleContainer) {
                            Intent mainIntent = new Intent(FCMReceiver.this, MainActivity.class);
                            Intent gameIntent;
                            if (gameSingleContainer.Properties.NewestPhaseMeta != null && gameSingleContainer.Properties.NewestPhaseMeta.size() > 0) {
                                gameIntent = GameActivity.startGameIntent(FCMReceiver.this, gameSingleContainer.Properties, gameSingleContainer.Properties.NewestPhaseMeta.get(0));
                            } else {
                                gameIntent = GameActivity.startGameIntent(FCMReceiver.this, gameSingleContainer.Properties, null);
                            }
                            TaskStackBuilder.create(FCMReceiver.this)
                                    .addNextIntent(mainIntent)
                                    .addNextIntent(gameIntent).startActivities();
                            finish();

                        }
                    }, getResources().getString(R.string.loading_state));
        } else {
            App.firebaseCrashReport("Unknown message type " + message.type);
        }
    }
}

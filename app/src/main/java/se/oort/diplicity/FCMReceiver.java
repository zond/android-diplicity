package se.oort.diplicity;

import android.os.Bundle;
import android.util.Log;

import com.google.firebase.crash.FirebaseCrash;

import rx.joins.JoinObserver;
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
                                    if (m.User.Id.equals(App.loggedInUser.Id)) {
                                        member = m;
                                        break;
                                    }
                                }
                                if (member != null) {
                                    ChannelService.Channel channel = new ChannelService.Channel();
                                    channel.GameID = message.message.GameID;
                                    channel.Members = message.message.ChannelMembers;
                                    PressActivity.startPressActivity(FCMReceiver.this, gameSingleContainer.Properties, channel, member);
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
                            if (gameSingleContainer.Properties.NewestPhaseMeta != null && gameSingleContainer.Properties.NewestPhaseMeta.size() > 0) {
                                GameActivity.startGameActivity(FCMReceiver.this, gameSingleContainer.Properties, gameSingleContainer.Properties.NewestPhaseMeta.get(0));
                            } else {
                                GameActivity.startGameActivity(FCMReceiver.this, gameSingleContainer.Properties, null);
                            }
                            finish();

                        }
                    }, getResources().getString(R.string.loading_state));
        } else {
            String msg = "Unknown message type " + message.type;
            FirebaseCrash.report(new RuntimeException(msg));
            Log.e("Diplicity", msg);
        }
    }
}

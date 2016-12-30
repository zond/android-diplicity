package se.oort.diplicity.game;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import se.oort.diplicity.ChannelService;
import se.oort.diplicity.R;
import se.oort.diplicity.RetrofitActivity;
import se.oort.diplicity.Sendable;
import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.Member;
import se.oort.diplicity.apigen.Message;
import se.oort.diplicity.apigen.MultiContainer;
import se.oort.diplicity.apigen.Phase;
import se.oort.diplicity.apigen.PhaseMeta;
import se.oort.diplicity.apigen.SingleContainer;
import se.oort.diplicity.apigen.Ticker;

public class PressActivity extends RetrofitActivity {

    public static final String SERIALIZED_CHANNEL_KEY = "serialized_channel";
    public static final String SERIALIZED_MEMBER_KEY = "serialized_member";
    public static final String SERIALIZED_GAME_KEY = "serialized_game";

    public ChannelService.Channel channel;
    public Member member;
    public Game game;

    public static void startPressActivity(Context context, Game game, ChannelService.Channel channel, Member member) {
        Intent intent = new Intent(context, PressActivity.class);
        intent.putExtra(PressActivity.SERIALIZED_CHANNEL_KEY, serialize(channel));
        intent.putExtra(PressActivity.SERIALIZED_MEMBER_KEY, serialize(member));
        intent.putExtra(PressActivity.SERIALIZED_GAME_KEY, serialize(game));
        context.startActivity(intent);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setContentView(R.layout.activity_press);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        byte[] serializedMember = getIntent().getByteArrayExtra(SERIALIZED_MEMBER_KEY);
        if (serializedMember != null) {
            member = (Member) unserialize(serializedMember);
        }

        byte[] serializedChannel = getIntent().getByteArrayExtra(SERIALIZED_CHANNEL_KEY);
        channel = (ChannelService.Channel) unserialize(serializedChannel);

        byte[] serializedGame = getIntent().getByteArrayExtra(SERIALIZED_GAME_KEY);
        game = (Game) unserialize(serializedGame);

        setTitle(TextUtils.join(", ", channel.Members));

        if (member == null) {
            findViewById(R.id.send_message_button).setVisibility(View.GONE);
            findViewById(R.id.body).setVisibility(View.GONE);
        } else {
            ((FloatingActionButton) findViewById(R.id.send_message_button)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Message message = new Message();
                    message.Body = ((EditText) findViewById(R.id.new_message_body)).getText().toString();
                    message.ChannelMembers = channel.Members;
                    message.CreatedAt = new Date();
                    message.Age = new Ticker(new Date(), (long) 0);
                    message.GameID = channel.GameID;
                    message.Sender = member.Nation;
                    handleReq(
                            messageService.MessageCreate(message, channel.GameID),
                            new Sendable<SingleContainer<Message>>() {
                                @Override
                                public void send(SingleContainer<Message> messageSingleContainer) {
                                    ((EditText) findViewById(R.id.new_message_body)).setText("");
                                    loadMessages();
                                }
                            },
                            getResources().getString(R.string.sending_message));
                }
            });
        }

        loadMessages();
    }

    private void loadMessages() {
        handleReq(
                messageService.ListMessages(channel.GameID, TextUtils.join(",", channel.Members)),
                new Sendable<MultiContainer<Message>>() {
                    @Override
                    public void send(final MultiContainer<Message> messageMultiContainer) {
                        ((LinearLayout) findViewById(R.id.press_messages)).removeAllViews();
                        for (int i = 0; i < messageMultiContainer.Properties.size(); i++) {
                            Message message = messageMultiContainer.Properties.get(messageMultiContainer.Properties.size() - i - 1).Properties;
                            View row = getLayoutInflater().inflate(R.layout.message_list_row, (ViewGroup) findViewById(R.id.press_layout), false);
                            String url = null;
                            for (Member member : game.Members) {
                                if (member.Nation.equals(message.Sender)) {
                                    url = member.User.Picture;
                                }
                            }

                            ((TextView) row.findViewById(R.id.body)).setText(message.Body);
                            ((TextView) row.findViewById(R.id.at)).setText(message.Age.deadlineAt().toString());
                            ((TextView) row.findViewById(R.id.sender)).setText(getResources().getString(R.string.x_, message.Sender));
                            if (url != null) {
                                PressActivity.this.populateImage((ImageView) row.findViewById(R.id.avatar), url);
                            }

                            ((LinearLayout) findViewById(R.id.press_messages)).addView(row);
                        }
                        findViewById(R.id.press_layout).invalidate();
                        findViewById(R.id.press_messages).invalidate();
                        final NestedScrollView pressScroll = (NestedScrollView) findViewById(R.id.press_scroll);
                        pressScroll.post(new Runnable() {
                            @Override
                            public void run() {
                                pressScroll.fullScroll(View.FOCUS_DOWN);
                            }
                        });
                    }
                }, getResources().getString(R.string.loading_messages));
    }
}

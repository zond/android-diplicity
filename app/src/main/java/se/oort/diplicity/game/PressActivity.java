package se.oort.diplicity.game;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.util.Linkify;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.Date;

import se.oort.diplicity.App;
import se.oort.diplicity.ChannelService;
import se.oort.diplicity.MessagingService;
import se.oort.diplicity.R;
import se.oort.diplicity.RetrofitActivity;
import se.oort.diplicity.Sendable;
import se.oort.diplicity.UserView;
import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.Member;
import se.oort.diplicity.apigen.Message;
import se.oort.diplicity.apigen.MultiContainer;
import se.oort.diplicity.apigen.SingleContainer;
import se.oort.diplicity.apigen.Ticker;

public class PressActivity extends RetrofitActivity {

    public static final String SERIALIZED_CHANNEL_KEY = "serialized_channel";
    public static final String SERIALIZED_MEMBER_KEY = "serialized_member";
    public static final String SERIALIZED_GAME_KEY = "serialized_game";

    public static final String MESSAGE_DRAFT_KEY = "message_draft";

    public ChannelService.Channel channel;
    public Member member;
    public Game game;

    public static Intent startPressIntent(Context context, Game game, ChannelService.Channel channel, Member member) {
        Intent intent = new Intent(context, PressActivity.class);
        intent.putExtra(PressActivity.SERIALIZED_CHANNEL_KEY, serialize(channel));
        intent.putExtra(PressActivity.SERIALIZED_MEMBER_KEY, serialize(member));
        intent.putExtra(PressActivity.SERIALIZED_GAME_KEY, serialize(game));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        return intent;
    }

    public static void startPressActivity(Context context, Game game, ChannelService.Channel channel, Member member) {
        context.startActivity(startPressIntent(context, game, channel, member));
    }

    @Override
    protected boolean consumeDiplicityJSON(MessagingService.DiplicityJSON diplicityJSON) {
        if (diplicityJSON.type.equals("message") && diplicityJSON.message.GameID.equals(game.ID) && diplicityJSON.message.ChannelMembers.equals(channel.Members)) {
            loadMessages(false);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        MessagingService.messageSubscribers.add(this);
    }

    @Override
    public void onPause() {
        MessagingService.messageSubscribers.remove(this);
        super.onPause();
    }

    private String draftKey() {
        return MESSAGE_DRAFT_KEY + "." + game.ID + "." + channel.Members.toString();
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

        ((TextView) findViewById(R.id.multi_line_title)).setText(TextUtils.join(", ", channel.Members));

        if (member == null) {
            findViewById(R.id.send_message_button).setVisibility(View.GONE);
            findViewById(R.id.new_message_body).setVisibility(View.GONE);
        } else {
            final TextInputEditText body = (TextInputEditText) findViewById(R.id.new_message_body);
            body.setText(prefs.getString(draftKey(), ""));
            body.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    prefs.edit().putString(draftKey(), body.getText().toString()).apply();
                }
            });
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
                                    prefs.edit().putString(draftKey(), "").apply();
                                    loadMessages(false);
                                }
                            },
                            getResources().getString(R.string.sending_message));
                }
            });
        }

        loadMessages(true);
        Toast.makeText(this, R.string.all_press_becomes_public, Toast.LENGTH_SHORT).show();
    }

    private void loadMessages(boolean withProgress) {
        String message = null;
        if (withProgress) {
            message = getResources().getString(R.string.loading_messages);
        }
        handleReq(
                messageService.ListMessages(channel.GameID, TextUtils.join(",", channel.Members)),
                new Sendable<MultiContainer<Message>>() {
                    @Override
                    public void send(final MultiContainer<Message> messageMultiContainer) {
                        ((LinearLayout) findViewById(R.id.press_messages)).removeAllViews();
                        for (int i = 0; i < messageMultiContainer.Properties.size(); i++) {
                            Message message = messageMultiContainer.Properties.get(messageMultiContainer.Properties.size() - i - 1).Properties;
                            View row = getLayoutInflater().inflate(R.layout.message_list_row, (ViewGroup) findViewById(R.id.press_layout), false);
                            Member author = App.getMemberByNation(game, message.Sender);

                            TextView body = (TextView) row.findViewById(R.id.body);
                            body.setText(message.Body);
                            Linkify.addLinks(body, Linkify.ALL);
                            ((TextView) row.findViewById(R.id.at)).setText(message.Age.createdAt().toString());
                            ((TextView) row.findViewById(R.id.sender)).setText(getResources().getString(R.string.x_, message.Sender));
                            if (author != null) {
                                ImageView avatar = (ImageView) row.findViewById(R.id.avatar);
                                PressActivity.this.populateImage(avatar, author.User.Picture, 36, 36);
                                avatar.setOnClickListener(UserView.getAvatarClickListener(PressActivity.this, game, author, author.User));
                            }

                            ((LinearLayout) findViewById(R.id.press_messages)).addView(row);
                        }
                        final NestedScrollView pressScroll = (NestedScrollView) findViewById(R.id.press_scroll);
                        pressScroll.post(new Runnable() {
                            @Override
                            public void run() {
                                findViewById(R.id.press_layout).invalidate();
                                findViewById(R.id.press_messages).invalidate();
                                pressScroll.invalidate();
                                pressScroll.fullScroll(View.FOCUS_DOWN);
                            }
                        });
                    }
                }, message);
    }
}

package se.oort.diplicity.game;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.util.Linkify;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import se.oort.diplicity.App;
import se.oort.diplicity.ChannelService;
import se.oort.diplicity.MessagingService;
import se.oort.diplicity.R;
import se.oort.diplicity.RetrofitActivity;
import se.oort.diplicity.Sendable;
import se.oort.diplicity.UserView;
import se.oort.diplicity.VariantService;
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
    public static final String SERIALIZED_PHASES_KEY = "serialized_phases";

    public static final String MESSAGE_DRAFT_KEY = "message_draft";

    public ChannelService.Channel channel;
    public Member member;
    public Game game;
    public MultiContainer<Phase> phases;

    public static final DateFormat timeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

    public static Intent startPressIntent(Context context, Game game, ChannelService.Channel channel, Member member, MultiContainer<Phase> phases) {
        Intent intent = new Intent(context, PressActivity.class);
        intent.putExtra(PressActivity.SERIALIZED_CHANNEL_KEY, serialize(channel));
        if (member != null) {
            intent.putExtra(PressActivity.SERIALIZED_MEMBER_KEY, serialize(member));
        }
        intent.putExtra(PressActivity.SERIALIZED_GAME_KEY, serialize(game));
        intent.putExtra(PressActivity.SERIALIZED_PHASES_KEY, serialize(phases));
        return intent;
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

        byte[] serializedPhases = getIntent().getByteArrayExtra(SERIALIZED_PHASES_KEY);
        phases = (MultiContainer<Phase>) unserialize(serializedPhases);

        SingleContainer<VariantService.Variant> variant = null;
        for (SingleContainer<VariantService.Variant> v : getVariants().Properties) {
            if (v.Properties.Name.equals(game.Variant)) {
                variant = v;
                break;
            }
        }
        if (variant != null && channel.Members.size() == variant.Properties.Nations.size()) {
            ((TextView) findViewById(R.id.multi_line_title)).setText(getResources().getString(R.string.everyone));
        } else {
            ((TextView) findViewById(R.id.multi_line_title)).setText(TextUtils.join(", ", channel.Members));
        }

        final MapView mapView = (MapView) findViewById(R.id.map_view);
        SingleContainer<PhaseMeta> phaseMetaSingleContainer = new SingleContainer<PhaseMeta>();
        if (game.Started) {
            phaseMetaSingleContainer.Properties = game.NewestPhaseMeta.get(0);
        }
        mapView.show(PressActivity.this, game, phaseMetaSingleContainer, phases, member, new Sendable<Object>() {
            @Override
            public void send(Object o) {
            }
        });

        final List<Boolean> mapShown = new ArrayList<>();
        final List<Boolean> loadedProperly = new ArrayList<>();
        mapShown.add(false);
        loadedProperly.add(false);
        final ViewSwitcher pressContainer = (ViewSwitcher) findViewById(R.id.press_container);
        ((FloatingActionButton) findViewById(R.id.map_toggle)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pressContainer.showNext();
                mapShown.set(0, !mapShown.get(0));
                if (!mapShown.get(0)) {
                    scrollToBottom(false);
                } else if (!loadedProperly.get(0)){
                    mapView.draw();
                    loadedProperly.set(0, true);
                }
            }
        });

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
                    EditText inputText = (EditText) findViewById(R.id.new_message_body);
                    Message message = new Message();
                    message.Body = inputText.getText().toString();
                    if (message.Body.trim().length() == 0) {
                        return;
                    }
                    message.ChannelMembers = channel.Members;
                    message.CreatedAt = new Date();
                    message.Age = new Ticker(new Date(), (long) 0);
                    message.GameID = channel.GameID;
                    message.Sender = member.Nation;

                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(inputText.getWindowToken(), 0);

                    inputText.setText("");

                    prefs.edit().putString(draftKey(), "").apply();

                    ((LinearLayout) findViewById(R.id.press_messages)).addView(getMessageRow(member, message, false));

                    scrollToBottom(false);

                    handleReq(
                            messageService.MessageCreate(message, channel.GameID),
                            new Sendable<SingleContainer<Message>>() {
                                @Override
                                public void send(SingleContainer<Message> messageSingleContainer) {
                                    loadMessages(false);

                                }
                            },
                            null);
                }
            });
        }

        loadMessages(true);
        Toast.makeText(this, R.string.all_press_becomes_public, Toast.LENGTH_SHORT).show();
    }

    private void scrollToBottom(final boolean reopenKeyboard) {
        final NestedScrollView pressScroll = (NestedScrollView) findViewById(R.id.press_scroll);
        pressScroll.post(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.press_layout).invalidate();
                findViewById(R.id.press_messages).invalidate();
                pressScroll.invalidate();
                pressScroll.fullScroll(View.FOCUS_DOWN);
                if (reopenKeyboard) {
                    findViewById(R.id.new_message_body).requestFocus();
                }
            }
        });
    }

    private void pushPhaseChange(ArrayList<SingleContainer<Phase>> phaseList) {
        Phase phase = phaseList.get(0).Properties;
        phaseList.remove(0);
        TextView phaseChange = new TextView(PressActivity.this);
        phaseChange.setText(getResources().getString(R.string.season_year_type_created, phase.Season, phase.Year, phase.Type, timeFormat.format(phase.CreatedAgo.deadlineAt())));
        ((LinearLayout) findViewById(R.id.press_messages)).addView(phaseChange);
    }

    private View getMessageRow(Member member, Message message, boolean completed) {
        View row = getLayoutInflater().inflate(R.layout.message_list_row, (ViewGroup) findViewById(R.id.press_layout), false);
        TextView body = (TextView) row.findViewById(R.id.body);
        body.setText(message.Body);
        Linkify.addLinks(body, Linkify.ALL);
        ((TextView) row.findViewById(R.id.at)).setText(timeFormat.format(new Date()));
        ((TextView) row.findViewById(R.id.sender)).setText(getResources().getString(R.string.x_, member.Nation));
        if (member != null) {
            ImageView avatar = (ImageView) row.findViewById(R.id.avatar);
            PressActivity.this.populateImage(avatar, member.User.Picture, 36, 36);
            avatar.setOnClickListener(UserView.getAvatarClickListener(PressActivity.this, game, member, member.User));
        }
        if (!completed) {
            row.findViewById(R.id.message_progress).setVisibility(View.VISIBLE);
        }
        return row;
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
                        EditText inputText = (EditText) findViewById(R.id.new_message_body);
                        boolean reopenKeyboard = false;
                        if (inputText.equals(PressActivity.this.getCurrentFocus())) {
                            reopenKeyboard = true;
                        }
                        ((LinearLayout) findViewById(R.id.press_messages)).removeAllViews();
                        ArrayList<SingleContainer<Phase>> phaseList = new ArrayList<SingleContainer<Phase>>(phases.Properties);
                        Collections.sort(phaseList, new Comparator<SingleContainer<Phase>>() {
                            @Override
                            public int compare(SingleContainer<Phase> o1, SingleContainer<Phase> o2) {
                                return new Long(o1.Properties.CreatedAt.getTime()).compareTo(new Long(o2.Properties.CreatedAt.getTime()));
                            }
                        });
                        for (int i = 0; i < messageMultiContainer.Properties.size(); i++) {
                            Message message = messageMultiContainer.Properties.get(messageMultiContainer.Properties.size() - i - 1).Properties;

                            while (phaseList.size() > 0 && phaseList.get(0).Properties.CreatedAt.getTime() < message.CreatedAt.getTime()) {
                                pushPhaseChange(phaseList);
                            }

                            Member author = App.getMemberByNation(game, message.Sender);

                            ((LinearLayout) findViewById(R.id.press_messages)).addView(getMessageRow(author, message, true));
                        }
                        while (!phaseList.isEmpty()) {
                            pushPhaseChange(phaseList);
                        }
                        scrollToBottom(reopenKeyboard);
                    }
                }, message);
    }
}

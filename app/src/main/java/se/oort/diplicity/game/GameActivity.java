package se.oort.diplicity.game;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.w3c.dom.Text;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.adapter.rxjava.HttpException;
import rx.functions.Func2;
import rx.observables.JoinObservable;
import se.oort.diplicity.Alarm;
import se.oort.diplicity.App;
import se.oort.diplicity.ChannelService;
import se.oort.diplicity.GameUnserializer;
import se.oort.diplicity.MemberTable;
import se.oort.diplicity.MessagingService;
import se.oort.diplicity.OptionsService;
import se.oort.diplicity.R;
import se.oort.diplicity.RetrofitActivity;
import se.oort.diplicity.Sendable;
import se.oort.diplicity.UserView;
import se.oort.diplicity.VariantService;
import se.oort.diplicity.apigen.Dislodged;
import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.GameResult;
import se.oort.diplicity.apigen.GameState;
import se.oort.diplicity.apigen.Link;
import se.oort.diplicity.apigen.Member;
import se.oort.diplicity.apigen.MultiContainer;
import se.oort.diplicity.apigen.Order;
import se.oort.diplicity.apigen.Phase;
import se.oort.diplicity.apigen.PhaseMeta;
import se.oort.diplicity.apigen.PhaseResult;
import se.oort.diplicity.apigen.PhaseState;
import se.oort.diplicity.apigen.Resolution;
import se.oort.diplicity.apigen.SC;
import se.oort.diplicity.apigen.SingleContainer;
import se.oort.diplicity.apigen.UnitWrapper;
import se.oort.diplicity.util.Counter;

public class GameActivity extends RetrofitActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String SERIALIZED_GAME_KEY = "serialized_game";
    public static final String SERIALIZED_PHASES_KEY = "serialized_phases";

    // Included in the intent bundle.
    public Game game;
    // Included in game if the game is started.
    public SingleContainer<PhaseMeta> phaseMeta = new SingleContainer<>();
    // Calculated from the intent bundle.
    public Member member;
    // Included in the intent bundle if the game is started.
    private MultiContainer<Phase> phases = new MultiContainer<>();

    // Used to swipe between phases.
    private FlickFrameLayout flickFrameLayout;
    // Used to remember which view we are in (map, orders, phases etc).
    private int currentView;

    public static Intent startGameIntent(Context context, Game game, MultiContainer<Phase> phases) {
        Intent intent = new Intent(context, GameActivity.class);
        intent.putExtra(GameActivity.SERIALIZED_GAME_KEY, RetrofitActivity.serialize(game));
        if (phases != null) {
            intent.putExtra(GameActivity.SERIALIZED_PHASES_KEY, RetrofitActivity.serialize(phases));
        }
        return intent;
    }

    @Override
    protected boolean consumeDiplicityJSON(final MessagingService.DiplicityJSON diplicityJSON) {
        if (diplicityJSON.type.equals("phase") && diplicityJSON.gameID.equals(game.ID)) {
            handleReq(phaseService.ListPhases(game.ID), new Sendable<MultiContainer<Phase>>() {
                @Override
                public void send(MultiContainer<Phase> phaseMultiContainer) {
                    phases = phaseMultiContainer;
                    game.NewestPhaseMeta.set(0, diplicityJSON.phaseMeta);
                    Toast.makeText(GameActivity.this, R.string.the_game_has_a_new_phase, Toast.LENGTH_SHORT).show();
                }
            }, getResources().getString(R.string.loading_phases));
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        MessagingService.messageSubscribers.add(this);
        draw();
    }

    @Override
    public void onPause() {
        MessagingService.messageSubscribers.remove(this);
        super.onPause();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        byte[] serializedGame = getIntent().getByteArrayExtra(SERIALIZED_GAME_KEY);
        if (serializedGame != null) {
            game = (Game) unserialize(serializedGame);
            Collections.sort(game.Members, new Comparator<Member>() {
                @Override
                public int compare(Member member, Member t1) {
                    return member.Nation.compareTo(t1.Nation);
                }
            });
            member = getLoggedInMember(game);

            if (game.NewestPhaseMeta != null && game.NewestPhaseMeta.size() > 0) {
                phaseMeta.Properties = game.NewestPhaseMeta.get(0);
            }
        }

        byte[] serializedPhases = getIntent().getByteArrayExtra(SERIALIZED_PHASES_KEY);
        if (serializedPhases != null) {
            phases = (MultiContainer<Phase>) unserialize(serializedPhases);
        }
    }

    public void nextPhase() {
        if (phaseMeta.Properties != null && phaseMeta.Properties.PhaseOrdinal < game.NewestPhaseMeta.get(0).PhaseOrdinal) {
            Gson gson = new Gson();
            Phase phase = phases.Properties.get(phaseMeta.Properties.PhaseOrdinal.intValue()).Properties;
            if (phase != null) {
                phaseMeta.Properties = gson.fromJson(gson.toJson(phase), PhaseMeta.class);
                draw();
            }
        }
    }

    public void prevPhase() {
        if (phaseMeta.Properties != null && phaseMeta.Properties.PhaseOrdinal > 1) {
            Gson gson = new Gson();
            Phase phase = phases.Properties.get(phaseMeta.Properties.PhaseOrdinal.intValue() - 2).Properties;
            if (phase != null) {
                phaseMeta.Properties = gson.fromJson(gson.toJson(phase), PhaseMeta.class);
                draw();
            }
        }
    }

    public String gameTitle() {
        String descPart = getResources().getString(R.string.untitled);
        if (member != null && member.GameAlias != null && !member.GameAlias.equals("")) {
            descPart = member.GameAlias;
        } else if (game != null && !game.Desc.equals("")) {
            descPart = game.Desc;
        }
        if (phaseMeta.Properties == null) {
            return descPart;
        }
        if (phaseMeta.Properties.NextDeadlineIn.nanos == 0) {
            return getResources().getString(R.string.desc_season_year_type, descPart, phaseMeta.Properties.Season, phaseMeta.Properties.Year, phaseMeta.Properties.Type);
        }
        return getResources().getString(R.string.desc_season_year_type_deadline, descPart, phaseMeta.Properties.Season, phaseMeta.Properties.Year, phaseMeta.Properties.Type, App.millisToDuration(phaseMeta.Properties.NextDeadlineIn.millisLeft()));
    }

    private Toolbar updateTitle() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        try {
            Field f = toolbar.getClass().getDeclaredField("mTitleTextView");
            f.setAccessible(true);
            TextView toolbarTextView = (TextView) f.get(toolbar);
            toolbarTextView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            toolbarTextView.setFocusable(true);
            toolbarTextView.setFocusableInTouchMode(true);
            toolbarTextView.requestFocus();
            toolbarTextView.setSingleLine(true);
            toolbarTextView.setSelected(true);
            toolbarTextView.setMarqueeRepeatLimit(-1);
        } catch (NoSuchFieldException e) {
        } catch (IllegalAccessException e) {
        }
        setTitle(gameTitle());

        return toolbar;
    }

    public void draw() {
        setContentView(R.layout.activity_game);

        if (game == null) {
            App.firebaseCrashReport("Drawing null game");
            Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_SHORT).show();
            return;
        }

        flickFrameLayout = (FlickFrameLayout) findViewById(R.id.game_content);
        flickFrameLayout.gameActivity = this;
        findViewById(R.id.flick_frame_background).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                flickFrameLayout.onTouchEvent(motionEvent);
                return true;
            }
        });

        Toolbar toolbar = updateTitle();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                toggle.onDrawerSlide(drawerView, slideOffset);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                ((TextView) findViewById(R.id.nav_title)).setText(gameTitle());
                toggle.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                toggle.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                toggle.onDrawerStateChanged(newState);
            }
        });
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        Menu nav_Menu = navigationView.getMenu();
        if (game.Started) {
            if (!game.Finished && game.DisableConferenceChat && game.DisableGroupChat && game.DisablePrivateChat) {
                nav_Menu.findItem(R.id.nav_press).setVisible(false);
            }
        } else {
            nav_Menu.findItem(R.id.nav_orders).setVisible(false);
            nav_Menu.findItem(R.id.nav_phases).setVisible(false);
            nav_Menu.findItem(R.id.nav_press).setVisible(false);
            nav_Menu.findItem(R.id.nav_phase_settings).setVisible(false);
            nav_Menu.findItem(R.id.nav_game_settings).setVisible(false);
            nav_Menu.findItem(R.id.nav_phase_status).setVisible(false);
        }
        if (phaseMeta.Properties == null || !phaseMeta.Properties.Resolved) {
            nav_Menu.findItem(R.id.nav_phase_result).setVisible(false);
        }
        if (!game.Finished) {
            nav_Menu.findItem(R.id.nav_game_result).setVisible(false);
        }

        if (currentView == 0) {
            currentView = R.id.nav_map;
        }
        navigateTo(currentView);
    }

    private void helpURL(final String url) {
        findViewById(R.id.help_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void setVisibility(int visibility, int... views) {
        for (int i = 0; i < views.length; i++)
            findViewById(views[i]).setVisibility(visibility);
    }

    public void hideAllExcept(int toShow) {
        for (int viewID : new int[]{
                R.id.map_view,
                R.id.orders_view,
                R.id.phase_status_view,
                R.id.phases_view,
                R.id.press_view,
                R.id.phase_results_view,
                R.id.game_results_view,
                R.id.phase_state_view,
                R.id.game_state_view,
                R.id.variant_info_view
        }) {
            if (viewID == toShow) {
                findViewById(viewID).setVisibility(View.VISIBLE);
            } else {
                findViewById(viewID).setVisibility(View.GONE);
            }
        }
        if (toShow == R.id.press_view) {
            findViewById(R.id.create_channel_button).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.create_channel_button).setVisibility(View.GONE);
        }
    }


    private class PhaseElement {
        public Phase phase;
        public PhaseElement(Phase phase) {
            this.phase = phase;
        }
        private DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        public String toString() {
            return getResources().getString(R.string.season_year_type_created, phase.Season, phase.Year, phase.Type, format.format(phase.CreatedAgo.deadlineAt()));
        }
    }

    public void showPress() {
        hideAllExcept(R.id.press_view);
        if (member != null) {
            FloatingActionButton button = (FloatingActionButton) findViewById(R.id.create_channel_button);
            button.setVisibility(View.VISIBLE);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final List<String> nations = new ArrayList<String>();
                    for (Member thisMember : game.Members) {
                        if (!member.Nation.equals(thisMember.Nation)) {
                            nations.add(thisMember.Nation);
                        }
                    }
                    final boolean[] checked = new boolean[nations.size()];
                    final AlertDialog dialog = new AlertDialog.Builder(GameActivity.this).setMultiChoiceItems(
                            nations.toArray(new String[]{}),
                            checked,
                            new DialogInterface.OnMultiChoiceClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                                    checked[i] = b;
                                    int numChecked = 0;
                                    for (int j = 0; j < checked.length; j++) {
                                        if (checked[j]) {
                                            numChecked++;
                                        }
                                    }
                                    if (!game.Finished && game.DisableConferenceChat && numChecked == checked.length) {
                                        ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                        Toast.makeText(GameActivity.this, getResources().getString(R.string.conference_chat_is_disabled), Toast.LENGTH_LONG).show();
                                    } else if (!game.Finished && game.DisablePrivateChat && numChecked == 1) {
                                        ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                        Toast.makeText(GameActivity.this, getResources().getString(R.string.private_chat_is_disabled), Toast.LENGTH_LONG).show();
                                    } else if (!game.Finished && game.DisableGroupChat && numChecked > 1 && numChecked < checked.length) {
                                        ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                        Toast.makeText(GameActivity.this, getResources().getString(R.string.group_chat_is_disabled), Toast.LENGTH_LONG).show();
                                    } else {
                                        ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                    }
                                }
                            })
                            .setTitle(R.string.members)
                            .setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialogInterface, int j) {
                            List<String> channelMembers = new ArrayList<String>();
                            for (int i = 0; i < checked.length; i++) {
                                if (checked[i]) {
                                    channelMembers.add(nations.get(i));
                                }
                            }
                            channelMembers.add(member.Nation);
                            Collections.sort(channelMembers);
                            ChannelService.Channel channel = new ChannelService.Channel();
                            channel.GameID = game.ID;
                            channel.Members = channelMembers;
                            GameActivity.this.startActivity(PressActivity.startPressIntent(GameActivity.this, game, channel, member, phases));
                            dialogInterface.dismiss();
                        }
                    }).show();

                }
            });
        } else {
            findViewById(R.id.create_channel_button).setVisibility(View.GONE);
        }
        handleReq(
                channelService.ListChannels(game.ID),
                new Sendable<MultiContainer<ChannelService.Channel>>() {
                    @Override
                    public void send(MultiContainer<ChannelService.Channel> channelMultiContainer) {
                        final List<ChannelService.Channel> channels = new ArrayList<>();
                        for (SingleContainer<ChannelService.Channel> channelSingleContainer : channelMultiContainer.Properties) {
                            channels.add(channelSingleContainer.Properties);
                        }
                        ChannelTable channelTable = (ChannelTable) findViewById(R.id.press_channel_table);
                        channelTable.setChannels(GameActivity.this, game, member, channels, phases);
                    }
                }, getResources().getString(R.string.loading_channels));
    }

    public void showGameStates() {
        hideAllExcept(R.id.game_state_view);

        if (member == null) {
            findViewById(R.id.edit_game_state_button).setVisibility(View.GONE);
        }

        handleReq(
                gameStateService.ListGameStates(game.ID),
                new Sendable<MultiContainer<GameState>>() {
                    @Override
                    public void send(MultiContainer<GameState> gameStateMultiContainer) {
                        GameState myState = null;
                        TableLayout mutedTable = (TableLayout) findViewById(R.id.muted_table);
                        mutedTable.removeAllViews();
                        FloatingActionButton button = ((FloatingActionButton) mutedTable.findViewById(R.id.open_button));
                        if (button != null) {
                            mutedTable.removeView(button);
                        }
                        final List<String> nations = new ArrayList<String>();
                        for (Member thisMember : game.Members) {
                            if (member == null || !thisMember.Nation.equals(member.Nation)) {
                                nations.add(thisMember.Nation);
                            }
                            GameState foundState = null;
                            for (SingleContainer<GameState> singleContainer : gameStateMultiContainer.Properties) {
                                if (singleContainer.Properties.Nation.equals(thisMember.Nation)) {
                                    foundState = singleContainer.Properties;
                                }
                            }
                            if (member != null && thisMember.Nation.equals(member.Nation)) {
                                myState = foundState;
                            }
                            TableRow.LayoutParams params =
                                    new TableRow.LayoutParams(
                                            TableRow.LayoutParams.WRAP_CONTENT,
                                            TableRow.LayoutParams.WRAP_CONTENT, 1.0f);
                            int margin = getResources().getDimensionPixelSize(R.dimen.muted_table_margin);
                            params.bottomMargin = margin;
                            params.topMargin = margin;
                            params.leftMargin = margin;
                            params.rightMargin = margin;
                            TableRow tableRow = new TableRow(GameActivity.this);
                            tableRow.setLayoutParams(params);
                            LinearLayout playerSide = new LinearLayout(GameActivity.this);
                            LinearLayout.LayoutParams linearParams =
                                    new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT);
                            playerSide.setLayoutParams(params);
                            playerSide.setOrientation(LinearLayout.VERTICAL);
                            UserView user = new UserView(GameActivity.this, null);
                            user.setUser(GameActivity.this, thisMember.User, true);
                            user.setLayoutParams(linearParams);
                            playerSide.addView(user);
                            TextView nation = new TextView(GameActivity.this);
                            nation.setText(thisMember.Nation);
                            nation.setLayoutParams(linearParams);
                            playerSide.addView(nation);
                            tableRow.addView(playerSide);
                            TextView muteds = new TextView(GameActivity.this);
                            muteds.setLayoutParams(params);
                            if (foundState != null && foundState.Muted != null) {
                                muteds.setText(TextUtils.join(", ", foundState.Muted));
                            } else {
                                muteds.setText("");
                            }
                            tableRow.addView(muteds);
                            mutedTable.addView(tableRow);
                        }
                        final GameState finalMyState = myState;
                        if (finalMyState != null) {
                            ((FloatingActionButton) findViewById(R.id.edit_game_state_button)).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    final boolean[] checked = new boolean[nations.size()];
                                    if (finalMyState.Muted != null) {
                                        for (String muted : finalMyState.Muted) {
                                            int pos = nations.indexOf(muted);
                                            if (pos > -1) {
                                                checked[pos] = true;
                                            }
                                        }
                                    }
                                    final AlertDialog dialog = new AlertDialog.Builder(GameActivity.this).setMultiChoiceItems(
                                            nations.toArray(new String[]{}),
                                            checked,
                                            new DialogInterface.OnMultiChoiceClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                                                    checked[i] = b;
                                                }
                                            })
                                            .setTitle(R.string.muted)
                                            .setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int j) {
                                            List<String> mutedMembers = new ArrayList<String>();
                                            for (int i = 0; i < checked.length; i++) {
                                                if (checked[i]) {
                                                    mutedMembers.add(nations.get(i));
                                                }
                                            }
                                            Collections.sort(mutedMembers);
                                            finalMyState.Muted = mutedMembers;
                                            handleReq(
                                                    gameStateService.GameStateUpdate(finalMyState, game.ID, finalMyState.Nation),
                                                    new Sendable<SingleContainer<GameState>>() {
                                                        @Override
                                                        public void send(SingleContainer<GameState> gameStateSingleContainer) {
                                                            showGameStates();
                                                        }
                                                    }, getResources().getString(R.string.updating_game_state));
                                        }
                                    }).show();
                                }
                            });
                        }
                    }
                }, getResources().getString(R.string.loading_game_settings));
    }

    public void showVariantInfo() {
        hideAllExcept(R.id.variant_info_view);

        final TextView variantName = (TextView) findViewById(R.id.variant_info_name);
        variantName.setText(game.Variant);

        MultiContainer<VariantService.Variant> variants = getVariants();
        for (SingleContainer<VariantService.Variant> sc : variants.Properties) {
            VariantService.Variant variant = sc.Properties;
            if (variant.Name.equals(game.Variant)) {
                setTextAndLabel(variant.CreatedBy, R.id.variant_info_created_by, R.id.variant_info_created_by_label);
                setTextAndLabel(variant.Version, R.id.variant_info_version, R.id.variant_info_version_label);
                setTextAndLabel(variant.Description, R.id.variant_info_description, R.id.variant_info_description_label);
                setTextAndLabel(variant.Rules, R.id.variant_info_rules, R.id.variant_info_rules_label);
            }
        }
    }

    /**
     * Display some text with its label if the text is not null or the empty string (otherwise hide both fields).
     *
     * @param text The text to display.
     * @param textField The id of the field to put the text into.
     * @param labelField The id of the label field.
     */
    private void setTextAndLabel(String text, int textField, int labelField) {
        if (text == null || text.isEmpty()) {
            findViewById(labelField).setVisibility(View.GONE);
            findViewById(textField).setVisibility(View.GONE);
        } else {
            findViewById(labelField).setVisibility(View.VISIBLE);
            findViewById(textField).setVisibility(View.VISIBLE);
            ((TextView) findViewById(textField)).setText(text);
        }
    }

    public void showPhaseStates() {
        hideAllExcept(R.id.phase_state_view);

        final MemberTable phaseStateView = (MemberTable) findViewById(R.id.phase_state_view_table);
        phaseStateView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                flickFrameLayout.onTouchEvent(motionEvent);
                phaseStateView.onTouchEvent(motionEvent);
                return true;
            }
        });

        handleReq(
                phaseStateService.ListPhaseStates(game.ID, phaseMeta.Properties.PhaseOrdinal.toString()),
                new Sendable<MultiContainer<PhaseState>>() {
                    @Override
                    public void send(MultiContainer<PhaseState> phaseStateMultiContainer) {
                        List<PhaseState> phaseStates = new ArrayList<PhaseState>();
                        for (SingleContainer<PhaseState> phaseStateSingleContainer : phaseStateMultiContainer.Properties) {
                            phaseStates.add(phaseStateSingleContainer.Properties);
                        }
                        phaseStateView.setPhaseStates(game, phaseMeta.Properties, phaseStates);
                        phaseStateView.setMembers(GameActivity.this, game, game.Members);
                    }
                }, getResources().getString(R.string.loading_phase_settings));
    }

    public void showPhases() {
        hideAllExcept(R.id.phases_view);
        final List<PhaseElement> phaseList = new ArrayList<>();
        for (int i = 0; i < phases.Properties.size(); i++) {
            phaseList.add(new PhaseElement(phases.Properties.get(phases.Properties.size() - i - 1).Properties));
        }
        ListView phasesView = (ListView) findViewById(R.id.phases_view);
        phasesView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Gson gson = new Gson();
                phaseMeta.Properties = gson.fromJson(gson.toJson(phaseList.get(i).phase), PhaseMeta.class);
                currentView = R.id.nav_map;
                showMap();
            }
        });
        phasesView.setAdapter(new ArrayAdapter<PhaseElement>(GameActivity.this, android.R.layout.simple_list_item_1, phaseList));
    }

    public void showGameResults() {
        setVisibility(View.GONE,
                        R.id.solo_winner_label,
                        R.id.solo_winner,
                        R.id.dias_members_label,
                        R.id.dias_members,
                        R.id.nmr_game_members_label,
                        R.id.nmr_game_members,
                        R.id.eliminated_members_label,
                        R.id.eliminated_members);
        hideAllExcept(R.id.game_results_view);

        final ScrollView gameResultsView = (ScrollView) findViewById(R.id.game_results_view);
        gameResultsView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                flickFrameLayout.onTouchEvent(motionEvent);
                gameResultsView.onTouchEvent(motionEvent);
                return true;
            }
        });
        if (game.Finished) {
            handleReq(
                    gameResultService.GameResultLoad(game.ID),
                    new Sendable<SingleContainer<GameResult>>() {
                        @Override
                        public void send(SingleContainer<GameResult> gameResultSingleContainer) {
                            MemberListPopulater populater = new MemberListPopulater();
                            if (gameResultSingleContainer.Properties.SoloWinnerUser != null &&
                                !gameResultSingleContainer.Properties.SoloWinnerUser.equals("")) {
                                populater.populate(R.id.solo_winner,
                                        Arrays.asList(gameResultSingleContainer.Properties.SoloWinnerUser));
                                setVisibility(View.VISIBLE, R.id.solo_winner, R.id.solo_winner_label);
                            }
                            if (gameResultSingleContainer.Properties.DIASUsers != null) {
                                populater.populate(R.id.dias_members,
                                        gameResultSingleContainer.Properties.DIASUsers);
                                setVisibility(View.VISIBLE, R.id.dias_members, R.id.dias_members_label);
                            }
                            if (gameResultSingleContainer.Properties.EliminatedUsers != null) {
                                populater.populate(R.id.eliminated_members,
                                        gameResultSingleContainer.Properties.EliminatedUsers);
                                setVisibility(View.VISIBLE, R.id.eliminated_members, R.id.eliminated_members_label);
                            }
                            if (gameResultSingleContainer.Properties.NMRUsers != null) {
                                populater.populate(R.id.nmr_game_members,
                                        gameResultSingleContainer.Properties.NMRUsers);
                                setVisibility(View.VISIBLE, R.id.nmr_game_members, R.id.nmr_game_members_label);
                            }
                            if (gameResultSingleContainer.Properties.Scores != null) {
                                ((MemberTable) findViewById(R.id.scored_members)).setScores(gameResultSingleContainer.Properties.Scores);
                                populater.populate(R.id.scored_members,
                                        gameResultSingleContainer.Properties.AllUsers);
                                setVisibility(View.VISIBLE, R.id.scored_members, R.id.scored_members_label);
                            }
                        }
                    }, getResources().getString(R.string.loading_game_result));
        }
    }

    private class MemberListPopulater {
        public void populate(int view, List<String> uids) {
            List<Member> members = new ArrayList<Member>();
            for (Member member : game.Members) {
                if (uids.contains(member.User.Id)) {
                    members.add(member);
                }
            }
            LinearLayoutManager membersLayoutManager = new LinearLayoutManager(GameActivity.this);
            membersLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            MemberTable table = (MemberTable) findViewById(view);
            table.setMembers(GameActivity.this, game, members);
            table.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    flickFrameLayout.onTouchEvent(motionEvent);
                    return true;
                }
            });
        }
    }

    public void showPhaseResults() {
        setVisibility(View.GONE,
                    R.id.nmr_members,
                    R.id.nmr_members_label,
                    R.id.active_members,
                    R.id.active_members_label,
                    R.id.ready_members,
                    R.id.ready_members_label);

        hideAllExcept(R.id.phase_results_view);

        final ScrollView phaseResultsView = (ScrollView) findViewById(R.id.phase_results_view);
        phaseResultsView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                flickFrameLayout.onTouchEvent(motionEvent);
                phaseResultsView.onTouchEvent(motionEvent);
                return true;
            }
        });
        if (phaseMeta.Properties.Resolved) {
            handleReq(
                    phaseResultService.PhaseResultLoad(game.ID, phaseMeta.Properties.PhaseOrdinal.toString()),
                    new Sendable<SingleContainer<PhaseResult>>() {
                        @Override
                        public void send(SingleContainer<PhaseResult> phaseResultSingleContainer) {
                            MemberListPopulater populater = new MemberListPopulater();
                            if (phaseResultSingleContainer.Properties.NMRUsers != null) {
                                populater.populate(R.id.nmr_members, phaseResultSingleContainer.Properties.NMRUsers);
                                setVisibility(View.VISIBLE, R.id.nmr_members, R.id.nmr_members_label);
                            }
                            if (phaseResultSingleContainer.Properties.ActiveUsers != null) {
                                populater.populate(R.id.active_members, phaseResultSingleContainer.Properties.ActiveUsers);
                                setVisibility(View.VISIBLE, R.id.active_members, R.id.active_members_label);
                            }
                            if (phaseResultSingleContainer.Properties.ReadyUsers != null) {
                                populater.populate(R.id.ready_members, phaseResultSingleContainer.Properties.ReadyUsers);
                                setVisibility(View.VISIBLE, R.id.ready_members, R.id.ready_members_label);
                            }
                        }
                    }, new SingleErrorHandler(404, new Sendable<HttpException>() {
                        @Override
                        public void send(HttpException e) {

                        }
                    }), getResources().getString(R.string.loading_phase_result));
        }
    }

    public void showPhaseStatus() {
        hideAllExcept(R.id.phase_status_view);
        if (phaseMeta.Properties != null && phaseMeta.Properties.PhaseOrdinal != null) {
            SingleContainer<Phase> phaseSingleContainer = phases.Properties.get(phaseMeta.Properties.PhaseOrdinal.intValue() - 1);
            Counter<String> scCount = new Counter<String>();
            if (phaseSingleContainer.Properties.SCs != null) {
                for (SC sc : phaseSingleContainer.Properties.SCs) {
                    scCount.increment(sc.Owner);
                }
            }

            Counter<String> unitCount = new Counter<String>();
            if (phaseSingleContainer.Properties.Units != null) {
                for (UnitWrapper wrapper : phaseSingleContainer.Properties.Units) {
                    unitCount.increment(wrapper.Unit.Nation);
                }
            }

            Counter<String> dislodgedCount = null;
            View dislodgedHeader = findViewById(R.id.dislodged_header);
            if (phaseSingleContainer.Properties.Dislodgeds != null) {
                dislodgedHeader.setVisibility(View.VISIBLE);
                dislodgedCount = new Counter<String>();
                for (Dislodged dislodged : phaseSingleContainer.Properties.Dislodgeds) {
                    dislodgedCount.increment(dislodged.Dislodged.Nation);
                }
            } else {
                dislodgedHeader.setVisibility(View.GONE);
            }

            Set<String> nations = new HashSet<String>();
            nations.addAll(scCount.keySet());
            nations.addAll(unitCount.keySet());

            ViewGroup phaseStatusInnerView = (ViewGroup) findViewById(R.id.phase_status_inner_view);
            phaseStatusInnerView.removeViews(1, phaseStatusInnerView.getChildCount() - 1);
            LayoutInflater layoutInflater = LayoutInflater.from(phaseStatusInnerView.getContext());

            List<String> nationsList = new ArrayList<>(nations);
            Collections.sort(nationsList);
            for (String nation : nationsList) {
                String dislodgedString = (dislodgedCount != null ? String.valueOf(dislodgedCount.get(nation)) : null);
                int delta = scCount.get(nation) - unitCount.get(nation) - (dislodgedCount != null ? dislodgedCount.get(nation) : 0);
                addPhaseStatusRow(phaseStatusInnerView, layoutInflater, nation,
                        String.valueOf(scCount.get(nation)),
                        String.valueOf(unitCount.get(nation)),
                        dislodgedString,
                        String.format("%+d", delta));
            }
        }
    }

    /**
     * Add a row to the phase status table.
     *
     * @param phaseStatusInnerView The parent to add the status row to.
     * @param layoutInflater The inflater for creating status rows.
     * @param nation The string for the nation column.
     * @param scCount The string for the SC column.
     * @param unitCount The string for the unit column.
     * @param dislodgedCount The string for the dislodged column.
     * @param delta The string for the delta column.
     */
    private void addPhaseStatusRow(ViewGroup phaseStatusInnerView, LayoutInflater layoutInflater,
                                   String nation, String scCount, String unitCount, String dislodgedCount, String delta) {
        View itemView = layoutInflater.inflate(R.layout.phase_status_row, phaseStatusInnerView, false);
        TextView nationView = (TextView) itemView.findViewById(R.id.nation);
        nationView.setText(nation);
        TextView scCountView = (TextView) itemView.findViewById(R.id.sc_count);
        scCountView.setText(scCount);
        TextView unitCountView = (TextView) itemView.findViewById(R.id.unit_count);
        unitCountView.setText(unitCount);
        if (dislodgedCount != null) {
            TextView dislodgedCountView = (TextView) itemView.findViewById(R.id.dislodged_count);
            dislodgedCountView.setText(dislodgedCount);
        }
        if (delta != null) {
            TextView unitDeltaView = (TextView) itemView.findViewById(R.id.unit_delta);
            unitDeltaView.setText(delta);
        }
        phaseStatusInnerView.addView(itemView);
    }

    public void showOrders() {
        hideAllExcept(R.id.orders_view);
        handleReq(orderService.ListOrders(game.ID, phaseMeta.Properties.PhaseOrdinal.toString()), new Sendable<MultiContainer<Order>>() {
            @Override
            public void send(MultiContainer<Order> orderMultiContainer) {
                SingleContainer<Phase> phaseSingleContainer = phases.Properties.get(phaseMeta.Properties.PhaseOrdinal.intValue() - 1);
                Map<String, String> resultMap = new HashMap<String, String>();
                if (phaseSingleContainer.Properties.Resolutions != null) {
                    for (Resolution resolution : phaseSingleContainer.Properties.Resolutions) {
                        resultMap.put(resolution.Province, resolution.Resolution);
                    }
                }
                List<String> orders = new ArrayList<String>();
                for (SingleContainer<Order> orderContainer : orderMultiContainer.Properties) {
                    String resolution = resultMap.get(orderContainer.Properties.Parts.get(0));
                    if (resolution == null) {
                        orders.add(getResources().getString(R.string.nation_order, orderContainer.Properties.Nation, TextUtils.join(" ", orderContainer.Properties.Parts)));
                    } else {
                        orders.add(getResources().getString(R.string.nation_order_result, orderContainer.Properties.Nation, TextUtils.join(" ", orderContainer.Properties.Parts), resolution));
                    }
                }
                Collections.sort(orders);
                final EditText ordersView = (EditText) findViewById(R.id.orders_view);
                ordersView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        flickFrameLayout.onTouchEvent(motionEvent);
                        ordersView.onTouchEvent(motionEvent);
                        return true;
                    }
                });
                StringBuffer orderBuffer = new StringBuffer();
                for (String order : orders) {
                    orderBuffer.append(order + "\n");
                }
                ordersView.setText(orderBuffer);
            }
        }, getResources().getString(R.string.loading_orders));
    }

    public void showMap() {
        hideAllExcept(R.id.map_view);

        ((MapView) findViewById(R.id.map_view)).show(GameActivity.this, game, phaseMeta, phases, member, new Sendable<Object>() {
            @Override
            public void send(Object o) {
                draw();
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        navigateTo(item.getItemId());
        return true;
    }

    private void navigateTo(int id) {
        helpURL("https://sites.google.com/view/diplicity/home/documentation/submitting-orders");
        int oldView = currentView;
        currentView = id;
        if (id == R.id.nav_map) {
            showMap();
        } else if (id == R.id.nav_orders) {
            showOrders();
        } else if (id == R.id.nav_phase_status) {
            showPhaseStatus();
        } else if (id == R.id.nav_phases) {
            showPhases();
        } else if (id == R.id.nav_press) {
            helpURL("https://sites.google.com/view/diplicity/home/documentation/diplomatic-press");
            showPress();
        } else if (id == R.id.nav_phase_result) {
            showPhaseResults();
        } else if (id == R.id.nav_game_result) {
            showGameResults();
        } else if (id == R.id.nav_phase_settings) {
            showPhaseStates();
        } else if (id == R.id.nav_game_settings) {
            showGameStates();
        } else if (id == R.id.nav_variant_info) {
            showVariantInfo();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.game, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent i = new Intent(this, se.oort.diplicity.game.PreferenceActivity.class);
            i.putExtra(PreferenceActivity.GAME_ID_INTENT_KEY, game.ID);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}

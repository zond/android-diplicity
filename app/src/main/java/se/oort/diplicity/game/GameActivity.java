package se.oort.diplicity.game;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

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
import rx.functions.Func3;
import rx.observables.JoinObservable;
import se.oort.diplicity.App;
import se.oort.diplicity.ChannelService;
import se.oort.diplicity.MemberTable;
import se.oort.diplicity.MessagingService;
import se.oort.diplicity.OptionsService;
import se.oort.diplicity.R;
import se.oort.diplicity.RetrofitActivity;
import se.oort.diplicity.Sendable;
import se.oort.diplicity.UserView;
import se.oort.diplicity.VariantService;
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
import se.oort.diplicity.apigen.Unit;
import se.oort.diplicity.apigen.UnitWrapper;
import se.oort.diplicity.util.Counter;

public class GameActivity extends RetrofitActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String SERIALIZED_GAME_KEY = "serialized_game";
    public static final String SERIALIZED_PHASES_KEY = "serialized_phases";

    // Included in the intent bundle.
    public Game game;
    // Included in game if the game is started.
    public PhaseMeta phaseMeta;
    // Calculated from the intent bundle.
    public Member member;
    // Included in the intent bundle if the game is started.
    private MultiContainer<Phase> phases;

    // Used to receive orders from the user.
    public Map<String, OptionsService.Option> options = new HashMap<>();
    // Used to update/render orders on the map.
    public Map<String, Order> orders = Collections.synchronizedMap(new HashMap<String, Order>());

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
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        return intent;
    }

    @Override
    protected boolean consumeDiplicityJSON(MessagingService.DiplicityJSON diplicityJSON) {
        if (diplicityJSON.type.equals("phase") && diplicityJSON.gameID.equals(game.ID)) {
            phases = null;
            game.NewestPhaseMeta.set(0, diplicityJSON.phaseMeta);
            Toast.makeText(this, R.string.the_game_has_a_new_phase, Toast.LENGTH_SHORT).show();
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
                phaseMeta = game.NewestPhaseMeta.get(0);
            }
        }

        byte[] serializedPhases = getIntent().getByteArrayExtra(SERIALIZED_PHASES_KEY);
        if (serializedPhases != null) {
            phases = (MultiContainer<Phase>) unserialize(serializedPhases);
        }
    }

    public void lastPhase() {
        Gson gson = new Gson();
        phaseMeta = gson.fromJson(gson.toJson(phases.Properties.get(game.NewestPhaseMeta.get(0).PhaseOrdinal.intValue() - 1).Properties), PhaseMeta.class);
        draw();
    }

    public void firstPhase() {
        Gson gson = new Gson();
        phaseMeta = gson.fromJson(gson.toJson(phases.Properties.get(0).Properties), PhaseMeta.class);
        draw();
    }

    public void nextPhase() {
        if (phaseMeta != null && phaseMeta.PhaseOrdinal < game.NewestPhaseMeta.get(0).PhaseOrdinal) {
            Gson gson = new Gson();
            phaseMeta = gson.fromJson(gson.toJson(phases.Properties.get(phaseMeta.PhaseOrdinal.intValue()).Properties), PhaseMeta.class);
            draw();
        }
    }

    public void prevPhase() {
        if (phaseMeta != null && phaseMeta.PhaseOrdinal > 1) {
            Gson gson = new Gson();
            phaseMeta = gson.fromJson(gson.toJson(phases.Properties.get(phaseMeta.PhaseOrdinal.intValue() - 2).Properties), PhaseMeta.class);
            draw();
        }
    }

    public String gameTitle() {
        String descPart = getResources().getString(R.string.untitled);
        if (member != null && member.GameAlias != null && !member.GameAlias.equals("")) {
            descPart = member.GameAlias;
        } else if (game != null && !game.Desc.equals("")) {
            descPart = game.Desc;
        }
        if (phaseMeta == null) {
            return descPart;
        }
        if (phaseMeta.NextDeadlineIn.nanos == 0) {
            return getResources().getString(R.string.desc_season_year_type, descPart, phaseMeta.Season, phaseMeta.Year, phaseMeta.Type);
        }
        return getResources().getString(R.string.desc_season_year_type_deadline, descPart, phaseMeta.Season, phaseMeta.Year, phaseMeta.Type, App.millisToDuration(phaseMeta.NextDeadlineIn.millisLeft()));
    }

    public void draw() {
        setTitle(gameTitle());

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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
        if (phaseMeta == null || !phaseMeta.Resolved) {
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

    private void setOrder(String province, List<String> parts) {
        Sendable<SingleContainer<Order>> handler = new Sendable<SingleContainer<Order>>() {
            @Override
            public void send(SingleContainer<Order> orderSingleContainer) {
                MapView mv = (MapView) findViewById(R.id.map_view);
                mv.evaluateJS("window.map.removeOrders()");
                for (Map.Entry<String, Order> entry : orders.entrySet()) {
                    mv.evaluateJS("window.map.addOrder(" + new Gson().toJson(entry.getValue().Parts) + ", col" + entry.getValue().Nation + ");");
                }

            }
        };
        if (parts == null || parts.size() == 0) {
            if (orders.containsKey(province)) {
                orders.remove(province);
                handleReq(
                        orderService.OrderDelete(game.ID, phaseMeta.PhaseOrdinal.toString(), province),
                        handler, getResources().getString(R.string.removing_order));
            }
        } else {
            Order order = new Order();
            order.GameID = game.ID;
            order.Nation = member.Nation;
            order.PhaseOrdinal = phaseMeta.PhaseOrdinal;
            order.Parts = parts;
            orders.put(province, order);
            handleReq(
                    orderService.OrderCreate(order, game.ID, phaseMeta.PhaseOrdinal.toString()),
                    handler, getResources().getString(R.string.saving_order));
        }
    }

    private void completeOrder(final List<String> prefix, final Map<String, OptionsService.Option> opts, final String srcProvince) {
        Set<String> optionTypes = new HashSet<>();
        Set<String> optionValues = new HashSet<>();
        for (Map.Entry<String, OptionsService.Option> opt : opts.entrySet()) {
            optionTypes.add(opt.getValue().Type);
            optionValues.add(opt.getKey());
        }
        if (optionTypes.size() != 1) {
            Log.e("Diplicity", "Options contain multiple types: " + optionTypes);
            return;
        }
        String optionType = optionTypes.iterator().next();
        if (optionType.equals("Province")) {
            final MapView m = (MapView) findViewById(R.id.map_view);
            for (Map.Entry<String, OptionsService.Option> opt : opts.entrySet()) {
                m.evaluateJS("window.map.addClickListener('" + opt.getKey() + "', function(prov) { Android.provinceClicked(prov); });");
            }
            m.setOnClickedProvince(new Sendable<String>() {
                @Override
                public void send(final String s) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (!opts.containsKey(s)) {
                                return;
                            }
                            m.evaluateJS("window.map.clearClickListeners();");
                            prefix.add(s);
                            Map<String, OptionsService.Option> next = opts.get(s).Next;
                            if (next == null || next.isEmpty()) {
                                setOrder(srcProvince, prefix);
                                acceptOrders();
                            } else {
                                String newSrcProvince = srcProvince;
                                if (newSrcProvince == null) {
                                    newSrcProvince = s;
                                    if (newSrcProvince.indexOf('/') != -1) {
                                        newSrcProvince = newSrcProvince.substring(0, newSrcProvince.indexOf('/'));
                                    }
                                }
                                completeOrder(prefix, next, newSrcProvince);
                            }
                        }
                    });
                }
            });
        } else if (optionType.equals("OrderType") || optionType.equals("UnitType")) {
            final List<String> optionVals = new ArrayList<>(optionValues);
            Collections.sort(optionVals);
            optionVals.add(getResources().getString(R.string.cancel));
            new AlertDialog.Builder(this).setItems(optionVals.toArray(new CharSequence[optionVals.size()]), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (optionVals.get(i).equals(getResources().getString(R.string.cancel))) {
                        setOrder(srcProvince, null);
                        acceptOrders();
                        return;
                    }
                    prefix.add(optionVals.get(i));
                    Map<String, OptionsService.Option> next = opts.get(optionVals.get(i)).Next;
                    if (next == null || next.isEmpty()) {
                        setOrder(srcProvince, prefix);
                        acceptOrders();
                    } else {
                        completeOrder(prefix, next, srcProvince);
                    }
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    acceptOrders();
                }
            }).show();
        } else if (optionType.equals("SrcProvince")) {
            if (optionValues.size() != 1) {
                throw new RuntimeException("Multiple SrcProvince options: " + optionValues);
            }
            prefix.set(0, optionValues.iterator().next());
            Map<String, OptionsService.Option> next = opts.get(optionValues.iterator().next()).Next;
            if (next == null || next.isEmpty()) {
                setOrder(srcProvince, prefix);
                acceptOrders();
            } else {
                completeOrder(prefix, next, srcProvince);
            }
        }

    }

    public void acceptOrders() {
        completeOrder(new ArrayList<String>(), options, null);
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

        final MemberTable phaseStateView = (MemberTable) findViewById(R.id.phase_state_view);
        phaseStateView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                flickFrameLayout.onTouchEvent(motionEvent);
                phaseStateView.onTouchEvent(motionEvent);
                return true;
            }
        });

        handleReq(
                phaseStateService.ListPhaseStates(game.ID, phaseMeta.PhaseOrdinal.toString()),
                new Sendable<MultiContainer<PhaseState>>() {
                    @Override
                    public void send(MultiContainer<PhaseState> phaseStateMultiContainer) {
                        List<PhaseState> phaseStates = new ArrayList<PhaseState>();
                        for (SingleContainer<PhaseState> phaseStateSingleContainer : phaseStateMultiContainer.Properties) {
                            phaseStates.add(phaseStateSingleContainer.Properties);
                        }
                        phaseStateView.setPhaseStates(game, phaseMeta, phaseStates);
                        phaseStateView.setMembers(GameActivity.this, game, game.Members);
                    }
                }, getResources().getString(R.string.loading_phase_settings));
    }

    public void showPhases(boolean loadPhases) {
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
                phaseMeta = gson.fromJson(gson.toJson(phaseList.get(i).phase), PhaseMeta.class);
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
        if (phaseMeta.Resolved) {
            handleReq(
                    phaseResultService.PhaseResultLoad(game.ID, phaseMeta.PhaseOrdinal.toString()),
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
        if (phaseMeta != null && phaseMeta.PhaseOrdinal != null) {
            SingleContainer<Phase> phaseSingleContainer = phases.Properties.get(phaseMeta.PhaseOrdinal.intValue() - 1);
            Counter<String> scCount = new Counter<String>();
            for (SC sc : phaseSingleContainer.Properties.SCs) {
                scCount.increment(sc.Owner);
            }

            Counter<String> unitCount = new Counter<String>();
            for (UnitWrapper wrapper : phaseSingleContainer.Properties.Units) {
                unitCount.increment(wrapper.Unit.Nation);
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
                addPhaseStatusRow(phaseStatusInnerView, layoutInflater, nation,
                        String.valueOf(scCount.get(nation)),
                        String.valueOf(unitCount.get(nation)),
                        String.format("%+d", scCount.get(nation) - unitCount.get(nation)));
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
     * @param delta The string for the delta column.
     */
    private void addPhaseStatusRow(ViewGroup phaseStatusInnerView, LayoutInflater layoutInflater, String nation, String scCount, String unitCount, String delta) {
        View itemView = layoutInflater.inflate(R.layout.phase_status_row, phaseStatusInnerView, false);
        TextView nationView = (TextView) itemView.findViewById(R.id.nation);
        nationView.setText(nation);
        TextView scCountView = (TextView) itemView.findViewById(R.id.sc_count);
        scCountView.setText(scCount);
        TextView unitCountView = (TextView) itemView.findViewById(R.id.unit_count);
        unitCountView.setText(unitCount);

        if (delta != null) {
            TextView unitDeltaView = (TextView) itemView.findViewById(R.id.unit_delta);
            unitDeltaView.setText(delta);
        }
        phaseStatusInnerView.addView(itemView);
    }

    public void showOrders() {
        hideAllExcept(R.id.orders_view);
        handleReq(orderService.ListOrders(game.ID, phaseMeta.PhaseOrdinal.toString()), new Sendable<MultiContainer<Order>>() {
            @Override
            public void send(MultiContainer<Order> orderMultiContainer) {
                SingleContainer<Phase> phaseSingleContainer = phases.Properties.get(phaseMeta.PhaseOrdinal.intValue() - 1);
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

        final Sendable<String> renderer = new Sendable<String>() {
            @Override
            public void send(String url) {
                ((MapView) findViewById(R.id.map_view)).load(GameActivity.this, url);
            }
        };

        if (game.Started) {
            findViewById(R.id.rewind).setVisibility(View.VISIBLE);
            FloatingActionButton firstPhaseButton = (FloatingActionButton) findViewById(R.id.rewind);
            if (phaseMeta.PhaseOrdinal < 3) {
                firstPhaseButton.setEnabled(false);
                firstPhaseButton.setAlpha(0.3f);
            } else {
                firstPhaseButton.setEnabled(true);
                firstPhaseButton.setAlpha(1.0f);
                firstPhaseButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        firstPhase();
                    }
                });
            }
            findViewById(R.id.previous).setVisibility(View.VISIBLE);
            FloatingActionButton previousPhaseButton = (FloatingActionButton) findViewById(R.id.previous);
            if (phaseMeta.PhaseOrdinal < 2) {
                previousPhaseButton.setEnabled(false);
                previousPhaseButton.setAlpha(0.3f);
            } else {
                previousPhaseButton.setEnabled(true);
                previousPhaseButton.setAlpha(1.0f);
                previousPhaseButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        prevPhase();
                    }
                });
            }
            findViewById(R.id.next).setVisibility(View.VISIBLE);
            FloatingActionButton nextPhaseButton = (FloatingActionButton) findViewById(R.id.next);
            if (game.NewestPhaseMeta != null && game.NewestPhaseMeta.get(0).PhaseOrdinal <= phaseMeta.PhaseOrdinal) {
                nextPhaseButton.setEnabled(false);
                nextPhaseButton.setAlpha(0.3f);
            } else {
                nextPhaseButton.setEnabled(true);
                nextPhaseButton.setAlpha(1.0f);
                nextPhaseButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        nextPhase();
                    }
                });
            }
            FloatingActionButton lastPhaseButton = (FloatingActionButton) findViewById(R.id.fast_forward);
            if (game.NewestPhaseMeta != null && game.NewestPhaseMeta.get(0).PhaseOrdinal <= phaseMeta.PhaseOrdinal + 1) {
                lastPhaseButton.setEnabled(false);
                lastPhaseButton.setAlpha(0.3f);
            } else{
                lastPhaseButton.setEnabled(true);
                lastPhaseButton.setAlpha(1.0f);
                findViewById(R.id.fast_forward).setVisibility(View.VISIBLE);
                lastPhaseButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        lastPhase();
                    }
                });
            }
            String url = getBaseURL() + "Game/" + game.ID + "/Phase/" + phaseMeta.PhaseOrdinal + "/Map";
            if (getLocalDevelopmentMode() && !getLocalDevelopmentModeFakeID().equals("")) {
                url = url + "?fake-id=" + getLocalDevelopmentModeFakeID();
            }
            renderer.send(url);
            if (member != null && !phaseMeta.Resolved) {
                handleReq(JoinObservable.when(JoinObservable
                        .from(optionsService.GetOptions(game.ID, phaseMeta.PhaseOrdinal.toString()))
                        .and(orderService.ListOrders(game.ID, phaseMeta.PhaseOrdinal.toString()))
                        .then(new Func2<SingleContainer<Map<String,OptionsService.Option>>, MultiContainer<Order>, Object>() {
                            @Override
                            public Object call(SingleContainer<Map<String, OptionsService.Option>> opts, MultiContainer<Order> ords) {
                                SingleContainer<Phase> phase = phases.Properties.get(phaseMeta.PhaseOrdinal.intValue() - 1);
                                options = opts.Properties;
                                orders.clear();
                                for (SingleContainer<Order> orderContainer : ords.Properties) {
                                    String srcProvince = orderContainer.Properties.Parts.get(0);
                                    if (srcProvince.indexOf('/') != -1) {
                                        srcProvince = srcProvince.substring(0, srcProvince.indexOf('/'));
                                    }
                                    orders.put(srcProvince, orderContainer.Properties);
                                }
                                boolean hasBuildOpts = false;
                                boolean hasDisbandOpts = false;
                                for (OptionsService.Option opt : options.values()) {
                                    if (opt.Next.containsKey("Build")) {
                                        hasBuildOpts = true;
                                    }
                                    if (opt.Next.containsKey("Disband")) {
                                        hasDisbandOpts = true;
                                    }
                                }
                                int units = 0;
                                int scs = 0;
                                for (UnitWrapper unit : phase.Properties.Units) {
                                    if (unit.Unit.Nation.equals(member.Nation)) {
                                        units++;
                                    }
                                }
                                for (SC sc : phase.Properties.SCs) {
                                    if (sc.Owner.equals(member.Nation)) {
                                        scs++;
                                    }
                                }
                                if (hasBuildOpts && units < scs) {
                                    Toast.makeText(GameActivity.this,
                                            getResources().getString(R.string.you_can_build_n_this_phase,
                                                    getResources().getQuantityString(R.plurals.unit,
                                                            scs - units,
                                                            scs - units)),
                                            Toast.LENGTH_LONG).show();
                                } else if (hasDisbandOpts && scs < units) {
                                    Toast.makeText(GameActivity.this,
                                            getResources().getString(R.string.you_have_to_disband_n_this_phase,
                                                    getResources().getQuantityString(R.plurals.unit,
                                                            units - scs,
                                                            units - scs)),
                                            Toast.LENGTH_LONG).show();
                                }
                                return null;
                            }
                        })).toObservable(), new Sendable<Object>() {
                    @Override
                    public void send(Object o) {
                        acceptOrders();
                    }
                }, getResources().getString(R.string.loading_state));
            }
        } else {
            findViewById(R.id.rewind).setVisibility(View.GONE);
            findViewById(R.id.previous).setVisibility(View.GONE);
            findViewById(R.id.next).setVisibility(View.GONE);
            findViewById(R.id.fast_forward).setVisibility(View.GONE);
            handleReq(variantService.GetStartPhase(game.Variant), new Sendable<SingleContainer<VariantService.Phase>>() {
                @Override
                public void send(SingleContainer<VariantService.Phase> phaseSingleContainer) {
                    String url = null;
                    for (Link link : phaseSingleContainer.Links) {
                        if (link.Rel.equals("map")) {
                            url = link.URL;
                            break;
                        }
                    }
                    if (url != null) {
                        renderer.send(url);
                    } else {
                        App.firebaseCrashReport("No map URL found in variant " + game.Variant + "?");
                        Toast.makeText(getBaseContext(), R.string.unknown_error, Toast.LENGTH_SHORT).show();
                    }
                }
            }, getResources().getString(R.string.loading_start_state));
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        navigateTo(item.getItemId());
        return true;
    }

    private void navigateTo(int id) {
        int oldView = currentView;
        currentView = id;
        if (id == R.id.nav_map) {
            showMap();
        } else if (id == R.id.nav_orders) {
            showOrders();
        } else if (id == R.id.nav_phase_status) {
            showPhaseStatus();
        } else if (id == R.id.nav_phases) {
            showPhases(oldView != currentView);
        } else if (id == R.id.nav_press) {
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

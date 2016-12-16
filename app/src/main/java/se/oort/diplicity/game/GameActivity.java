package se.oort.diplicity.game;

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
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.adapter.rxjava.HttpException;
import rx.functions.Func2;
import rx.observables.JoinObservable;
import se.oort.diplicity.App;
import se.oort.diplicity.ChannelService;
import se.oort.diplicity.MainActivity;
import se.oort.diplicity.MemberAdapter;
import se.oort.diplicity.OptionsService;
import se.oort.diplicity.R;
import se.oort.diplicity.RetrofitActivity;
import se.oort.diplicity.Sendable;
import se.oort.diplicity.VariantService;
import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.GameResult;
import se.oort.diplicity.apigen.Link;
import se.oort.diplicity.apigen.Member;
import se.oort.diplicity.apigen.MultiContainer;
import se.oort.diplicity.apigen.Order;
import se.oort.diplicity.apigen.Phase;
import se.oort.diplicity.apigen.PhaseMeta;
import se.oort.diplicity.apigen.PhaseResult;
import se.oort.diplicity.apigen.Resolution;
import se.oort.diplicity.apigen.SingleContainer;

public class GameActivity extends RetrofitActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String SERIALIZED_GAME_KEY = "serialized_game";
    public static final String SERIALIZED_PHASE_META_KEY = "serialized_phase_meta";

    // Included in the intent bundle.
    public Game game;
    // Included in the intent bundle.
    public PhaseMeta phaseMeta;
    // Calculated from the intent bundle.
    public Member member;

    // Used to receive orders from the user.
    public Map<String, OptionsService.Option> options = new HashMap<>();
    // Used to update/render orders on the map.
    public Map<String, Order> orders = Collections.synchronizedMap(new HashMap<String, Order>());

    // Used to swipe between phases.
    private FlickFrameLayout flickFrameLayout;
    // Used to remember which view we are in (map, orders, phases etc).
    private int currentView;
    // Used to display the phases view again after clicking it, without having to load all phases again.
    private MultiContainer<Phase> phases;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        byte[] serializedGame = getIntent().getByteArrayExtra(SERIALIZED_GAME_KEY);
        game = (Game) unserialize(serializedGame);
        byte[] serializedPhaseMeta = getIntent().getByteArrayExtra(SERIALIZED_PHASE_META_KEY);
        if (serializedPhaseMeta != null) {
            phaseMeta = (PhaseMeta) unserialize(serializedPhaseMeta);
        }
    }

    public void onResume() {
        super.onResume();
        draw();
    }

    public void nextPhase() {
        if (phaseMeta != null && phaseMeta.PhaseOrdinal < game.NewestPhaseMeta.get(0).PhaseOrdinal) {
            handleReq(
                    phaseService.PhaseLoad(game.ID, "" + (phaseMeta.PhaseOrdinal + 1)),
                    new Sendable<SingleContainer<Phase>>() {
                        @Override
                        public void send(SingleContainer<Phase> phaseSingleContainer) {
                            Gson gson = new Gson();
                            phaseMeta = gson.fromJson(gson.toJson(phaseSingleContainer.Properties), PhaseMeta.class);
                            draw();
                        }
                    }, getResources().getString(R.string.loading_state));
        }
    }

    public void prevPhase() {
        if (phaseMeta != null && phaseMeta.PhaseOrdinal > 1) {
            handleReq(
                    phaseService.PhaseLoad(game.ID, "" + (phaseMeta.PhaseOrdinal - 1)),
                    new Sendable<SingleContainer<Phase>>() {
                        @Override
                        public void send(SingleContainer<Phase> phaseSingleContainer) {
                            Gson gson = new Gson();
                            phaseMeta = gson.fromJson(gson.toJson(phaseSingleContainer.Properties), PhaseMeta.class);
                            draw();
                        }
                    }, getResources().getString(R.string.loading_state));
        }
    }

    public void draw() {
        if (phaseMeta != null) {
            setTitle(getResources().getString(R.string.desc_season_year_type, game.Desc, phaseMeta.Season, phaseMeta.Year, phaseMeta.Type));
        } else {
            setTitle(game.Desc);
        }

        for (Member m : game.Members) {
            if (m.User.Id.equals(App.loggedInUser.Id)) {
                member = m;
            }
        }

        setContentView(R.layout.activity_game);

        flickFrameLayout = (FlickFrameLayout) findViewById(R.id.game_content);
        flickFrameLayout.gameActivity = this;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        Menu nav_Menu = navigationView.getMenu();
        if (!game.Started) {
            nav_Menu.findItem(R.id.nav_orders).setVisible(false);
            nav_Menu.findItem(R.id.nav_phases).setVisible(false);
            nav_Menu.findItem(R.id.nav_press).setVisible(false);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.game, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setVisibility(int visibility, int... views) {
        for (int i = 0; i < views.length; i++)
            findViewById(views[i]).setVisibility(visibility);
    }

    public void hideAllExcept(int toShow) {
        for (int viewID : new int[]{
                R.id.map_view,
                R.id.orders_view,
                R.id.phases_view,
                R.id.press_view,
                R.id.phase_results_view,
                R.id.game_results_view
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
            orders.put(parts.get(0), order);
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
        public String toString() {
            return getResources().getString(R.string.season_year_type, phase.Season, phase.Year, phase.Type);
        }
    }

    private class ChannelElement {
        public ChannelService.Channel channel;
        public ChannelElement(ChannelService.Channel channel) {
            this.channel = channel;
        }
        public String toString() {
            return getResources().getString(
                    R.string.members_unread_total,
                    TextUtils.join(", ", channel.Members),
                    channel.NMessagesSince.NMessages, channel.NMessages);
        }
    }

    public void showPress() {
        hideAllExcept(R.id.press_view);
        if (member != null) {
            ((FloatingActionButton) findViewById(R.id.create_channel_button)).setOnClickListener(new View.OnClickListener() {
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
                                }
                            }).setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int j) {
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
                            Intent intent = new Intent(GameActivity.this, PressActivity.class);
                            intent.putExtra(PressActivity.SERIALIZED_CHANNEL_KEY, serialize(channel));
                            intent.putExtra(PressActivity.SERIALIZED_MEMBER_KEY, serialize(member));
                            intent.putExtra(PressActivity.SERIALIZED_GAME_KEY, serialize(game));
                            startActivity(intent);
                            dialogInterface.dismiss();
                        }
                    }).show();

                }
            });
        }
        handleReq(
                channelService.ListChannels(game.ID),
                new Sendable<MultiContainer<ChannelService.Channel>>() {
                    @Override
                    public void send(MultiContainer<ChannelService.Channel> channelMultiContainer) {
                        final List<ChannelElement> channels = new ArrayList<>();
                        for (SingleContainer<ChannelService.Channel> channelSingleContainer : channelMultiContainer.Properties) {
                            channels.add(new ChannelElement(channelSingleContainer.Properties));
                        }
                        ListView pressView = (ListView) findViewById(R.id.press_view);
                        pressView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                Intent intent = new Intent(GameActivity.this, PressActivity.class);
                                intent.putExtra(PressActivity.SERIALIZED_CHANNEL_KEY, serialize(channels.get(i).channel));
                                intent.putExtra(PressActivity.SERIALIZED_GAME_KEY, serialize(game));
                                if (member != null) {
                                    intent.putExtra(PressActivity.SERIALIZED_MEMBER_KEY, serialize(member));
                                }
                                startActivity(intent);
                            }
                        });
                        pressView.setAdapter(new ArrayAdapter<ChannelElement>(GameActivity.this, android.R.layout.simple_list_item_1, channels));
                    }
                }, getResources().getString(R.string.loading_channels));
    }

    public void showPhases(boolean loadPhases) {
        hideAllExcept(R.id.phases_view);
        final Sendable<MultiContainer<Phase>> renderer = new Sendable<MultiContainer<Phase>>() {
            @Override
            public void send(MultiContainer<Phase> phaseMultiContainer) {
                final List<PhaseElement> phases = new ArrayList<>();
                for (SingleContainer<Phase> phaseSingleContainer : phaseMultiContainer.Properties) {
                    phases.add(new PhaseElement(phaseSingleContainer.Properties));
                }
                ListView phasesView = (ListView) findViewById(R.id.phases_view);
                phasesView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        Gson gson = new Gson();
                        phaseMeta = gson.fromJson(gson.toJson(phases.get(i).phase), PhaseMeta.class);
                        draw();
                    }
                });
                phasesView.setAdapter(new ArrayAdapter<PhaseElement>(GameActivity.this, android.R.layout.simple_list_item_1, phases));
            }
        };
        if (loadPhases || phases == null) {
            handleReq(
                    phaseService.ListPhases(game.ID),
                    new Sendable<MultiContainer<Phase>>() {
                        @Override
                        public void send(MultiContainer<Phase> phaseMultiContainer) {
                            phases = phaseMultiContainer;
                            renderer.send(phaseMultiContainer);
                        }
                    }, getResources().getString(R.string.loading_phases));
        } else {
            renderer.send(phases);
        }
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

        ((LinearLayout) findViewById(R.id.game_results_view)).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                flickFrameLayout.onTouchEvent(motionEvent);
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
                                populater.populate((RecyclerView) findViewById(R.id.solo_winner),
                                        Arrays.asList(gameResultSingleContainer.Properties.SoloWinnerUser));
                                setVisibility(View.VISIBLE, R.id.solo_winner, R.id.solo_winner_label);
                            }
                            if (gameResultSingleContainer.Properties.DIASUsers != null) {
                                populater.populate((RecyclerView) findViewById(R.id.dias_members),
                                        gameResultSingleContainer.Properties.DIASUsers);
                                setVisibility(View.VISIBLE, R.id.dias_members, R.id.dias_members_label);
                            }
                            if (gameResultSingleContainer.Properties.EliminatedUsers != null) {
                                populater.populate((RecyclerView) findViewById(R.id.eliminated_members),
                                        gameResultSingleContainer.Properties.EliminatedUsers);
                                setVisibility(View.VISIBLE, R.id.eliminated_members, R.id.eliminated_members_label);
                            }
                            if (gameResultSingleContainer.Properties.NMRUsers != null) {
                                populater.populate((RecyclerView) findViewById(R.id.nmr_game_members),
                                        gameResultSingleContainer.Properties.NMRUsers);
                                setVisibility(View.VISIBLE, R.id.nmr_game_members, R.id.nmr_game_members_label);
                            }
                            if (gameResultSingleContainer.Properties.Scores != null) {
                                populater.populate((RecyclerView) findViewById(R.id.scored_members),
                                        gameResultSingleContainer.Properties.AllUsers);
                                ((MemberAdapter) ((RecyclerView) findViewById(R.id.scored_members)).getAdapter())
                                        .setScores(gameResultSingleContainer.Properties.Scores);
                                setVisibility(View.VISIBLE, R.id.scored_members, R.id.scored_members_label);
                            }
                        }
                    }, getResources().getString(R.string.loading_game_result));
        }
    }

    private class MemberListPopulater {
        public void populate(RecyclerView view, List<String> uids) {
            List<Member> members = new ArrayList<Member>();
            for (Member member : game.Members) {
                if (uids.contains(member.User.Id)) {
                    members.add(member);
                }
            }
            LinearLayoutManager membersLayoutManager = new LinearLayoutManager(GameActivity.this);
            membersLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            view.setLayoutManager(membersLayoutManager);
            view.setAdapter(new MemberAdapter(GameActivity.this, members, null, new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    flickFrameLayout.onTouchEvent(motionEvent);
                    return true;
                }
            }));
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
        ((LinearLayout) findViewById(R.id.phase_results_view)).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                flickFrameLayout.onTouchEvent(motionEvent);
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
                                populater.populate((RecyclerView) findViewById(R.id.nmr_members), phaseResultSingleContainer.Properties.NMRUsers);
                                setVisibility(View.VISIBLE, R.id.nmr_members, R.id.nmr_members_label);
                            }
                            if (phaseResultSingleContainer.Properties.ActiveUsers != null) {
                                populater.populate((RecyclerView) findViewById(R.id.active_members), phaseResultSingleContainer.Properties.ActiveUsers);
                                setVisibility(View.VISIBLE, R.id.active_members, R.id.active_members_label);
                            }
                            if (phaseResultSingleContainer.Properties.ReadyUsers != null) {
                                populater.populate((RecyclerView) findViewById(R.id.ready_members), phaseResultSingleContainer.Properties.ReadyUsers);
                                setVisibility(View.VISIBLE, R.id.ready_members, R.id.ready_members_label);
                            }
                        }
                    }, new ErrorHandler(404, new Sendable<HttpException>() {
                        @Override
                        public void send(HttpException e) {

                        }
                    }), getResources().getString(R.string.loading_phase_result));
        }
    }

    public void showOrders() {
        hideAllExcept(R.id.orders_view);
        handleReq(
                JoinObservable.when(JoinObservable
                        .from(orderService.ListOrders(game.ID, phaseMeta.PhaseOrdinal.toString()))
                        .and(phaseService.PhaseLoad(game.ID, phaseMeta.PhaseOrdinal.toString()))
                        .then(new Func2<MultiContainer<Order>, SingleContainer<Phase>, Object>() {
                            @Override
                            public Object call(MultiContainer<Order> orderMultiContainer, SingleContainer<Phase> phaseSingleContainer) {
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
                                ListView ordersView = (ListView) findViewById(R.id.orders_view);
                                ordersView.setOnTouchListener(new View.OnTouchListener() {
                                    @Override
                                    public boolean onTouch(View view, MotionEvent motionEvent) {
                                        flickFrameLayout.onTouchEvent(motionEvent);
                                        return true;
                                    }
                                });
                                ordersView.setAdapter(new ArrayAdapter<String>(GameActivity.this, android.R.layout.simple_list_item_1, orders));
                                return null;
                            }
                        })).toObservable(),
                null,
                getResources().getString(R.string.loading_orders));
    }

    public void showMap() {
        hideAllExcept(R.id.map_view);

        final Sendable<String> renderer = new Sendable<String>() {
            @Override
            public void send(String url) {
                ((MapView) findViewById(R.id.map_view)).load(url);
            }
        };

        if (game.Started) {
            renderer.send(App.baseURL + "Game/" + game.ID + "/Phase/" + phaseMeta.PhaseOrdinal + "/Map");
            if (member != null && !phaseMeta.Resolved) {
                handleReq(JoinObservable.when(JoinObservable
                        .from(optionsService.GetOptions(game.ID, phaseMeta.PhaseOrdinal.toString()))
                        .and(orderService.ListOrders(game.ID, phaseMeta.PhaseOrdinal.toString()))
                        .then(new Func2<SingleContainer<Map<String, OptionsService.Option>>, MultiContainer<Order>, Object>() {
                            @Override
                            public Object call(SingleContainer<Map<String, OptionsService.Option>> opts, MultiContainer<Order> ords) {
                                options = opts.Properties;
                                orders.clear();
                                for (SingleContainer<Order> orderContainer : ords.Properties) {
                                    orders.put(orderContainer.Properties.Parts.get(0), orderContainer.Properties);
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
        } else if (id == R.id.nav_phases) {
            showPhases(oldView != currentView);
        } else if (id == R.id.nav_press) {
            showPress();
        } else if (id == R.id.nav_phase_result) {
            showPhaseResults();
        } else if (id == R.id.nav_game_result) {
            showGameResults();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }
}

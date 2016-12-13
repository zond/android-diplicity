package se.oort.diplicity.game;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rx.functions.Func2;
import rx.observables.JoinObservable;
import se.oort.diplicity.App;
import se.oort.diplicity.OptionsService;
import se.oort.diplicity.R;
import se.oort.diplicity.RetrofitActivity;
import se.oort.diplicity.Sendable;
import se.oort.diplicity.VariantService;
import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.Link;
import se.oort.diplicity.apigen.Member;
import se.oort.diplicity.apigen.MultiContainer;
import se.oort.diplicity.apigen.Order;
import se.oort.diplicity.apigen.PhaseMeta;
import se.oort.diplicity.apigen.SingleContainer;

public class GameActivity extends RetrofitActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String SERIALIZED_GAME_KEY = "serialized_game";

    public Game game;
    public Member member;
    public Map<String, OptionsService.Option> options = new HashMap<>();
    public Map<String, Order> orders = Collections.synchronizedMap(new HashMap<String, Order>());

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        byte[] serializedGame = getIntent().getByteArrayExtra(SERIALIZED_GAME_KEY);
        game = (Game) unserialize(serializedGame);

        for (Member m : game.Members) {
            if (m.User.Id.equals(App.loggedInUser.Id)) {
                member = m;
            }
        }

        if (game.NewestPhaseMeta != null && game.NewestPhaseMeta.size() > 0) {
            PhaseMeta pm = game.NewestPhaseMeta.get(0);
            setTitle(getResources().getString(R.string.desc_season_year_type, game.Desc, pm.Season, pm.Year, pm.Type));
        } else {
            setTitle(game.Desc);
        }

        setContentView(R.layout.activity_game);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (!game.Started) {
            navigationView = (NavigationView) findViewById(R.id.nav_view);
            Menu nav_Menu = navigationView.getMenu();
            nav_Menu.findItem(R.id.nav_orders).setVisible(false);
        }

        showMap();
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

    public void hideAllExcept(int toShow) {
        for (int viewID : new int[]{R.id.map_view, R.id.orders_view}) {
            if (viewID == toShow) {
                findViewById(viewID).setVisibility(View.VISIBLE);
            } else {
                findViewById(viewID).setVisibility(View.GONE);
            }
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
            orders.remove(province);
            handleReq(
                    orderService.OrderDelete(game.ID, game.NewestPhaseMeta.get(0).PhaseOrdinal.toString(), province),
                    handler, getResources().getString(R.string.removing_order));
        } else {
            Order order = new Order();
            order.GameID = game.ID;
            order.Nation = member.Nation;
            order.PhaseOrdinal = game.NewestPhaseMeta.get(0).PhaseOrdinal;
            order.Parts = parts;
            orders.put(parts.get(0), order);
            handleReq(
                    orderService.OrderCreate(order, game.ID, game.NewestPhaseMeta.get(0).PhaseOrdinal.toString()),
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
        } else if (optionType.equals("OrderType")) {
            final List<String> orderTypes = new ArrayList<>(optionValues);
            orderTypes.add(getResources().getString(R.string.cancel));
            Collections.sort(orderTypes);
            new AlertDialog.Builder(this).setItems(orderTypes.toArray(new CharSequence[orderTypes.size()]), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (orderTypes.get(i).equals(getResources().getString(R.string.cancel))) {
                        setOrder(srcProvince, null);
                        acceptOrders();
                        return;
                    }
                    prefix.add(orderTypes.get(i));
                    Map<String, OptionsService.Option> next = opts.get(orderTypes.get(i)).Next;
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

    public void showOrders() {
        hideAllExcept(R.id.orders_view);
        handleReq(
                orderService.ListOrders(game.ID, game.NewestPhaseMeta.get(0).PhaseOrdinal.toString()),
                new Sendable<MultiContainer<Order>>() {
                    @Override
                    public void send(MultiContainer<Order> orderMultiContainer) {
                        List<String> orders = new ArrayList<String>();
                        for (SingleContainer<Order> orderContainer : orderMultiContainer.Properties) {
                            orders.add(getResources().getString(R.string.nation_order, orderContainer.Properties.Nation, TextUtils.join(" ", orderContainer.Properties.Parts)));
                        }
                        Collections.sort(orders);
                        ListView ordersView = (ListView) findViewById(R.id.orders_view);
                        ordersView.setEnabled(false);
                        ordersView.setAdapter(new ArrayAdapter<String>(GameActivity.this, android.R.layout.simple_list_item_1, orders));
                    }
                }, getResources().getString(R.string.loading_orders));
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
            PhaseMeta newestPhase = game.NewestPhaseMeta.get(0);
            renderer.send(App.baseURL + "Game/" + game.ID + "/Phase/" + newestPhase.PhaseOrdinal + "/Map");
            if (member != null) {
                handleReq(JoinObservable.when(JoinObservable
                        .from(optionsService.GetOptions(game.ID, newestPhase.PhaseOrdinal.toString()))
                        .and(orderService.ListOrders(game.ID, newestPhase.PhaseOrdinal.toString()))
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
        int id = item.getItemId();

        if (id == R.id.nav_map) {
            showMap();
        } else if (id == R.id.nav_orders) {
            showOrders();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}

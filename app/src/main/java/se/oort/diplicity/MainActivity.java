package se.oort.diplicity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.Link;
import se.oort.diplicity.apigen.MultiContainer;
import se.oort.diplicity.apigen.SingleContainer;
import se.oort.diplicity.apigen.UserStats;

public class MainActivity extends RetrofitActivity {

    private RecyclerView contentList;
    private EndlessRecyclerViewScrollListener scrollListener;

    private List<String> nextCursorContainer = new ArrayList<String>(Arrays.asList(new String[]{""}));
    private Callable<String> nextCursor = new Callable<String>() {
        @Override
        public String call() throws Exception {
            return nextCursorContainer.get(0);
        }
    };

    private List<Sendable<String>> loadMoreProcContainer = new ArrayList<>();

    private GamesAdapter gamesAdapter = new GamesAdapter(this, new ArrayList<SingleContainer<Game>>());
    private UserStatsAdapter userStatsAdapter = new UserStatsAdapter(this, new ArrayList<SingleContainer<UserStats>>());

    private List<List<Map<String, String>>> navigationChildGroups;
    private List<Map<String, String>> navigationRootGroups;
    private FloatingActionButton button;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        button = (FloatingActionButton) findViewById(R.id.add_game_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleReq(userStatsService.UserStatsLoad(getLoggedInUser().Id), new Sendable<SingleContainer<UserStats>>() {
                    @Override
                    public void send(SingleContainer<UserStats> userStatsSingleContainer) {
                        final List<UserStats> statsContainer = new ArrayList<>();
                        statsContainer.add(userStatsSingleContainer.Properties);

                        final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).setView(R.layout.create_game_dialog).show();
                        final Returner<Game, Boolean> validateGame = new Returner<Game, Boolean>() {
                            @Override
                            public Boolean Return(Game g) {
                                if (g.PhaseLengthMinutes != null && g.PhaseLengthMinutes > 30 * 24 * 60) {
                                    Toast.makeText(MainActivity.this, R.string.phase_length_must_be_less_than_30_days, Toast.LENGTH_LONG).show();
                                    return false;
                                }
                                UserStats us = statsContainer.get(0);
                                if (g.MinRating == null)
                                    g.MinRating = 0.0;
                                if (g.MinRating != 0.0 && g.MinRating > us.Glicko.PracticalRating) {
                                    Toast.makeText(MainActivity.this, getResources().getString(R.string.minimum_rating_must_be_below_your_rating_x, us.Glicko.PracticalRating), Toast.LENGTH_LONG).show();
                                    return false;
                                }
                                if (g.MaxRating == null)
                                    g.MaxRating = 0.0;
                                if (g.MaxRating != 0.0 && g.MaxRating < us.Glicko.PracticalRating) {
                                    Toast.makeText(MainActivity.this, getResources().getString(R.string.maximum_rating_must_be_above_your_rating_x, us.Glicko.PracticalRating), Toast.LENGTH_LONG).show();
                                    return false;
                                }
                                if (g.MinReliability == null)
                                    g.MinReliability = 0.0;
                                if (g.MinReliability != 0.0 && g.MinReliability > us.Reliability) {
                                    Toast.makeText(MainActivity.this, getResources().getString(R.string.minimum_reliability_must_be_below_your_reliability_x, us.Reliability), Toast.LENGTH_LONG).show();
                                    return false;
                                }
                                if (g.MinQuickness == null)
                                    g.MinQuickness = 0.0;
                                if (g.MinQuickness != 0.0 && g.MinQuickness > us.Quickness) {
                                    Toast.makeText(MainActivity.this, getResources().getString(R.string.minimum_quickness_must_be_below_your_quickness_x, us.Quickness), Toast.LENGTH_LONG).show();
                                    return false;
                                }
                                if (g.MaxHated == null)
                                    g.MaxHated = 0.0;
                                if (g.MaxHated != 0.0 && g.MaxHated < us.Hated) {
                                    Toast.makeText(MainActivity.this, getResources().getString(R.string.maximum_hated_must_be_above_your_hated_x, us.Hated), Toast.LENGTH_LONG).show();
                                    return false;
                                }
                                if (g.MaxHater == null)
                                    g.MaxHater = 0.0;
                                if (g.MaxHater != 0.0 && g.MaxHater < us.Hater) {
                                    Toast.makeText(MainActivity.this, getResources().getString(R.string.maximum_hater_must_be_above_your_hater_x, us.Hater), Toast.LENGTH_LONG).show();
                                    return false;
                                }
                                return true;
                            }
                        };
                        final Spinner variants = ((Spinner) dialog.findViewById(R.id.variants));
                        List<String> variantNames = new ArrayList<String>();
                        for (SingleContainer<VariantService.Variant> variantContainer : getVariants().Properties) {
                            variantNames.add(variantContainer.Properties.Name);
                        }
                        int classical = 0;
                        Collections.sort(variantNames);
                        for (int i = 0; i < variantNames.size(); i++) {
                            if (variantNames.get(i).equals("Classical")) {
                                classical = i;
                            }
                        }
                        ArrayAdapter<String> variantAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, variantNames);
                        variants.setAdapter(variantAdapter);
                        variantAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        variants.setSelection(classical);

                        ((FloatingActionButton) dialog.findViewById(R.id.create_game_button)).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                final Game game = new Game();
                                game.Desc = ((EditText) dialog.findViewById(R.id.desc)).getText().toString();
                                game.Variant = variants.getSelectedItem().toString();
                                try {
                                    game.PhaseLengthMinutes = Long.parseLong(((EditText) dialog.findViewById(R.id.phase_length)).getText().toString());
                                } catch (NumberFormatException e) {
                                }
                                try {
                                    game.MinRating = Double.parseDouble(((EditText) dialog.findViewById(R.id.min_rating)).getText().toString());
                                } catch (NumberFormatException e) {
                                }
                                try {
                                    game.MaxRating = Double.parseDouble(((EditText) dialog.findViewById(R.id.max_rating)).getText().toString());
                                } catch (NumberFormatException e) {
                                }
                                try {
                                    game.MinReliability = Double.parseDouble(((EditText) dialog.findViewById(R.id.min_reliability)).getText().toString());
                                } catch (NumberFormatException e) {
                                }
                                try {
                                    game.MinQuickness = Double.parseDouble(((EditText) dialog.findViewById(R.id.min_quickness)).getText().toString());
                                } catch (NumberFormatException e) {
                                }
                                try {
                                    game.MaxHated = Double.parseDouble(((EditText) dialog.findViewById(R.id.max_hated)).getText().toString());
                                } catch (NumberFormatException e) {
                                }
                                try {
                                    game.MaxHater = Double.parseDouble(((EditText) dialog.findViewById(R.id.max_hater)).getText().toString());
                                } catch (NumberFormatException e) {
                                }
                                if (validateGame.Return(game)) {
                                    handleReq(gameService.GameCreate(game), new Sendable<SingleContainer<Game>>() {
                                        @Override
                                        public void send(SingleContainer<Game> gameSingleContainer) {
                                            if (nextCursorContainer.get(0).length() == 0) {
                                                findViewById(R.id.empty_view).setVisibility(View.GONE);
                                                contentList.setVisibility(View.VISIBLE);
                                                gamesAdapter.items.add(gameSingleContainer);
                                                gamesAdapter.notifyItemInserted(gamesAdapter.items.size() - 1);
                                            }
                                            dialog.dismiss();
                                        }
                                    }, new ErrorHandler(412, new Sendable<HttpException>() {
                                        @Override
                                        public void send(HttpException e) {
                                            handleReq(userStatsService.UserStatsLoad(getLoggedInUser().Id), new Sendable<SingleContainer<UserStats>>() {
                                                @Override
                                                public void send(SingleContainer<UserStats> userStatsSingleContainer) {
                                                    statsContainer.set(0, userStatsSingleContainer.Properties);
                                                    validateGame.Return(game);
                                                }
                                            }, getResources().getString(R.string.creating_game));
                                        }
                                    }), getResources().getString(R.string.creating_game));
                                }
                            }
                        });
                    }
                }, getResources().getString(R.string.loading_user_stats));
            }
        });

        setupNavigation();

        contentList = (RecyclerView) findViewById(R.id.content_list);
        LinearLayoutManager contentLayoutManager = new LinearLayoutManager(this);
        contentLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        contentList.setLayoutManager(contentLayoutManager);
        scrollListener = new EndlessRecyclerViewScrollListener(contentLayoutManager, nextCursor) {
            @Override
            public void onLoadMore(String cursor, int totalItemsCount, RecyclerView view) {
                if (cursor.length() > 0) {
                    loadMoreProcContainer.get(0).send(cursor);
                }
            }
        };
        contentList.addOnScrollListener(scrollListener);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, contentLayoutManager.getOrientation());
        contentList.addItemDecoration(dividerItemDecoration);

        loadMoreProcContainer.add(null);

        navigateTo(0, 0);
    }

    private void setupNavigation() {
        navigationRootGroups = new ArrayList<Map<String, String>>() {{
            add(new HashMap<String, String>() {{
                put("ROOT_NAME", getResources().getString(R.string.games));
            }});
            add(new HashMap<String, String>() {{
                put("ROOT_NAME", getResources().getString(R.string.users));
            }});
        }};
        navigationChildGroups = new ArrayList<List<Map<String, String>>>();

        List<Map<String, String>> childGroupForFirstGroupRow = new ArrayList<Map<String, String>>(){{
            add(new HashMap<String, String>() {{
                put("CHILD_NAME", getResources().getString(R.string.my_started));
            }});
            add(new HashMap<String, String>() {{
                put("CHILD_NAME", getResources().getString(R.string.my_staging));
            }});
            add(new HashMap<String, String>() {{
                put("CHILD_NAME", getResources().getString(R.string.my_finished));
            }});
            add(new HashMap<String, String>() {{
                put("CHILD_NAME", getResources().getString(R.string.open));
            }});
            add(new HashMap<String, String>() {{
                put("CHILD_NAME", getResources().getString(R.string.started));
            }});
            add(new HashMap<String, String>() {{
                put("CHILD_NAME", getResources().getString(R.string.finished));
            }});
        }};
        navigationChildGroups.add(childGroupForFirstGroupRow);

        List<Map<String, String>> childGroupForSecondGroupRow = new ArrayList<Map<String, String>>(){{
            add(new HashMap<String, String>() {{
                put("CHILD_NAME", getResources().getString(R.string.top_rated));
            }});
            add(new HashMap<String, String>() {{
                put("CHILD_NAME", getResources().getString(R.string.top_reliable));
            }});
            add(new HashMap<String, String>() {{
                put("CHILD_NAME", getResources().getString(R.string.top_quick));
            }});
            add(new HashMap<String, String>() {{
                put("CHILD_NAME", getResources().getString(R.string.top_hated));
            }});
            add(new HashMap<String, String>() {{
                put("CHILD_NAME", getResources().getString(R.string.top_hater));
            }});
        }};
        navigationChildGroups.add(childGroupForSecondGroupRow);

        SimpleExpandableListAdapter navigationListAdapter = new SimpleExpandableListAdapter(
                this,
                navigationRootGroups,
                android.R.layout.simple_expandable_list_item_1,
                new String[] { "ROOT_NAME" },
                new int[] { android.R.id.text1 },
                navigationChildGroups,
                android.R.layout.simple_expandable_list_item_1,
                new String[] { "CHILD_NAME" },
                new int[] { android.R.id.text1 }
        );

        ExpandableListView navigationList = (ExpandableListView) findViewById(R.id.nav_list);
        navigationList.setAdapter(navigationListAdapter);
        connectNavigationList(navigationList);

    }

    private void connectNavigationList(ExpandableListView navigationList) {
        navigationList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i1, long l) {
                navigateTo(i, i1);
                return false;
            }
        });
    }

    private void navigateTo(final int root, final int child) {
        if (root == 0 && child == 1) {
            button.setVisibility(View.VISIBLE);
        } else {
            button.setVisibility(View.GONE);
        }
        switch (root) {
        case 0: // Games
            switch (child) {
            case 0: // My started
                loadMoreProcContainer.set(0, new Sendable<String>() {
                    @Override
                    public void send(String s) {
                        appendItems(gameService.ListMyStartedGames(null, null, null, null, null, null, null, null, s), null, navigationRootGroups.get(root).get("ROOT_NAME").toLowerCase(), gamesAdapter);
                    }
                });
                displayItems(gameService.ListMyStartedGames(null, null, null, null, null, null, null, null, null), navigationChildGroups.get(root).get(child).get("CHILD_NAME"), navigationRootGroups.get(root).get("ROOT_NAME").toLowerCase(), gamesAdapter);
                break;
            case 1: // My staging
                loadMoreProcContainer.set(0, new Sendable<String>() {
                    @Override
                    public void send(String s) {
                        appendItems(gameService.ListMyStagingGames(null, null, null, null, null, null, null, null, s), null, navigationRootGroups.get(root).get("ROOT_NAME").toLowerCase(), gamesAdapter);
                    }
                });
                displayItems(gameService.ListMyStagingGames(null, null, null, null, null, null, null, null, null), navigationChildGroups.get(root).get(child).get("CHILD_NAME"), navigationRootGroups.get(root).get("ROOT_NAME").toLowerCase(), gamesAdapter);
                break;
            case 2: // My finished
                loadMoreProcContainer.set(0, new Sendable<String>() {
                    @Override
                    public void send(String s) {
                        appendItems(gameService.ListMyFinishedGames(null, null, null, null, null, null, null, null, s), null, navigationRootGroups.get(root).get("ROOT_NAME").toLowerCase(), gamesAdapter);
                    }
                });
                displayItems(gameService.ListMyFinishedGames(null, null, null, null, null, null, null, null, null), navigationChildGroups.get(root).get(child).get("CHILD_NAME"), navigationRootGroups.get(root).get("ROOT_NAME").toLowerCase(), gamesAdapter);
                break;
            case 3: // Open
                loadMoreProcContainer.set(0, new Sendable<String>() {
                    @Override
                    public void send(String s) {
                        appendItems(gameService.ListOpenGames(null, null, null, null, null, null, null, null, s), null, navigationRootGroups.get(root).get("ROOT_NAME").toLowerCase(), gamesAdapter);
                    }
                });
                displayItems(gameService.ListOpenGames(null, null, null, null, null, null, null, null, null), navigationChildGroups.get(root).get(child).get("CHILD_NAME"), navigationRootGroups.get(root).get("ROOT_NAME").toLowerCase(), gamesAdapter);
                break;
            case 4: // Started
                loadMoreProcContainer.set(0, new Sendable<String>() {
                    @Override
                    public void send(String s) {
                        appendItems(gameService.ListStartedGames(null, null, null, null, null, null, null, null, s), null, navigationRootGroups.get(root).get("ROOT_NAME").toLowerCase(), gamesAdapter);
                    }
                });
                displayItems(gameService.ListStartedGames(null, null, null, null, null, null, null, null, null), navigationChildGroups.get(root).get(child).get("CHILD_NAME"), navigationRootGroups.get(root).get("ROOT_NAME").toLowerCase(), gamesAdapter);
                break;
            case 5: // Finished
                loadMoreProcContainer.set(0, new Sendable<String>() {
                    @Override
                    public void send(String s) {
                        appendItems(gameService.ListFinishedGames(null, null, null, null, null, null, null, null, s), null, navigationRootGroups.get(root).get("ROOT_NAME").toLowerCase(), gamesAdapter);
                    }
                });
                displayItems(gameService.ListFinishedGames(null, null, null, null, null, null, null, null, null), navigationChildGroups.get(root).get(child).get("CHILD_NAME"), navigationRootGroups.get(root).get("ROOT_NAME").toLowerCase(), gamesAdapter);
                break;
            }
            break;
        case 1: // Users
            switch (child) {
            case 0: // Top rated
                userStatsAdapter.setEmitter(new UserStatsAdapter.StatsEmitter() {
                    @Override
                    public String emit(UserStats stats) {
                        return MainActivity.this.toString(stats.Glicko.PracticalRating);
                    }
                });
                loadMoreProcContainer.set(0, new Sendable<String>() {
                    @Override
                    public void send(String s) {
                        appendItems(userStatsService.ListTopRatedPlayers(null, s), null, navigationRootGroups.get(root).get("ROOT_NAME").toLowerCase(), userStatsAdapter);
                    }
                });
                displayItems(userStatsService.ListTopRatedPlayers(null, null), navigationChildGroups.get(root).get(child).get("CHILD_NAME"), navigationRootGroups.get(root).get("ROOT_NAME").toLowerCase(), userStatsAdapter);
                break;
            case 1: // Top reliable
                userStatsAdapter.setEmitter(new UserStatsAdapter.StatsEmitter() {
                    @Override
                    public String emit(UserStats stats) {
                        return MainActivity.this.toString(stats.Reliability);
                    }
                });
                loadMoreProcContainer.set(0, new Sendable<String>() {
                    @Override
                    public void send(String s) {
                        appendItems(userStatsService.ListTopReliablePlayers(null, s), null, navigationRootGroups.get(root).get("ROOT_NAME").toLowerCase(), userStatsAdapter);
                    }
                });
                displayItems(userStatsService.ListTopReliablePlayers(null, null), navigationChildGroups.get(root).get(child).get("CHILD_NAME"), navigationRootGroups.get(root).get("ROOT_NAME").toLowerCase(), userStatsAdapter);
                break;
            case 2: // Top quick
                userStatsAdapter.setEmitter(new UserStatsAdapter.StatsEmitter() {
                    @Override
                    public String emit(UserStats stats) {
                        return MainActivity.this.toString(stats.Quickness);
                    }
                });
                loadMoreProcContainer.set(0, new Sendable<String>() {
                    @Override
                    public void send(String s) {
                        appendItems(userStatsService.ListTopQuickPlayers(null, s), null, navigationRootGroups.get(root).get("ROOT_NAME").toLowerCase(), userStatsAdapter);
                    }
                });
                displayItems(userStatsService.ListTopQuickPlayers(null, null), navigationChildGroups.get(root).get(child).get("CHILD_NAME"), navigationRootGroups.get(root).get("ROOT_NAME").toLowerCase(), userStatsAdapter);
                break;
            case 3: // Top hated
                userStatsAdapter.setEmitter(new UserStatsAdapter.StatsEmitter() {
                    @Override
                    public String emit(UserStats stats) {
                        return MainActivity.this.toString(stats.Hated);
                    }
                });
                loadMoreProcContainer.set(0, new Sendable<String>() {
                    @Override
                    public void send(String s) {
                        appendItems(userStatsService.ListTopHatedPlayers(null, s), null, navigationRootGroups.get(root).get("ROOT_NAME").toLowerCase(), userStatsAdapter);
                    }
                });
                displayItems(userStatsService.ListTopHatedPlayers(null, null), navigationChildGroups.get(root).get(child).get("CHILD_NAME"), navigationRootGroups.get(root).get("ROOT_NAME").toLowerCase(), userStatsAdapter);
                break;
            case 4: // Top hater
                userStatsAdapter.setEmitter(new UserStatsAdapter.StatsEmitter() {
                    @Override
                    public String emit(UserStats stats) {
                        return MainActivity.this.toString(stats.Hater);
                    }
                });
                loadMoreProcContainer.set(0, new Sendable<String>() {
                    @Override
                    public void send(String s) {
                        appendItems(userStatsService.ListTopHaterPlayers(null, s), null, navigationRootGroups.get(root).get("ROOT_NAME").toLowerCase(), userStatsAdapter);
                    }
                });
                displayItems(userStatsService.ListTopHaterPlayers(null, null), navigationChildGroups.get(root).get(child).get("CHILD_NAME"), navigationRootGroups.get(root).get("ROOT_NAME").toLowerCase(), userStatsAdapter);
                break;
            }
            break;
        }
    }

    private <T> void appendItems(Observable<MultiContainer<T>> call, final String what, final String typ, final RecycleAdapter<SingleContainer<T>,?> adapter) {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        String msg = null;
        if (what != null) {
            msg = getResources().getString(R.string.loading_x_y, what, typ);
        } else {
            msg = getResources().getString(R.string.loading_x_y, getResources().getString(R.string.more), typ);
        }

        handleReq(call, new Sendable<MultiContainer<T>>() {
            @Override
            public void send(MultiContainer<T> container) {
                nextCursorContainer.set(0, "");
                for (Link link : container.Links) {
                    if (link.Rel.equals("next")) {
                        Uri uri = Uri.parse(link.URL);
                        nextCursorContainer.set(0, uri.getQueryParameter("cursor"));
                    }
                }
                adapter.addAll(container.Properties);

                if (what != null) {
                    ((TextView) findViewById(R.id.content_title)).setText(getResources().getString(R.string.x_y, what, typ));
                }
                ((TextView) findViewById(R.id.content_title)).setVisibility(View.VISIBLE);
                if (container.Properties.isEmpty()) {
                    findViewById(R.id.empty_view).setVisibility(View.VISIBLE);
                    contentList.setVisibility(View.GONE);
                } else {
                    findViewById(R.id.empty_view).setVisibility(View.GONE);
                    contentList.setVisibility(View.VISIBLE);
                }
            }
        }, msg);
    }

    private <T> void displayItems(Observable<MultiContainer<T>> call, String what, String typ, RecycleAdapter<SingleContainer<T>,?> adapter) {
        adapter.clear();
        gamesAdapter.clearExpanded();
        contentList.setAdapter(adapter);
        scrollListener.resetState();
        appendItems(call, what, typ, adapter);
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
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent i = new Intent(this, PreferenceActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}

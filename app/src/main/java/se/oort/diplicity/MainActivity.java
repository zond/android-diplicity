package se.oort.diplicity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
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

    private List<Sendable<String>> loadMoreProcContainer;

    private GamesAdapter gamesAdapter = new GamesAdapter(new ArrayList<SingleContainer<Game>>());
    private UserStatsAdapter userStatsAdapter = new UserStatsAdapter(new ArrayList<SingleContainer<UserStats>>());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        List<Map<String, String>> groupData = new ArrayList<Map<String, String>>() {{
            add(new HashMap<String, String>() {{
                put("ROOT_NAME", getResources().getString(R.string.games));
            }});
            add(new HashMap<String, String>() {{
                put("ROOT_NAME", getResources().getString(R.string.users));
            }});
        }};
        final List<List<Map<String, String>>> listOfChildGroups = new ArrayList<List<Map<String, String>>>();

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
        listOfChildGroups.add(childGroupForFirstGroupRow);

        List<Map<String, String>> childGroupForSecondGroupRow = new ArrayList<Map<String, String>>(){{
            add(new HashMap<String, String>() {{
                put("CHILD_NAME", getResources().getString(R.string.top_rated));
            }});
            add(new HashMap<String, String>() {{
                put("CHILD_NAME", getResources().getString(R.string.top_hated));
            }});
        }};
        listOfChildGroups.add(childGroupForSecondGroupRow);
        SimpleExpandableListAdapter navigationListAdapter = new SimpleExpandableListAdapter(
                this,
                groupData,
                android.R.layout.simple_expandable_list_item_1,
                new String[] { "ROOT_NAME" },
                new int[] { android.R.id.text1 },
                listOfChildGroups,
                android.R.layout.simple_expandable_list_item_1,
                new String[] { "CHILD_NAME" },
                new int[] { android.R.id.text1 }
        );

        ExpandableListView navigationList = (ExpandableListView) findViewById(R.id.nav_list);
        navigationList.setAdapter(navigationListAdapter);
        connectNavigationList(listOfChildGroups, groupData, navigationList);

        contentList = (RecyclerView) findViewById(R.id.content_list);
        LinearLayoutManager contentLayoutManager = new LinearLayoutManager(this);
        contentLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        contentList.setLayoutManager(contentLayoutManager);
        scrollListener = new EndlessRecyclerViewScrollListener(contentLayoutManager, nextCursor) {
            @Override
            public void onLoadMore(String cursor, int totalItemsCount, RecyclerView view) {
                if (cursor.length() > 0) {
                    loadMoreProcContainer.get(0).Send(cursor);
                }
            }
        };
        contentList.addOnScrollListener(scrollListener);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, contentLayoutManager.getOrientation());
        contentList.addItemDecoration(dividerItemDecoration);

        loadMoreProcContainer = new ArrayList<Sendable<String>>();
        loadMoreProcContainer.add(new Sendable<String>() {
            public void Send(String s) {
            }
        });

    }

    private void connectNavigationList(final List<List<Map<String, String>>> listOfChildGroups, final List<Map<String, String>> groupData, ExpandableListView navigationList) {
        navigationList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i1, long l) {
                switch (i) {
                case 0: // Games
                    switch (i1) {
                    case 0: // My started
                        loadMoreProcContainer.set(0, new Sendable<String>() {
                            @Override
                            public void Send(String s) {
                                appendItems(gameService.MyStartedGames(null, null, null, null, null, null, null, null, s), null, null, gamesAdapter);
                            }
                        });
                        displayItems(gameService.MyStartedGames(null, null, null, null, null, null, null, null, null), listOfChildGroups.get(i).get(i1).get("CHILD_NAME"), groupData.get(i).get("ROOT_NAME").toLowerCase(), gamesAdapter);
                        break;
                    case 1: // My staging
                        loadMoreProcContainer.set(0, new Sendable<String>() {
                            @Override
                            public void Send(String s) {
                                appendItems(gameService.MyStagingGames(null, null, null, null, null, null, null, null, s), null, null, gamesAdapter);
                            }
                        });
                        displayItems(gameService.MyStagingGames(null, null, null, null, null, null, null, null, null), listOfChildGroups.get(i).get(i1).get("CHILD_NAME"), groupData.get(i).get("ROOT_NAME").toLowerCase(), gamesAdapter);
                        break;
                    case 2: // My finished
                        loadMoreProcContainer.set(0, new Sendable<String>() {
                            @Override
                            public void Send(String s) {
                                appendItems(gameService.MyFinishedGames(null, null, null, null, null, null, null, null, s), null, null, gamesAdapter);
                            }
                        });
                        displayItems(gameService.MyFinishedGames(null, null, null, null, null, null, null, null, null), listOfChildGroups.get(i).get(i1).get("CHILD_NAME"), groupData.get(i).get("ROOT_NAME").toLowerCase(), gamesAdapter);
                        break;
                    case 3: // Open
                        loadMoreProcContainer.set(0, new Sendable<String>() {
                            @Override
                            public void Send(String s) {
                                appendItems(gameService.OpenGames(null, null, null, null, null, null, null, null, s), null, null, gamesAdapter);
                            }
                        });
                        displayItems(gameService.OpenGames(null, null, null, null, null, null, null, null, null), listOfChildGroups.get(i).get(i1).get("CHILD_NAME"), groupData.get(i).get("ROOT_NAME").toLowerCase(), gamesAdapter);
                        break;
                    case 4: // Started
                        loadMoreProcContainer.set(0, new Sendable<String>() {
                            @Override
                            public void Send(String s) {
                                appendItems(gameService.StartedGames(null, null, null, null, null, null, null, null, s), null, null, gamesAdapter);
                            }
                        });
                        displayItems(gameService.StartedGames(null, null, null, null, null, null, null, null, null), listOfChildGroups.get(i).get(i1).get("CHILD_NAME"), groupData.get(i).get("ROOT_NAME").toLowerCase(), gamesAdapter);
                        break;
                    case 5: // Finished
                        loadMoreProcContainer.set(0, new Sendable<String>() {
                            @Override
                            public void Send(String s) {
                                appendItems(gameService.FinishedGames(null, null, null, null, null, null, null, null, s), null, null, gamesAdapter);
                            }
                        });
                        displayItems(gameService.FinishedGames(null, null, null, null, null, null, null, null, null), listOfChildGroups.get(i).get(i1).get("CHILD_NAME"), groupData.get(i).get("ROOT_NAME").toLowerCase(), gamesAdapter);
                        break;
                    }
                    break;
                case 1: // Users
                    switch (i1) {
                    case 0: // Top rated
                        loadMoreProcContainer.set(0, new Sendable<String>() {
                            @Override
                            public void Send(String s) {
                                appendItems(userStatsService.ListTopRatedPlayers(null, s), null, null, userStatsAdapter);
                            }
                        });
                        displayItems(userStatsService.ListTopRatedPlayers(null, null), listOfChildGroups.get(i).get(i1).get("CHILD_NAME"), groupData.get(i).get("ROOT_NAME").toLowerCase(), userStatsAdapter);
                        break;
                    case 1: // Top hated
                        loadMoreProcContainer.set(0, new Sendable<String>() {
                            @Override
                            public void Send(String s) {
                                appendItems(userStatsService.ListTopHatedPlayers(null, s), null, null, userStatsAdapter);
                            }
                        });
                        displayItems(userStatsService.ListTopHatedPlayers(null, null), listOfChildGroups.get(i).get(i1).get("CHILD_NAME"), groupData.get(i).get("ROOT_NAME").toLowerCase(), userStatsAdapter);
                        break;
                    }
                    break;
                }
                return false;
            }
        });
    }

    private <T> void appendItems(Observable<MultiContainer<T>> call, final String what, final String typ, final RecycleAdapter<SingleContainer<T>,?> adapter) {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        final ProgressDialog progress = new ProgressDialog(this);
        if (what != null) {
            progress.setTitle(getResources().getString(R.string.loading_x_y, what, typ));
        }
        progress.setCancelable(false);
        progress.show();

        call
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<MultiContainer<T>>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("Diplicity", "Error loading " + what + " " + typ + ": " + e);
                        Toast.makeText(MainActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNext(MultiContainer<T> container) {
                        Log.d("Diplicity", "Finally received " + container.Properties.size() + " items");
                        nextCursorContainer.set(0, "");
                        for (Link link : container.Links) {
                            if (link.Rel.equals("next")) {
                                Uri uri = Uri.parse(link.URL);
                                nextCursorContainer.set(0, uri.getQueryParameter("cursor"));
                            }
                        }

                        adapter.AddAll(container.Properties);

                        if (what != null) {
                            ((TextView) findViewById(R.id.content_title)).setText(getResources().getString(R.string.x_y, what, typ));
                        }
                        ((TextView) findViewById(R.id.content_title)).setVisibility(View.VISIBLE);
                        if (container.Properties.isEmpty()) {
                            findViewById(R.id.empty_view).setVisibility(View.VISIBLE);
                        } else {
                            findViewById(R.id.empty_view).setVisibility(View.GONE);
                        }

                        progress.dismiss();
                    }
                });


    }

    private <T> void displayItems(Observable<MultiContainer<T>> call, String what, String typ, RecycleAdapter<SingleContainer<T>,?> adapter) {
        adapter.Clear();
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

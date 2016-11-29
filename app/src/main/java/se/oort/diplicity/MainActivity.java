package se.oort.diplicity;

import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import se.oort.diplicity.apigen.GamesContainer;

public class MainActivity extends RetrofitActivity {

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
        List<List<Map<String, String>>> listOfChildGroups = new ArrayList<List<Map<String, String>>>();

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
        navigationList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i1, long l) {
                switch (i) {
                case 0: // Games
                    switch (i1) {
                    case 0: // My started
                        displayGames(gameService.MyStartedGames());
                    case 1: // My staging
                    case 2: // My finished
                    case 3: // Open
                    case 4: // Started
                    case 5: // Finished
                    }
                case 1: // Users
                    switch (i1) {
                    case 0: // Top rated
                    case 1: // Top hated
                    }
                }
                return false;
            }
        });
    }

    private void displayGames(Observable<GamesContainer> call) {
        call
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<GamesContainer>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("Diplicity", "Error loading games " + e);
                        Toast.makeText(MainActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNext(GamesContainer gamesContainer) {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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

}

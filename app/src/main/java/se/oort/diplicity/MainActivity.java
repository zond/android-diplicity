package se.oort.diplicity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.Subscriber;
import se.oort.diplicity.apigen.Ban;
import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.Link;
import se.oort.diplicity.apigen.MultiContainer;
import se.oort.diplicity.apigen.SingleContainer;
import se.oort.diplicity.apigen.User;
import se.oort.diplicity.apigen.UserStats;

import static java.util.Arrays.asList;

public class MainActivity extends RetrofitActivity {
    private static final long HOUR_IN_MINUTES = 60;
    private static final long DAY_IN_MINUTES = 24 * HOUR_IN_MINUTES;

    private final Random random = new Random();

    private RecyclerView contentList;

    private EndlessRecyclerViewScrollListener scrollListener;
    private List<String> nextCursorContainer = new ArrayList<String>(asList(new String[]{""}));

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

    private FloatingActionButton addGameButton;

    public static final String FINISHED = "finished";
    public static final String STARTED = "started";
    public static final String STAGING = "staging";
    public static final String ACTION_VIEW_USER_GAMES = "view_user_games";
    public static final String SERIALIZED_USER_KEY = "serialized_user";
    public static final String GAME_STATE_KEY = "game_state";
    public static final String HAS_JOINED_GAME_KEY = "has_joined_game";

    private static Pattern viewGamePattern = Pattern.compile("/Game/(.*)");

    private class SpinnerVariantElement implements Comparable<SpinnerVariantElement>{
        public String name;
        public Integer players;
        public String toString() {
            if (players == null) {
                return name;
            } else {
                return getResources().getString(R.string.name_x_players, name, players);
            }
        }
        public int compareTo(SpinnerVariantElement other) {
            return name.compareTo(other.name);
        }
    }

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

        addGameButton = (FloatingActionButton) findViewById(R.id.add_game_button);
        addGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getLoggedInUser() != null && getLoggedInUser().Id != null) {
                    handleReq(userStatsService.UserStatsLoad(getLoggedInUser().Id), new Sendable<SingleContainer<UserStats>>() {
                        @Override
                        public void send(SingleContainer<UserStats> userStatsSingleContainer) {
                            final List<UserStats> statsContainer = new ArrayList<>();
                            statsContainer.add(userStatsSingleContainer.Properties);

                            final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).setView(R.layout.create_game_dialog).show();
                            final Returner<Game, Boolean> validateGame = new Returner<Game, Boolean>() {
                                @Override
                                public Boolean Return(Game g) {
                                    if (g.PhaseLengthMinutes == null)
                                        g.PhaseLengthMinutes = DAY_IN_MINUTES;
                                    if (g.PhaseLengthMinutes > 30 * DAY_IN_MINUTES) {
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
                            final List<SpinnerVariantElement> variantNames = new ArrayList<>();
                            for (SingleContainer<VariantService.Variant> variantContainer : getVariants().Properties) {
                                SpinnerVariantElement el = new SpinnerVariantElement();
                                el.name = variantContainer.Properties.Name;
                                if (variantContainer.Properties.Nations != null) {
                                    el.players = variantContainer.Properties.Nations.size();
                                }
                                variantNames.add(el);
                            }
                            int classical = 0;
                            Collections.sort(variantNames);
                            for (int i = 0; i < variantNames.size(); i++) {
                                if (variantNames.get(i).name.equals("Classical")) {
                                    classical = i;
                                }
                            }
                            ArrayAdapter<SpinnerVariantElement> variantAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, variantNames);
                            variants.setAdapter(variantAdapter);
                            variantAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            variants.setSelection(classical);

                            final EditText gameNameView = (EditText) dialog.findViewById(R.id.desc);
                            final EditText phaseLengthView = (EditText) dialog.findViewById(R.id.phase_length);
                            final Spinner phaseLengthUnitsSpinner = (Spinner) dialog.findViewById(R.id.phase_length_units);
                            final EditText minRatingView = (EditText) dialog.findViewById(R.id.min_rating);
                            final EditText maxRatingView = (EditText) dialog.findViewById(R.id.max_rating);
                            final EditText minReliabilityView = (EditText) dialog.findViewById(R.id.min_reliability);
                            final EditText minQuicknessView = (EditText) dialog.findViewById(R.id.min_quickness);
                            final EditText maxHatedView = (EditText) dialog.findViewById(R.id.max_hated);
                            final EditText maxHaterView = (EditText) dialog.findViewById(R.id.max_hater);
                            final CheckBox privateView = (CheckBox) dialog.findViewById(R.id._private);
                            final List<Boolean> noMergeContainer = new ArrayList<Boolean>();
                            noMergeContainer.add(Boolean.FALSE);

                            final View.OnFocusChangeListener gameNameListener = new View.OnFocusChangeListener() {
                                private final int key = random.nextInt(Integer.MAX_VALUE);
                                private boolean generatedName = true;

                                @Override
                                public void onFocusChange(View view, boolean b) {
                                    if (view.equals(gameNameView)) {
                                        if (gameNameView.getText().toString().isEmpty()) {
                                            generatedName = true;
                                        }
                                    }
                                    noMergeContainer.set(0, !generatedName);
                                    if (generatedName) {
                                        long phaseLength = getPhaseLengthMinutes(phaseLengthView, phaseLengthUnitsSpinner);
                                        String battle;
                                        if (phaseLength < DAY_IN_MINUTES) {
                                            battle = getKeyedString(R.array.blitz);
                                        } else if (phaseLength <= 2 * DAY_IN_MINUTES) {
                                            battle = getKeyedString(R.array.battle);
                                        } else {
                                            battle = getKeyedString(R.array.war);
                                        }
                                        String adjective;
                                        if (getDoubleValue(minRatingView, 0) > 1400) {
                                            adjective = getKeyedString(R.array.quality);
                                        } else if (getDoubleValue(maxRatingView, 2000) < 1000) {
                                            adjective = getKeyedString(R.array.fun);
                                        } else if (getDoubleValue(minReliabilityView, 0) > 10) {
                                            adjective = getKeyedString(R.array.reliable);
                                        } else if (getDoubleValue(minQuicknessView, 0) > 10) {
                                            adjective = getKeyedString(R.array.fast);
                                        } else if (getDoubleValue(maxHatedView, 100) < 10) {
                                            adjective = getKeyedString(R.array.pleasant);
                                        } else if (getDoubleValue(maxHaterView, 100) < 10) {
                                            adjective = getKeyedString(R.array.patient);
                                        } else {
                                            adjective = getKeyedString(R.array.other);
                                        }
                                        String prize = getKeyedString(R.array.prize);
                                        String calculatedName = getResources().getString(R.string.game_name_template, battle, adjective, prize);
                                        if (view.equals(gameNameView)) {
                                            String enteredName = gameNameView.getText().toString();
                                            generatedName = (enteredName.isEmpty() || enteredName.equals(calculatedName));
                                            noMergeContainer.set(0, !generatedName);
                                        } else {
                                            gameNameView.setText(calculatedName);
                                        }
                                    }
                                }

                                private double getDoubleValue(EditText editText, int def) {
                                    try {
                                        return Double.parseDouble(editText.getText().toString());
                                    } catch (NumberFormatException e) {
                                        return def;
                                    }
                                }

                                private String getKeyedString(int arrayId) {
                                    return getResources().getStringArray(arrayId)[key % getResources().getStringArray(arrayId).length];
                                }
                            };

                            AdapterView.OnItemSelectedListener phaseLengthUnitsListener = new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    gameNameListener.onFocusChange(view, false);
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {
                                    parent.setSelection(0);
                                }
                            };

                            setDefaultPhaseLength(phaseLengthView, phaseLengthUnitsSpinner);
                            setDefaultMinReliability(minReliabilityView, statsContainer.get(0));

                            gameNameView.setOnFocusChangeListener(gameNameListener);
                            phaseLengthView.setOnFocusChangeListener(gameNameListener);
                            minRatingView.setOnFocusChangeListener(gameNameListener);
                            maxRatingView.setOnFocusChangeListener(gameNameListener);
                            minReliabilityView.setOnFocusChangeListener(gameNameListener);
                            minQuicknessView.setOnFocusChangeListener(gameNameListener);
                            maxHatedView.setOnFocusChangeListener(gameNameListener);
                            maxHaterView.setOnFocusChangeListener(gameNameListener);

                            phaseLengthUnitsSpinner.setOnItemSelectedListener(phaseLengthUnitsListener);

                            ((FloatingActionButton) dialog.findViewById(R.id.create_game_button)).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    final Game game = new Game();
                                    game.Desc = gameNameView.getText().toString();
                                    game.Variant = variantNames.get(variants.getSelectedItemPosition()).name;
                                    game.PhaseLengthMinutes = getPhaseLengthMinutes(phaseLengthView, phaseLengthUnitsSpinner);
                                    game.NoMerge = noMergeContainer.get(0);
                                    game.Private = privateView.isChecked();
                                    try {
                                        game.MinRating = Double.parseDouble(minRatingView.getText().toString());
                                    } catch (NumberFormatException e) {
                                    }
                                    try {
                                        game.MaxRating = Double.parseDouble(maxRatingView.getText().toString());
                                    } catch (NumberFormatException e) {
                                    }
                                    try {
                                        game.MinReliability = Double.parseDouble(minReliabilityView.getText().toString());
                                    } catch (NumberFormatException e) {
                                    }
                                    try {
                                        game.MinQuickness = Double.parseDouble(minQuicknessView.getText().toString());
                                    } catch (NumberFormatException e) {
                                    }
                                    try {
                                        game.MaxHated = Double.parseDouble(maxHatedView.getText().toString());
                                    } catch (NumberFormatException e) {
                                    }
                                    try {
                                        game.MaxHater = Double.parseDouble(maxHaterView.getText().toString());
                                    } catch (NumberFormatException e) {
                                    }
                                    if (validateGame.Return(game)) {
                                        handleReq(gameService.GameCreate(game), new Sendable<SingleContainer<Game>>() {
                                            @Override
                                            public void send(SingleContainer<Game> gameSingleContainer) {
                                                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                                                prefs.edit().putBoolean(HAS_JOINED_GAME_KEY, true).apply();
                                                if (nextCursorContainer.get(0).length() == 0) {
                                                    findViewById(R.id.empty_view).setVisibility(View.GONE);
                                                    contentList.setVisibility(View.VISIBLE);
                                                    gamesAdapter.items.add(gameSingleContainer);
                                                    gamesAdapter.notifyDataSetChanged();
                                                }
                                                dialog.dismiss();
                                            }
                                        }, new ErrorHandler(new int[]{412, 418}, new Sendable<HttpException>() {
                                            @Override
                                            public void send(HttpException e) {
                                                if (e.code() == 412) {
                                                    handleReq(userStatsService.UserStatsLoad(getLoggedInUser().Id), new Sendable<SingleContainer<UserStats>>() {
                                                        @Override
                                                        public void send(SingleContainer<UserStats> userStatsSingleContainer) {
                                                            statsContainer.set(0, userStatsSingleContainer.Properties);
                                                            validateGame.Return(game);
                                                        }
                                                    }, getResources().getString(R.string.creating_game));
                                                } else if (e.code() == 418) {
                                                    dialog.dismiss();
                                                    Toast.makeText(MainActivity.this, getResources().getString(R.string.you_were_added_to_another_game), Toast.LENGTH_LONG).show();
                                                    navigateTo(0, 1);
                                                }
                                            }
                                        }), getResources().getString(R.string.creating_game));
                                    }
                                }
                            });
                        }
                    }, getResources().getString(R.string.loading_user_stats));
                }
            }

            /**
             * Determine the current user entered phase length.
             *
             * @param phaseLengthView The view containing the selected quantity.
             * @param phaseLengthUnitsView The spiner containing the selected units.
             * @return The selected phase length in minutes.
             */
            private long getPhaseLengthMinutes(EditText phaseLengthView, Spinner phaseLengthUnitsView) {
                // Get the quantity entered
                long quantity;
                try {
                    quantity = Long.valueOf(phaseLengthView.getText().toString());
                } catch (NumberFormatException e) {
                    setDefaultPhaseLength(phaseLengthView, phaseLengthUnitsView);
                    quantity = Long.valueOf(phaseLengthView.getText().toString());
                }
                // Use the quantity and the selected unit to find the phase length.
                long phaseLength;
                int selectedItem = phaseLengthUnitsView.getSelectedItemPosition();
                if (selectedItem == getIndexOfUnit(R.string._day_s)) {
                    phaseLength = quantity * DAY_IN_MINUTES;
                } else if (selectedItem == getIndexOfUnit(R.string._hour_s)) {
                    phaseLength = quantity * HOUR_IN_MINUTES;
                } else if (selectedItem == getIndexOfUnit(R.string._minute_s)) {
                    phaseLength = quantity;
                } else {
                    Log.e("Diplicity", "Programmer error: Unexpected phase length units selected");
                    throw new IllegalStateException("Programmer error: Unexpected phase length units selected");
                }
                return phaseLength;
            }

            /** Set the phase length to the default of one day. */
            private void setDefaultPhaseLength(EditText phaseLengthView, Spinner phaseLengthUnitsView) {
                phaseLengthView.setText("1");
                phaseLengthUnitsView.setSelection(getIndexOfUnit(R.string._day_s));
            }

            private void setDefaultMinReliability(EditText minReliabilityView, UserStats userStats) {
                if (userStats.Reliability > 10) {
                    minReliabilityView.setText("10");
                } else {
                    minReliabilityView.setText("" + userStats.Reliability.intValue());
                }
            }

            /** Get the index of the given unit string resource in the array list used by the units spinner. */
            private int getIndexOfUnit(int unitString) {
                List<String> unitLabels = asList(getResources().getStringArray(R.array.phaseLengthUnits));
                return unitLabels.indexOf(getResources().getString(unitString));
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

        if (!ACTION_VIEW_USER_GAMES.equals(getIntent().getAction()) && !Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            if (prefs.getBoolean(HAS_JOINED_GAME_KEY, false)) {
                // My started.
                navigateTo(0, 0);
            } else {
                // Open.
                navigateTo(0, 3);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ACTION_VIEW_USER_GAMES.equals(getIntent().getAction())) {
            byte[] serializedUser = getIntent().getByteArrayExtra(SERIALIZED_USER_KEY);
            if (serializedUser != null) {
                User user = (User) unserialize(serializedUser);
                displayUserGames(user, getIntent().getStringExtra(GAME_STATE_KEY));
            }
        } else if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Uri uri = getIntent().getData();
            Matcher m = viewGamePattern.matcher(uri.getPath());
            if (m.matches()) {
                displaySingleGame(m.group(1));
            }
        }
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
            add(new HashMap<String, String>() {{
                put("CHILD_NAME", getResources().getString(R.string.lookup));
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

    private void displaySingleGame(final String gameID) {
        handleReq(gameService.GameLoad(gameID), new Sendable<SingleContainer<Game>>() {
            @Override
            public void send(final SingleContainer<Game> gameSingleContainer) {
                addGameButton.setVisibility(View.GONE);
                loadMoreProcContainer.set(0, new Sendable<String>() {
                    @Override
                    public void send(String s) {
                    }
                });
                displayItems(Observable.create(new Observable.OnSubscribe<MultiContainer<Game>>() {
                    @Override
                    public void call(final Subscriber<? super MultiContainer<Game>> subscriber) {
                        MultiContainer<Game> multiContainer = new MultiContainer<Game>();
                        multiContainer.Properties = new ArrayList<SingleContainer<Game>>();
                        multiContainer.Properties.add(gameSingleContainer);
                        multiContainer.Links = new ArrayList<Link>();
                        subscriber.onNext(multiContainer);
                        subscriber.onCompleted();
                    }
                }), gameID, null, gamesAdapter);
            }
        }, getString(R.string.loading_game));
    }

    private void displayUserGames(final User user, String state) {
        if (state.equals(FINISHED)) {
            addGameButton.setVisibility(View.GONE);
            loadMoreProcContainer.set(0, new Sendable<String>() {
                @Override
                public void send(String s) {
                    appendItems(gameService.ListOtherFinishedGames(user.Id, null, null, null, null, null, null, null, null, s), null, getString(R.string.games), gamesAdapter);
                }
            });
            displayItems(gameService.ListOtherFinishedGames(user.Id, null, null, null, null, null, null, null, null, null), getString(R.string.x_s_finished, user.Name), getString(R.string._games), gamesAdapter);
        } else if (state.equals(STAGING)) {
            addGameButton.setVisibility(View.GONE);
            loadMoreProcContainer.set(0, new Sendable<String>() {
                @Override
                public void send(String s) {
                    appendItems(gameService.ListOtherStagingGames(user.Id, null, null, null, null, null, null, null, null, s), null, getString(R.string.games), gamesAdapter);
                }
            });
            displayItems(gameService.ListOtherStagingGames(user.Id, null, null, null, null, null, null, null, null, null), getString(R.string.x_s_staging, user.Name), getString(R.string._games), gamesAdapter);
        } else if (state.equals(STARTED)) {
            addGameButton.setVisibility(View.GONE);
            loadMoreProcContainer.set(0, new Sendable<String>() {
                @Override
                public void send(String s) {
                    appendItems(gameService.ListOtherStartedGames(user.Id, null, null, null, null, null, null, null, null, s), null, getString(R.string.games), gamesAdapter);
                }
            });
            displayItems(gameService.ListOtherStartedGames(user.Id, null, null, null, null, null, null, null, null, null), getString(R.string.x_s_started, user.Name), getString(R.string._games), gamesAdapter);
        }
    }

    private void navigateTo(final int root, final int child) {
        if (root == 0 && (child == 1 || child == 3)) {
            addGameButton.setVisibility(View.VISIBLE);
        } else {
            addGameButton.setVisibility(View.GONE);
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
            case 6: // Lookup
                final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).setView(R.layout.lookup_game_dialog).show();
                addGameButton = (FloatingActionButton) dialog.findViewById(R.id.lookup_game_button);
                addGameButton.setOnClickListener(new View.OnClickListener() {
                                                     @Override
                                                     public void onClick(View view) {
                                                         String gameID = ((EditText) dialog.findViewById(R.id.game_id)).getText().toString();
                                                         Uri uri = Uri.parse(gameID);
                                                         Matcher m = viewGamePattern.matcher(uri.getPath());
                                                         if (m.matches()) {
                                                             gameID = m.group(1);
                                                         }
                                                         dialog.dismiss();
                                                         displaySingleGame(gameID);
                                                     }
                });
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                }
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
        if (typ != null) {
            if (what != null) {
                msg = getResources().getString(R.string.loading_x_y, what, typ);
            } else {
                msg = getResources().getString(R.string.loading_x_y, getResources().getString(R.string.more), typ);
            }
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
                    if (typ != null) {
                        ((TextView) findViewById(R.id.content_title)).setText(getResources().getString(R.string.x_y, what, typ));
                    } else {
                        ((TextView) findViewById(R.id.content_title)).setText(what);
                    }
                }
                ((TextView) findViewById(R.id.content_title)).setVisibility(View.VISIBLE);
                if (adapter.items.isEmpty()) {
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
            if (getLoggedInUser() != null && getLoggedInUser().Id != null) {
                Intent i = new Intent(this, PreferenceActivity.class);
                startActivity(i);
            }
            return true;
        } else if (id == R.id.action_error_log) {
            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).setView(R.layout.error_log_dialog).show();
            ((EditText) dialog.findViewById(R.id.error_log)).setText(App.errorLog.toString());
        } else if (id == R.id.action_forum) {
            String url = "https://groups.google.com/forum/#!forum/diplicity-talk";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        } else if (id == R.id.action_source) {
            String url = "https://github.com/zond/android-diplicity";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        } else if (id == R.id.action_alarms) {
            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).setView(R.layout.alarms_dialog).show();
            Map<String, ?> alarms = Alarm.getAlarmPreferences(this).getAll();
            LinearLayout layout = (LinearLayout) dialog.findViewById(R.id.alarms_list);
            if (alarms.size() == 0) {
                dialog.findViewById(R.id.empty_view).setVisibility(View.VISIBLE);
                layout.setVisibility(View.GONE);
            } else {
                dialog.findViewById(R.id.empty_view).setVisibility(View.GONE);
                for (Object json : Alarm.getAlarmPreferences(this).getAll().values()) {
                    Alarm.Alert alert = Alarm.Alert.fromJSON("" + json);
                    LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View row = inflater.inflate(R.layout.alarm_list_row, null);
                    ((TextView) row.findViewById(R.id.desc)).setText(alert.desc);
                    ((TextView) row.findViewById(R.id.at)).setText(alert.alertAt(this).toString());
                    layout.addView(row);
                }
                layout.setVisibility(View.VISIBLE);
            }
        } else if (id == R.id.action_bans) {
            if (getLoggedInUser() != null && getLoggedInUser().Id != null) {
                handleReq(
                    banService.ListBans(getLoggedInUser().Id),
                    new Sendable<MultiContainer<Ban>>() {
                        private RelativeLayout.LayoutParams wrapContentParams =
                                new RelativeLayout.LayoutParams(
                                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                                        RelativeLayout.LayoutParams.WRAP_CONTENT);

                        private void setMargins(RelativeLayout.LayoutParams params) {
                            int margin = getResources().getDimensionPixelSize(R.dimen.member_table_margin);
                            params.bottomMargin = margin;
                            params.topMargin = margin;
                            params.leftMargin = margin;
                            params.rightMargin = margin;
                        }

                        private void setupView(final AlertDialog dialog, final MultiContainer<Ban> banMultiContainer, final SingleContainer<Ban> ban, View convertView) {
                            User other = null;
                            for (User u : ban.Properties.Users) {
                                if (!u.Id.equals(getLoggedInUser().Id)) {
                                    other = u;
                                    break;
                                }
                            }
                            if (other != null) {
                                final User finalOther = other;
                                ((UserView) convertView.findViewById(R.id.other_user)).setUser(MainActivity.this, other, true);
                                if (ban.Properties.OwnerIds.contains(getLoggedInUser().Id)) {
                                    LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                    FloatingActionButton button = (FloatingActionButton) inflater.inflate(R.layout.clear_button, null);
                                    setMargins(wrapContentParams);
                                    wrapContentParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                                    wrapContentParams.addRule(RelativeLayout.ALIGN_PARENT_END);
                                    wrapContentParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                                    button.setLayoutParams(wrapContentParams);
                                    button.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            handleReq(banService.BanDelete(getLoggedInUser().Id, finalOther.Id),
                                                    new Sendable<SingleContainer<Ban>>() {
                                                        @Override
                                                        public void send(SingleContainer<Ban> banSingleContainer) {
                                                            ban.Properties.OwnerIds.remove(getLoggedInUser().Id);
                                                            if (ban.Properties.OwnerIds.isEmpty()) {
                                                                banMultiContainer.Properties.remove(ban);
                                                            }
                                                            setupDialog(dialog, banMultiContainer);
                                                        }
                                                    }, getResources().getString(R.string.updating));
                                        }
                                    });
                                    ((RelativeLayout) convertView.findViewById(R.id.ban_list_row_layout)).addView(button);
                                }
                            }
                        }

                        @Override
                        public void send(MultiContainer<Ban> banMultiContainer) {
                            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).setView(R.layout.bans_dialog).show();
                            setupDialog(dialog, banMultiContainer);
                        }

                        private void setupDialog(AlertDialog dialog, MultiContainer<Ban> banMultiContainer) {
                            List<SingleContainer<Ban>> asBannerList = new ArrayList<>();
                            List<SingleContainer<Ban>> asBannedList = new ArrayList<>();
                            for (SingleContainer<Ban> banSingleContainer : banMultiContainer.Properties) {
                                if (banSingleContainer.Properties.OwnerIds.contains(getLoggedInUser().Id)) {
                                    asBannerList.add(banSingleContainer);
                                } else {
                                    asBannedList.add(banSingleContainer);
                                }
                            }
                            ViewGroup parent = (ViewGroup) dialog.findViewById(R.id.bans_dialog_layout);

                            LinearLayout asBanner = (LinearLayout) dialog.findViewById(R.id.hater_list);
                            if (asBannerList.isEmpty()) {
                                dialog.findViewById(R.id.as_banner_no_data).setVisibility(View.VISIBLE);
                                asBanner.setVisibility(View.GONE);
                            } else {
                                dialog.findViewById(R.id.as_banner_no_data).setVisibility(View.GONE);
                                asBanner.removeAllViews();
                                for (SingleContainer<Ban> ban : asBannerList) {
                                    View itemView = LayoutInflater.from(MainActivity.this)
                                            .inflate(R.layout.ban_list_row, parent, false);
                                    asBanner.addView(itemView);
                                    setupView(dialog, banMultiContainer, ban, itemView);
                                }
                            }
                            LinearLayout asBanned = (LinearLayout) dialog.findViewById(R.id.hated_list);
                            if (asBannedList.isEmpty()) {
                                dialog.findViewById(R.id.as_banned_no_data).setVisibility(View.VISIBLE);
                                asBanned.setVisibility(View.GONE);
                            } else {
                                dialog.findViewById(R.id.as_banner_no_data).setVisibility(View.GONE);
                                asBanned.removeAllViews();
                                for (SingleContainer<Ban> ban : asBannedList) {
                                    View itemView = LayoutInflater.from(MainActivity.this)
                                            .inflate(R.layout.ban_list_row, parent, false);
                                    asBanned.addView(itemView);
                                    setupView(dialog, banMultiContainer, ban, itemView);
                                }
                            }
                        }
                    }, getResources().getString(R.string.loading_bans));
            }
        }

        return super.onOptionsItemSelected(item);
    }

}

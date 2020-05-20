package se.oort.diplicity.game;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

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
import se.oort.diplicity.GameUnserializer;
import se.oort.diplicity.OptionsService;
import se.oort.diplicity.R;
import se.oort.diplicity.RetrofitActivity;
import se.oort.diplicity.Sendable;
import se.oort.diplicity.VariantService;
import se.oort.diplicity.apigen.Dislodged;
import se.oort.diplicity.apigen.Game;
import se.oort.diplicity.apigen.Link;
import se.oort.diplicity.apigen.Member;
import se.oort.diplicity.apigen.MultiContainer;
import se.oort.diplicity.apigen.Order;
import se.oort.diplicity.apigen.Phase;
import se.oort.diplicity.apigen.PhaseMeta;
import se.oort.diplicity.apigen.PhaseState;
import se.oort.diplicity.apigen.SC;
import se.oort.diplicity.apigen.SingleContainer;
import se.oort.diplicity.apigen.Unit;
import se.oort.diplicity.apigen.UnitWrapper;

public class MapView extends FrameLayout {

    private List<Runnable> onFinished = new ArrayList<>();
    private Sendable<String> onClickedProvince;
    private Game game;
    private Member member;
    private SingleContainer<PhaseMeta> phaseMeta;
    private MultiContainer<Phase> phases;
    private RetrofitActivity retrofitActivity;
    private Map<String, OptionsService.Option> options = new HashMap<>();
    private Map<String, Order> orders = Collections.synchronizedMap(new HashMap<String, Order>());
    private Sendable<Object> phaseChangeNotifier;
    private String url;
    private Map<String, String> SCs = new HashMap<>();
    private Map<String, Unit> units = new HashMap<>();
    private Map<String, Unit> dislodged = new HashMap<>();

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
    }

    private void inflate() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        addView(inflater.inflate(R.layout.map_view, null));
    }
    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate();
    }

    public void setOnClickedProvince(Sendable<String> l) {
        this.onClickedProvince = l;
    }

    private void addOnFinished(Runnable r) {
        synchronized (this) {
            if (onFinished == null) {
                r.run();
            } else {
                onFinished.add(r);
            }
        }
    }

    public void evaluateJS(final String js) {
        this.addOnFinished(new Runnable() {
            @Override
            public void run() {
                WebView mWebView = (WebView) findViewById(R.id.web_view);
                String toEvaluate = "window.map.addReadyAction(function() { " + js + " });";
                if (Build.VERSION.SDK_INT >= 19) {
                    try {
                        mWebView.evaluateJavascript(toEvaluate, null);
                    } catch (IllegalStateException e) {
                        // Since, evidently, the Build.VERSION.SDK_INT check isn't enough,
                        // according to Firebase crash reports....???!???!
                        if (e.getMessage().contains("This API not supported on Android 4.3 and earlier")) {
                            mWebView.loadUrl("javascript:" + toEvaluate);
                        }
                        Log.e("Diplicity", "While evaluating '" + toEvaluate + "'", e);
                    }
                } else {
                    mWebView.loadUrl("javascript:" + toEvaluate);
                }
            }
        });
    }

    private void setOrder(String province, List<String> parts) {
        Sendable<SingleContainer<Order>> handler = new Sendable<SingleContainer<Order>>() {
            @Override
            public void send(SingleContainer<Order> orderSingleContainer) {
                MapView mv = (MapView) findViewById(R.id.map_view);
                mv.evaluateJS("window.map.removeOrders()");
                for (Map.Entry<String, Order> entry : orders.entrySet()) {
                    String nationVariable = entry.getValue().Nation.replaceAll("[^A-Za-z0-9]", "");
                    mv.evaluateJS("window.map.addOrder(" + new Gson().toJson(entry.getValue().Parts) + ", col" + nationVariable + ");");
                }

            }
        };
        if (parts == null || parts.size() == 0) {
            if (orders.containsKey(province)) {
                orders.remove(province);
                retrofitActivity.handleReq(
                        retrofitActivity.orderService.OrderDelete(game.ID, phaseMeta.Properties.PhaseOrdinal.toString(), province),
                        handler, getResources().getString(R.string.removing_order));
            }
        } else {
            Order order = new Order();
            order.GameID = game.ID;
            order.Nation = member.Nation;
            order.PhaseOrdinal = phaseMeta.Properties.PhaseOrdinal;
            order.Parts = parts;
            orders.put(province, order);
            retrofitActivity.handleReq(
                    retrofitActivity.orderService.OrderCreate(order, game.ID, phaseMeta.Properties.PhaseOrdinal.toString()),
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
                m.evaluateJS("window.map.addClickListener('" + opt.getKey() + "', function(prov) { Android.orderClicked(prov); });");
            }
            m.setOnClickedProvince(new Sendable<String>() {
                @Override
                public void send(final String s) {
                    retrofitActivity.handler.post(new Runnable() {
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
            new AlertDialog.Builder(retrofitActivity).setItems(optionVals.toArray(new CharSequence[optionVals.size()]), new DialogInterface.OnClickListener() {
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

    public void lastPhase() {
        if (game.NewestPhaseMeta != null &&
                game.NewestPhaseMeta.size() > 0 &&
                game.NewestPhaseMeta.get(0).PhaseOrdinal <= phases.Properties.size()) {
            Gson gson = new Gson();
            Phase phase = phases.Properties.get(game.NewestPhaseMeta.get(0).PhaseOrdinal.intValue() - 1).Properties;
            if (phase != null) {
                phaseMeta.Properties = gson.fromJson(gson.toJson(phase), PhaseMeta.class);
                draw();
                phaseChangeNotifier.send(new Object());
            }
        }
    }

    public void firstPhase() {
        Gson gson = new Gson();
        Phase phase = phases.Properties.get(0).Properties;
        if (phase != null) {
            phaseMeta.Properties = gson.fromJson(gson.toJson(phase), PhaseMeta.class);
            draw();
            phaseChangeNotifier.send(new Object());
        }
    }

    public void nextPhase() {
        if (phaseMeta.Properties != null &&
                phaseMeta.Properties.PhaseOrdinal <= phases.Properties.size() &&
                phaseMeta.Properties.PhaseOrdinal < game.NewestPhaseMeta.get(0).PhaseOrdinal) {
            Gson gson = new Gson();
            Phase phase = phases.Properties.get(phaseMeta.Properties.PhaseOrdinal.intValue()).Properties;
            if (phase != null) {
                phaseMeta.Properties = gson.fromJson(gson.toJson(phase), PhaseMeta.class);
                draw();
                phaseChangeNotifier.send(new Object());
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
                phaseChangeNotifier.send(new Object());
            }
        }
    }

    private String cleanProvince(String prov) {
        if (prov.indexOf("/") != -1) {
            String[] parts = prov.split("/");
            return parts[0];
        }
        return prov;
    }

    private void addInfo() {
        if (game.Started && phases.Properties.size() >= phaseMeta.Properties.PhaseOrdinal.intValue()) {
            Phase phase = phases.Properties.get(phaseMeta.Properties.PhaseOrdinal.intValue() - 1).Properties;
            Set<String> all = new HashSet<>();
            SCs.clear();
            if (phase.SCs != null) {
                for (SC sc : phase.SCs) {
                    SCs.put(cleanProvince(sc.Province), sc.Owner);
                    all.add(cleanProvince(sc.Province));
                }
            }
            units.clear();
            if (phase.Units != null) {
                for (UnitWrapper unit : phase.Units) {
                    units.put(cleanProvince(unit.Province), unit.Unit);
                    all.add(cleanProvince(unit.Province));
                }
            }
            dislodged.clear();
            if (phase.Dislodgeds != null) {
                for (Dislodged dis : phase.Dislodgeds) {
                    dislodged.put(cleanProvince(dis.Province), dis.Dislodged);
                    all.add(cleanProvince(dis.Province));
                }
            }
            for (String prov : all) {
                evaluateJS("window.map.addClickListener('" + prov + "', function(prov) { Android.infoClicked(prov); }, {nohighlight: true, permanent: true});");
            }
        }
    }

    public void draw() {
        if (game.Started) {
            url = retrofitActivity.getBaseURL() + "Game/" + game.ID + "/Phase/" + phaseMeta.Properties.PhaseOrdinal + "/Map";
            if (retrofitActivity.getLocalDevelopmentMode() && !retrofitActivity.getLocalDevelopmentModeFakeID().equals("")) {
                url = url + "?fake-id=" + retrofitActivity.getLocalDevelopmentModeFakeID();
            }
            load();
            addInfo();

            FrameLayout rdyButtonFrame = (FrameLayout) findViewById(R.id.rdy_button_frame);
            if (member != null && !phaseMeta.Properties.Resolved && !member.NewestPhaseState.NoOrders) {
                final TextView rdyButtonText = (TextView) findViewById(R.id.rdy_button_text);
                rdyButtonFrame.setVisibility(View.VISIBLE);
                if (member.NewestPhaseState.ReadyToResolve) {
                    rdyButtonText.setPaintFlags(rdyButtonText.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                    rdyButtonText.setTextColor(Color.WHITE);
                } else {
                    rdyButtonText.setPaintFlags(rdyButtonText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    rdyButtonText.setTextColor(Color.RED);
                }
                rdyButtonFrame.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        member.NewestPhaseState.ReadyToResolve = !member.NewestPhaseState.ReadyToResolve;
                        retrofitActivity.handleReq(
                                retrofitActivity.phaseStateService.PhaseStateUpdate(member.NewestPhaseState, game.ID, game.NewestPhaseMeta.get(0).PhaseOrdinal.toString()),
                                new Sendable<SingleContainer<PhaseState>>() {
                                    @Override
                                    public void send(SingleContainer<PhaseState> phaseStateSingleContainer) {
                                        member.NewestPhaseState = phaseStateSingleContainer.Properties;
                                        GameUnserializer.manageAlarms(retrofitActivity, game, member);
                                        if (member.NewestPhaseState.ReadyToResolve) {
                                            rdyButtonText.setPaintFlags(rdyButtonText.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                                            rdyButtonText.setTextColor(Color.WHITE);
                                        } else {
                                            rdyButtonText.setPaintFlags(rdyButtonText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                                            rdyButtonText.setTextColor(Color.RED);
                                        }
                                    }
                                }, getResources().getString(R.string.updating_phase_state));
                    }
                });
            } else {
                rdyButtonFrame.setVisibility(View.GONE);
            }

            findViewById(R.id.rewind).setVisibility(View.VISIBLE);
            FloatingActionButton firstPhaseButton = (FloatingActionButton) findViewById(R.id.rewind);
            if (phaseMeta.Properties.PhaseOrdinal < 3) {
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
            if (phaseMeta.Properties.PhaseOrdinal < 2) {
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
            if (game.NewestPhaseMeta != null && game.NewestPhaseMeta.get(0).PhaseOrdinal <= phaseMeta.Properties.PhaseOrdinal) {
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
            if (game.NewestPhaseMeta != null && game.NewestPhaseMeta.get(0).PhaseOrdinal <= phaseMeta.Properties.PhaseOrdinal + 1) {
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
            if (member != null && !phaseMeta.Properties.Resolved) {
                retrofitActivity.handleReq(JoinObservable.when(JoinObservable
                        .from(retrofitActivity.optionsService.GetOptions(game.ID, phaseMeta.Properties.PhaseOrdinal.toString()))
                        .and(retrofitActivity.orderService.ListOrders(game.ID, phaseMeta.Properties.PhaseOrdinal.toString()))
                        .then(new Func2<SingleContainer<Map<String,OptionsService.Option>>, MultiContainer<Order>, Object>() {
                            @Override
                            public Object call(SingleContainer<Map<String, OptionsService.Option>> opts, MultiContainer<Order> ords) {
                                SingleContainer<Phase> phase = phases.Properties.get(phaseMeta.Properties.PhaseOrdinal.intValue() - 1);
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
                                if (phase.Properties.Units != null) {
                                    for (UnitWrapper unit : phase.Properties.Units) {
                                        if (unit.Unit.Nation.equals(member.Nation)) {
                                            units++;
                                        }
                                    }
                                }
                                if (phase.Properties.SCs != null) {
                                    for (SC sc : phase.Properties.SCs) {
                                        if (sc.Owner.equals(member.Nation)) {
                                            scs++;
                                        }
                                    }
                                }
                                if (hasBuildOpts && units < scs) {
                                    Toast.makeText(retrofitActivity,
                                            getResources().getString(R.string.you_can_build_n_this_phase,
                                                    getResources().getQuantityString(R.plurals.unit,
                                                            scs - units,
                                                            scs - units)),
                                            Toast.LENGTH_LONG).show();
                                } else if (hasDisbandOpts && scs < units) {
                                    Toast.makeText(retrofitActivity,
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
            retrofitActivity.handleReq(retrofitActivity.variantService.GetStartPhase(game.Variant), new Sendable<SingleContainer<VariantService.Phase>>() {
                @Override
                public void send(SingleContainer<VariantService.Phase> phaseSingleContainer) {
                    url = null;
                    for (Link link : phaseSingleContainer.Links) {
                        if (link.Rel.equals("map")) {
                            url = link.URL;
                            break;
                        }
                    }
                    if (url != null) {
                        load();
                    } else {
                        App.firebaseCrashReport("No map URL found in variant " + game.Variant + "?");
                        Toast.makeText(retrofitActivity, R.string.unknown_error, Toast.LENGTH_SHORT).show();
                    }
                }
            }, getResources().getString(R.string.loading_start_state));
        }
    }

    public void show(RetrofitActivity retrofitActivity,
                     Game game,
                     SingleContainer<PhaseMeta> phaseMeta,
                     MultiContainer<Phase> phases,
                     Member member,
                     Sendable<Object> phaseChangeNotifier) {
        this.retrofitActivity = retrofitActivity;
        this.game = game;
        this.phaseMeta = phaseMeta;
        this.phases = phases;
        this.member = member;
        this.phaseChangeNotifier = phaseChangeNotifier;
        draw();
    }
    
    public void load() {
        synchronized (this) {
            if (onFinished == null) {
                onFinished = new ArrayList<>();
            }
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(retrofitActivity);

        final WebView webView = (WebView) findViewById(R.id.web_view);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDisplayZoomControls(prefs.getBoolean(RetrofitActivity.ZOOM_BUTTONS_PREF_KEY, false));
        webView.setBackgroundColor(Color.parseColor("#212121"));

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                synchronized (MapView.this) {
                    if (onFinished != null) {
                        for (Runnable r : onFinished) {
                            r.run();
                        }
                    }
                    onFinished = null;
                }
            }
        });
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void infoClicked(String province) {
                List<String> infos = new ArrayList<>();
                if (units.containsKey(province)) {
                    infos.add(getResources().getString(R.string.unit_x, units.get(province).Nation));
                }
                if (SCs.containsKey(province)) {
                    infos.add(getResources().getString(R.string.supply_center_x, SCs.get(province)));
                }
                if (dislodged.containsKey(province)) {
                    infos.add(getResources().getString(R.string.dislodged_x, dislodged.get(province).Nation));
                }
                if (infos.size() > 0) {
                    Toast.makeText(getContext(), TextUtils.join("\n", infos), Toast.LENGTH_LONG).show();
                }
            }
            @JavascriptInterface
            public void orderClicked(String province) {
                onClickedProvince.send(province);
            }
        }, "Android");

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "bearer " + retrofitActivity.getAuthToken());

        Log.d("Diplicity", "Loading game view " + url);
        webView.loadUrl(url, headers);
    }
}

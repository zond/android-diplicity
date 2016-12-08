package se.oort.diplicity.game;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.webkit.WebView;
import android.widget.FrameLayout;

import se.oort.diplicity.App;
import se.oort.diplicity.R;
import se.oort.diplicity.apigen.Game;

public class MapView extends FrameLayout {

    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        addView(inflater.inflate(R.layout.map_view, null));
    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public void load(String url) {
        WebView webView = (WebView) findViewById(R.id.web_view);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setJavaScriptEnabled(true);

        Log.d("Diplicity", "Loading game view " + url);
        webView.loadUrl(url);
    }
}

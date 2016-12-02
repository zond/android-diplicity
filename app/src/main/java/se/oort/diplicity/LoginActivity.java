package se.oort.diplicity;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public class LoginActivity extends RetrofitActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setTitle(R.string.logging_in);
            WebView mWebView = new WebView(this);
            mWebView.setBackgroundColor(Color.parseColor("#212121"));
            mWebView.getSettings().setJavaScriptEnabled(true);
            final String fakeHost = "android-diplicity";
            String redirectTo = URLEncoder.encode("https://" + fakeHost + "/", "UTF-8");
            mWebView.loadUrl(((App) getApplication()).baseURL + "Auth/Login?redirect-to=" + redirectTo);
            mWebView.setWebViewClient(new WebViewClient() {
                @SuppressWarnings("deprecation")
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    Uri parsedURI = Uri.parse(url);
                    if (parsedURI.getHost().equals(fakeHost)) {
                        ((App) getApplication()).authToken = parsedURI.getQueryParameter("token");
                        Intent returnIntent = new Intent();
                        setResult(RESULT_OK,returnIntent);
                        finish();
                        return true;
                    }
                    view.loadUrl(url);
                    return false;
                }
            });

            this.setContentView(mWebView);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

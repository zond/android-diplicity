package se.oort.diplicity;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.net.URLEncoder;

public class LoginActivity extends RetrofitActivity {
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        try {
            super.onPostCreate(savedInstanceState);
            setTitle(R.string.logging_in);
            WebView mWebView = new WebView(this);
            mWebView.setBackgroundColor(Color.parseColor("#212121"));
            mWebView.getSettings().setJavaScriptEnabled(true);
            final String fakeHost = "android-diplicity";
            String redirectTo = URLEncoder.encode("https://" + fakeHost + "/", "UTF-8");
            String url = getBaseURL() + "Auth/Login?redirect-to=" + redirectTo;
            Log.d("Diplicity", "LoginActivity GETing " + url);
            mWebView.loadUrl(url);
            mWebView.setWebViewClient(new WebViewClient() {
                @SuppressWarnings("deprecation")
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    Uri parsedURI = Uri.parse(url);
                    if (parsedURI.getHost().equals(fakeHost)) {
                        Log.d("Diplicity", "LoginActivity got auth token");
                        PreferenceManager.getDefaultSharedPreferences(LoginActivity.this).edit().putString(RetrofitActivity.AUTH_TOKEN_PREF_KEY, parsedURI.getQueryParameter("token")).apply();
                        Intent returnIntent = new Intent();
                        setResult(RESULT_OK, returnIntent);
                        finish();
                        return true;
                    }
                    Log.d("Diplicity", "LoginActivity GETing " + url);
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

package com.yourapp.ui;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.yourapp.R;
import com.yourapp.bridge.WebAppInterface;

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);

        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient());

        // JS → Android bridge: JS side se "Android.xxx()" call yahin bind hota hai
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");

        // CLOUDFLARE UI: yahi line decide karti hai UI kahan se load hoga
        webView.loadUrl("https://kyapata.sauravjha.workers.dev/");
        // Agar future me local asset use karna ho:
        // webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
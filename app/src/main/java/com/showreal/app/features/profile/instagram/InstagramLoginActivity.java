package com.showreal.app.features.profile.instagram;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.showreal.app.BaseActivity;
import com.showreal.app.R;
import com.showreal.app.data.model.InstagramUser;
import com.showreal.app.databinding.ActivityInstagramBinding;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class InstagramLoginActivity extends BaseActivity {

    private ActivityInstagramBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_instagram);

        setUpWebView();
    }

    private void setUpWebView() {
        CookieManager.getInstance().removeAllCookie();
        binding.webView.setVerticalScrollBarEnabled(false);
        binding.webView.setHorizontalScrollBarEnabled(false);
        binding.webView.setWebViewClient(new LoginWebViewClient());
        binding.webView.getSettings().setJavaScriptEnabled(true);
        WebSettings webSettings = binding.webView.getSettings();
        webSettings.setSaveFormData(false);
        webSettings.setSavePassword(false);
        webSettings.setAppCacheEnabled(false);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        String url = String.format("https://api.instagram.com/oauth/authorize/?client_id=%s&redirect_uri=%s&response_type=token", getString(R.string.instagram_id), getString(R.string.instagram_redirect_url));
        binding.webView.loadUrl(url);
    }


    private void finishWithSuccess(InstagramUser instagramUser) {
        Intent intent = getIntent();
        intent.putExtra("user", instagramUser);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    private void finishWithError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        setResult(Activity.RESULT_CANCELED, null);
        finish();
    }

    @Override
    protected String getScreenName() {
        return null;
    }

    private class LoginWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            if (url.startsWith(getString(R.string.instagram_redirect_url))) {
                if (url.contains("access_token")) {
                    String token[] = url.split("=");
                    getUser(token[1]);

                } else if (url.contains("error")) {
                    String message[] = url.split("=");
                    finishWithError(message[message.length - 1]);
                }

                return true;
            }
            return false;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            binding.loadingLayout.loadingLayout.show();
            finishWithError(description);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            binding.loadingLayout.loadingLayout.show();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            binding.loadingLayout.loadingLayout.hide();
        }
    }

    private void getUser(final String token) {
        binding.loadingLayout.loadingLayout.show();

        new InstagramDataSource(getAppComponent().instagram())
                .setToken(token)
                .getData()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<InstagramUser>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        finishWithError(e.getMessage());
                    }

                    @Override
                    public void onNext(InstagramUser instagramUser) {
                        instagramUser.token = token;
                        finishWithSuccess(instagramUser);
                    }
                });
    }


}

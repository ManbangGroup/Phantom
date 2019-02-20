/*
 * Copyright (C) 2017-2018 Manbang Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wlqq.phantom.plugin.view;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.qihoo360.replugin.sample.webview.utils.WebViewResourceHelper;
import com.wlqq.phantom.communication.IService;
import com.wlqq.phantom.communication.MethodNotFoundException;
import com.wlqq.phantom.communication.PhantomServiceManager;
import com.wlqq.phantom.communication.PhantomUtils;

public class MainActivity extends FragmentActivity implements View.OnClickListener {

    private WebView mWebView;

    NotificationManagerCompat nm;

    @SuppressLint("JavascriptInterface")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_toast).setOnClickListener(this);
        findViewById(R.id.btn_notification).setOnClickListener(this);
        findViewById(R.id.btn_webview).setOnClickListener(this);

        initWebView();

        nm = NotificationManagerCompat.from(this);
    }

    private void initWebView() {
        mWebView = (WebView) findViewById(R.id.webview);
        WebViewResourceHelper.addChromeResourceIfNeeded(this);
        final WebSettings settings = mWebView.getSettings();
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setUseWideViewPort(true);
        settings.setJavaScriptEnabled(true);
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.addJavascriptInterface(this, "android");
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return super.shouldOverrideUrlLoading(view, url);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_toast:
                Toast.makeText(this, "host application id: " + getHostApplicationId(), Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_notification:
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
                builder.setSmallIcon(getHostLauncherIconId());
                builder.setAutoCancel(true);
                builder.setContentInfo("ContentInfo")
                        .setContentText("ContentText")
                        .setContentTitle("ContentTitle")
                        .setTicker("Ticker");

                final Intent intent = new Intent();
                intent.setClassName("com.wlqq.phantom.plugin.component",
                        "com.wlqq.phantom.plugin.component.MainActivity");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                // 启动插件中的 Activity 需要使用 PhantomUtils#resolveActivity 将插件原始 Intent 包装成坑位 Activity
                final Intent proxyIntent = PhantomUtils.resolveActivity(intent, ActivityInfo.LAUNCH_MULTIPLE);

                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                        0, proxyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setContentIntent(pendingIntent);
                builder.setWhen(System.currentTimeMillis());
                nm.notify(0xFF, builder.build());
                break;
            case R.id.btn_webview:
                mWebView.setVisibility(View.VISIBLE);
                mWebView.loadUrl("http://www.baidu.com");
                break;
            default:
                break;
        }
    }

    private int getHostLauncherIconId() {
        return PhantomUtils.getHostContext(this).getResources().getIdentifier("ic_launcher", "mipmap",
                "com.wlqq.phantom.sample");
    }

    private String getHostApplicationId() {
        final IService service = PhantomServiceManager.getService("HostInfoService");
        if (service != null) {
            try {
                return (String) service.call("getApplicationId");
            } catch (MethodNotFoundException e) {
                e.printStackTrace();
            }
        }
        return "unknown";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        nm.cancelAll();
    }
}
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

package com.wlqq.phantom.plugin.component;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;

abstract public class LaunchModeActivity extends FragmentActivity {

    public static final String KEY_LABEL = "LABEL";
    public static final String KEY_INDEX = "INDEX";

    private String mLabel;
    private int mIndex;
    private int mNext;
    private TextView mTvLog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);

        init();

        appendLog("onCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        appendLog("onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        appendLog("onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        appendLog("onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        appendLog("onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        appendLog("onDestroy");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        appendLog("onNewIntent");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        appendLog("onSaveInstanceState");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        appendLog("onRestoreInstanceState");
    }

    private void appendLog(String message) {
        mTvLog.append(String.format(Locale.CHINA, "[%s] %s\n", mLabel + mIndex, message));
    }

    private void init() {
        Intent intent = getIntent();
        if (intent != null) {
            mLabel = intent.getStringExtra(KEY_LABEL);
            mIndex = intent.getIntExtra(KEY_INDEX, 1);
            mNext = mIndex + 1;
        }

        ((TextView)findViewById(R.id.tv_title)).setText(mLabel + mIndex);

        ((Button)findViewById(R.id.btn_start_activity)).setText(mLabel);
        findViewById(R.id.btn_start_activity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                open();
            }
        });

        mTvLog = (TextView) findViewById(R.id.tv_log);
    }

    private void open() {
        Intent intent = new Intent(this, this.getClass());
        intent.putExtra(LaunchModeActivity.KEY_LABEL, mLabel);
        intent.putExtra(LaunchModeActivity.KEY_INDEX, mNext);
        startActivity(intent);
    }
}

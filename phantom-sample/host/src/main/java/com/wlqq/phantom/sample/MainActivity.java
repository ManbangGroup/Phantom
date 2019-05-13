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

package com.wlqq.phantom.sample;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.wlqq.phantom.library.PhantomCore;
import com.wlqq.phantom.library.pm.InstallResult;
import com.wlqq.phantom.library.pm.PluginInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Button mBtnEmbedPluginView;
    private RecyclerView mRvPluginList;
    private List<Pair<String, PluginInfo>> mPluginList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnEmbedPluginView = (Button) findViewById(R.id.btn_embed_plugin_view);
        mBtnEmbedPluginView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, EmbedPluginViewActivity.class));
            }
        });
        mRvPluginList = (RecyclerView) findViewById(R.id.rv_plugin_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRvPluginList.setLayoutManager(layoutManager);
        mRvPluginList.addItemDecoration(new DividerItemDecoration(this, layoutManager.getOrientation()));
        initPluginListAsync();
    }

    /**
     * Plugin apk list in assets/plugins
     */
    private void initPluginListAsync() {
        // Pair<plugin_file_name, PluginInfo>
        mPluginList = new ArrayList<>();

        new AsyncTask<Void, Void, Void>() {
            private ProgressDialog mProgressDialog;

            @Override
            protected void onPreExecute() {
                mProgressDialog = ProgressDialog.show(MainActivity.this, "please wait",
                        "init plugin list", true, false);
            }

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    final String pluginsDir = "plugins";
                    String[] list = getAssets().list(pluginsDir);
                    if (list != null) {
                        for (String file : list) {
                            if (file.endsWith(".apk")) {
                                final String filePath = pluginsDir + "/" + file;
                                InstallResult installResult = PhantomCore.getInstance().installPluginFromAssets(
                                        filePath);
                                if (installResult.isSuccess() && installResult.plugin != null) {
                                    installResult.plugin.start();
                                    mPluginList.add(Pair.<String, PluginInfo>create(filePath, installResult.plugin));
                                } else {
                                    // should not happen
                                    mPluginList.add(Pair.<String, PluginInfo>create(filePath, null));
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }

                mRvPluginList.setAdapter(new PluginAdapter(mPluginList) {

                    @Override
                    public void onItemClick(int position) {
                        launchPlugin(position);
                    }
                });
            }
        }.execute();
    }

    private void launchPlugin(final int position) {
        final Pair<String, PluginInfo> item = mPluginList.get(position);
        final String fileName = item.first;
        final PluginInfo pluginInfo = item.second;

        if (pluginInfo != null && pluginInfo.isStarted()) {
            launchPluginActivity(pluginInfo.packageName, pluginInfo.getLauncherActivities());
        } else {
            new AsyncTask<Object, Void, PluginInfo>() {

                private ProgressDialog mProgressDialog;

                @Override
                protected void onPreExecute() {
                    mProgressDialog = ProgressDialog.show(MainActivity.this, "please wait",
                            "install plugin: " + fileName, true, false);
                }

                @Override
                protected PluginInfo doInBackground(Object... params) {
                    if (params[1] instanceof PluginInfo) {
                        final PluginInfo pluginInfo = (PluginInfo) params[1];
                        pluginInfo.start();
                        return pluginInfo;
                    } else {
                        // install plugin apk in host assets, and then start it
                        InstallResult installResult = PhantomCore.getInstance().installPluginFromAssets(
                                (String) params[0]);
                        if (installResult.isSuccess() && installResult.plugin != null) {
                            installResult.plugin.start();
                        }
                        return installResult.plugin;
                    }
                }

                @Override
                protected void onPostExecute(PluginInfo pluginInfo) {
                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                    }

                    if (pluginInfo != null && pluginInfo.isStarted()) {
                        mPluginList.set(position, Pair.create(fileName, pluginInfo));
                        launchPluginActivity(pluginInfo.packageName, pluginInfo.getLauncherActivities());
                    }
                }

            }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, fileName, pluginInfo);
        }


    }

    private void launchPluginActivity(String packageName, List<String> launcherActivities) {
        if (launcherActivities.isEmpty()) {
            Toast.makeText(this, "launcher activity not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent();
        intent.setComponent(new ComponentName(packageName, launcherActivities.get(0)));

        PhantomCore.getInstance().startActivity(this, intent);

    }

    public abstract static class PluginAdapter extends RecyclerView.Adapter<PluginAdapter.ViewHolder> {

        private List<Pair<String, PluginInfo>> mAppInfos;

        public PluginAdapter(List<Pair<String, PluginInfo>> list) {
            mAppInfos = list;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            // Create a new view.
            View v = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_plugin, viewGroup, false);

            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, final int position) {
            final Pair<String, PluginInfo> item = mAppInfos.get(position);
            viewHolder.tvLabel.setText(item.first.replace("plugins/", ""));
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClick(position);
                }
            });
        }

        // Return the size of your data set (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mAppInfos.size();
        }

        public abstract void onItemClick(int position);

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public TextView tvLabel;

            ViewHolder(View v) {
                super(v);
                tvLabel = (TextView) v.findViewById(R.id.tv_app_label);
            }
        }
    }
}

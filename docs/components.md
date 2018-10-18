# Phantom 组件使用

Phantom 支持在插件中使用 `Activity`、`Service`、`BroadcastReceiver` 组件，暂不支持`ContentProvider` 组件。

## Activity 组件

### Activity 声明

* 声明一个插件 `Activity` 时只需在插件的 `AndroidManifest.xml` 中声明即可。代码如下：

    ```xml
    <activity
          android:name="com.wlqq.phantom.plugin.component.SingleInstanceActivity"
          android:theme="@android:style/Theme.Translucent.NoTitleBar"
          android:screenOrientation="portrait"
          android:launchMode="singleInstance"/>
    ```
    ***注：***插件中暂时不支持组件设置 `android:process` 属性；即使设置，独立进程也不生效。
    > 有关声明组件的具体内容，请参见 『[Android 官方文档：《Activity》](https://developer.android.com/guide/components/activities.html)』

### 宿主启动插件 Activity：

* 宿主启动插件 Activity 时与正常启动 Activity 相同，通过 `Intent` 方式进行启动（暂不支持宿主隐式 `Intent` 模式启动插件 Activity），代码如下：

    ```java
    Intent intent = new Intent();
    // 参数 1：插件 package name， 参数 2：插件 activity className
    intent.setClassName("com.wlqq.phantom.plugin.component", "com.wlqq.phantom.plugin.component.MainActivity");
    PhantomCore.getInstance().startActivity(this, intent);
    ```
  ***注：***而在这之前，值得注意的是，要启动一个插件中的 Activity，则该插件必须是一个已经安装的并且启动的插件。
  
  > 有关安装与启动插件的具体内容，请参见 [Phantom 插件管理](./plugin-management.md)
  
* 如果想要直接启动一个插件，但又不知道对被标为 `android.intent.category.LAUNCHER` 的入口 `Activity` 时，可通过 `PhantomCore` 提供的 `getLauncherActivities(..)` 接口来获取入口  `Activity` 进行启动。代码如下：

    ```java
    List<String> launcherActivities = pluginInfo.getLauncherActivities();
    Intent intent = new Intent();
    if (launcherActivities.size() != 0) {
        intent.setClassName(pluginInfo.packageName, launcherActivities.get(0));
    }
    PhantomCore.getInstance().startActivity(this, intent);
    ```

### 插件启动宿主 Activity：

* 插件启动宿主 Activity 与 宿主启动插件 Activity 相同，代码如下：

    ```java
    Intent intent = new Intent();
    // 参数 1：宿主 packageName， 参数 2：宿主 activity className
    intent.setClassName("com.wlqq.phantom.sample", "com.wlqq.phantom.sample.SubActivity");
    startActivity(intent);
    ```

* 插件启动其它插件 Activity 与上述方法相同（代码略）

### 插件启动当前插件 Activity：

* 插件启动当前插件 Activity，代码如下：

    ```java
    Intent intent = new Intent();
    intent.setClass(this, SingleInstanceActivity.class);
    startActivity(intent);
    ```
* 同时也支持通过 `intent.setClassName(...)` 启动（代码略）

## Service 组件

### Service 定义

* 插件中定义一个 `Service`，代码如下：

    ```java
    public class PluginService extends Service {

        public static final String TAG = "PluginService";

        private final IBinder mBinder = new LocalBinder();

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Log.i(TAG, "PluginService onStartCommand...");
            return super.onStartCommand(intent, flags, startId);
        }

        @Override
        public void onDestroy() {
            Log.i(TAG, "PluginService onDestroy...");
            super.onDestroy();
        }

        @Override
        public IBinder onBind(Intent intent) {
            Log.i(TAG, "PluginService bind...");
            return mBinder;
        }

        @Override
        public boolean onUnbind(Intent intent) {
            Log.i(TAG, "PluginService unbind...");
            return super.onUnbind(intent);
        }

        public class LocalBinder extends Binder {
            PluginService getService() {
                return PluginService.this;
            }
        }
    }
    ```

### Service 注册

* 在插件中，在 `AndroidManifest.xml` 中对 `Service` 进行注册，代码如下：

    ```xml
    <service
         android:name="com.wlqq.phantom.plugin.component.service.PluginService"
         android:enabled="true"
         android:exported="false"/>
    ```

### Service 启动

* 插件中，使用 `startService` 模式启动 `Service`，代码如下：

    ```java
    Intent intent = new Intent();
    intent.setClass(getActivity(), PluginService.class);
    startService(intent);
    ```
    ***注：***`stopService()` 与其相同（代码略）。

* 插件中，使用 `bindService` 模式启动 `Service`，代码如下：

    ```java
    Intent intent = new Intent();
    intent.setClass(this, PluginService.class);
    bindService(intent, new ServiceConnection() {
         @Override
         public void onServiceConnected(ComponentName name, IBinder service) {
               PluginService.LocalBinder binder = (PluginService.LocalBinder) service;
         }

         @Override
         public void onServiceDisconnected(ComponentName name) {
         }
    }, Service.BIND_AUTO_CREATE);
    ```
    ***注：***`unbindService()` 与其相同（代码略）。

## BroadcastReceiver 组件

### BroadcastReceiver 定义

* 插件中定义一个 `BroadcastReceiver`。代码如下：

    ```java
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String data = intent.getStringExtra("result");
        }
    };
    ```

### BroadcastReceiver 注册

* 插件中注册一个 `BroadcastReceiver`。代码如下：

    ```java
    registerReceiver(mBroadcastReceiver, new IntentFilter("com.phantom.plugin.component.msg"));
    ```
    ***注：***同时支持静态注册（代码略）

### BroadcastReceiver 发送

* 插件中发送 `BroadcastReceiver`。代码如下：

    ```java
    Intent broadcast = new Intent("com.phantom.plugin.component.msg");
    broadcast.putExtra("result", "Broadcast Msg.");
    sendBroadcast(broadcast);
    ```

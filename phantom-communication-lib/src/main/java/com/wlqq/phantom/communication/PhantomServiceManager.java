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

package com.wlqq.phantom.communication;


import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * 管理插件或宿主提供给其他插件/宿主调用的功能接口。
 * <p>
 * 本类提供功能模块的注册、卸载、查询和获取功能。
 * <p>
 * 为了方便管理，注册 {@link #registerService(String, String, Object)} 功能模块的时候必须指定模块所属类别。<br/>
 * 例如，同一个插件提供多个功能模块，他们应该属于同一类别，在插件被卸载的时候，该插件注册的功能模块也应该被卸载。<br/>
 * 被注册的功能模块需要用 {@link RemoteMethod}注解来标记提供供给外部调用的方法使用。例如：
 * <pre>{@code
 * public class PluginService {
 *     @RemoteMethod(name="pluginMessage")
 *     public String pluginMsg(String back){
 *         return "from plugin " + back;
 *     }
 * }}
 * </pre>
 * <p>
 * <p>
 * <p>
 * <code>name="pluginMessage"</code> 表示外部调用 <code>pluginMsg</code> 方法时使用的名称，例如：
 * <pre>{@code
 * IService service = PhantomServiceManager.getService("plugin1");
 * String message = (String)service.call("pluginMessage", "hi");
 * }
 * </pre>
 * 除了使用 <code>IService</code> 的 <code>call</code> 方法来调用功能模块的方法外还可以将 <code>service</code> 转换成自定义接口来调用，
 * 调用端首先需要自定义一个接口，接口中的方法，就是要调用的模块的方法，例如：
 * <pre>{@code
 * public interface PluginService {
 *     //接口方法声明与实现类中的注解名字一样
 *     public String pluginMessage(String back);
 * }
 *
 * PluginService ps = PhantomServiceManager.asInterface(service, PluginService.class);
 * String message = ps.pluginMessage("hi");
 * }
 * </pre>
 */
public class PhantomServiceManager {

    // { service_category -> { service_name -> service_object } }
    private static final HashMap<String, HashMap<String, Object>> CATEGORY_SERVICES_MAP = new HashMap<>();
    private static String sHostPackage;
    private static String sHostVersionName;
    private static int sHostVersionCode;
    private static String sPhantomVersionName;
    private static int sPhantomVersionCode;
    private static boolean sInitialized;

    private PhantomServiceManager() {
    }

    /**
     * 初始化 {@link PhantomServiceManager}，<b>必须</b>在调用该类其他方法之前调用该方法
     *
     * @param hostPackage     宿主包名
     * @param hostVersionName 宿主版本名
     * @param hostVersionCode 宿主版本号
     */
    public static synchronized void init(String hostPackage,
            String hostVersionName,
            int hostVersionCode,
            String phantomVersionName,
            int phantomVersionCode) {
        if (sInitialized) {
            return;
        }

        sHostPackage = hostPackage;
        sHostVersionName = hostVersionName;
        sHostVersionCode = hostVersionCode;
        sPhantomVersionName = phantomVersionName;
        sPhantomVersionCode = phantomVersionCode;

        sInitialized = true;
    }

    /**
     * 获取宿主包名
     *
     * @return 宿主包名
     */
    public static String getHostPackage() {
        return sHostPackage;
    }

    /**
     * 获取宿主版本名
     *
     * @return 宿主版本名
     */
    public static String getHostVersionName() {
        return sHostVersionName;
    }

    /**
     * 获取宿主版本号
     *
     * @return 宿主版本号
     */
    public static int getHostVersionCode() {
        return sHostVersionCode;
    }

    /**
     * 获取插件框架版本名
     *
     * @return 插件框架版本名
     */
    public static String getPhantomVersionName() {
        return sPhantomVersionName;
    }

    /**
     * 获取插件框架版本号
     *
     * @return 插件框架版本号
     */
    public static int getPhantomVersionCode() {
        return sPhantomVersionCode;
    }

    /**
     * 注册功能模块
     *
     * @param category 功能模块所属类别， 同一个插件类别应该一致，通常使用 <b>宿主/插件 包名</b>
     * @param name     功能模块的名字，同一类别可以有多个功能模块
     * @param service  功能模块的实现
     * @return 注册成功返回 true，否则返回 false
     */
    public static boolean registerService(String category, String name, Object service) {
        if (null == category || 0 == category.length()
                || null == name || 0 == name.length()
                || null == service) {
            return false;
        }

        synchronized (CATEGORY_SERVICES_MAP) {
            HashMap<String, Object> services = CATEGORY_SERVICES_MAP.get(category);
            if (null == services) {
                services = new HashMap<>();
                CATEGORY_SERVICES_MAP.put(category, services);
            }

            //存在服务名称相同而类不同的情况不允许注册，服务名称和服务类都相同允许覆盖之前的服务
            Object hasService = services.get(name);
            if (null != hasService && !hasService.getClass().getName().equals(service.getClass().getName())) {
                return false;
            }

            services.put(name, service);
        }

        return true;
    }

    /**
     * 注册功能模块
     *
     * @param name    服务名字格式 packageName/name, 如果服务名称不加 packageName，则默认被注册成 sHostPackage/name，被认为是宿主提供的服务
     * @param service 功能模块的实现
     * @return 注册成功返回 true，否则返回 false
     */
    public static boolean registerService(String name, Object service) {
        if (null == name) {
            return false;
        }

        if (!name.contains("/") && null != sHostPackage) {
            return registerService(sHostPackage, name, service);
        }

        String[] items = name.split("/");
        if (items.length != 2) {
            return false;
        }

        return registerService(items[0], items[1], service);
    }

    /**
     * 注册功能模块, 服务对象<b>必须</b>使用 {@link PhantomService} 注解
     *
     * @param service service 的实现类必须使用 {@link PhantomService} 注解
     * @return 成功返回 true，否则返回 false
     * @throws IllegalArgumentException 若服务为 <b>null</b> 或没有使用 {@link PhantomService} 注解
     */
    public static boolean registerService(Object service) throws IllegalArgumentException {
        PhantomService serviceTypeAnnotation = checkPhantomServiceAnnotation(service);

        String serviceType = serviceTypeAnnotation.name();
        return registerService(serviceType, service);
    }

    /**
     * 检查服务对象类是否用 {@link PhantomService} 注解
     *
     * @param service 服务对象
     * @return {@link PhantomService} 若服务使用了 {@link PhantomService} 注解
     * @throws IllegalArgumentException 若服务为 <b>null</b> 或没有使用 {@link PhantomService} 注解
     */
    private static PhantomService checkPhantomServiceAnnotation(Object service)
            throws IllegalArgumentException {
        if (service == null) {
            throw new IllegalArgumentException("service object must not be null");
        }
        final Class<?> aClass = service.getClass();
        PhantomService serviceTypeAnnotation = aClass.getAnnotation(PhantomService.class);
        if (serviceTypeAnnotation == null) {
            throw new IllegalArgumentException(
                    "service class must has PhantomService annotation: " + aClass.getName());
        } else {
            return serviceTypeAnnotation;
        }
    }

    /**
     * 反注册指定类别的所有服务
     *
     * @param category 功能模块所属类别，通常使用 <b>宿主/插件 包名</b>
     */
    public static void unregisterService(String category) {
        synchronized (CATEGORY_SERVICES_MAP) {
            CATEGORY_SERVICES_MAP.remove(category);
        }
    }

    /**
     * 反注册所有服务
     */
    public static void unregisterAllService() {
        synchronized (CATEGORY_SERVICES_MAP) {
            CATEGORY_SERVICES_MAP.clear();
        }
    }

    /**
     * 获取指定模块下的 service 列表
     *
     * @param category 功能模块类别，通常使用 <b>宿主/插件 包名</b>
     * @return category 类别下的 service 列表
     */
    public static List<IService> getServices(String category) {
        List<IService> services;

        synchronized (CATEGORY_SERVICES_MAP) {
            HashMap<String, Object> serviceObjectMap = CATEGORY_SERVICES_MAP.get(category);
            if (serviceObjectMap == null) {
                services = new ArrayList<>(0);
            } else {
                services = new ArrayList<>(serviceObjectMap.size());
                for (Object serviceObject : serviceObjectMap.values()) {
                    services.add(new ServiceModule(serviceObject));
                }
            }
        }

        return services;
    }

    /**
     * 获取指定类别/名字下的功能模块
     *
     * @param category 功能模块类别，通常使用 <b>宿主/插件 包名</b>
     * @param name     功能模块名字
     * @return 成功返回功能模块实例，否则返回 null
     */
    public static IService getService(String category, String name) {
        synchronized (CATEGORY_SERVICES_MAP) {
            final HashMap<String, Object> services = CATEGORY_SERVICES_MAP.get(category);
            if (services == null) {
                return null;
            }

            return new ServiceModule(services.get(name));
        }
    }

    /**
     * 根据名字获取功能模块，返回找到的第一个名字为 name 的功能模块，否则返回 null
     *
     * @param name 服务名字格式 packageName/name, 如果参数 name 中省去了 packageName，则被认为是查找宿主中的服务 sHostPackage/name
     * @return 成功返回功能模块实例，否则返回 null
     */
    public static IService getService(String name) {
        if (null == name) {
            return null;
        }

        if (!name.contains("/") && null != sHostPackage) {
            return getService(sHostPackage, name);
        }

        String[] items = name.split("/");
        if (items.length != 2) {
            return null;
        }

        return getService(items[0], items[1]);
    }

    /**
     * 根据名字获取功能模块，返回找到的第一个名字为 name 的功能模块，并转换为 type 参数指定的接口类型，否则返回 null
     *
     * @param name 服务名字格式 packageName/name，如果参数 name 中省去了 packageName，则被认为是查找宿主中的服务 sHostPackage/name
     * @param type 返回的对象类型， type 为必须为一个 interface
     * @return 成功返回 type 的一个对象，否则返回 null
     */
    public static <T> T getService(String name, Class<T> type) {
        IService service = getService(name);
        return asInterface(service, type);
    }

    /**
     * 是否有注册指定名字的服务
     *
     * @param name 服务名字格式 packageName/name，如果参数 name 中省去了 packageName，则被认为是查找宿主中的服务 sHostPackage/name
     * @return 如何有注册，返回 true；否则返回 false
     */
    public static boolean hasService(String name) {
        boolean res = false;

        synchronized (CATEGORY_SERVICES_MAP) {
            Collection<HashMap<String, Object>> allServices = CATEGORY_SERVICES_MAP.values();
            Iterator<HashMap<String, Object>> iter = allServices.iterator();
            while (iter.hasNext() && (!res)) {
                HashMap<String, Object> services = iter.next();
                res = services.containsKey(name);
            }
        }

        return res;
    }

    /**
     * 将功能模块 service 转换成自定义接口，方便调用。
     *
     * @param service 需要转换的功能模块
     * @param type    自定义接口
     * @return 自定义接口的实现类
     */
    public static <T> T asInterface(IService service, Class<T> type) {
        T res = null;
        if (type.isInterface()) {
            ServiceInvocationHandler handler = new ServiceInvocationHandler(service);
            res = (T) Proxy.newProxyInstance(type.getClassLoader(),
                    new Class[]{type, ServiceInfo.class}, handler);
        }
        return res;
    }

    /**
     * 是否有注册指定名字的服务
     *
     * @param category 功能模块所属类别， 同一个插件类别应该一致，通常使用 <b>宿主/插件 包名</b>
     * @param name     功能模块的名字，同一类别可以有多个功能模块
     * @return 如何有注册，返回 true; 否则返回 false
     */
    public static boolean hasService(String category, String name) {
        boolean res = false;

        synchronized (CATEGORY_SERVICES_MAP) {
            if (CATEGORY_SERVICES_MAP.containsKey(category)) {
                res = CATEGORY_SERVICES_MAP.get(category).containsKey(name);
            }
        }

        return res;
    }

    private static class ServiceInvocationHandler implements InvocationHandler {
        private final IService mService;

        public ServiceInvocationHandler(IService service) {
            this.mService = service;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (null == mService) {
                return null;
            }
            try {
                if (method.getDeclaringClass() == ServiceInfo.class) {
                    Method targetMethod = ServiceInfo.class.getDeclaredMethod(method.getName());
                    return targetMethod.invoke(mService, args);
                }

                return mService.call(method.getName(), args);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}



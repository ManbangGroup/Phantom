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

package com.wlqq.phantom.library.pm;

import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.os.PatternMatcher;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.ArraySet;
import android.text.TextUtils;

import com.wlqq.phantom.library.utils.VLog;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 插件 APK AndroidManifest.xml 解析器
 */

final class AndroidManifestParser {

    private static final String NAMESPACE_ANDROID = "http://schemas.android.com/apk/res/android";
    private static final String ACTION_MAIN = "android.intent.action.MAIN";
    private static final String CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER";
    private static final String MANIFEST = "manifest";
    private static final String ACTIVITY = "activity";
    private static final String SERVICE = "service";
    private static final String RECEIVER = "receiver";
    private static final String INTENT_FILTER = "intent-filter";
    private static final String ACTION = "action";
    private static final String CATEGORY = "category";
    private static final String DATA = "data";

    private AndroidManifestParser() {
        // prevent instantiate
    }


    @Nullable
    private static String getFullClassName(String pkgName, String className) {
        if (className == null) {
            return null;
        }

        if (className.startsWith(".")) {
            return pkgName + className;
        } else if (!className.contains(".")) {
            return pkgName + "." + className;
        } else {
            return className;
        }
    }

    @NonNull
    @SuppressWarnings("checkstyle:AvoidNestedBlocks")
    static ComponentIntentFilters parse(@NonNull AssetManager assetManager) throws ParseApkException {
        String curName = null;
        IntentFilter curFilter = null;
        List<IntentFilter> curFilters = null;
        List<String> curActions = null;
        List<String> curCategories = null;
        List<DataElement> curDataElements = null;

        String packageName = null;
        final ComponentIntentFilters componentIntentFilters = new ComponentIntentFilters();
        XmlResourceParser parser = null;
        try {
            parser = assetManager.openXmlResourceParser("AndroidManifest.xml");
            int eventType = parser.next();
            while (eventType != XmlResourceParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlResourceParser.START_TAG: {
                        final String name = parser.getName();
                        switch (name) {
                            case MANIFEST:
                                packageName = parser.getAttributeValue(null, "package");
                                break;
                            case ACTIVITY:
                                curName = getFullClassName(packageName, getNameAttrValue(parser));
                                curFilters = new ArrayList<>();
                                break;
                            case SERVICE:
                                curName = getFullClassName(packageName, getNameAttrValue(parser));
                                curFilters = new ArrayList<>();
                                break;
                            case RECEIVER:
                                curName = getFullClassName(packageName, getNameAttrValue(parser));
                                curFilters = new ArrayList<>();
                                break;
                            case INTENT_FILTER:
                                if (curFilters != null) {
                                    curFilter = new IntentFilter();
                                    curFilters.add(curFilter);
                                }
                                break;
                            case ACTION:
                                if (curActions == null) {
                                    curActions = new ArrayList<>();
                                }
                                curActions.add(getNameAttrValue(parser));
                                break;
                            case CATEGORY:
                                if (curCategories == null) {
                                    curCategories = new ArrayList<>();
                                }
                                curCategories.add(getNameAttrValue(parser));
                                break;

                            case DATA:
                                if (curDataElements == null) {
                                    curDataElements = new ArrayList<>();
                                }
                                DataElement data = new DataElement();
                                data.scheme = parser.getAttributeValue(NAMESPACE_ANDROID, "scheme");
                                data.mimeType = parser.getAttributeValue(NAMESPACE_ANDROID, "mimeType");
                                data.host = parser.getAttributeValue(NAMESPACE_ANDROID, "host");
                                data.port = parser.getAttributeValue(NAMESPACE_ANDROID, "port");
                                data.path = parser.getAttributeValue(NAMESPACE_ANDROID, "path");
                                data.pathPattern = parser.getAttributeValue(NAMESPACE_ANDROID, "pathPattern");
                                data.pathPrefix = parser.getAttributeValue(NAMESPACE_ANDROID, "pathPrefix");

                                curDataElements.add(data);
                                break;
                            default:
                                break;
                        }
                        break;
                    }
                    case XmlResourceParser.END_TAG: {
                        final String name = parser.getName();
                        switch (name) {
                            case INTENT_FILTER:
                                if (curActions != null) {
                                    for (String action : curActions) {
                                        curFilter.addAction(action);
                                    }
                                }
                                if (curCategories != null) {
                                    for (String cate : curCategories) {
                                        curFilter.addCategory(cate);
                                    }
                                }

                                if (curDataElements != null) {
                                    for (DataElement bean : curDataElements) {
                                        if (!TextUtils.isEmpty(bean.scheme)) {
                                            curFilter.addDataScheme(bean.scheme);
                                        }

                                        if (!TextUtils.isEmpty(bean.host) && !TextUtils.isEmpty(bean.port)) {
                                            curFilter.addDataAuthority(bean.host, bean.port);
                                        }

                                        if (!TextUtils.isEmpty(bean.path)) {
                                            curFilter.addDataPath(bean.path, bean.getPatternMatcherType());
                                        }

                                        try {
                                            if (!TextUtils.isEmpty(bean.mimeType)) {
                                                curFilter.addDataType(bean.mimeType);
                                            }
                                        } catch (IntentFilter.MalformedMimeTypeException e) {
                                            VLog.w(e, "invalid mime type: %s", bean.mimeType);
                                        }
                                    }
                                }

                                curActions = null;
                                curCategories = null;
                                curDataElements = null;
                                break;
                            case ACTIVITY:
                                if (!TextUtils.isEmpty(curName) && curFilters != null) {
                                    componentIntentFilters.mActivities.put(curName, curFilters);
                                    for (IntentFilter filter : curFilters) {
                                        if (filter.hasAction(ACTION_MAIN) && filter.hasCategory(CATEGORY_LAUNCHER)) {
                                            componentIntentFilters.mLauncherActivities.add(curName);
                                        }
                                    }
                                }
                                break;
                            case SERVICE:
                                if (!TextUtils.isEmpty(curName) && curFilters != null) {
                                    componentIntentFilters.mServices.put(curName, curFilters);
                                }
                                break;
                            case RECEIVER:
                                if (!TextUtils.isEmpty(curName) && curFilters != null) {
                                    componentIntentFilters.mReceivers.put(curName, curFilters);
                                }
                                break;
                            default:
                                break;
                        }
                        break;
                    }

                    default:
                        break;
                }
                eventType = parser.next();
            }
            return componentIntentFilters;
        } catch (IOException e) {
            throw new ParseApkException("open AndroidManifest.xml", e);
        } catch (XmlPullParserException e) {
            throw new ParseApkException("parse AndroidManifest.xml", e);
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
    }

    // 获取 android:name 属性的值
    private static String getNameAttrValue(XmlResourceParser parser) {
        return parser.getAttributeValue(NAMESPACE_ANDROID, "name");
    }

    static class ComponentIntentFilters {
        /**
         * All Activities
         */
        Map<String, List<IntentFilter>> mActivities = new ArrayMap<>();

        /**
         * All Services
         */
        Map<String, List<IntentFilter>> mServices = new ArrayMap<>();

        /**
         * All Receivers
         */
        Map<String, List<IntentFilter>> mReceivers = new ArrayMap<>();

        /**
         * Launcher Activities
         */
        Set<String> mLauncherActivities = new ArraySet<>();
    }

    private static class DataElement {

        public String scheme;
        public String host;
        public String port;
        public String mimeType;
        public String path;
        public String pathPattern;
        public String pathPrefix;

        @Override
        public String toString() {
            return String.format(
                    "{scheme:%s, host:%s, mimeType:%s, path:%s, pathPattern:%s, pathPrefix:%s, port:%s}", scheme, host,
                    mimeType, pathPattern, pathPrefix, path, port);
        }

        /**
         * 获得 path 匹配类型
         */
        int getPatternMatcherType() {
            if (TextUtils.isEmpty(pathPattern) && TextUtils.isEmpty(pathPattern)) {
                return PatternMatcher.PATTERN_LITERAL;
            } else if (!TextUtils.isEmpty(pathPrefix)) {
                return PatternMatcher.PATTERN_PREFIX;
            } else {
                return PatternMatcher.PATTERN_SIMPLE_GLOB;
            }
        }
    }

}

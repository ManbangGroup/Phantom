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


import java.lang.reflect.Method;

/**
 * 供远程调用的服务模块描述
 */
class ServiceModule implements IService {

    private final Object mService;

    ServiceModule(Object service) {
        this.mService = service;
    }

    @Override
    public Object call(String methodName, Object... args) throws MethodNotFoundException {
        if (null == mService) {
            return null;
        }

        if (null == args) {
            args = new Object[0];
        }

        MethodInfo targetMethod = findMethod(methodName, args);
        Object resObj = null;
        if (null != targetMethod.mMethod) {
            try {
                if (targetMethod.mDynamicArgs) {
                    resObj = targetMethod.mMethod.invoke(mService,
                            reBuildArg(targetMethod.mFixedArgsLen, args));
                } else {
                    resObj = targetMethod.mMethod.invoke(mService, args);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return resObj;
    }

    @Override
    public String getServiceName() {
        if (null == mService) {
            return null;
        }

        PhantomService ps = mService.getClass().getAnnotation(PhantomService.class);
        if (null == ps) {
            return null;
        }

        return ps.name();
    }

    @Override
    public int getServiceVersion() {
        if (null == mService) {
            return 0;
        }

        PhantomService ps = mService.getClass().getAnnotation(PhantomService.class);
        if (null == ps) {
            return 0;
        }

        return ps.version();
    }

    private Object[] reBuildArg(int fixedArgsLen, Object... args) {
        Object[] dyArgs = new Object[fixedArgsLen + 1];
        Object[] extArgs = new Object[args.length - fixedArgsLen];
        dyArgs[fixedArgsLen] = extArgs;
        int i = 0;
        for (; i < fixedArgsLen; i++) {
            dyArgs[i] = args[i];
        }

        if (null != args[i] && args[i].getClass().getName().equals("[Ljava.lang.Object;")) {
            dyArgs[fixedArgsLen] = args[i];
        } else {
            for (int j = 0; i < args.length; i++, j++) {
                extArgs[j] = args[i];
            }
        }

        return dyArgs;
    }

    private MethodInfo findMethod(String methodName, Object... args)
            throws MethodNotFoundException {
        Method[] methods = mService.getClass().getDeclaredMethods();
        for (Method method : methods) {
            RemoteMethod annotation = method.getAnnotation(RemoteMethod.class);
            if (null == annotation) {
                continue;
            }

            if (!annotation.name().equals(methodName)) {
                continue;
            }

            MethodInfo methodInfo = new MethodInfo();

            Class[] requestParams = method.getParameterTypes();
            int matchLen = requestParams.length;
            if (requestParams.length > 0 && requestParams[requestParams.length
                    - 1].getName().equals("[Ljava.lang.Object;")) {
                matchLen -= 1;
                methodInfo.mDynamicArgs = true;
            } else if (requestParams.length != args.length) {
                continue;
            }

            methodInfo.mFixedArgsLen = matchLen;

            //匹配参数
            int i = 0;
            for (int j = 0; j < matchLen; j++) {
                Class cls = requestParams[j];

                if (null == args[i]) {
                    i++;
                    continue;
                }
                if (cls == Integer.TYPE) {
                    cls = Integer.class;
                } else if (cls == Short.TYPE) {
                    cls = Short.class;
                } else if (cls == Double.TYPE) {
                    cls = Double.class;
                } else if (cls == Float.TYPE) {
                    cls = Float.class;
                } else if (cls == Long.TYPE) {
                    cls = Long.class;
                } else if (cls == Boolean.TYPE) {
                    cls = Boolean.class;
                } else if (cls == Byte.TYPE) {
                    cls = Byte.class;
                }

                if (!cls.isAssignableFrom(args[i].getClass())) {
                    break;
                }

                i++;
            }

            if (i == matchLen) {
                methodInfo.mMethod = method;
                return methodInfo;
            }
        }

        throw new MethodNotFoundException(
                "the method " + methodName + " for service " + mService.getClass().getName()
                        + " not found. please check the methodName and params");
    }

    private static class MethodInfo {
        Method mMethod;
        int mFixedArgsLen;
        boolean mDynamicArgs;
    }
}

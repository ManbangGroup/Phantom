/*
 * Copyright (C) 2017-2019 Manbang Group
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

package com.wlqq.phantom.gradle.replace

import javassist.CannotCompileException
import javassist.CtClass
import javassist.expr.ExprEditor
import javassist.expr.MethodCall


class ReplaceMethodCall {
    static boolean replaceFragmentMethods(CtClass targetCls) {
        return replaceMethod(targetCls,
                'android.app.Fragment', ['getActivity'] as ArrayList,
                'com.wlqq.phantom.library.proxy.PhantomActivityAware', ['getPhantomActivity'] as ArrayList)
    }

    /**
     * 将 targetClass类中引用的originClass类中的这些方法（originMethods中包含的方法）替换为replaceCls类中的方法（replaceMethods中包含的方法），
     * 注意：1. replaceCls是originClass的父类
     *      2. originMethods与replaceMethods中的方法一一对应，他们具体相同的参数和返回值类型
     * 例如：
     * class A extends android.app.Fragment {
     *     public Activity getPhantomActivity() {
     *         System.out.println("do more work");
     *         return super.getActivity();
     *     }
     * }
     *
     * class B extends A {
     *     View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
     *          Activity activity = getActivity();
     *     }
     * }
     *
     * 该类就是将class B中对android.app.Fragment的getActivity()方法调用替换为class A中的getPhantomActivity(),
     * 或者有类
     * class C {
     *     。。。
     *     B b = new B();
     *     b.getActivity()
     *     。。。
     * }
     *将class C中的b.getActivity()调用替换成b.getPhantomActivity()
     *
     * @param targetClass 当前被处理的类
     * @param originClass 需要被替换的类（类全名）,例如android.app.Fragment
     * @param originMethods 需要被替换的方法
     * @param replaceCls 去替换的类（类全名）
     * @param replaceMethods 去替换的方法，与originMethods中的方法一一对应，比如replaceMethods index为0的方法对应originMethods中index为0的方法
     * @return 发生了替换返回true，否则返回false
     */
    static boolean replaceMethod(CtClass targetClass, String originClass, ArrayList<String> originMethods, String replaceCls, ArrayList<String>  replaceMethods) {
        boolean changed = false
        targetClass.instrument(new ExprEditor() {
            @Override
            void edit(MethodCall method) throws CannotCompileException {

                String methodName = method.getMethodName()
                int index = originMethods.indexOf(methodName)
                if (index > -1) {
                    //查找被调用方法method所属的类和它的父类，来判断这个类是否是originClass的子类，如果是才替换
                    CtClass realCls = method.method.declaringClass
                    String superClass = realCls.getName()
                    while (null != superClass && superClass != originClass) {
                        superClass = realCls.classFile.superclass
                        realCls = realCls.superclass
                    }
                    if (superClass == originClass) {
                        println "ReplaceMethodCall replace ${targetClass.getName()} call ${methodName} to ${replaceMethods.get(index)}"
                        method.replace('$_ = ((' + replaceCls + ')$0).' + replaceMethods.get(index) + '($$);')
                        changed = true
                    }
                }
            }
        })

        return changed
    }
}

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

package com.wlqq.phantom.gradle.debugger

import com.wlqq.phantom.gradle.Constant
import com.wlqq.phantom.gradle.PhantomPluginConfig
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * Modified from https://github.com/Qihoo360/RePlugin/blob/dev/replugin-plugin-gradle/src/main/groovy/com/qihoo360/replugin/gradle/plugin/debugger/PluginDebugger.groovy
 */
class PhantomDebugger {

    Project project
    PhantomPluginConfig config
    def variant
    File apkFile
    File adbFile

    PhantomDebugger(Project project, PhantomPluginConfig config, def variant) {
        this.project = project
        this.config = config
        this.variant = variant
    }

    void init() {
        def variantData = variant.variantData
        def scope = variantData.scope
        def globalScope = scope.globalScope

        apkFile = variant.outputs.first().outputFile
        adbFile = globalScope.androidBuilder.sdkInfo.adb
    }

    /**
     * 安装插件
     * @return 是否命令执行成功
     */
    boolean install() {
        //推送apk文件到手机
        String pushCmd = "${adbFile.absolutePath} push ${apkFile.absolutePath} ${config.phoneStorageDir}"
        if (0 != executeCommand(pushCmd)) {
            return false
        }

        //此处是在安卓机上的目录，直接"/"路径
        String apkPath = "${config.phoneStorageDir}"
        if (!apkPath.endsWith("/")) {
            //容错处理
            apkPath += "/"
        }
        apkPath += "${apkFile.name}"

        //发送安装广播
        String cmd = "${adbFile.absolutePath} shell am broadcast -a ${config.hostApplicationId}.phantom.debug.action.INSTALL_PLUGIN -e path ${apkPath} -e package_name ${config.pluginApplicationId} -e version_name ${config.pluginVersionName}"
        if (0 != executeCommand(cmd)) {
            return false
        }

        return true
    }

    /**
     * 卸载插件
     * @return 是否命令执行成功
     */
    boolean uninstall() {
        String cmd = "${adbFile.absolutePath} shell am broadcast -a ${config.hostApplicationId}.phantom.debug.action.UNINSTALL_PLUGIN -e package_name ${config.pluginApplicationId}"
        if (0 != executeCommand(cmd)) {
            return false
        }
        return true
    }

    /**
     * 强制停止宿主app
     * @return 是否命令执行成功
     */
    boolean forceStopHostApp() {
        String cmd = "${adbFile.absolutePath} shell am force-stop ${config.hostApplicationId}"
        if (0 != executeCommand(cmd)) {
            return false
        }
        return true
    }

    /**
     * 启动宿主app
     *
     * @return 是否命令执行成功
     */
    boolean startHostApp() {
        String cmd = "${adbFile.absolutePath} shell am start -n \"${config.hostApplicationId}/${config.hostAppLauncherActivity}\" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER"
        if (0 != executeCommand(cmd)) {
            return false
        }
        return true
    }

    /**
     * 检查宿主是否正在运行，若没有运行则抛出异常
     *
     * @throws GradleException 当宿主没有运行
     */
    void checkHostRunning() throws GradleException {
        if (!hostRunning) {
            throw new GradleException(
                    "Please launch host application '${config.hostApplicationId}' before running this task")
        }
    }

    /**
     * 判断宿主是否正在运行
     *
     * @return 判断是否正在运行
     */
    private boolean isHostRunning() {
        String command = "${adbFile.absolutePath} shell ps | grep ${config.hostApplicationId}"

        def process = command.execute()
        def outputStream = new StringBuffer()
        process.waitForProcessOutput(outputStream, System.err)

        boolean isHostRunning = false

        outputStream.eachLine { line ->
            println("exec $command output: $line")
            if (line.endsWith(config.hostApplicationId)) {
                isHostRunning = true
            }
        }

        return isHostRunning
    }

    /**
     * 检查用户配置项是否合法，若不合法则抛出异常
     *
     * @throws GradleException
     */
    void checkConfig() throws GradleException {
        if (isConfigNull()) {
            throw new GradleException("invalid phantomDebuggerConfig, please check README.md for details")
        }
    }

    /**
     * 用户配置项是否为空
     *
     * @param config
     * @return true 如果用户配置项为空
     */
    private boolean isConfigNull() {

        //检查adb环境
        if (null == adbFile || !adbFile.exists()) {
            System.err.println "${Constant.PLUGIN_TAG} Could not find the adb file !!!"
            return true
        }

        if (null == config) {
            System.err.println "${Constant.PLUGIN_TAG} the config object can not be null!!!"
            System.err.println "${Constant.CONFIG_EXAMPLE}"
            return true
        }

        if (null == config.hostApplicationId) {
            System.err.println "${Constant.PLUGIN_TAG} the config hostApplicationId can not be null!!!"
            System.err.println "${Constant.CONFIG_EXAMPLE}"
            return true
        }

        if (null == config.hostAppLauncherActivity) {
            System.err.println "${Constant.PLUGIN_TAG} the config hostAppLauncherActivity can not be null!!!"
            System.err.println "${Constant.CONFIG_EXAMPLE}"
            return true
        }

        if (null == config.pluginApplicationId) {
            System.err.println "${Constant.PLUGIN_TAG} the config pluginApplicationId can not be null!!!"
            System.err.println "${Constant.CONFIG_EXAMPLE}"
            return true
        }

        if (null == config.pluginVersionName) {
            System.err.println "${Constant.PLUGIN_TAG} the config pluginVersionName can not be null!!!"
            System.err.println "${Constant.CONFIG_EXAMPLE}"
            return true
        }

        return false
    }

    private static int executeCommand(String command) {
        def process = command.execute()
        def outputStream = new StringBuffer()
        process.waitForProcessOutput(outputStream, System.err)

        outputStream.eachLine {
            println("exec $command output: $it")
        }

        return process.exitValue()
    }
}

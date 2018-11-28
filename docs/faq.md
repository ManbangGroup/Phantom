
[环境类问题](#1) 

&nbsp;&nbsp;&nbsp;[ 1、项目初始构建出现 Could not get resource 'https:xxxxx'](#1.1) 


[配置类问题](#2) 

&nbsp;&nbsp;&nbsp;[ 1、使用系统组件加载异常](#2.1)  

&nbsp;&nbsp;&nbsp;[ 2、manifest allowBackup问题](#2.2) 

&nbsp;&nbsp;&nbsp;[ 3、运行启动插件异常——Excution failed for task ':app:phInstallPluginDebug'](#2.3) 

[代码实现问题](#3) 

------------
<h2 id='1'> 环境类问题</h2>

<h3 id='1.1'>1、项目初始构建出现 Could not get resource 'https:xxxxx'</h3>

**问题描述：** https访问地址受限，导致工程无法运行。

**原因：** 工程配置所需的lib在https上可能没有个，需要重配置工程。

**举例：** 

> 暂无


**解决方法：** 

> 参考：https://blog.csdn.net/shenjinalin123/article/details/84235187

------------
<h2 id='2'> 配置类问题</h2>

<h3 id='2.1'>1、使用系统组件加载异常</h3>

**问题描述：** 在插件中使用部分系统组件加载不出来，达不到预期效果。

**原因：** 可能使用到的系统组件未在宿主中进行权限声明。

**举例：** 如下场景

> *在插件中调用相机，扫描二维码，surfaceView无任何图像展示。*


**解决方法：** 权限的声明是以宿主为主的，因为在检测应用权限的时候，标识是以宿主权限为参考。所以上述场景中，使用相机时，如果未在宿主中进行权限声明，那么在插件中，仍然无法使用相应的系统组件。所以，此类问题的解决方法，只需要再宿主中申请相应的权限即可。

<h3 id='2.2'>2、manifest allowBackup问题</h3>

**问题描述：**  如图：

![backallow错误](https://github.com/JianLin-Shen/common_doc/blob/master/back_alow.png)

**原因：** 插件运行需要配置是否支持后台响应

**举例：** 如下场景

> 无


**解决方法：** 

在manifest中配置加入 tools:replace="anroid:allowBackup"

<h3 id='2.3'>3、运行启动插件异常——Excution failed for task ':app:phInstallPluginDebug'</h3>

**问题描述：**  如图：

![backallow错误](https://github.com/JianLin-Shen/common_doc/blob/master/install_error.png)

**原因：** 要从As直接启动或调试目标插件，插件是运行在宿主的，所以在启动插件的时候，必须保证宿主处于启动状态，否则启动任务无法完成插件安装和启动操作。

**举例：** 

> 无


**解决方法：** 

启动调试插件之前，先启动宿主。保证宿主在前台。

------------
<h2 id='3'> 代码实现问题</h2>


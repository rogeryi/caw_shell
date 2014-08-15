# Debugging of Chrome Android WebView

Tags: Chrome Android WebView Debugging

作者: 易旭昕 ([@roger2yi][1])

----

本文主要描述如何将 Chrome Android WebView （下文简称CAW）的代码从 AOSP 中抽离出来，编译成独立的应用，方便对 CAW 的 Java/C++ 代码进行跟踪调试。

相关的代码位于 GitHub [Chrome Android WebView TestShell][7] （下文简称 CAW Shell） 项目上，读者可以下载代码，只要在 Eclipse 工程里面创建一个 Android Project，就可以对 Java 部分的代码进行调试，并且仓库里面也包含了一个编译好的 APK 安装包，可以直接安装试用。如果要调试 C++ 代码，读者还需要下载 AOSP Android 4.4.3 的代码，在 ROM 编译环境下编译出所需的 .so 库。

为了容易理解本文的内容，建议读者阅读官方文档 [Organization of code for Android WebView][2] 先对 CAW 的代码结构有一个大致的了解。

## 独立应用

将 CAW 代码抽离出来编译成独立应用的做法，参考了[放飞梦想][4]的 [ChromeView][5] 项目，不过因为两者的目的不一样，CAW Shell 项目的目的只是为了方便调试 CAW 的代码，所以一些具体的做法并不一样：

1. 首先 CAW Shell 代码的来源是 AOSP Android Source 而不是 Chromium Source；
2. 编译 .so 库是在 ROM 编译环境下进行，所以在 C++ 部分会使用非公开的 Native API；

所以，CAW Shell 理论上只能在 Android 4.4.3/4.4.4 上运行。

总的说来，CAW Shell 所需的操作步骤包括：

1. 从 AOSP 下载 Android 4.4.3/4.4.4 源码，并先编译出 ROM；
2. 将所有 Java 代码从 AOSP 里面拷贝到自己的工程，包括属于 Android Source 和属于 Chromium Source 的部分，还包括一些预编译过程自动生成的 Java 代码；
3. 修改 Java 代码，主要修改包名避免跟 SDK 冲突和解决 Hidden API 调用的问题，解决的方法包括：
  1. 拷贝使用到的内部类到自己的工程；
  2. 使用反射调用隐藏 API；
  3. 一些涉及内部资源使用的代码，大部分都直接注释掉；
4. 修改相关 .mk 工程文件里面库的名字，修改 JNI 调用相关文件里面的 Java 类路径，重新编译得到新的 .so 库，避免跟系统库冲突；
5. 将新的 .so 库拷贝到自己的工程，修改 Java 代码加载新的 .so 库，加上一个简单的 TestShell 代码，然后打包生成独立应用 APK;

## 调试代码

调试 Java 代码比较简单，使用 CAW Shell 的代码在 Eclipse 里面创建一个 Android Project 即可，如果要调试 C++ 的代码，则需要读者自行编译出 .so 库。

<div style="text-align:center; padding:20px 0px"><img src="http://img.blog.csdn.net/20140815170708781"></img></div>
> CAW Native Debugging

**1. 下载 AOSP Android 4.4.3 的代码，并按照官方文档先编译出 ROM 镜像**

**2. 重新编译出 libwebviewuc_plat_support.so**

- 修改 /frameworks/webview/chromium 目录下的 Android.mk 文件里面库的名字：

```
LOCAL_MODULE:= libwebviewuc_plat_support
```

- 修改 /frameworks/webview/chromium/plat_support 目录下代码里面 JNI 调用涉及的 Java 类路径，已经修改好的代码位于 CAW Shell 的 /aosp/plat_support 目录下，直接覆盖同名文件即可。

- 在 /frameworks/webview/chromium 目录下运行：

```
mm -j8
```

编译出新的 libwebviewuc_plat_support.so 库。

**3. 重新编译出 libwebviewuc.so**

- 修改 /external/chromium_org/android_webview 目录下的 libwebviewchromium.target.linux-arm.mk 文件里面库的名字：

```
LOCAL_MODULE:= libwebviewuc
```
- 修改 /out/target/product/mako/obj/GYP/shared_intermediates 目录下 XXX_JNI.h 代码里面 JNI 调用涉及的 Java 类路径，已经修改好的代码位于 CAW Shell 的 /aosp/jni 目录下，直接覆盖同名文件即可。其中 mako 子目录会根据读者编译 ROM 的目标设备不同而不同。

- 在 /external/chromium_org 目录下运行：

```
mm -j8
```

编译出新的 libwebviewuc.so 库。

**4. 将 libwebviewuc_plat_support.so 和 libwebviewuc.so 导入自己的 Android Project**

- 将不带符号的库拷贝到自己工程的 libs/armeabi 目录下；
- 将带符号的库拷贝到自己工程的 obj/local/armeabi 目录下；
- 为 Android Projct 增加 Native 支持，**并在 Builder 属性页面里面取消掉 CDT Builder 选项**，避免 Eclipse 去调用 ndk-build 编译，因为我们是使用事先编译好的 .so 库。
- 在 Eclipse 里面创建一个 C++ Project，导入 CAW 的 C++ 代码，根据自己的需要打断点；
- **使用 Debug As > Android Native Application 进行 C++ 代码调试**；

[1]: http://weibo.com/roger2yi
[2]: https://docs.google.com/document/d/1a_cUP1dGIlRQFUSic8bhAOxfclj4Xzw-yRDljVk1wB0/edit#
[3]: http://mogoweb.github.io/blog/2014/01/16/analysis-of-android-4-4-webview-implementation
[4]: http://mogoweb.github.io/
[5]: https://github.com/mogoweb/chromium_webview
[6]: http://mogoweb.github.io/blog/2014/06/10/about-chromium-webview-project/
[7]: https://github.com/rogeryi/caw_shell



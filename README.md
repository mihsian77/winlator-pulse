<p align="center">
  <img src="https://raw.githubusercontent.com/brunodev85/winlator/main/logo.png" width="376" height="128" alt="Winlator Pulse" />
</p>

<p align="center">
  <a href="https://github.com/mihsian77/winlator-pulse/releases">
    <img src="https://img.shields.io/github/downloads/mihsian77/winlator-pulse/total" alt="Downloads" />
  </a>
  <a href="https://github.com/mihsian77/winlator-pulse/releases">
    <img src="https://img.shields.io/github/v/release/mihsian77/winlator-pulse" alt="Release" />
  </a>
  <a href="https://github.com/mihsian77/winlator-pulse/stargazers">
    <img src="https://img.shields.io/github/stars/mihsian77/winlator-pulse" alt="Stars" />
  </a>
  <img src="https://img.shields.io/github/license/mihsian77/winlator-pulse" alt="License" />
</p>

# Winlator Pulse

Winlator 的个人修改版，主要改了多版本共存和一些界面细节。直接跟进 brunodev85 官方上游。

## 下载

[Releases](https://github.com/mihsian77/winlator-pulse/releases) 有两个包，功能一样，包名不同：

| 包 | 包名 | 说明 |
|----|------|------|
| standard | `com.winlator` | 和原版包名一样，覆盖安装 |
| coexist | `com.winlator.pulse` | 共存版，可以和原版及其他共存版一起装 |

## 改了什么

### 多版本共存

原版包名写死，一台手机只能装一个。这里把包名改成编译参数，同时修了改包名后 native 层路径硬编码导致的黑屏/进不去容器的问题：

- native 缓存目录从 Java 层通过环境变量传入，不写死
- rootfs 内硬编码路径用等长字节替换 + 软链别名指向实际包名
- FileProvider authority 随包名自动变
- MT 管理器改包后也能用

### XRandR 刷新率

实现了 XRandR X11 扩展，Wine 和游戏可以通过标准接口读到显示器刷新率。容器设置里可以选固定档位或跟随系统。

### 后台安装组件

组件安装不再卡界面，通知栏显示进度，可以取消，装完自动刷新列表。

### 组件列表和下载进度

组件列表改成卡片式，能看到类型、大小、是否已安装。下载进度对话框加了速度、大小和剩余时间。

### 屏幕参数自动识别

分辨率下拉里加了"系统分辨率"，自动填设备实际宽高。刷新率下拉可以选"系统最高刷新率"或固定值。

### 其他

- Box64 preset 加了骁龙 8 至尊的选项
- native 库链接时设了 16KB 页大小，兼容新设备
- 界面全面中文化

## 版本号

用自己的版本号，不跟上上游的 11.2。语义化：第三位修 bug，第二位加功能，第一位大改。

## 自己编译

```bash
git clone https://github.com/mihsian77/winlator-pulse.git
cd winlator-pulse/app

# 共存版（默认 com.winlator.pulse）
./gradlew assembleCoexistRelease

# 指定共存包名
./gradlew assembleCoexistRelease -PcoexistAppId=com.your.name

# 标准版
./gradlew assembleStandardRelease
```

需要 JDK 17、Android SDK 35、NDK 24.0.8215888、CMake 3.22.1。

## 致谢

- [brunodev85/winlator](https://github.com/brunodev85/winlator) — 原版
- [hostei33/winlator-cn](https://github.com/hostei33/winlator-cn) — 早期汉化参考
- Wine、Box86/Box64、Mesa、DXVK、VKD3D — 底层依赖
- [marcomorosi06/DroidWine](https://github.com/marcomorosi06/DroidWine) — 16KB 页大小参考

## License

LGPL-2.1

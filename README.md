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

基于 Winlator 的独立增强分支，专注多版本共存、UI/UX 改进和中文用户体验。直接跟进官方上游，不依赖第三方汉化分支。

## 下载

[Releases](https://github.com/mihsian77/winlator-pulse/releases) 提供两个变体，功能一致仅包名不同：

| 变体 | 包名 | 说明 |
|------|------|------|
| `standard` | `com.winlator` | 与原版包名一致，覆盖安装 |
| `coexist` | `com.winlator.pulse` | 共存版，可与原版及其他共存版同时安装 |

## Pulse 特性

### 多版本共存（核心）

原版包名固定，一台设备只能装一个。Pulse 把包名变为编译参数，同时彻底解决改包名后的路径硬编码问题：

- native 缓存目录通过环境变量从 Java 层传入，不写死
- rootfs 内硬编码路径通过等长字节替换 + 软链别名指向实际包名
- FileProvider authority 随包名自动变化
- MT 管理器改包后同样可用，共存包名建议控制在 20 字符以内

### XRandR 刷新率

实现 XRandR X11 扩展，Wine 和游戏可通过标准接口查询显示器刷新率。容器设置中可选 60/90/120/144Hz，设置后容器内程序能获取到对应刷新率，不再是摆设。

### 后台安装服务

组件安装不再阻塞 UI。安装时通知栏实时显示进度，可随时取消，安装完成自动刷新列表。安装过程中可自由切换窗口、操作其他功能。

### 组件管理界面升级

卡片式布局，按组件类型显示主题色图标、类型描述、文件大小和"已安装"状态标签，比原版纯文本列表更直观。

### 下载进度对话框现代化

卡片式布局 + 水平进度条，实时显示下载速度、已下载大小和剩余时间，不再只有一个转圈百分比。

### 骁龙 8 至尊调优

Box64 preset 新增 `SNAPDRAGON_8_ELITE`，FORWARD 提升至 1024，针对 Oryon 大核架构。容器设置 → 高级 → Box64 Preset 可选。

### 屏幕参数自动识别

- **分辨率**：屏幕尺寸下拉栏新增「系统分辨率」，自动写入设备实际宽高，避免拉伸或黑边
- **刷新率**：新增刷新率下拉栏，可选「系统最高刷新率」或固定档位

### 16KB 页大小

native 库链接时设置 `max-page-size=16384`，兼容骁龙 8 至尊等 16KB 页设备。

### 中文界面

全面中文化，包括设置项、对话框、错误提示和开始菜单，面向中文用户优化。

## 版本号

Pulse 使用独立的语义化版本号，不跟随上游的 11.2 体系：

- `1.0.x`：修复更新
- `1.x.0`：功能更新
- `x.0.0`：大版本更新

当前版本：`1.0.0`

## 构建

```bash
git clone https://github.com/mihsian77/winlator-pulse.git
cd winlator-pulse/app

# 共存版（默认包名 com.winlator.pulse）
./gradlew assembleCoexistRelease

# 指定共存包名
./gradlew assembleCoexistRelease -PcoexistAppId=com.your.name

# 标准版
./gradlew assembleStandardRelease
```

环境要求：JDK 17 · Android SDK 35 · NDK 24.0.8215888 · CMake 3.22.1

## 致谢

| 项目 | 说明 |
|------|------|
| [brunodev85/winlator](https://github.com/brunodev85/winlator) | 原始项目，Pulse 直接跟进其官方上游 |
| [hostei33/winlator-cn](https://github.com/hostei33/winlator-cn) | 早期中文汉化参考 |
| Wine · Box86/Box64 | 兼容层与转译 |
| Mesa (Turnip/VirGL) · DXVK · VKD3D | 图形渲染 |
| [marcomorosi06/DroidWine](https://github.com/marcomorosi06/DroidWine) | 16KB 页大小修复参考 |

## License

LGPL-2.1

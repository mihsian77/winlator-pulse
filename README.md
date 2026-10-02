<p align="center">
  <img src="https://raw.githubusercontent.com/brunodev85/winlator/main/logo.png" width="376" height="128" alt="Winlator Pulse" />
</p>

<p align="center">
  <a href="https://github.com/winlator-123/winlator-pulse/releases">
    <img src="https://img.shields.io/github/downloads/winlator-123/winlator-pulse/total" alt="Downloads" />
  </a>
  <a href="https://github.com/winlator-123/winlator-pulse/releases">
    <img src="https://img.shields.io/github/v/release/winlator-123/winlator-pulse" alt="Release" />
  </a>
  <a href="https://github.com/winlator-123/winlator-pulse/stargazers">
    <img src="https://img.shields.io/github/stars/winlator-123/winlator-pulse" alt="Stars" />
  </a>
  <img src="https://img.shields.io/github/license/winlator-123/winlator-pulse" alt="License" />
</p>

# Winlator Pulse

Winlator 11.2 中文共存分支。在原版基础上实现多版本共存，增加骁龙专属调优和屏幕参数自动识别。

## 下载

[Releases](https://github.com/winlator-123/winlator-pulse/releases) 提供两个变体，功能一致仅包名不同：

| 变体 | 包名 | 说明 |
|------|------|------|
| `standard` | `com.winlator` | 与原版包名一致，覆盖安装 |
| `coexist` | `com.winlator.pulse` | 共存版，可与原版及其他共存版同时安装 |

## 特性

### 多版本共存

原版包名固定 `com.winlator`，一台设备只能装一个。本分支把包名变为编译参数，同时解决了改包名后的路径硬编码：

- native 缓存目录通过 `APP_CACHE_DIR` 环境变量从 Java 层传入，不写死
- rootfs 内 445 处硬编码路径通过等长字节替换 + 软链别名指向实际包名
- FileProvider authority 随 `${applicationId}` 自动变化

共存包名建议控制在 20 字符以内。MT 管理器改包后同样可用。

### 骁龙 8 至尊调优

Box64 preset 新增 `SNAPDRAGON_8_ELITE`，FORWARD 提升至 1024，针对 Oryon 大核架构。容器设置 → 高级 → Box64 Preset 可选。

### 屏幕参数自动识别

- **分辨率**：屏幕尺寸下拉栏新增「系统分辨率」，自动写入设备实际宽高，避免拉伸或黑边
- **刷新率**：新增刷新率下拉栏，可选「系统最高刷新率」或固定 60/90/120/144Hz，通过 Surface.setFrameRate 控制 Android 端合成（需 Android 12+）

### 16KB 页大小

native 库链接时设置 `max-page-size=16384`，兼容骁龙 8 至尊等 16KB 页设备。

## 构建

```bash
git clone https://github.com/winlator-123/winlator-pulse.git
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
| [brunodev85/winlator](https://github.com/brunodev85/winlator) | 原始项目 |
| [hostei33/winlator-cn](https://github.com/hostei33/winlator-cn) | 中文汉化 |
| Wine · Box86/Box64 | 兼容层与转译 |
| Mesa (Turnip/VirGL) · DXVK · VKD3D | 图形渲染 |
| [marcomorosi06/DroidWine](https://github.com/marcomorosi06/DroidWine) | 16KB 页大小修复参考 |

## License

GPL-3.0

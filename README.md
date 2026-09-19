<div align="center">

<img src="logo.png" width="376" height="128" alt="Winlator Pulse" />

# Winlator Pulse

在 Android 上运行 Windows 程序的容器，聚焦**多版本共存**与**性能监控**。

[![Build](https://github.com/hao728/winlator-pulse/actions/workflows/build-coexist.yml/badge.svg)](https://github.com/hao728/winlator-pulse/actions/workflows/build-coexist.yml)
[![Platform](https://img.shields.io/badge/Android-arm64--v8a-brightgreen)]()
[![License](https://img.shields.io/badge/License-GPL--3.0-blue)]()

[下载 APK](../../releases) · [上游](https://github.com/hostei33/winlator-cn) · [RootFS 构建](https://github.com/hao728/bfm-zh)

</div>

---

## 它解决什么问题

原版 Winlator 只能装一个，换版本要卸载。这个分支把包名变成编译参数——想装几个版本就编几个，数据各自隔离。

性能监控方面，原版 HUD 只有数字。这里加了帧时间柱状图和电池温度，按快慢染色，跑游戏时一眼看出掉帧在哪。

## 共存怎么工作

```bash
./gradlew assembleCoexistRelease -PcoexistAppId=com.your.name
```

| 编译变体 | 包名 | 说明 |
|---------|------|------|
| `standard` | `com.winlator` | 和原版完全一致 |
| `coexist` | 任意 | `-PcoexistAppId=` 指定，数据目录自动隔离 |

包名遵循 Android 规范：每个点分段 ≤ 63 字符，总长 ≤ 256 字符。`FileProvider` 权限标识跟随 `${applicationId}`，数据目录走 `getExternalFilesDir()`，rootfs、容器、配置全部隔离。

## HUD

| 模式 | 显示内容 |
|------|---------|
| 关闭 | 无 |
| 简单 | FPS + 帧时间柱 |
| 完整 | FPS + 帧时间柱 + GPU + 内存 + CPU 频率 + 电池温度 |

帧率染色：≥50 绿 · 30–50 黄 · <30 红。帧时间柱同理，16.7ms / 33.3ms 两条参考线。

## 自动同步

每天 UTC 18:00 拉取上游 main，合并到 `auto-sync` 分支开 PR。以下文件合并时强制保留本仓库版本：

- `app/build.gradle`（共存变体）
- `app/src/main/AndroidManifest.xml`（权限标识）
- `app/src/main/java/com/winlator/widget/FrameRating.java`（HUD）
- `README.md`

## 致谢

- 原始项目：[brunodev85/winlator](https://github.com/brunodev85/winlator)
- 上游中文维护：[hostei33/winlator-cn](https://github.com/hostei33/winlator-cn)
- Wine · Box86/Box64 · Mesa · DXVK · VKD3D

## License

GPL-3.0

<p align="center">
	<img src="logo.png" width="376" height="128" alt="Winlator Logo" />
</p>

# Winlator Pulse

Winlator 的二次维护分支，聚焦两件事：**多版本共存**和**可观测性升级**。

## 这个仓库解决什么问题

原版 Winlator 只能装一个，换版本就得卸载。本仓库通过 `productFlavors` 把包名变成编译期参数——你想装几个版本就编几个，包名任意合法字符串，数据目录自动隔离，互不干扰。

性能监控方面，原版 HUD 只有数字。这里加了帧时间柱状图和电池温度，配合颜色编码，跑游戏时不用猜卡在哪。

## 共存机制

```bash
./gradlew assembleCoexistRelease -PcoexistAppId=com.your.name
```

- `standard` flavor：`com.winlator`，和原版完全一致
- `coexist` flavor：读 `-PcoexistAppId`，默认 `com.winlator.coexist`

包名遵循 Android 规范——每个点分段不超过 63 字符，总长不超过 256 字符。`FileProvider` 和 `MTDataFilesProvider` 的 authority 跟着 `${applicationId}` 走，不会冲突。`getExternalFilesDir()` 自动指向新包名的目录，rootfs、容器、配置全隔离。

## HUD

| 模式 | 显示 |
|------|------|
| 关闭 | 无 |
| 简单 | FPS + 帧时间柱 |
| 完整 | FPS + 帧时间柱 + GPU + RAM + CPU 频率 + 电池温度 |

帧率染色：≥50 绿，30–50 黄，<30 红。帧时间柱同理，16.7ms / 33.3ms 两条参考线。

## 自动同步

每天 UTC 18:00 拉取 [hostei33/winlator-cn](https://github.com/hostei33/winlator-cn) 的 main，合并到 `auto-sync` 分支开 PR。`build.gradle`、`AndroidManifest.xml`、`FrameRating.java`、`README.md` 这四个文件在合并时强制保留本仓库版本，其余走三方合并。

## 相关项目

- RootFS / Box64 构建：[hao728/bfm-zh](https://github.com/hao728/bfm-zh)
- 上游：[hostei33/winlator-cn](https://github.com/hostei33/winlator-cn)
- 原始项目：[brunodev85/winlator](https://github.com/brunodev85/winlator)

## License

GPL-3.0

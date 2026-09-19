<p align="center">
	<img src="logo.png" width="376" height="128" alt="Winlator Logo" />
</p>

# Winlator CN Coexist

> 基于 [hostei33/winlator-cn](https://github.com/hostei33/winlator-cn) 的二次开发版，专注**多版本共存**与**性能监控升级**。

## 核心特性

- **多版本共存**：支持编译时一键指定任意包名，与原版/其他共存版同时安装互不干扰
- **升级版性能 HUD**：在原版 FPS/GPU/RAM/CPU 基础上，新增帧时间柱状图、电池温度、帧率颜色编码
- **自动跟随上游**：每日自动同步 hostei33/winlator-cn 最新提交，PR 模式验证后合并

## 下载

前往 [Releases](../../releases) 下载最新 APK。

## 共存版编译说明

本仓库支持通过 Gradle 属性编译不同包名的共存版本：

```bash
# 编译共存版（包名 com.xxx.xxx）
./gradlew assembleCoexistRelease -PcoexistAppId=com.xxx.xxx
```

- `main` flavor：默认包名 `com.winlator`
- `coexist` flavor：通过 `-PcoexistAppId=` 指定新包名，数据目录自动隔离

## HUD 说明

| 模式 | 显示内容 |
|------|---------|
| SIMPLE | FPS + 帧时间柱状图 |
| FULL | FPS + 帧时间柱状图 + GPU + RAM + CPU + 电池温度 |

帧率颜色：**绿色** ≥50 FPS，**黄色** 30-50 FPS，**红色** <30 FPS

帧时间柱状图：绿色 <16.7ms（60fps），黄色 16.7-33.3ms（30-60fps），红色 >33.3ms（<30fps）

## 致谢与第三方项目

- 原项目 [brunodev85/winlator](https://github.com/brunodev85/winlator)
- GLIBC Patches by [Termux Pacman](https://github.com/termux-pacman/glibc-packages)
- Wine ([winehq.org](https://www.winehq.org/))
- Box86/Box64 by [ptitSeb](https://github.com/ptitSeb)
- Mesa (Turnip/Zink/VirGL) ([mesa3d.org](https://www.mesa3d.org))
- DXVK ([github.com/doitsujin/dxvk](https://github.com/doitsujin/dxvk))
- VKD3D ([gitlab.winehq.org/wine/vkd3d](https://gitlab.winehq.org/wine/vkd3d))
- CNC DDraw ([github.com/FunkyFr3sh/cnc-ddraw](https://github.com/FunkyFr3sh/cnc-ddraw))
- RootFS & WFM by [Waim908](https://github.com/Waim908)
- HUD 设计参考 [Xnick417x/WinNative](https://github.com/Xnick417x/WinNative)

特别感谢所有参与这些项目的开发者。<br>
感谢所有信任并支持本项目的人们。

## License

GPL v3

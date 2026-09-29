<div align="center">

# Winlator Pulse

Android 上运行 Windows 程序的容器 · 多版本共存分支

[下载](../../releases) · [上游汉化](https://github.com/hostei33/winlator-cn) · [原版](https://github.com/brunodev85/winlator)

</div>

---

## 关于这个分支

基于 hostei33/winlator-cn（汉化版，底层原版 Winlator 11.2），核心改进是**多版本共存**。

原版 Winlator 包名固定为 `com.winlator`，同一台设备只能装一个。这个分支把包名变成编译参数，可以同时安装多个版本，数据目录各自隔离，互不影响。

## 共存技术

### 编译时指定包名

```bash
./gradlew assembleCoexistRelease -PcoexistAppId=com.example.winlator
```

| 变体 | 包名 | 用途 |
|------|------|------|
| `standard` | `com.winlator` | 覆盖原版安装 |
| `coexist` | 编译参数指定，默认 `com.winlator.pulse` | 与原版共存 |

包名遵循 Android 规范：每段 ≤ 63 字符，总长 ≤ 256 字符。MT 管理器等工具改包后同样可用。

### 运行时路径隔离

共存不只是改个包名。以下路径全部随实际包名动态变化：

- **数据目录**：`/data/data/<实际包名>/`，rootfs、容器、配置各自独立
- **FileProvider**：权限标识跟随 `${applicationId}`，改包后打开文件不崩溃
- **native 缓存目录**：通过 `APP_CACHE_DIR` 环境变量从 Java 层传入，Vulkan API 版本缓存、纹理缓存不再写死 `com.winlator` 路径
- **rootfs 路径替换**：解压产物中硬编码的 `/data/data/com.winlator/files/rootfs` 通过等长字节替换 + 软链别名指向实际路径，覆盖 rootfs 内 445 处命中文件

### 已知限制

- 包名超过 23 字符时（`/data/data/` 短写法），rootfs 等长替换空间不足，建议控制在 20 字符以内
- 组件（box64/wine/dxvk）安装后路径自动跟随包名，无需额外操作

## 构建

```bash
git clone https://github.com/mihsian77/winlator-pulse.git
cd winlator-pulse/app
./gradlew assembleCoexistRelease
```

环境要求：JDK 17 · Android SDK 35 · NDK 24.0.8215888 · CMake 3.22.1 · arm64-v8a

## 致谢

| 项目 | 说明 |
|------|------|
| [brunodev85/winlator](https://github.com/brunodev85/winlator) | 原始项目 |
| [hostei33/winlator-cn](https://github.com/hostei33/winlator-cn) | 中文汉化维护 |
| Wine · Box86/Box64 | 兼容层与转译 |
| Mesa (Turnip/VirGL) · DXVK · VKD3D | 图形渲染 |

## License

GPL-3.0

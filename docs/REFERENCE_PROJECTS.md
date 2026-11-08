# 参考项目与来源清单

这份文件的用途只有一个：不靠记忆。后面要动构建、动组件、动驱动之前，先看这里，别再从零想一遍。
最后更新：2026-11-08

---

## 一、我们自己的三个仓库

| 仓库 | 职责 | 关键文件 |
|---|---|---|
| `mihsian77/winlator-pulse` | 主项目，Winlator 的 Glibc fork，主打多版本容器共存 | `ContentsFragment.java`（组件安装）、`GeneralComponents.java`（组件解析）、`GraphicsDrivers.java` |
| `mihsian77/winlator-components` | 组件分发，所有可下载组件的 release + index | `sync_components.py`、`.github/workflows/sync-*.yml` |
| `mihsian77/winlator-driver-builder` | Turnip 驱动的 CI 编译与发布 | `build_turnip.sh`、`build.yml`、`mesa-watcher.yml` |

分支约定：主分支 `main`，本地开发在 `sync-merge` 上叠，最后推 `HEAD:main`。

---

## 二、上游与组件来源

| 用途 | 来源 | 备注 |
|---|---|---|
| Pulse 的代码基底 | `hostei33/winlator-cn` | 已加为 `upstream` remote |
| Winlator 官方 | `brunodev85/winlator` | 判断"原版行为"时的对照基准 |
| Turnip 驱动源码 | `brunodev85/mesa3d-custom` | 每个版本一个 `turnip-*` 子目录，脚本取名字最大的那个 |
| 每夜版组件 | `nicholasx417/WinNative-Components` | `.wcp` 转 `.tzst`，同步进 `dxvk-nightly` / `vkd3d-nightly` |
| hostei33 组件 CDN | `cdn4.52emu.cn/wlt/v10/installable_components/<分类>/index.txt` | 只覆盖部分分类，`virgl` / `soundfont` / `wine` / `proton` / `adrenotools` 都是 404 |
| 第三方组件目录 | `The412Banner/winlator-contents` | 有 `components.json`、`linux-protons.json`、`steamlite.json` 等，**wine/proton 可能的现成源，还没研究** |

---

## 三、参考项目池

按"能不能直接搬东西过来"排序，不是按星数。

| 项目 | 定位 | 可移植的资产 | 结论 |
|---|---|---|---|
| `utkarshdalal/GameNative` | Native PC gaming，Steam / Epic / GOG / Amazon 集成 | 商店登录链路、云存档、社区配置库 | 优先级最高的功能参考 |
| `The412Banner/Bannerlator` | Winlator 分支，帧生成是招牌 | lsfg-vk 是深度 C++ 集成，拆不出独立 .so | 只能整体参考，没法直接搬 |
| `The412Banner/BannerHub` | Bannerlator 的 GameHub 改造版 | 组件目录组织方式 | 参考组件管理结构 |
| `The412Banner/winlator-contents` | 组件目录索引仓库 | 见上一节 | 有研究价值 |
| `StevenMXZ/Winlator-Ludashi` | 鲁大师分支，Bionic 路线 | **audio_plugin（PulseAudio ASERVER 模块）**、`android_sysvshm` 补丁 | 我们是 Glibc，整体不能搬，但 audio_plugin 能解决老游戏无声，仍是下一个优先项 |
| `alexvorxx/winlator` | Glibc mod + Box64/Box32 | Box32 路线、Mesa 编译方法 | 编译方法参考过，驱动源已确认不可用 |
| `ggt64818-sys/winlator` | Glibc，带 VKWIND | **VKWIND / VKWIND11（D3D9→Vulkan 翻译层）** | 候选移植组件 |
| `faux123/GameNative-Mali` | GameNative 的 Mali 分支 | Mali/高通向适配 | 只看 Mali 相关 |
| `adesyaefull/WinlatorMaliPlus` | Mali 增强版 | ZeroFG 帧生成 | Mali 设备才有意义 |
| `zenithblue-oss/panprobe` | PanVK 的信息与驱动测试工具 | PanVK 驱动验证方法 | 调研性质 |
| `ajay9634/winlator-ajay` | 老牌 Winlator 分支（原 `ajay9634/winlator` 已改名） | 多版本管理思路 | 备用参考 |
| `afeimod/winlator` | 多 DXVK 版本预打包在 assets | 早期组件的组织方式 | 我们已超越，只作历史参考 |
| `REF4IK/CronyX` | — | — | **已删除（404），不要再找** |

### 明确否掉的

- `lfdevs/mesa-for-android-container`：会主动切断 libdrm 和 xcb 链接，**不是可用的 Turnip 驱动来源**。构建思路参考过，产物不行。
- PanVK / Mali 驱动：依赖 adrenotools 框架，MVP 阶段不可行。
- 把 adrenotools 和官方 turnip 的 tzst 当成一回事：错的，两条路。

---

## 四、已知构建陷阱

这些是踩过的坑，别再踩第二遍。

**1. Ubuntu 的 `libc.so` 是 linker script**
里面写着 `AS_NEEDED(ld-linux-aarch64.so.1)`。链接时只要 `--no-as-needed` 生效，动态链接器本身就会被记进 NEEDED。运行时 dlopen 一个"依赖 PIE loader"的 .so，loader 直接拒绝，表现就是闪退。
→ 打包前用 `patchelf --remove-needed ld-linux-aarch64.so.1` 剔掉（已在 `build_turnip.sh` 里）。

**2. 依赖必须和官方对齐**
官方 turnip 的 NEEDED 里有 `libexpat` / `libxcb-sync` / `libxcb-randr`，没有 `libzstd` / `ld-linux`。
我们的构建 `-Dxmlconfig=disabled`、默认 `zstd` 开着，所以正好反过来。
这两条是否影响运行还没验证完，标为疑点。

**3. 试过不行的编译参数**
- `-Ddri3=enabled`：新版 Mesa 已移除该选项
- `-Dglx=xlib` + `-Dopengl=false`：组合矛盾
- 去掉 `libxshmfence-dev`：meson 配置期直接报 not found
- `-Dplatforms` 与 `-Dgallium-drivers=` 一起用时要注意 backend 依赖

**4. 构建环境**
CI 跑在 `ubuntu-24.04-arm` 原生 arm64 runner，输出落在 `usr/lib/aarch64-linux-gnu/`，是标准 glibc 产物，不是 Android NDK 的 bionic 产物。

---

## 五、组件分类现状

| 分类 | 在线源 | 项目内置 | 说明 |
|---|---|---|---|
| `dxvk` / `vkd3d` | ✅ | — | 主组件，含 gplasync / sarek / arm64ec 变体 |
| `dxvk-nightly` / `vkd3d-nightly` | ✅ | — | 每夜同步，只留最近几个 release |
| `box64` | ✅ | ✅ | 官方构建 |
| `turnip` | ✅ | ✅（`turnip-26.2.0`） | 只列官方版本，自编译的带 `-dev` 不进列表 |
| `wined3d` / `d7vk` / `d8vk` | ✅ | — | 小分类 |
| `virgl` | ❌ | ✅ `virgl-23.1.9` | 上游 404，内置够用 |
| `soundfont` | ✅（新加） | ✅ SONiVOX 精简 GM | 见下 |
| `wine` / `proton` | ❌ | ❌ | **本来就不走在线下载**，`isWineOrProton()` 走本地文件导入。没有 index 是设计，不是缺口 |
| `adrenotools` | ❌ | ❌ | 真空转：UI 入口在、内容没有、上游 404。已改成没驱动就隐藏入口 |

### SoundFont 取舍
- 内置 `SONiVOX-EAS-GM-Wavetable.sf2`：Android 系统自带那套精简 GM，体积小、preset 少，音色偏电子。
- 新加 `GeneralUser-GS.sf2`：完整 GS 库，preset 覆盖面大得多，代价是体积大很多，低内存设备加载会慢。
- 默认仍是内置的，新增的只作为可选项。

---

## 六、约定

- 自编译驱动一律带 `-dev` 后缀，公开 `index.txt` 用 `grep -v -- '-dev'` 过滤，只留官方包。
- 每夜版每个分类只保留最近几个，旧的 release asset 删掉，避免列表堆积。
- commit 身份固定 `mihsian77` + `209595350+mihsian77@users.noreply.github.com`。
- token 绝不写进代码、脚本、仓库文件。
- 组件列表的 index 必须"先落盘再 `git add`"，顺序反了提交进去的永远是上一版。

---

## 七、未决

- wine / proton 的在线源：`The412Banner/winlator-contents` 的 `linux-protons.json` 可能有用，未验证。
- virgl 与 adrenotools 的内容源：上游确实没有，要不要自建待定。
- 自编译驱动的 `libzstd` 与缺失的 xcb 库是否真的影响运行：等新版实测确认。

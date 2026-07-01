# ⚡ ChunkPregeneratorUI

> 为 [Chunky](https://github.com/pop4959/Chunky) 区块预生成器提供 **Win11 Fluent Design** 风格的可视化操作界面。

[![NeoForge](https://img.shields.io/badge/NeoForge-1.21.1-blue?logo=curseforge)](https://neoforged.net/)
[![Forge](https://img.shields.io/badge/Forge-1.20.1-orange)](https://files.minecraftforge.net/)
[![Fabric](https://img.shields.io/badge/Fabric-1.21.1-yellow)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-MIT-green)](./LICENSE)

---

## 🎯 核心理念

**不需要记指令！** Chunky 本身功能强大，但完全依赖命令行操作。本模组提供：

```
   用户点击 UI  →  构建Chunky命令  →  Chunky执行预生成  →  实时进度反馈
```

玩家只需打开界面、设置参数、点击"开始"——就像操作普通软件一样自然。

---

## 📊 Chunky 功能调研

基于对 [pop4959/Chunky](https://github.com/pop4959/Chunky) 源码（GPL-3.0）的完整分析：

| 功能模块 | Chunky实现 | 我们的UI封装 |
|----------|-----------|-------------|
| **命令系统** | 21个子命令（start/pause/continue/cancel...） | 按钮驱动的操作 |
| **形状系统** | 圆形/方形/矩形/椭圆/菱形/五边形/六边形/八边形/星形 | 下拉菜单选择 |
| **遍历模式** | concentric（同心）/ loop（螺旋） | 下拉菜单选择 |
| **并发控制** | Semaphore限流（默认50并发） | 无需配置，透明传递 |
| **进度追踪** | chunkCount / % / rate / ETA | 实时进度条 + 统计面板 |
| **事件系统** | GenerationProgressEvent / GenerationCompleteEvent | ChunkyBridge订阅转发 |
| **持久化** | TaskLoader (JSON) — 崩溃恢复 | 自动支持 |
| **世界集成** | WorldBorder / RegionCache | 「设为世界边界」一键按钮 |
| **公开API** | `ChunkyAPI` 编程接口 | `ChunkyBridge` 反射调用 |

### 协议兼容性
Chunky 使用 **GPL-3.0** 协议。本模组通过 **ChunkyAPI 反射调用**（独立程序通过API通信不构成衍生作品），使用 **MIT** 协议发布，两者完全兼容。

---

## 🏗️ 项目架构

```
ChunkPregeneratorUI/
├── common/                          # 共享核心（纯Java，无MC依赖）
│   ├── bridge/ChunkyBridge.java     #   桥接接口 — 连接UI与Chunky
│   ├── model/                       #   数据模型
│   │   ├── PregenerationConfig.java #     预生成配置（不可变record）
│   │   ├── ProgressSnapshot.java    #     进度快照
│   │   └── ShapeType.java           #     形状枚举
│   ├── api/                         #   备用API（自研引擎接口）
│   ├── core/                        #   备用引擎实现
│   ├── ui/                          #   UI抽象层
│   └── data/                        #   数据持久化
│
├── neoforge/                        # NeoForge 1.21.1（主平台）
│   ├── NeoForgePlugin.java          #   模组入口
│   ├── bridge/NeoForgeChunkyBridge  #   桥接实现（反射调用ChunkyAPI）
│   ├── screen/PregeneratorScreen    #   🎨 Win11风格主界面
│   ├── client/ClientEventHandler    #   按键绑定 + 客户端命令
│   └── ui/
│       ├── theme/ModernColors       #   Fluent Design色彩系统
│       ├── render/ModernRenderer    #   毛玻璃/圆角/渐变渲染引擎
│       └── widget/ModernButton      #   现代按钮组件
│
├── forge/                           # Forge 1.20.1（兼容）
├── fabric/                          # Fabric 1.21.1（兼容）
└── buildSrc/                        # 构建脚本共享
```

### 数据流

```
PregeneratorScreen                  ChunkyBridge                   Chunky
       │                                │                            │
       │  buildConfig()                 │                            │
       ├──→ PregenerationConfig ──────→│                            │
       │                                │ 反射查找ChunkyAPI           │
       │                                ├──→ ChunkyAPI.startTask() ──→│
       │                                │                            │ 执行预生成
       │                                │←── GenerationProgressEvent ─┤
       │                                │                            │
       │←── onProgressReceived() ──────┤                            │
       │    更新进度条 + 统计            │                            │
       │                                │                            │
       │                                │←── GenerationCompleteEvent ─┤
       │←── onTaskCompleted() ─────────┤                            │
```

---

## 🎨 Win11 Fluent Design 设计系统

### 色彩层次

```
┌─────────────────────────────────────┐
│  Acrylic背景  #1A1A1A @ 85%         │ ← 毛玻璃基底
│  ┌───────────────────────────────┐  │
│  │ 卡片表面  #2A2A2A @ 90%        │  │ ← 内容面板
│  │  ┌─────────────────────┐      │  │
│  │  │ 按钮 #0078D4 (Win11蓝)│      │  │ ← 交互元素
│  │  └─────────────────────┘      │  │
│  │  边框 #FFFFFF @ 10%           │  │ ← 微边框
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

### UI组件

| 组件 | 特性 |
|------|------|
| **ModernButton** | 6px圆角、悬停变亮、按下变暗、三种变体（PRIMARY/SECONDARY/DANGER/SUCCESS） |
| **ModernRenderer** | 亚克力背景、圆角矩形、面板卡片、渐变进度条、阴影、文本输入框边框 |
| **ModernColors** | Win11主题色板、ARGB渐变色插值、进度条颜色过渡 |

### 主界面布局

```
┌──────────────────────────────────────────────┐
│  ⚡ ChunkPregen                    Chunky ✓   │ ← 标题栏
├──────────────────────────────────────────────┤
│                                              │
│  ┌─ 世界 & 形状 ──────────────────────────┐  │
│  │  [overworld ▾]    [圆形 ▾]              │  │ ← 双下拉框
│  └────────────────────────────────────────┘  │
│                                              │
│  ┌─ 中心坐标 ─────────────────────────────┐  │
│  │  X: [________]  Z: [________]           │  │ ← 圆角输入框
│  │  [📍 设为当前位置]                       │  │ ← 次要按钮
│  └────────────────────────────────────────┘  │
│                                              │
│  ┌─ 预生成范围 ───────────────────────────┐  │
│  │  X: [___128___]  Z: [___128___]         │  │
│  │  [📐 设为世界边界]                       │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  [▶ 开始]  [⏸ 暂停]  [▶ 继续]  [⏹ 取消]   │ ← 操作按钮
│                                              │
│  ┌─ 进度 ─────────────────────────────────┐  │
│  │  ████████████░░░░░░░░ 52.3%             │  │ ← 渐变进度条
│  │  已生成 1,234 区块  ⚡ 12.5/s  ⏱ 1m32s  │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  ● Chunky就绪 — 配置参数后开始预生成          │ ← 状态栏
└──────────────────────────────────────────────┘
```

---

## 🚀 快速开始

### 前置条件
- **Chunky** 模组（必须安装！[下载地址](https://modrinth.com/plugin/chunky)）
- NeoForge 1.21.1 / Forge 1.20.1 / Fabric 1.21.1

### 构建

```bash
# 克隆项目
git clone <repo-url>
cd ChunkPregeneratorUI

# 构建NeoForge版本（主平台）
./gradlew :neoforge:build

# 构建所有平台
./gradlew build
```

### 使用

1. 安装 Chunky + ChunkPregeneratorUI 两个模组
2. 进入游戏，按 **P 键** 或输入 `/chunkui` 打开界面
3. 选择世界、形状、设置范围和半径
4. 点击「▶ 开始预生成」
5. 实时查看进度条和统计信息

### 快捷键

| 操作 | 按键/命令 |
|------|----------|
| 打开UI | `P` 键 或 `/chunkui` |
| 居中坐标 | 点击「📍 设为当前位置」 |
| 设世界边界 | 点击「📐 设为世界边界」 |

---

## 🧱 技术栈

| 层级 | 技术 |
|------|------|
| 语言 | Java 21 |
| 构建 | Gradle 8.x (Kotlin DSL) |
| 加载器 | NeoForge 1.21.1 / Forge 1.20.1 / Fabric 1.21.1 |
| UI | Vanilla Screen + 自研Fluent Design渲染引擎 |
| 集成 | ChunkyAPI (反射) / 命令回退 |
| 协议 | MIT |

---

## 📁 各模块代码量

| 模块 | 大小 | 说明 |
|------|------|------|
| `common/` | 77.6 KB | 共享核心：API、模型、桥接接口、备用引擎 |
| `neoforge/` | 60.4 KB | **主平台**：Win11 UI、桥接实现、事件处理 |
| `forge/` | 2.5 KB | Forge兼容层（骨架） |
| `fabric/` | 1.1 KB | Fabric兼容层（骨架） |
| root | 2.9 KB | 构建配置 |
| **合计** | **144.4 KB** | **39个文件** |

---

## 🤝 借鉴与致谢

- [Chunky](https://github.com/pop4959/Chunky) (GPL-3.0) — 研究其架构、API、命令系统
- Microsoft Fluent Design System — UI设计语言参考


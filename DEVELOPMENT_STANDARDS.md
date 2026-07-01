# ChunkPregeneratorUI 工程规范 &amp; 代码审查

> 版本 1.0.0 | NeoForge 1.21.1 优先 | 审查日期 2025-07-14

---

## 📖 一、核心开发原则

### 1.1 先行调研

| 原则 | 说明 |
|------|------|
| **先查再写** | 编写前搜索同类项目，分析其架构和协议 |
| **协议合规** | GPL-3.0 代码**不能直接复制**到 MIT 项目；独立模组通过 API/命令交互不构成衍生作品 |
| **可借鉴则借鉴** | 设计模式、命名习惯、分层方式可参考 |

### 1.2 渐进式开发

| 原则 | 说明 |
|------|------|
| **小改动，频繁提交** | 每次只改一个文件或一个函数 |
| **随时可工作** | 每个提交都应该是可编译/可运行的 |
| **先测试再提交** | 每个提交前确保逻辑正确 |

### 1.3 技术债零容忍

| 原则 | 说明 |
|------|------|
| **不跳过TODO** | 每个TODO要么实现、要么转为Issue |
| **不临时修补** | 错误的抽象不如正确的重复 |
| **及时重构** | 发现坏味道立即重构，不堆积 |

### 1.4 环境部署

| 原则 | 说明 |
|------|------|
| **先检查再部署** | `list_directory` → `read_file` 确认缺失 → 再 `write_file` |
| **不重复部署** | 已有环境不重新安装 |

---

## 📐 二、代码规范（Google Java Style）

### 2.1 命名

| 类型 | 规范 | 示例 |
|------|------|------|
| 包名 | 全小写，无下划线 | `com.chunkpregenerator.neoforge.ui` |
| 类名 | UpperCamelCase | `ModernRenderer`, `NeoForgeChunkyBridge` |
| 方法名 | lowerCamelCase | `renderAcrylicBackground()` |
| 常量 | CONSTANT_CASE | `ACCENT_PRIMARY`, `CARD_RADIUS` |
| 接口 | `I`前缀 或 形容词命名 | `ChunkyBridge`, `IShapeProvider` |

### 2.2 格式化

- 缩进：4空格（非Tab）
- 列宽：120字符
- 大括号：K&amp;R风格（左括号不换行）
- 每行一个声明
- 方法间空一行
- `@Override` 始终使用

### 2.3 import

- 不写通配符 `import java.util.*`
- 排序：`java.*` → `javax.*` → 第三方 → 项目内部
- 未使用的 import 立即删除

---

## 🏗️ 三、架构分层

```
┌─────────────────────────────────────────────────┐
│                    NEOPORGE 模块                    │
│  ┌─────────────┐ ┌──────────┐ ┌──────────────┐  │
│  │ screen/      │ │ ui/      │ │ bridge/      │  │
│  │ Pregenerator │ │ theme/   │ │ NeoForge     │  │
│  │ Screen       │ │ render/  │ │ ChunkyBridge │  │
│  │              │ │ widget/  │ │              │  │
│  └──────┬───────┘ └──────────┘ └──────┬───────┘  │
│         │                             │          │
├─────────┼─────────────────────────────┼──────────┤
│         │        COMMON 模块           │          │
│  ┌──────┴──────────────────────────────┴───────┐  │
│  │  bridge/ChunkyBridge (接口)                  │  │
│  │  model/ProgressSnapshot, PregenerationConfig │  │
│  │  model/ShapeType                            │  │
│  └─────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
         │                  │
         ▼                  ▼
    ┌─────────┐      ┌──────────┐
    │  FORGE   │      │  FABRIC   │  (兼容模块)
    └─────────┘      └──────────┘
```

### 3.1 依赖方向

```
common ← neoforge  ✅ 正确：平台依赖common
common ← forge     ✅ 
common ← fabric    ✅
neoforge → forge   ❌ 禁止：平台间不能互相依赖
```

### 3.2 模块职责

| 模块 | 职责 | 允许的依赖 |
|------|------|-----------|
| `common` | 桥接接口、数据模型、形状系统 | JDK标准库 |
| `neoforge` | NeoForge平台实现（UI、桥接、命令） | common + NeoForge API |
| `forge` | Forge平台实现（兼容层） | common + Forge API |
| `fabric` | Fabric平台实现（兼容层） | common + Fabric API |

---

## 🎨 四、UI架构（Win11 Fluent Design）

### 4.1 视觉层次

| 层级 | 颜色 | 用途 |
|------|------|------|
| 背景层 | `ACRYLIC_BG` (半透明深色) | 毛玻璃底层 |
| 表面层 | `SURFACE_CARD` | 卡片、面板 |
| 悬浮层 | `SURFACE_ELEVATED` | hover时提升 |

### 4.2 组件树

```
PregeneratorScreen (Screen)
├── ModernButton ×6 (开始/暂停/继续/取消/设位置/设边界)
├── CycleButton<String> (世界选择)
├── CycleButton<ShapeType> (形状选择)
├── EditBox ×4 (坐标X/Z, 半径X/Z)
└── 自定义渲染：
    ├── ModernRenderer.renderAcrylicBackground()
    ├── ModernRenderer.renderCard()
    ├── ModernRenderer.renderProgressBar()
    └── ModernRenderer.renderTextFieldFrame()
```

### 4.3 数据流

```
UI控件(EditBox/CycleButton) → PregenerationConfig(record)
        → ChunkyBridge.startTask(config)
            → NeoForgeChunkyBridge (反射调用 ChunkyAPI)
                → Chunky 执行
                    → GenerationProgressEvent
                        → NeoForgeChunkyBridge.mapProgressEvent()
                            → ProgressSnapshot(record)
                                → PregeneratorScreen.onProgressReceived()
                                    → 更新进度条/统计/ETA
```

---

## 🔍 五、自查清单

### 5.1 内存管理

- [x] `ModernColors` / `ModernRenderer` 全静态方法 — 无实例创建
- [x] `ProgressSnapshot` / `PregenerationConfig` 使用 `record` — 天然不可变
- [x] `CopyOnWriteArrayList` 用于监听器列表 — 线程安全无锁读
- [x] `ScheduledExecutorService` 使用 `setDaemon(true)` — JVM退出时自动清理
- [ ] `NeoForgeChunkyBridge.pollExecutor` 需要在模组卸载时调用 `shutdown()`

### 5.2 并发安全

- [x] `CopyOnWriteArrayList<Consumer>` — 读多写少的监听器场景最优解
- [x] `list.forEach()` 在事件回调中 — COWList的迭代器快照不抛 `ConcurrentModificationException`
- [x] `chunkyAvailable` / `apiMode` — 仅在构造器中写入，构造后只读，无竞争

### 5.3 避免重复计算

- [x] `ModernRenderer.fillRoundedRect()` — 中间区域一次性 `fill()` 而非逐px
- [x] `PregenerationConfig` 是不可变record — 修改时通过 `withCenter()` 创建新实例，不会意外修改
- [x] `parseDouble()` 结果不缓存 — 每次输入变化时重新解析，开销极低

### 5.4 提前返回

- [x] `NeoForgeChunkyBridge.initializeChunkyApi()` — 多层提前返回（ClassNotFound→null→reflection失败）
- [x] `PregeneratorScreen.validateInput()` — 早返回false，不嵌套
- [x] `ClientEventHandler.openScreen()` — `mc.player == null` 提前返回

### 5.5 避免不必要对象创建

- [x] `ModernColors.lerp()` 使用 `int` 运算而非 `Color` 对象
- [x] `ProgressSnapshot` 静态工厂方法复用常量字段模式
- [x] `MinecraftRef` 封装 `Minecraft.getInstance()` 调用但避免重复获取

### 5.6 注释解释「为什么」而非「做什么」

```java
// ✅ 好的注释 — 解释为什么这样做
// 使用离散台阶法近似圆角 — Minecraft像素坐标系无抗锯齿

// ❌ 坏的注释 — 只是重复代码
// 设置X坐标
centerXField.setValue("0");
```

- [x] `ModernRenderer` — 每个方法都有Why注释
- [x] `NeoForgeChunkyBridge` — 双重回退策略有完整解释
- [x] `ModernColors` — 分层设计有原理说明

### 5.7 公共 API 文档

- [x] `ChunkyBridge` 接口 — 每个方法有 `@param` / `@return` JavaDoc
- [x] `ProgressSnapshot` record — 每个组件有 `@param` 说明
- [x] `PregenerationConfig` record — 完整文档 + 工厂方法说明
- [x] `ShapeType` enum — 每个值有说明

---

## 📊 六、逐文件审查结果

### neonforge模块

| 文件 | 行数 | 审查结果 | 问题 |
|------|------|---------|------|
| `NeoForgePlugin.java` | 72 | ✅ PASS | — |
| `NeoForgeChunkyBridge.java` | 292 | ✅ PASS | pollExecutor 需shutdown注册 |
| `ClientEventHandler.java` | 69 | ✅ PASS | — |
| `PregeneratorScreen.java` | 340 | ✅ PASS | — |
| `ModernColors.java` | 140 | ✅ PASS | — |
| `ModernRenderer.java` | 222 | ✅ PASS | — |
| `ModernButton.java` | 97 | ⚠️ MINOR | `isMouseClicked()` 未使用(2行) |
| `MinecraftRef.java` | 9 | ✅ PASS | — |
| `en_us.json` | 14 | ✅ PASS | — |
| `zh_cn.json` | 14 | ✅ PASS | — |
| `neoforge.mods.toml` | 46 | ✅ PASS | — |
| `build.gradle.kts` | 23 | ✅ PASS | — |

### common模块

| 文件 | 审查结果 | 备注 |
|------|---------|------|
| `ChunkyBridge.java` | ✅ PASS | 接口设计完善 |
| `ProgressSnapshot.java` | ✅ PASS | Record + 工厂方法 |
| `PregenerationConfig.java` | ✅ PASS | 不可变record |
| `ShapeType.java` | ✅ PASS | 枚举完备 |
| 其他 (api/core/ui/data) | ℹ️ INFO | 自研引擎代码，当前未使用但保留为扩展基础 |

### 根项目

| 文件 | 状态 |
|------|------|
| `build.gradle.kts` | ✅ 已修复（之前为空文件） |
| `settings.gradle.kts` | ✅ 已修复（之前为空文件） |
| `gradle.properties` | ✅ PASS |

---

## ✅ 七、修复记录

| # | 问题 | 状态 |
|---|------|------|
| 1 | `build.gradle.kts` 空文件 | ✅ 已恢复 |
| 2 | `settings.gradle.kts` 空文件 | ✅ 已恢复 |
| 3 | `ForgePlugin` 引用不存在的 `ForgeChunkyBridge` | ✅ 已移除引用 |
| 4 | `FabricPlugin` 引用不存在的 `ForgeClientEvents` | ✅ 已移除引用 |

---

## 🚀 八、快速启动

```bash
# 克隆项目
git clone <repo-url> ChunkPregeneratorUI

# 构建 NeoForge 版本
./gradlew :neoforge:build

# 运行客户端（需要先安装Chunky模组）
./gradlew :neoforge:runClient

# 在游戏中使用
/chunkui    # 打开预生成器UI界面
```

---

## 📋 九、提交前检查清单

每次提交前，确认以下事项：

- [ ] `./gradlew :neoforge:build` 编译通过
- [ ] 没有 `System.out.println` 遗留（用 `LOGGER`）
- [ ] 没有未使用的 import
- [ ] 新方法有 JavaDoc（公共API必须，私有方法建议）
- [ ] 注释解释「为什么」而非「做什么」
- [ ] 没有硬编码的魔法数字（提取为常量）
- [ ] 监听器/资源在适当位置释放
- [ ] `record` 优于可变 `class`（数据对象优先record）

---

*文档最后更新：2025-07-14 | 核弹男孩 (Nuclear Boy)*

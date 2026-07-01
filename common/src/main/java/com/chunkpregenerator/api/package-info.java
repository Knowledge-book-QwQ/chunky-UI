/**
 * ChunkPregeneratorUI — Minecraft 区块预生成可视化UI模组
 *
 * 本文档定义了整个模组的公共 API 契约。
 * 所有跨模块交互必须通过此 API 进行，以保证各平台实现的独立性。
 *
 * <h3>API 设计原则</h3>
 * <ul>
 *   <li>全部基于接口，不暴露实现细节</li>
 *   <li>使用 Builder 模式构建复杂对象</li>
 *   <li>所有公共方法必须标注 @Nonnull/@Nullable</li>
 *   <li>使用 Optional 表达可能为空的结果</li>
 *   <li>所有回调使用 Consumer&lt;T&gt;，避免继承监听器接口</li>
 * </ul>
 *
 * <h3>包概览</h3>
 * <pre>
 * com.chunkpregenerator.api — 公共 API 入口
 *   ├── IPregenerationEngine      — 预生成引擎核心接口
 *   ├── IPregenerationTask        — 单个预生成任务
 *   ├── IChunkProgressCallback    — 进度回调
 *   ├── IShapeProvider            — 区域形状提供者
 *   └── config/
 *       └── IPregenerationConfig  — 预生成配置
 * </pre>
 *
 * <p>使用时通过 ServiceLoader 或平台注入获取实例。
 */
package com.chunkpregenerator.api;

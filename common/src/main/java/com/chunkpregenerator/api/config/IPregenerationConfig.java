package com.chunkpregenerator.api.config;

import com.chunkpregenerator.api.shape.IShapeProvider;

import javax.annotation.Nonnull;

/**
 * 区块预生成配置。
 *
 * <p>使用 Builder 模式构建不可变配置对象。
 * 所有参数都有合理的默认值，只需设置必要参数即可。
 *
 * <p>示例：
 * <pre>{@code
 * IPregenerationConfig config = IPregenerationConfig.builder()
 *     .shape(RectangularShape.squareCentered(0, 0, 100))
 *     .concurrencyLevel(4)
 *     .build();
 * }</pre>
 */
public interface IPregenerationConfig {

    /**
     * 获取要生成的区域形状。
     */
    @Nonnull
    IShapeProvider getShape();

    /**
     * 获取并发级别（同时生成的区块数）。
     *
     * <p>默认值根据 CPU 核心数动态计算：max(1, Runtime.availableProcessors() - 1)。
     * 建议值：服务器 4-8，客户端 2-4。
     */
    int getConcurrencyLevel();

    /**
     * 是否强制重新生成已存在的区块。
     *
     * <p>默认为 false，只生成尚未加载的区块。
     * 设置为 true 时会重新生成所有指定区域的区块。
     */
    boolean isForceRegenerate();

    /**
     * 获取每个区块生成前的延迟（毫秒）。
     *
     * <p>用于减轻生成对服务器性能的影响。默认 50ms。
     * 设置为 0 表示不延迟。
     */
    long getChunkDelayMillis();

    /**
     * 获取区块卸载延迟（刻）。
     *
     * <p>生成完成后等待的刻数再卸载区块。默认 100 tick（5秒）。
     * 较长的时间可以让区块完全加载实体和红石。
     */
    int getUnloadDelayTicks();

    /**
     * 是否在生成完成后保存世界。
     *
     * <p>默认为 true，确保所有区块数据写入磁盘。
     */
    boolean isSaveWorldOnComplete();

    /**
     * 获取 Builder 实例。
     */
    @Nonnull
    static Builder builder() {
        return new Builder();
    }

    /**
     * 配置构建器。
     */
    final class Builder {
        private IShapeProvider shape;
        private int concurrencyLevel = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        private boolean forceRegenerate = false;
        private long chunkDelayMillis = 50L;
        private int unloadDelayTicks = 100;
        private boolean saveWorldOnComplete = true;

        private Builder() {}

        /**
         * 必需：设置要生成的区域形状。
         */
        @Nonnull
        public Builder shape(@Nonnull IShapeProvider shape) {
            this.shape = shape;
            return this;
        }

        /**
         * 设置并发级别。默认根据CPU核心数计算。
         *
         * @param concurrencyLevel 1-16之间的值
         */
        @Nonnull
        public Builder concurrencyLevel(int concurrencyLevel) {
            if (concurrencyLevel < 1 || concurrencyLevel > 16) {
                throw new IllegalArgumentException(
                    "Concurrency level must be between 1 and 16, got: " + concurrencyLevel);
            }
            this.concurrencyLevel = concurrencyLevel;
            return this;
        }

        @Nonnull
        public Builder forceRegenerate(boolean forceRegenerate) {
            this.forceRegenerate = forceRegenerate;
            return this;
        }

        @Nonnull
        public Builder chunkDelayMillis(long chunkDelayMillis) {
            this.chunkDelayMillis = Math.max(0, chunkDelayMillis);
            return this;
        }

        @Nonnull
        public Builder unloadDelayTicks(int unloadDelayTicks) {
            this.unloadDelayTicks = Math.max(0, unloadDelayTicks);
            return this;
        }

        @Nonnull
        public Builder saveWorldOnComplete(boolean saveWorldOnComplete) {
            this.saveWorldOnComplete = saveWorldOnComplete;
            return this;
        }

        @Nonnull
        public IPregenerationConfig build() {
            if (shape == null) {
                throw new IllegalStateException("Shape must be set before building config");
            }
            return new PregenerationConfig(
                shape, concurrencyLevel, forceRegenerate,
                chunkDelayMillis, unloadDelayTicks, saveWorldOnComplete
            );
        }
    }
}

/**
 * 内部不可变配置实现。
 */
record PregenerationConfig(
    IShapeProvider shape,
    int concurrencyLevel,
    boolean forceRegenerate,
    long chunkDelayMillis,
    int unloadDelayTicks,
    boolean saveWorldOnComplete
) implements IPregenerationConfig {
    PregenerationConfig {
        java.util.Objects.requireNonNull(shape, "shape must not be null");
    }
}

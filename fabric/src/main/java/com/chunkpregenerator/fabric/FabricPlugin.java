package com.chunkpregenerator.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * [兼容模块] ChunkPregeneratorUI Fabric 1.21.1 入口。
 *
 * <p>此为Fabric兼容实现，待完善反射桥接逻辑。
 * 目标是与NeoForge版本功能对等。</p>
 *
 * <p>主开发平台：NeoForge 1.21.1</p>
 */
public class FabricPlugin implements ModInitializer {

    public static final String MOD_ID = "chunkpregeneratorui";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("ChunkPregeneratorUI Fabric 初始化完成");
    }

    /** Fabric客户端入口 */
    public static class Client implements ClientModInitializer {
        @Override
        public void onInitializeClient() {
            LOGGER.info("ChunkPregeneratorUI Fabric 客户端初始化完成");
        }
    }
}

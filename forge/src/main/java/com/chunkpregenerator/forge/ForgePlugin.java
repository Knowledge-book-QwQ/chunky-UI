package com.chunkpregenerator.forge;

import com.chunkpregenerator.bridge.ChunkyBridge;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * [兼容模块] ChunkPregeneratorUI Forge 1.20.1 入口。
 *
 * <p>Forge 1.20.1兼容实现。ChunkyBridge由平台实现提供
 * （目前为stub，待完善反射桥接逻辑）。</p>
 *
 * <p>主开发平台为NeoForge 1.21.1，此模块保持API对等。</p>
 *
 * @see ChunkyBridge
 */
@Mod(ForgePlugin.MOD_ID)
public final class ForgePlugin {

    public static final String MOD_ID = "chunkpregeneratorui";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static ForgePlugin instance;

    public ForgePlugin() {
        instance = this;

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::onCommonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("ChunkPregeneratorUI Forge 初始化完成");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Forge 通用初始化完成");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.warn("⚠ Forge模块为兼容实现，推荐使用NeoForge 1.21.1版本");
    }

    public static ForgePlugin getInstance() {
        return instance;
    }
}

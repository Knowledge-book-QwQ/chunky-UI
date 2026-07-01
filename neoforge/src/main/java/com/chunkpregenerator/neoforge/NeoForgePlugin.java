package com.chunkpregenerator.neoforge;

import com.chunkpregenerator.neoforge.bridge.NeoForgeChunkyBridge;
import com.chunkpregenerator.neoforge.client.ClientEventHandler;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

/**
 * ChunkPregeneratorUI NeoForge 模组入口。
 *
 * <h3>两个UI入口</h3>
 * <ol>
 *   <li><b>/chunkui 指令</b> — 在聊天栏输入即可打开</li>
 *   <li><b>ESC 暂停菜单按钮</b> — 打开游戏菜单后，在底部有"区块预生成器"按钮</li>
 * </ol>
 *
 * <h3>架构</h3>
 * <ol>
 *   <li>{@link NeoForgeChunkyBridge} — 通过反射调用 ChunkyAPI 通信</li>
 *   <li>{@link ClientEventHandler} — 注册 /chunkui 指令和ESC菜单按钮</li>
 *   <li>{@link com.chunkpregenerator.neoforge.screen.PregeneratorScreen} — Win11风格UI</li>
 * </ol>
 */
@Mod(NeoForgePlugin.MOD_ID)
public final class NeoForgePlugin {

    public static final String MOD_ID = "chunkpregeneratorui";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static NeoForgePlugin instance;

    private final NeoForgeChunkyBridge chunkyBridge;

    public NeoForgePlugin(IEventBus modEventBus, ModContainer modContainer) {
        instance = this;
        this.chunkyBridge = new NeoForgeChunkyBridge();

        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(ClientEventHandler::onRegisterClientCommands);

        // 注册ESC暂停菜单入口 — 监听ScreenEvent在游戏事件总线
        ClientEventHandler.register();

        NeoForge.EVENT_BUS.addListener(this::onServerStarting);

        LOGGER.info("ChunkPregeneratorUI v{} 已加载 | /chunkui 指令 + ESC菜单按钮",
                modContainer.getModInfo().getVersion());
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Chunky状态: {}", chunkyBridge.isChunkyAvailable() ? "已连接" : "未安装");
    }

    private void onServerStarting(ServerStartingEvent event) {
        event.getServer().execute(() -> {
            if (chunkyBridge.isChunkyAvailable()) {
                LOGGER.info("✅ Chunky已连接，预生成器UI就绪");
            } else {
                LOGGER.warn("⚠️ Chunky未安装！请安装后使用 /chunkui 或 ESC菜单");
            }
        });
    }

    public static NeoForgePlugin getInstance() {
        return instance;
    }

    public NeoForgeChunkyBridge getBridge() {
        return chunkyBridge;
    }
}

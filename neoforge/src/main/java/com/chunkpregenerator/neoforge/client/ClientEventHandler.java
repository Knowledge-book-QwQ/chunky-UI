package com.chunkpregenerator.neoforge.client;

import com.chunkpregenerator.neoforge.NeoForgePlugin;
import com.chunkpregenerator.neoforge.screen.PregeneratorScreen;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import static net.minecraft.commands.Commands.literal;

/**
 * 客户端事件处理器。
 *
 * <h3>两个入口</h3>
 * <ol>
 *   <li><b>/chunkui 指令</b> — 在聊天栏输入即可打开界面</li>
 *   <li><b>ESC 暂停菜单按钮</b> — 打开游戏菜单后，
 *       在"选项"和"断开连接"之间有一个"区块预生成器"按钮</li>
 * </ol>
 *
 * <p>第一个入口注册在模组总线，第二个入口注册在游戏事件总线。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class ClientEventHandler {

    private ClientEventHandler() {
    }

    /**
     * 注册 ESC 暂停菜单入口。
     *
     * <p>当玩家按 ESC 打开暂停菜单后，我们在菜单底部添加一个按钮，
     * 点击即可进入预生成器界面。因为这个事件需要在游戏运行时监听，
     * 所以注册到 NeoForge 游戏事件总线。</p>
     */
    public static void register() {
        NeoForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onScreenInit(ScreenEvent.Init.Post event) {
                onPauseScreenInit(event);
            }
        });
    }

    /**
     * 暂停菜单初始化后钩入——在 PauseScreen 底部添加预生成器按钮。
     *
     * <p>MC 1.21.1 的 PauseScreen 布局是居中竖直排列的按钮栈。
     * 我们在最后一个按钮下方 8px 处放置我们的按钮，视觉上成为菜单的一部分。</p>
     */
    private static void onPauseScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen pauseScreen)) {
            return;
        }

        // 暂停菜单中的按钮宽度：204px，居中
        int btnWidth = 204;
        int btnX = pauseScreen.width / 2 - btnWidth / 2;

        // 找到已有最后一个按钮的底部坐标
        int btnY = 0;
        var children = pauseScreen.children();
        for (var child : children) {
            if (child instanceof Button btn) {
                int bottom = btn.getY() + btn.getHeight();
                if (bottom > btnY) btnY = bottom;
            }
        }
        // 在最后一个按钮下方 8px
        btnY += 8;

        Button chunkBtn = Button.builder(
                        Component.translatable("menu.chunkpregenerator"),
                        btn -> openScreen())
                .pos(btnX, btnY)
                .size(btnWidth, 20)
                .build();

        event.addListener(chunkBtn);
    }

    /**
     * 注册客户端指令 {@code /chunkui}。
     *
     * <p>这是第二个入口，和所有MC指令一样在聊天栏使用。</p>
     */
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                literal("chunkui")
                        .executes(ctx -> {
                            openScreen();
                            return 1;
                        })
        );
    }

    /**
     * 打开预生成器 UI。
     *
     * <p>即使 Chunky 未安装也会打开界面（让玩家先看看），
     * 同时通过聊天消息给出提示。</p>
     */
    public static void openScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        NeoForgePlugin plugin = NeoForgePlugin.getInstance();
        if (plugin == null) {
            mc.player.displayClientMessage(
                    Component.literal("§c[ChunkPregeneratorUI] 模组加载异常"), false);
            return;
        }

        if (!plugin.getBridge().isChunkyAvailable()) {
            mc.player.displayClientMessage(
                    Component.literal("§6⚠ 未检测到 Chunky 模组"), false);
        }

        mc.setScreen(new PregeneratorScreen(plugin.getBridge()));
    }
}

package com.chunkpregenerator.neoforge.ui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

/**
 * Minecraft客户端实例持有者，避免到处写Minecraft.getInstance()。
 */
final class MinecraftRef {
    static Font font() { return Minecraft.getInstance().font; }
    static Minecraft mc() { return Minecraft.getInstance(); }
}

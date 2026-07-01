package com.chunkpregenerator.neoforge.ui.widget;

import com.chunkpregenerator.neoforge.ui.render.ModernRenderer;
import com.chunkpregenerator.neoforge.ui.theme.ModernColors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Win11 Fluent Design 现代按钮组件。
 *
 * <p>视觉行为：
 * <ul>
 *   <li>主按钮 — 主题蓝实心填充，hover变亮，press变暗</li>
 *   <li>次要按钮 — 透明背景 + 细边框，hover出现微光</li>
 *   <li>危险按钮 — 红色实心，用于取消/删除</li>
 *   <li>成功按钮 — 绿色实心，用于继续/确认</li>
 * </ul>
 *
 * <p>继承{@link AbstractWidget}以获得Minecraft原生事件处理，
 * 渲染则由{@link ModernRenderer}负责。</p>
 */
@OnlyIn(Dist.CLIENT)
public class ModernButton extends AbstractWidget {

    public enum Variant {
        PRIMARY,
        SECONDARY,
        DANGER,
        SUCCESS
    }

    private static final int RADIUS = 6;
    private static final int MIN_WIDTH = 40;

    private final Variant variant;
    private final Consumer<ModernButton> onClick;
    private boolean isHovered;

    public ModernButton(int x, int y, int width, int height, Component message,
                        Variant variant, Consumer<ModernButton> onClick) {
        super(x, y, Math.max(width, MIN_WIDTH), height, message);
        this.variant = variant;
        this.onClick = onClick;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (onClick != null) {
            onClick.accept(this);
        }
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // 更新悬停状态 — MC每帧调用renderWidget，这里是最佳检测点
        isHovered = isMouseOver(mouseX, mouseY);

        // isActive()由AbstractWidget管理（enabled/disabled状态）
        boolean isPressed = isHovered && isActive();

        int bgColor = switch (variant) {
            case PRIMARY -> ModernColors.ACCENT_PRIMARY;
            case DANGER -> ModernColors.ACCENT_DANGER;
            case SUCCESS -> ModernColors.ACCENT_SUCCESS;
            case SECONDARY -> ModernColors.blackAlpha(0);
        };

        int textColor = (variant == Variant.SECONDARY && !isHovered)
                ? ModernColors.TEXT_SECONDARY
                : ModernColors.TEXT_PRIMARY;

        if (variant == Variant.SECONDARY) {
            ModernRenderer.renderSecondaryButton(gui, getX(), getY(), width, height,
                    RADIUS, isHovered, isPressed);
        } else {
            ModernRenderer.renderButton(gui, getX(), getY(), width, height,
                    RADIUS, bgColor, textColor, isHovered, isPressed);
        }

        // 文字居中 — MC字体的测量宽度与实际渲染宽度一致
        int textWidth = MinecraftRef.font().width(getMessage());
        int textX = getX() + (width - textWidth) / 2;
        int textY = getY() + (height - 8) / 2;
        gui.drawString(MinecraftRef.font(), getMessage(), textX, textY, textColor);
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }
}

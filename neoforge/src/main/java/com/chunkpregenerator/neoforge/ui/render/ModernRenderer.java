package com.chunkpregenerator.neoforge.ui.render;

import com.chunkpregenerator.neoforge.ui.theme.ModernColors;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Win11 Fluent Design 渲染引擎。
 *
 * <p>在Minecraft的GuiGraphics基础上提供现代UI渲染原语：
 * <ul>
 *   <li>亚克力毛玻璃背景</li>
 *   <li>圆角矩形（带边框和阴影）</li>
 *   <li>渐变进度条</li>
 *   <li>悬停/按下状态的视觉反馈</li>
 * </ul>
 *
 * <p>Minecraft的像素坐标系没有抗锯齿，使用离散台阶近似圆角。</p>
 */
public final class ModernRenderer {

    private ModernRenderer() {}

    // ==================== 亚克力背景 ====================

    /**
     * 渲染Win11风格的亚克力毛玻璃背景。
     *
     * <p>在Minecraft中通过以下方式模拟毛玻璃效果：
     * <ol>
     *   <li>先调用标准背景渲染（游戏画面变暗模糊）</li>
     *   <li>叠加深色半透明层作为基层</li>
     *   <li>叠加微弱的噪声纹理（横向条纹）模拟玻璃纹理</li>
     * </ol>
     *
     * @param gui 图形上下文
     * @param x 左上X
     * @param y 左上Y
     * @param width 宽度
     * @param height 高度
     */
    public static void renderAcrylicBackground(GuiGraphics gui, int x, int y, int width, int height) {
        // 基础填充
        gui.fill(x, y, x + width, y + height, ModernColors.ACRYLIC_BG);

        // 微弱噪声条纹（横向，间距4px）模拟玻璃纹理
        for (int row = y; row < y + height; row += 4) {
            gui.fill(x, row, x + width, row + 1, ModernColors.whiteAlpha(3));
        }
    }

    // ==================== 圆角矩形 ====================

    /**
     * 绘制圆角矩形填充。
     * 使用离散台阶法近似圆角——每个角逐步内缩。
     *
     * @param gui 图形上下文
     * @param x 左上X
     * @param y 左上Y
     * @param width 宽度
     * @param height 高度
     * @param radius 圆角半径
     * @param color ARGB颜色
     */
    public static void fillRoundedRect(GuiGraphics gui, int x, int y, int width, int height, int radius, int color) {
        int right = x + width;
        int bottom = y + height;

        // 主体矩形（减去圆角区域的顶部和底部）
        gui.fill(x, y + radius, right, bottom - radius, color);

        // 渲染圆角：逐行从边缘向内缩进
        for (int i = 0; i < radius; i++) {
            int inset = radius - i;
            // 顶部圆角（左右对称）
            gui.fill(x + inset, y + i, right - inset, y + i + 1, color);
            // 底部圆角
            gui.fill(x + inset, bottom - i - 1, right - inset, bottom - i, color);
        }

        // 中间填充（无缩进区域）
        gui.fill(x, y + radius, right, bottom - radius, color);
    }

    /**
     * 绘制圆角矩形边框。
     * 通过像素级偏移实现1px边框。
     *
     * @param gui 图形上下文
     * @param x 左上X
     * @param y 左上Y
     * @param width 宽度
     * @param height 高度
     * @param radius 圆角半径
     * @param borderWidth 边框宽度（通常1-2px）
     * @param color ARGB颜色
     */
    public static void drawRoundedBorder(GuiGraphics gui, int x, int y, int width, int height,
                                          int radius, int borderWidth, int color) {
        for (int w = 0; w < borderWidth; w++) {
            int bx = x + w;
            int by = y + w;
            int bw = width - 2 * w;
            int bh = height - 2 * w;
            int right = bx + bw;
            int bottom = by + bh;

            for (int i = 0; i < radius; i++) {
                int inset = radius - i;
                // 顶部
                gui.fill(bx + inset, by + i, right - inset, by + i + 1, color);
                // 底部
                gui.fill(bx + inset, bottom - i - 1, right - inset, bottom - i, color);
            }
            // 左右直边
            gui.fill(bx, by + radius, bx + 1, bottom - radius, color);
            gui.fill(right - 1, by + radius, right, bottom - radius, color);
        }
    }

    /**
     * 绘制圆角矩形卡片（填充+边框）。
     */
    public static void renderCard(GuiGraphics gui, int x, int y, int width, int height,
                                   int radius, int fillColor, int borderColor) {
        fillRoundedRect(gui, x, y, width, height, radius, fillColor);
        drawRoundedBorder(gui, x, y, width, height, radius, 1, borderColor);
    }

    // ==================== 阴影模拟 ====================

    /**
     * 在卡片下方渲染柔和的投影（使用多层半透明偏移实现）。
     *
     * @param gui 图形上下文
     * @param x 卡片X
     * @param y 卡片Y
     * @param width 卡片宽度
     * @param height 卡片高度
     * @param radius 圆角半径
     */
    public static void renderDropShadow(GuiGraphics gui, int x, int y, int width, int height, int radius) {
        // 3层半透明偏移模拟高斯模糊衰减
        int[][] layers = {
                {2, 2, 4},
                {4, 4, 3},
                {6, 6, 2},
        };
        for (int[] layer : layers) {
            int ox = layer[0], oy = layer[1], alpha = layer[2];
            fillRoundedRect(gui, x + ox, y + oy, width, height, radius, ModernColors.blackAlpha(alpha * 6));
        }
    }

    // ==================== 渐变进度条 ====================

    /**
     * 渲染Win11风格的渐变进度条。
     *
     * @param gui 图形上下文
     * @param x 左上X
     * @param y 左上Y
     * @param width 总宽度
     * @param height 高度
     * @param percent 完成百分比 [0.0, 1.0]
     * @param radius 圆角半径
     */
    public static void renderProgressBar(GuiGraphics gui, int x, int y, int width, int height,
                                          float percent, int radius) {
        // 轨道
        fillRoundedRect(gui, x, y, width, height, radius, ModernColors.PROGRESS_TRACK);

        if (percent <= 0) return;

        int filledWidth = Math.max(radius * 2, (int) (width * Math.clamp(percent, 0f, 1f)));
        int barX = x + 1;
        int barY = y + 1;
        int barW = filledWidth - 2;
        int barH = height - 2;

        // 分段渲染渐变（每4px一段，颜色平滑过渡）
        for (int px = 0; px < barW; px++) {
            float t = (float) px / width;
            int color = ModernColors.progressColorAt(t);
            gui.fill(barX + px, barY, barX + px + 1, barY + barH, color);
        }
    }

    // ==================== 按钮 ====================

    /**
     * 渲染 Win11 风格按钮。
     *
     * @param gui 图形上下文
     * @param x 左上X
     * @param y 左上Y
     * @param width 宽度
     * @param height 高度
     * @param radius 圆角半径
     * @param bgColor 背景色
     * @param textColor 文字色
     * @param isHovered 是否悬停
     * @param isPressed 是否按下
     */
    public static void renderButton(GuiGraphics gui, int x, int y, int width, int height,
                                     int radius, int bgColor, int textColor,
                                     boolean isHovered, boolean isPressed) {
        int actualBg = bgColor;
        if (isPressed) {
            actualBg = ModernColors.ACCENT_PRESSED;
        } else if (isHovered) {
            actualBg = ModernColors.ACCENT_HOVER;
        }

        fillRoundedRect(gui, x, y, width, height, radius, actualBg);

        // hover时添加微弱的内部高光
        if (isHovered && !isPressed) {
            fillRoundedRect(gui, x + 1, y + 1, width - 2, height / 2, radius - 1,
                    ModernColors.whiteAlpha(15));
        }
    }

    /**
     * 渲染次要按钮（带边框的幽灵按钮风格）。
     */
    public static void renderSecondaryButton(GuiGraphics gui, int x, int y, int width, int height,
                                              int radius, boolean isHovered, boolean isPressed) {
        int bgColor = isPressed ? ModernColors.whiteAlpha(15) :
                      isHovered ? ModernColors.whiteAlpha(8) : ModernColors.blackAlpha(0);
        fillRoundedRect(gui, x, y, width, height, radius, bgColor);
        drawRoundedBorder(gui, x, y, width, height, radius, 1,
                isHovered ? ModernColors.BORDER_ACTIVE : ModernColors.BORDER_SUBTLE);
    }

    // ==================== 文本输入框 ====================

    /**
     * 渲染现代风格的文本输入框底板（圆角+边框+焦点光晕）。
     *
     * @param gui 图形上下文
     * @param x 左上X
     * @param y 左上Y
     * @param width 宽度
     * @param height 高度
     * @param isFocused 是否聚焦
     */
    public static void renderTextFieldFrame(GuiGraphics gui, int x, int y, int width, int height,
                                             boolean isFocused) {
        int bgColor = isFocused ? 0xE63A3A3A : ModernColors.SURFACE_CARD;
        int borderColor = isFocused ? ModernColors.BORDER_ACTIVE : ModernColors.BORDER_SUBTLE;

        fillRoundedRect(gui, x, y, width, height, 4, bgColor);
        drawRoundedBorder(gui, x, y, width, height, 4, 1, borderColor);

        // 焦点光晕
        if (isFocused) {
            drawRoundedBorder(gui, x - 1, y - 1, width + 2, height + 2, 5, 1,
                    ModernColors.whiteAlpha(20));
        }
    }

    // ==================== 分隔线 ====================

    /**
     * 渲染微妙的横向分隔线。
     */
    public static void renderDivider(GuiGraphics gui, int x, int y, int width) {
        gui.fill(x, y, x + width, y + 1, ModernColors.BORDER_SUBTLE);
    }
}

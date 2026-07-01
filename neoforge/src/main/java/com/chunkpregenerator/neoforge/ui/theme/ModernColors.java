package com.chunkpregenerator.neoforge.ui.theme;

/**
 * Win11 Fluent Design 色彩系统。
 *
 * <p>设计参考微软Fluent Design System，适配Minecraft ABGR色彩空间。
 * 整体采用深色主题 + 亚克力半透明材质，营造层次感和现代感。</p>
 *
 * <p>色彩分层（从底到顶）：
 * <ol>
 *   <li>背景层 - 游戏画面模糊 + 深色覆盖</li>
 *   <li>表面层 - 半透明卡片表面</li>
 *   <li>内容层 - 按钮、输入框等交互元素</li>
 *   <li>强调层 - 高亮和悬停效果</li>
 * </ol>
 */
public final class ModernColors {

    private ModernColors() {}

    // ==================== 背景与表面 ====================

    /** 亚克力背景 - 毛玻璃效果底层，深色半透明 */
    public static final int ACRYLIC_BG = 0xD91A1A1A;

    /** 卡片表面 - 略亮的表面色，用于面板和卡片 */
    public static final int SURFACE_CARD = 0xE62A2A2A;

    /** 悬浮卡片 - 更高层级，hover时使用 */
    public static final int SURFACE_ELEVATED = 0xE6383838;

    /** 遮罩层 - 用于非活跃区域 */
    public static final int OVERLAY_DIM = 0x99000000;

    // ==================== 边框 ====================

    /** 卡片边框 - 微妙的白色边框增加层次感 */
    public static final int BORDER_SUBTLE = 0x18FFFFFF;

    /** 活跃边框 - 焦点状态的边框颜色 */
    public static final int BORDER_ACTIVE = 0x800078D4;

    // ==================== 强调色 ====================

    /** Win11强调蓝 - 默认主题色 */
    public static final int ACCENT_PRIMARY = 0xFF0078D4;

    /** 悬停强调 - 按钮hover时更亮 */
    public static final int ACCENT_HOVER = 0xFF1A8CD8;

    /** 按下强调 - 按钮点击时更暗 */
    public static final int ACCENT_PRESSED = 0xFF005A9E;

    /** 成功绿 - 任务完成指示 */
    public static final int ACCENT_SUCCESS = 0xFF13C25B;

    /** 警告橙 */
    public static final int ACCENT_WARNING = 0xFFFF8C00;

    /** 危险红 - 取消/错误操作 */
    public static final int ACCENT_DANGER = 0xFFE81123;

    // ==================== 文字 ====================

    /** 主文字 - 标题和重要内容 */
    public static final int TEXT_PRIMARY = 0xFFFFFFFF;

    /** 次文字 - 标签和描述 */
    public static final int TEXT_SECONDARY = 0xFFAAAAAA;

    /** 弱文字 - 辅助信息 */
    public static final int TEXT_DISABLED = 0xFF666666;

    // ==================== 进度条 ====================

    /** 进度条轨道背景 */
    public static final int PROGRESS_TRACK = 0xFF333333;

    /** 进度条开始色（低进度） */
    public static final int PROGRESS_START = 0xFF0078D4;

    /** 进度条中间色 */
    public static final int PROGRESS_MID = 0xFF13C25B;

    /** 进度条结束色（高进度） */
    public static final int PROGRESS_END = 0xFF13C25B;

    // ==================== 工具方法 ====================

    /**
     * 在两个ARGB色之间线性插值。
     *
     * @param from 起始色
     * @param to 结束色
     * @param t 插值因子 [0.0, 1.0]
     * @return 插值颜色
     */
    public static int lerp(int from, int to, float t) {
        float tf = Math.clamp(t, 0f, 1f);
        int a = (int) (((from >> 24) & 0xFF) + (((to >> 24) & 0xFF) - ((from >> 24) & 0xFF)) * tf);
        int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * tf);
        int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * tf);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * tf);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * 根据进度百分比获取渐变颜色（蓝→绿过渡）。
     */
    public static int progressColorAt(float percent) {
        if (percent < 0.5f) {
            return lerp(PROGRESS_START, PROGRESS_MID, percent * 2f);
        } else {
            return lerp(PROGRESS_MID, PROGRESS_END, (percent - 0.5f) * 2f);
        }
    }

    /** 带alpha的白色 - 用于微妙的叠加效果 */
    public static int whiteAlpha(int alpha) {
        return (alpha << 24) | 0xFFFFFF;
    }

    /** 带alpha的黑色 */
    public static int blackAlpha(int alpha) {
        return (alpha << 24);
    }
}

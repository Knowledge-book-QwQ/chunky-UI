package com.chunkpregenerator.neoforge.screen;

import com.chunkpregenerator.bridge.ChunkyBridge;
import com.chunkpregenerator.model.PregenerationConfig;
import com.chunkpregenerator.model.ProgressSnapshot;
import com.chunkpregenerator.model.ShapeType;
import com.chunkpregenerator.neoforge.ui.render.ModernRenderer;
import com.chunkpregenerator.neoforge.ui.theme.ModernColors;
import com.chunkpregenerator.neoforge.ui.widget.ModernButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Win11 Fluent Design 风格的区块预生成器主界面。
 *
 * <h3>设计语言</h3>
 * <ul>
 *   <li>亚克力毛玻璃背景 — 半透明深色底层模拟Mica材质</li>
 *   <li>圆角卡片 — 6px圆角，微边框分隔内容区</li>
 *   <li>现代按钮 — 强调蓝主按钮 + 次要边框按钮</li>
 *   <li>渐变进度条 — 从蓝到绿的平滑过渡</li>
 *   <li>悬停反馈 — 按钮hover变亮，提供即时视觉确认</li>
 * </ul>
 *
 * <h3>数据流</h3>
 * UI控件 → PregenerationConfig → ChunkyBridge → ChunkyAPI → Chunky执行
 * Chunky进度事件 → ChunkyBridge.onProgressUpdate → 本界面被动刷新
 */
@OnlyIn(Dist.CLIENT)
public final class PregeneratorScreen extends Screen {

    // ==================== 布局常量 ====================

    /** 内容区宽度 */
    private static final int PANEL_WIDTH = 360;
    /** 卡片圆角 */
    private static final int CARD_RADIUS = 8;
    /** 卡片内边距 */
    private static final int CARD_PAD = 12;
    /** 卡片间距 */
    private static final int CARD_GAP = 10;
    /** 字段标签宽度 */
    private static final int LABEL_W = 28;
    /** 字段高度 */
    private static final int FIELD_H = 20;
    /** 按钮高度 */
    private static final int BTN_H = 22;

    // ==================== 依赖 ====================

    private final ChunkyBridge bridge;

    // ==================== UI组件 ====================

    private CycleButton<String> worldSelector;
    private CycleButton<ShapeType> shapeSelector;
    private EditBox centerXField, centerZField;
    private EditBox radiusXField, radiusZField;
    private ModernButton startBtn, pauseBtn, continueBtn, cancelBtn;
    private ModernButton setPosBtn, setBorderBtn;

    // ==================== 状态 ====================

    private ProgressSnapshot progress = ProgressSnapshot.empty("minecraft:overworld");
    private boolean isTaskRunning;
    private String activeWorld = "minecraft:overworld";

    /** 当前内容区左上X（居中计算） */
    private int panelX;
    /** 当前Y游标 */
    private int cursorY;

    // ==================== 构造 ====================

    public PregeneratorScreen(ChunkyBridge bridge) {
        super(Component.translatable("screen.chunkpregenerator.title"));
        this.bridge = bridge;
    }

    // ==================== 初始化 ====================

    @Override
    protected void init() {
        super.init();
        panelX = (width - PANEL_WIDTH) / 2;
        cursorY = 28;

        buildWorldShapeRow();
        buildCoordinateCard();
        buildRadiusCard();
        buildActionButtons();
        buildProgressCard();

        // 注册Chunky进度事件
        bridge.onProgressUpdate(this::onProgressReceived);
        bridge.onTaskComplete(this::onTaskCompleted);

        updateButtonStates();
    }

    /**
     * 第一行：世界选择 + 形状选择（并排双下拉框）
     */
    private void buildWorldShapeRow() {
        int halfW = (PANEL_WIDTH - CARD_GAP) / 2;

        // 世界选择器
        List<String> worlds = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.players().stream()
                    .map(p -> p.level().dimension().location().toString())
                    .distinct().toList()
                : List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");

        worldSelector = CycleButton.<String>builder(name -> Component.literal(name))
                .withValues(worlds.isEmpty() ? List.of("minecraft:overworld") : worlds)
                .withInitialValue("minecraft:overworld")
                .displayOnlyValue()
                .create(panelX, cursorY, halfW, FIELD_H,
                        Component.translatable("screen.chunkpregenerator.world"));
        addRenderableWidget(worldSelector);

        // 形状选择器
        shapeSelector = CycleButton.<ShapeType>builder(type -> Component.literal(type.displayName()))
                .withValues(ShapeType.values())
                .withInitialValue(ShapeType.CIRCLE)
                .displayOnlyValue()
                .create(panelX + halfW + CARD_GAP, cursorY, halfW, FIELD_H,
                        Component.translatable("screen.chunkpregenerator.shape"));
        addRenderableWidget(shapeSelector);

        cursorY += FIELD_H + CARD_GAP + 4;
    }

    /**
     * 坐标卡片：中心X/Z输入 + 设当前位置按钮
     */
    private void buildCoordinateCard() {
        int cardH = 64;
        int innerX = panelX + CARD_PAD;
        int fieldW = 120;

        cursorY += 2;
        int cardTop = cursorY;

        // 字段
        centerXField = new EditBox(font, innerX + LABEL_W, cursorY + 2, fieldW, FIELD_H,
                Component.literal("X"));
        centerXField.setValue("0");
        addRenderableWidget(centerXField);

        centerZField = new EditBox(font, innerX + LABEL_W + fieldW + 20, cursorY + 2, fieldW, FIELD_H,
                Component.literal("Z"));
        centerZField.setValue("0");
        addRenderableWidget(centerZField);

        // 按钮
        cursorY += FIELD_H + 6;
        setPosBtn = new ModernButton(innerX, cursorY, 140, BTN_H,
                Component.translatable("screen.chunkpregenerator.set_position"),
                ModernButton.Variant.SECONDARY, btn -> setCenterToPlayer());
        addRenderableWidget(setPosBtn);

        cursorY = cardTop + cardH + CARD_GAP;
    }

    /**
     * 半径卡片：半径X/Z输入 + 设世界边界按钮
     */
    private void buildRadiusCard() {
        int cardH = 64;
        int innerX = panelX + CARD_PAD;
        int fieldW = 120;
        int cardTop = cursorY;

        radiusXField = new EditBox(font, innerX + LABEL_W, cursorY + 2, fieldW, FIELD_H,
                Component.literal("半径X"));
        radiusXField.setValue("128");
        addRenderableWidget(radiusXField);

        radiusZField = new EditBox(font, innerX + LABEL_W + fieldW + 20, cursorY + 2, fieldW, FIELD_H,
                Component.literal("半径Z"));
        radiusZField.setValue("128");
        addRenderableWidget(radiusZField);

        cursorY += FIELD_H + 6;
        setBorderBtn = new ModernButton(innerX, cursorY, 140, BTN_H,
                Component.translatable("screen.chunkpregenerator.set_worldborder"),
                ModernButton.Variant.SECONDARY, btn -> setRadiusToBorder());
        addRenderableWidget(setBorderBtn);

        cursorY = cardTop + cardH + CARD_GAP;
    }

    /**
     * 操作按钮行：开始/暂停/继续/取消
     */
    private void buildActionButtons() {
        int btnW = (PANEL_WIDTH - 3 * CARD_GAP) / 4;

        startBtn = new ModernButton(panelX, cursorY, btnW, BTN_H + 4,
                Component.translatable("screen.chunkpregenerator.start"),
                ModernButton.Variant.PRIMARY, btn -> onStartPressed());
        addRenderableWidget(startBtn);

        pauseBtn = new ModernButton(panelX + btnW + CARD_GAP, cursorY, btnW, BTN_H + 4,
                Component.translatable("screen.chunkpregenerator.pause"),
                ModernButton.Variant.SECONDARY, btn -> onPausePressed());
        addRenderableWidget(pauseBtn);

        continueBtn = new ModernButton(panelX + 2 * (btnW + CARD_GAP), cursorY, btnW, BTN_H + 4,
                Component.translatable("screen.chunkpregenerator.continue_btn"),
                ModernButton.Variant.SUCCESS, btn -> onContinuePressed());
        addRenderableWidget(continueBtn);

        cancelBtn = new ModernButton(panelX + 3 * (btnW + CARD_GAP), cursorY, btnW, BTN_H + 4,
                Component.translatable("screen.chunkpregenerator.cancel"),
                ModernButton.Variant.DANGER, btn -> onCancelPressed());
        addRenderableWidget(cancelBtn);

        cursorY += BTN_H + 4 + CARD_GAP + 6;
    }

    /**
     * 进度卡片：在底部动态空间渲染
     */
    private void buildProgressCard() {
        // 进度卡片通过render方法直接绘制，不创建交互widget
        // 预留足够空间
        cursorY += 6;
    }

    // ==================== 渲染 ====================

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // 1. 亚克力毛玻璃背景
        renderBackground(gui, mouseX, mouseY, partialTick);
        ModernRenderer.renderAcrylicBackground(gui, 0, 0, width, height);

        // 2. 标题栏
        renderTitleBar(gui);

        // 3. 内容区卡片
        renderCoordinateCard(gui);
        renderRadiusCard(gui);
        renderProgressCard(gui);

        // 4. Chunky状态指示器
        renderStatusIndicator(gui);

        // 5. 渲染子组件（按钮、下拉框、文本框）
        super.render(gui, mouseX, mouseY, partialTick);
    }

    /** Win11风格标题栏 */
    private void renderTitleBar(GuiGraphics gui) {
        // 标题栏底部分隔线
        gui.fill(0, 24, width, 25, ModernColors.BORDER_SUBTLE);

        // 图标 + 标题
        String title = "⚡ " + getTitle().getString();
        gui.drawString(font, title, 12, 7, ModernColors.TEXT_PRIMARY);

        // Chunky版本信息（右侧）
        String version = bridge.isChunkyAvailable() ? "Chunky ✓" : "Chunky ✗";
        int verColor = bridge.isChunkyAvailable() ? ModernColors.ACCENT_SUCCESS : ModernColors.ACCENT_WARNING;
        gui.drawString(font, version, width - font.width(version) - 12, 7, verColor);
    }

    /** 坐标卡片背景 + 标签 */
    private void renderCoordinateCard(GuiGraphics gui) {
        int cardY = 48;
        int cardH = 64;
        ModernRenderer.renderCard(gui, panelX, cardY, PANEL_WIDTH, cardH,
                CARD_RADIUS, ModernColors.SURFACE_CARD, ModernColors.BORDER_SUBTLE);

        // 卡片标题
        int innerX = panelX + CARD_PAD;
        gui.drawString(font, "📍 中心坐标", innerX, cardY + 6, ModernColors.TEXT_SECONDARY);

        // 字段标签
        int fieldY = cardY + 24;
        gui.drawString(font, "X", innerX + 8, fieldY + 5, ModernColors.TEXT_SECONDARY);
        gui.drawString(font, "Z", innerX + LABEL_W + 140, fieldY + 5, ModernColors.TEXT_SECONDARY);

        // 输入框底板
        ModernRenderer.renderTextFieldFrame(gui, innerX + LABEL_W, fieldY + 2, 120, FIELD_H,
                centerXField.isFocused());
        ModernRenderer.renderTextFieldFrame(gui, innerX + LABEL_W + 140, fieldY + 2, 120, FIELD_H,
                centerZField.isFocused());
    }

    /** 半径卡片背景 + 标签 */
    private void renderRadiusCard(GuiGraphics gui) {
        int cardY = 118;
        int cardH = 64;
        ModernRenderer.renderCard(gui, panelX, cardY, PANEL_WIDTH, cardH,
                CARD_RADIUS, ModernColors.SURFACE_CARD, ModernColors.BORDER_SUBTLE);

        int innerX = panelX + CARD_PAD;
        gui.drawString(font, "📐 预生成范围（区块数）", innerX, cardY + 6, ModernColors.TEXT_SECONDARY);

        int fieldY = cardY + 24;
        gui.drawString(font, "X", innerX + 8, fieldY + 5, ModernColors.TEXT_SECONDARY);
        gui.drawString(font, "Z", innerX + LABEL_W + 140, fieldY + 5, ModernColors.TEXT_SECONDARY);

        ModernRenderer.renderTextFieldFrame(gui, innerX + LABEL_W, fieldY + 2, 120, FIELD_H,
                radiusXField.isFocused());
        ModernRenderer.renderTextFieldFrame(gui, innerX + LABEL_W + 140, fieldY + 2, 120, FIELD_H,
                radiusZField.isFocused());
    }

    /** 进度卡片 —— 核心视觉元素 */
    private void renderProgressCard(GuiGraphics gui) {
        int cardY = 210;
        int cardH = 76;
        ModernRenderer.renderCard(gui, panelX, cardY, PANEL_WIDTH, cardH,
                CARD_RADIUS, ModernColors.SURFACE_CARD, ModernColors.BORDER_SUBTLE);

        int innerX = panelX + CARD_PAD;
        int barW = PANEL_WIDTH - CARD_PAD * 2;

        if (progress.chunkCount() == 0 && !progress.isComplete()) {
            gui.drawString(font, "等待任务启动...", innerX, cardY + CARD_PAD + 4,
                    ModernColors.TEXT_DISABLED);
            // 空闲状态的小提示
            gui.drawString(font, "设置参数后点击「开始预生成」", innerX, cardY + CARD_PAD + 20,
                    ModernColors.TEXT_DISABLED);
            return;
        }

        // 进度条
        int barY = cardY + CARD_PAD;
        ModernRenderer.renderProgressBar(gui, innerX, barY, barW, 16,
                progress.percentComplete() / 100f, 8);

        // 百分比文字（在进度条上）
        String pctText = String.format("%.1f%%", progress.percentComplete());
        gui.drawCenteredString(font, pctText, panelX + PANEL_WIDTH / 2, barY + 4,
                ModernColors.TEXT_PRIMARY);

        // 统计数据行
        int statY = barY + 22;
        String stats = String.format("已生成 %,d 区块", progress.chunkCount());
        if (progress.totalChunks() > 0) {
            stats += String.format(" / %,d", progress.totalChunks());
        }
        gui.drawString(font, stats, innerX, statY, ModernColors.TEXT_PRIMARY);

        // 速率 + ETA
        if (!progress.isComplete() && progress.rate() > 0) {
            String rateStr = String.format("⚡ %.1f 区块/秒", progress.rate());
            gui.drawString(font, rateStr, innerX + barW - font.width(rateStr), statY,
                    ModernColors.ACCENT_SUCCESS);

            String etaStr = "⏱ " + progress.formattedTimeRemaining();
            gui.drawString(font, etaStr, innerX, statY + 14, ModernColors.TEXT_SECONDARY);

            // 当前坐标
            String coordStr = String.format("区块 (%d, %d)", progress.currentChunkX(), progress.currentChunkZ());
            gui.drawString(font, coordStr, innerX + barW - font.width(coordStr), statY + 14,
                    ModernColors.TEXT_DISABLED);
        } else if (progress.isComplete()) {
            gui.drawString(font, "✅ 预生成完成！", innerX, statY + 14, ModernColors.ACCENT_SUCCESS);
        }
    }

    /** 底部状态指示器 */
    private void renderStatusIndicator(GuiGraphics gui) {
        String status;
        int color;

        if (!bridge.isChunkyAvailable()) {
            status = "⚠ Chunky未安装 — 请安装Chunky模组以启用预生成";
            color = ModernColors.ACCENT_WARNING;
        } else if (isTaskRunning) {
            status = "● 正在预生成 " + activeWorld;
            color = ModernColors.ACCENT_SUCCESS;
        } else if (progress.isComplete()) {
            status = "● 任务已完成 — " + activeWorld;
            color = ModernColors.ACCENT_SUCCESS;
        } else {
            status = "● Chunky就绪 — 配置参数后开始预生成";
            color = ModernColors.TEXT_SECONDARY;
        }

        int statusY = height - 16;
        gui.drawCenteredString(font, status, width / 2, statusY, color);
    }

    // ==================== 事件处理 ====================

    private void onStartPressed() {
        if (!validateInput()) return;

        PregenerationConfig config = buildConfig();
        if (bridge.startTask(config)) {
            isTaskRunning = true;
            activeWorld = config.worldName();
            progress = ProgressSnapshot.empty(activeWorld);
            updateButtonStates();
        }
    }

    private void onPausePressed() {
        if (bridge.pauseTask(activeWorld)) {
            isTaskRunning = false;
            updateButtonStates();
        }
    }

    private void onContinuePressed() {
        if (bridge.continueTask(activeWorld)) {
            isTaskRunning = true;
            updateButtonStates();
        }
    }

    private void onCancelPressed() {
        if (bridge.cancelTask(activeWorld)) {
            isTaskRunning = false;
            progress = ProgressSnapshot.empty(activeWorld);
            updateButtonStates();
        }
    }

    private void onProgressReceived(ProgressSnapshot snapshot) {
        this.progress = snapshot;
        this.isTaskRunning = !snapshot.isComplete();
        if (!snapshot.isComplete()) {
            this.activeWorld = snapshot.worldName();
        }
        updateButtonStates();
    }

    private void onTaskCompleted(String worldName) {
        this.isTaskRunning = false;
        this.activeWorld = worldName;
        this.progress = ProgressSnapshot.completed(worldName, progress.chunkCount());
        updateButtonStates();
    }

    private void setCenterToPlayer() {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            centerXField.setValue(String.format("%.0f", player.getX()));
            centerZField.setValue(String.format("%.0f", player.getZ()));
        }
    }

    private void setRadiusToBorder() {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            int radius = (int) (level.getWorldBorder().getSize() / 32); // 区块数
            radiusXField.setValue(String.valueOf(radius));
            radiusZField.setValue(String.valueOf(radius));
        }
    }

    // ==================== 辅助方法 ====================

    private PregenerationConfig buildConfig() {
        ShapeType shape = shapeSelector.getValue();
        double cx = parseDouble(centerXField.getValue(), 0);
        double cz = parseDouble(centerZField.getValue(), 0);
        double rx = parseDouble(radiusXField.getValue(), 128);
        double rz = parseDouble(radiusZField.getValue(), 128);
        return new PregenerationConfig(
                worldSelector.getValue(), shape, cx, cz, rx, rz);
    }

    private boolean validateInput() {
        double rx = parseDouble(radiusXField.getValue(), -1);
        double rz = parseDouble(radiusZField.getValue(), -1);
        return rx > 0 && rz > 0 && rx <= 100000 && rz <= 100000;
    }

    private static double parseDouble(String s, double def) {
        try { return Double.parseDouble(s.trim()); }
        catch (NumberFormatException e) { return def; }
    }

    private void updateButtonStates() {
        boolean chunkyOk = bridge.isChunkyAvailable();
        startBtn.active = chunkyOk && !isTaskRunning;
        pauseBtn.active = chunkyOk && isTaskRunning;
        continueBtn.active = chunkyOk && !isTaskRunning && progress.chunkCount() > 0 && !progress.isComplete();
        cancelBtn.active = chunkyOk && isTaskRunning;
    }

    // ==================== 生命周期 ====================

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        // 监听器保持活跃以接收后台进度
    }
}

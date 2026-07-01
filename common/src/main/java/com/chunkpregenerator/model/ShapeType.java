package com.chunkpregenerator.model;

/**
 * Chunky支持的形状类型枚举。
 * 与Chunky的ShapeType一一对应，用于UI选择器和命令构建。
 */
public enum ShapeType {
    CIRCLE("circle", "圆形"),
    SQUARE("square", "方形"),
    RECTANGLE("rectangle", "矩形"),
    ELLIPSE("ellipse", "椭圆"),
    DIAMOND("diamond", "菱形"),
    PENTAGON("pentagon", "五边形"),
    HEXAGON("hexagon", "六边形"),
    OCTAGON("octagon", "八边形"),
    STAR("star", "星形");

    private final String chunkyName;
    private final String displayName;

    ShapeType(String chunkyName, String displayName) {
        this.chunkyName = chunkyName;
        this.displayName = displayName;
    }

    /** 传给Chunky API的形状名称 */
    public String chunkyName() {
        return chunkyName;
    }

    /** UI显示名称 */
    public String displayName() {
        return displayName;
    }
}

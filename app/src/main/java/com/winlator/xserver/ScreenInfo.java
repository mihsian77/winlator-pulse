package com.winlator.xserver;

import android.util.Rational;

import com.winlator.math.Mathf;

public class ScreenInfo {
    public static final short MIN_WIDTH = 320;
    public static final short MIN_HEIGHT = 160;
    public final short width;
    public final short height;
    // 显示器刷新率（Hz），0 表示自动匹配系统最高刷新率
    public int refreshRate;

    public ScreenInfo(String value) {
        String[] parts = value.split("x");
        width = Short.parseShort(parts[0]);
        height = Short.parseShort(parts[1]);
        refreshRate = 0;
    }

    public ScreenInfo(int width, int height) {
        this.width = (short)width;
        this.height = (short)height;
        refreshRate = 0;
    }

    public ScreenInfo(int width, int height, int refreshRate) {
        this.width = (short)width;
        this.height = (short)height;
        this.refreshRate = refreshRate;
    }

    public short getWidthInMillimeters() {
        return (short)(width / 10);
    }

    public short getHeightInMillimeters() {
        return (short)(height / 10);
    }

    public Rational aspectRatio() {
        return Mathf.farey((float)width / height, 10);
    }

    @Override
    public String toString() {
        return width+"x"+height;
    }
}

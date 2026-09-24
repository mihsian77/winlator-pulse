package com.winlator.widget;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.winlator.hud.WinlatorHUD;

/**
 * FrameRating —— WinlatorHUD 的兼容包装器。
 *
 * 上游原版用 frame_rating.xml（根布局 match_parent）+ TextView 实现，
 * 导致半透明背景覆盖整块桌面，且只能显示 FPS/GPU/RAM/CPU 四项。
 * 本版本改为包装自包含的 WinlatorHUD（Canvas 绘制，尺寸自适应），
 * 保留上游全部接口（Mode / setMode / update / setGPUInfo / reset / refreshSettings），
 * XServerDisplayActivity 无需任何修改。
 *
 * 修改原因：上游 HUD 简陋且布局尺寸错误（覆盖全屏）；
 * 影响范围：本文件 + 新增 com.winlator.hud.WinlatorHUD；
 * 回滚方法：恢复上游 FrameRating.java，删除 WinlatorHUD.java。
 */
public class FrameRating extends FrameLayout {

    public enum Mode {DISABLED, SIMPLE, FULL}

    private final WinlatorHUD hud;
    private Mode mode = Mode.DISABLED;

    public FrameRating(Context context) {
        this(context, null);
    }

    public FrameRating(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FrameRating(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // 不 inflate frame_rating.xml，避免 match_parent 根布局覆盖整块桌面
        setBackgroundColor(Color.TRANSPARENT);
        // 创建 WinlatorHUD 实例（自包含 Canvas 绘制，WRAP_CONTENT 自适应）
        hud = new WinlatorHUD(context, WinlatorHUD.SHOW_DEFAULT, WinlatorHUD.DENSITY_NORMAL);
        addView(hud);
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        switch (mode) {
            case DISABLED:
                hud.setVisibility(GONE);
                break;
            case SIMPLE:
                hud.setDensity(WinlatorHUD.DENSITY_COMPACT);
                hud.setVisibility(VISIBLE);
                break;
            case FULL:
                hud.setDensity(WinlatorHUD.DENSITY_DETAILED);
                hud.setVisibility(VISIBLE);
                break;
        }
    }

    /**
     * 刷新设置。WinlatorHUD 自动从系统读取指标，无需手动刷新。
     * 保留此方法仅为兼容上游接口。
     */
    public void refreshSettings() {
        // 无操作：WinlatorHUD 内部 statsThread 每 500ms 自动更新
    }

    /**
     * 设置 GPU/驱动信息，显示在 HUD 底部小字行。
     */
    public void setGPUInfo(String gpuInfo) {
        if (gpuInfo != null && !gpuInfo.isEmpty()) {
            hud.setGameInfoDirect(gpuInfo, null, null, null);
        }
    }

    /**
     * 重置 FPS 统计（1% low / 0.1% low / 帧时间图）。
     */
    public void reset() {
        hud.resetStats();
    }

    /**
     * 每帧调用，记录帧时间用于 FPS 计算。
     * 上游逻辑：窗口内容更新时自动显示 HUD（changeFrameRatingVisibility 只设 windowId，不设可见性）。
     */
    public void update() {
        hud.onFrame();
        if (getVisibility() != VISIBLE) setVisibility(VISIBLE);
    }
}

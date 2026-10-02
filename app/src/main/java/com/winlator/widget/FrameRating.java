package com.winlator.widget;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.winlator.hud.WinlatorHUD;

/**
 * FrameRating —— WinlatorHUD v3.0 的兼容包装器。
 *
 * v3.0 的 WinlatorHUD 是静态类（内部 HUDView 自动加到 decorView），
 * 本类保留上游全部接口（Mode / setMode / update / setGPUInfo / reset / refreshSettings），
 * XServerDisplayActivity 无需任何修改。
 *
 * 修改原因：上游 HUD 简陋且布局尺寸错误（frame_rating.xml 根布局 match_parent 覆盖整块桌面）；
 * 影响范围：本文件 + com.winlator.hud.WinlatorHUD（v3.0）；
 * 回滚方法：恢复上游 FrameRating.java，删除 WinlatorHUD.java。
 */
public class FrameRating extends FrameLayout {

    public enum Mode {DISABLED, SIMPLE, FULL}

    private Mode mode = Mode.DISABLED;
    private final Activity activity;

    public FrameRating(Context context) {
        this(context, null);
    }

    public FrameRating(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FrameRating(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        activity = (Activity) context;
        // 不 inflate frame_rating.xml，避免 match_parent 根布局覆盖整块桌面
        setBackgroundColor(Color.TRANSPARENT);
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        switch (mode) {
            case DISABLED:
                WinlatorHUD.setVisible(false);
                break;
            case SIMPLE:
                ensureInitialized(WinlatorHUD.DENSITY_COMPACT);
                WinlatorHUD.setDensity(WinlatorHUD.DENSITY_COMPACT);
                WinlatorHUD.setVisible(true);
                break;
            case FULL:
                ensureInitialized(WinlatorHUD.DENSITY_DETAILED);
                WinlatorHUD.setDensity(WinlatorHUD.DENSITY_DETAILED);
                WinlatorHUD.setVisible(true);
                break;
        }
    }

    private void ensureInitialized(int density) {
        if (!WinlatorHUD.isInitialized()) {
            WinlatorHUD.init(activity, WinlatorHUD.SHOW_DEFAULT, density, WinlatorHUD.ORIENT_HORIZONTAL);
        }
    }

    /**
     * 刷新设置。WinlatorHUD 内部 HandlerThread 每 500ms 自动更新指标，无需手动刷新。
     * 保留此方法仅为兼容上游接口。
     */
    public void refreshSettings() {
        // 无操作
    }

    /**
     * 设置 GPU/驱动信息，显示在 HUD 上。
     */
    public void setGPUInfo(String gpuInfo) {
        if (gpuInfo != null && !gpuInfo.isEmpty()) {
            WinlatorHUD.setGameInfo(gpuInfo, null, null);
        }
    }

    /**
     * 重置 FPS 统计。
     * 上游在 changeFrameRatingVisibility（渲染线程）找到游戏窗口后调用此方法，
     * 可见性操作必须 post 到 UI 线程，否则在渲染线程操作 View 会导致 SurfaceView 黑屏。
     */
    public void reset() {
        WinlatorHUD.reset();
        post(() -> WinlatorHUD.setVisible(true));
    }

    /**
     * 每帧调用，记录帧时间用于 FPS 计算。
     * onUpdateWindowContent 在渲染线程调用，可见性操作必须 post 到 UI 线程。
     */
    public void update() {
        WinlatorHUD.recordFrame();
        post(() -> WinlatorHUD.setVisible(true));
    }

    /**
     * 转发上游的可见性控制到 WinlatorHUD。
     */
    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        WinlatorHUD.setVisible(visibility == VISIBLE);
    }
}

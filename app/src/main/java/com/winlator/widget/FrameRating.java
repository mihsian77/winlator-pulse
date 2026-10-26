package com.winlator.widget;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.winlator.hud.WinlatorHUD;

/**
 * FrameRating — WinlatorHUD v3.9.0 兼容包装器。
 *
 * 线程安全：v3.9.0 的 init()/release() 会直接操作 decorView（addView/removeView），
 * 而 reset()/setVisibility() 可能在渲染线程（onUpdateWindowContent）被调用，
 * 因此所有 init/release 必须 post 到 UI 线程，否则 SurfaceView 黑屏。
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
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        post(() -> applyMode());
    }

    private void applyMode() {
        switch (mode) {
            case DISABLED:
                WinlatorHUD.release();
                break;
            case SIMPLE:
                WinlatorHUD.setLanguage(WinlatorHUD.LANG_ZH);
                WinlatorHUD.init(activity, WinlatorHUD.SHOW_COMPACT,
                        WinlatorHUD.DENSITY_COMPACT, WinlatorHUD.ORIENT_HORIZONTAL);
                break;
            case FULL:
                WinlatorHUD.setLanguage(WinlatorHUD.LANG_ZH);
                WinlatorHUD.init(activity, WinlatorHUD.SHOW_DETAILED,
                        WinlatorHUD.DENSITY_DETAILED, WinlatorHUD.ORIENT_HORIZONTAL);
                break;
        }
    }

    public void refreshSettings() {
    }

    public void setGPUInfo(String gpuInfo) {
        if (gpuInfo != null && !gpuInfo.isEmpty()) {
            WinlatorHUD.setGameInfo(gpuInfo, null, null);
        }
    }

    public void reset() {
        // 渲染线程调用，必须 post 到 UI 线程
        post(() -> {
            WinlatorHUD.release();
            if (mode != Mode.DISABLED) {
                int showMask = (mode == Mode.SIMPLE) ? WinlatorHUD.SHOW_COMPACT : WinlatorHUD.SHOW_DETAILED;
                int density = (mode == Mode.SIMPLE) ? WinlatorHUD.DENSITY_COMPACT : WinlatorHUD.DENSITY_DETAILED;
                WinlatorHUD.setLanguage(WinlatorHUD.LANG_ZH);
                WinlatorHUD.init(activity, showMask, density, WinlatorHUD.ORIENT_HORIZONTAL);
            }
        });
    }

    public void update() {
        WinlatorHUD.recordFrame();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        post(() -> {
            if (visibility == VISIBLE && mode != Mode.DISABLED) {
                int showMask = (mode == Mode.SIMPLE) ? WinlatorHUD.SHOW_COMPACT : WinlatorHUD.SHOW_DETAILED;
                int density = (mode == Mode.SIMPLE) ? WinlatorHUD.DENSITY_COMPACT : WinlatorHUD.DENSITY_DETAILED;
                WinlatorHUD.setLanguage(WinlatorHUD.LANG_ZH);
                WinlatorHUD.init(activity, showMask, density, WinlatorHUD.ORIENT_HORIZONTAL);
            } else {
                WinlatorHUD.release();
            }
        });
    }
}

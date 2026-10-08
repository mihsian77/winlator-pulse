package com.winlator.widget;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.winlator.hud.WinlatorHUD;

/**
 * FrameRating — WinlatorHUD v3.8.0 兼容包装器。
 * 保留上游全部接口，XServerDisplayActivity 无需修改。
 * v3.8.0 用 init() 显示、release() 隐藏，配置自动持久化到 SharedPreferences。
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
        switch (mode) {
            case DISABLED:
                WinlatorHUD.release();
                break;
            case SIMPLE:
                WinlatorHUD.init(activity, WinlatorHUD.SHOW_COMPACT,
                        WinlatorHUD.DENSITY_COMPACT, WinlatorHUD.ORIENT_HORIZONTAL);
                break;
            case FULL:
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
        WinlatorHUD.release();
        if (mode != Mode.DISABLED) {
            int showMask = (mode == Mode.SIMPLE) ? WinlatorHUD.SHOW_COMPACT : WinlatorHUD.SHOW_DETAILED;
            int density = (mode == Mode.SIMPLE) ? WinlatorHUD.DENSITY_COMPACT : WinlatorHUD.DENSITY_DETAILED;
            WinlatorHUD.init(activity, showMask, density, WinlatorHUD.ORIENT_HORIZONTAL);
        }
    }

    public void update() {
        WinlatorHUD.recordFrame();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == VISIBLE) {
            if (mode != Mode.DISABLED) {
                int showMask = (mode == Mode.SIMPLE) ? WinlatorHUD.SHOW_COMPACT : WinlatorHUD.SHOW_DETAILED;
                int density = (mode == Mode.SIMPLE) ? WinlatorHUD.DENSITY_COMPACT : WinlatorHUD.DENSITY_DETAILED;
                WinlatorHUD.init(activity, showMask, density, WinlatorHUD.ORIENT_HORIZONTAL);
            }
        } else {
            WinlatorHUD.release();
        }
    }
}

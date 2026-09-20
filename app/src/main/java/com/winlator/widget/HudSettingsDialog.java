package com.winlator.widget;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

/**
 * HUD 设置对话框
 * 让用户勾选要显示的 HUD 元素
 */
public class HudSettingsDialog extends Dialog {

    public static final String[][] ELEMENTS = {
        {"fps", "FPS（每秒帧数）"},
        {"gpu_load", "GPU 负载"},
        {"cpu_load", "CPU 负载"},
        {"ram", "内存使用"},
        {"battery", "电池电量"},
        {"gpu_temp", "GPU 温度"},
        {"cpu_temp", "CPU 温度"},
        {"frame_graph", "帧时间图"},
        {"engine", "渲染引擎"},
        {"box64_version", "Box64 版本"},
        {"dxvk_version", "DXVK 版本"},
        {"driver_version", "驱动版本"},
        {"vram", "显存使用"},
        {"cpu_mhz", "CPU 频率"},
        {"gpu_clock", "GPU 频率"},
        {"cpu_cores", "CPU 核心"},
        {"network", "网络速度"},
        {"swap", "Swap 分区"},
        {"resolution", "分辨率"},
        {"wine_version", "Wine 版本"},
        {"duration", "运行时长"},
        {"clock", "当前时间"},
        {"throttle", "降频状态"}
    };

    private final OnSettingsChangedListener listener;

    public interface OnSettingsChangedListener {
        void onSettingsChanged();
    }

    public HudSettingsDialog(Context context, OnSettingsChangedListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("HUD 设置");

        Context context = getContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        ScrollView scrollView = new ScrollView(context);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 32);

        TextView desc = new TextView(context);
        desc.setText("勾选要显示的 HUD 元素，立即生效");
        desc.setTextSize(13);
        desc.setPadding(0, 0, 0, 24);
        layout.addView(desc);

        for (String[] element : ELEMENTS) {
            String key = element[0];
            String name = element[1];
            boolean enabled = prefs.getBoolean("hud_" + key, isDefaultEnabled(key));

            Switch sw = new Switch(context);
            sw.setText(name);
            sw.setChecked(enabled);
            sw.setPadding(0, 12, 0, 12);
            sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("hud_" + key, isChecked).apply();
                if (listener != null) listener.onSettingsChanged();
            });
            layout.addView(sw);
        }

        scrollView.addView(layout);
        setContentView(scrollView);
        getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private boolean isDefaultEnabled(String key) {
        switch (key) {
            case "fps":
            case "gpu_load":
            case "cpu_load":
            case "ram":
            case "battery":
            case "gpu_temp":
            case "cpu_temp":
            case "frame_graph":
            case "engine":
            case "box64_version":
            case "dxvk_version":
            case "driver_version":
                return true;
            default:
                return false;
        }
    }
}

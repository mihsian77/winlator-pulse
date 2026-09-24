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
        {"frame_graph", "帧时间图"},
        {"engine", "GPU 信息与渲染引擎"},
        {"ram", "内存使用"},
        {"cpu_load", "CPU 频率与 Box64 版本"},
        {"battery", "温度"}
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
            case "frame_graph":
            case "engine":
            case "ram":
            case "cpu_load":
            case "battery":
                return true;
            default:
                return false;
        }
    }
}

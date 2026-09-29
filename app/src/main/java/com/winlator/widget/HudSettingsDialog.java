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
        {"fps", "FPS\uFF08\u6BCF\u79D2\u5E27\u6570\uFF09"},
        {"frame_graph", "\u5E27\u65F6\u95F4\u56FE"},
        {"engine", "GPU \u4FE1\u606F\u4E0E\u6E32\u67D3\u5F15\u64CE"},
        {"ram", "\u5185\u5B58\u4F7F\u7528"},
        {"cpu_load", "CPU \u9891\u7387\u4E0E Box64 \u7248\u672C"},
        {"battery", "\u6E29\u5EA6"}
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
        setTitle("HUD \u8BBE\u7F6E");

        Context context = getContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        ScrollView scrollView = new ScrollView(context);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 32);

        TextView desc = new TextView(context);
        desc.setText("\u52FE\u9009\u8981\u663E\u793A\u7684 HUD \u5143\u7D20\uFF0C\u7ACB\u5373\u751F\u6548");
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
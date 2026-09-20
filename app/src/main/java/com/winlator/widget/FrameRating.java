package com.winlator.widget;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.winlator.R;
import com.winlator.box64.Box64Utils;
import com.winlator.core.CPUStatus;
import com.winlator.core.StringUtils;

import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import java.util.Locale;

/**
 * 升级版性能 HUD
 * 新增：帧时间柱状图、电池温度、帧率颜色编码（绿/黄/红）
 */
public class FrameRating extends FrameLayout implements Runnable {
    public enum Mode {DISABLED, SIMPLE, FULL}
    private long lastTime = 0;
    private short frameCount = 0;
    private float lastFPS = 0;
    private long lastFrameTimeNs = 0;
    private float lastFrameTimeMs = 0;
    private final float[] frameTimeHistory = new float[60];
    private int frameTimeIndex = 0;
    private final LinearLayout fpsPanel;
    private final LinearLayout gpuPanel;
    private final LinearLayout ramPanel;
    private final LinearLayout cpuPanel;
    private final FrameTimeChart frameTimeChart;
    private final TextView batteryView;
    private Mode mode = Mode.SIMPLE;
    private ActivityManager activityManager;
    private ActivityManager.MemoryInfo memoryInfo;
    private String cpuInfo = null;
    private byte tick = 0;

    public FrameRating(Context context) {
        this(context, null);
    }

    public FrameRating(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FrameRating(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        View view = LayoutInflater.from(context).inflate(R.layout.frame_rating, this, false);
        fpsPanel = view.findViewById(R.id.LLFPSPanel);
        gpuPanel = view.findViewById(R.id.LLGPUPanel);
        ramPanel = view.findViewById(R.id.LLRAMPanel);
        cpuPanel = view.findViewById(R.id.LLCPUPanel);

        // 帧时间柱状图和电池温度（新增，放在 fpsPanel 下方）
        frameTimeChart = new FrameTimeChart(context);
        batteryView = new TextView(context);
        batteryView.setTextSize(10);
        batteryView.setTextColor(Color.WHITE);
        batteryView.setPadding(4, 2, 4, 2);
        LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 30);
        frameTimeChart.setLayoutParams(chartParams);
        ((LinearLayout) fpsPanel.getParent()).addView(frameTimeChart);
        ((LinearLayout) fpsPanel.getParent()).addView(batteryView);

        addView(view);
        setupPanels();
    }

    private void setupPanels() {
        switch (mode) {
            case DISABLED:
                fpsPanel.setVisibility(GONE);
                gpuPanel.setVisibility(GONE);
                ramPanel.setVisibility(GONE);
                cpuPanel.setVisibility(GONE);
                frameTimeChart.setVisibility(GONE);
                batteryView.setVisibility(GONE);
                activityManager = null;
                memoryInfo = null;
                break;
            case SIMPLE:
                fpsPanel.setVisibility(VISIBLE);
                gpuPanel.setVisibility(GONE);
                ramPanel.setVisibility(GONE);
                cpuPanel.setVisibility(GONE);
                frameTimeChart.setVisibility(VISIBLE);
                batteryView.setVisibility(GONE);
                activityManager = null;
                memoryInfo = null;
                break;
            case FULL:
                fpsPanel.setVisibility(VISIBLE);
                gpuPanel.setVisibility(VISIBLE);
                ramPanel.setVisibility(VISIBLE);
                cpuPanel.setVisibility(VISIBLE);
                frameTimeChart.setVisibility(VISIBLE);
                batteryView.setVisibility(VISIBLE);
                Context context = getContext();
                activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
                memoryInfo = new ActivityManager.MemoryInfo();
                break;
        }
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        setupPanels();
    }

    /** 按 HUD 设置对话框的勾选动态刷新显示 */
    public void refreshSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean showFps = prefs.getBoolean("hud_fps", true);
        boolean showFrameGraph = prefs.getBoolean("hud_frame_graph", true);
        fpsPanel.setVisibility(showFps ? VISIBLE : GONE);
        frameTimeChart.setVisibility(showFrameGraph && showFps ? VISIBLE : GONE);
    }

    public void setGPUInfo(String gpuInfo) {
        post(() -> ((TextView)gpuPanel.getChildAt(1)).setText(gpuInfo));
    }

    public void reset() {
        frameCount = 0;
        lastTime = SystemClock.elapsedRealtime();
        lastFPS = 0;
        tick = 2;
        frameTimeIndex = 0;
        for (int i = 0; i < 60; i++) frameTimeHistory[i] = 0;
    }

    public void update() {
        long now = System.nanoTime();
        if (lastFrameTimeNs != 0) {
            lastFrameTimeMs = (now - lastFrameTimeNs) / 1_000_000.0f;
            frameTimeHistory[frameTimeIndex] = lastFrameTimeMs;
            frameTimeIndex = (frameTimeIndex + 1) % 60;
        }
        lastFrameTimeNs = now;

        long time = SystemClock.elapsedRealtime();
        if (time >= lastTime + 500) {
            lastFPS = ((float)(frameCount * 1000) / (time - lastTime));
            post(this);
            lastTime = time;
            frameCount = 0;
        }
        frameCount++;
    }

    @Override
    public void run() {
        if (getVisibility() == GONE) setVisibility(View.VISIBLE);

        // 帧率颜色编码：绿>=50, 黄>=30, 红<30
        int fpsColor;
        if (lastFPS >= 50) fpsColor = Color.parseColor("#35D0BA");
        else if (lastFPS >= 30) fpsColor = Color.parseColor("#FFB020");
        else fpsColor = Color.parseColor("#FF5A5A");

        TextView fpsText = (TextView) fpsPanel.getChildAt(1);
        fpsText.setText(String.format(Locale.ENGLISH, "%.1f", lastFPS));
        fpsText.setTextColor(fpsColor);

        // 刷新柱状图
        frameTimeChart.invalidate();

        if (mode == Mode.FULL && ++tick >= 2) {
            tick = 0;
            activityManager.getMemoryInfo(memoryInfo);
            long usedMem = memoryInfo.totalMem - memoryInfo.availMem;
            String ramText = StringUtils.formatBytes(usedMem, false)+"/"+StringUtils.formatBytes(memoryInfo.totalMem);
            ((TextView)ramPanel.getChildAt(1)).setText(ramText);

            if (cpuInfo == null) {
                cpuInfo = "Box64 v"+ Box64Utils.extractBinVersion(cpuPanel.getContext());
            }

            short[] clockSpeeds = CPUStatus.getCurrentClockSpeeds();
            int maxClockSpeed = 0;
            for (short clockSpeed : clockSpeeds) maxClockSpeed = Math.max(maxClockSpeed, clockSpeed);
            ((TextView)cpuPanel.getChildAt(1)).setText(CPUStatus.formatClockSpeed(maxClockSpeed)+" | "+cpuInfo);

            // 电池温度（通过 sticky broadcast 获取，BATTERY_PROPERTY_TEMPERATURE 不存在）
            android.content.Intent batteryIntent = getContext().registerReceiver(null, new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
            if (batteryIntent != null) {
                int tempTenths = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0);
                float tempC = tempTenths / 10.0f;
                batteryView.setText(String.format(Locale.ENGLISH, "%.0f°C", tempC));
            }
        }
    }

    /**
     * 帧时间柱状图 View：显示最近 60 帧的帧时间
     * 绿色=快(<16ms), 黄色=中(16-33ms), 红色=慢(>33ms)
     */
    private class FrameTimeChart extends View {
        private final Paint barPaint = new Paint();
        private final Paint linePaint = new Paint();

        public FrameTimeChart(Context context) {
            super(context);
            barPaint.setAntiAlias(true);
            linePaint.setColor(Color.parseColor("#66FFFFFF"));
            linePaint.setStrokeWidth(1);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            float barWidth = w / 60.0f;

            // 60fps 线（16.67ms）
            float y60 = h * (16.67f / 50.0f);
            canvas.drawLine(0, y60, w, y60, linePaint);

            // 绘制柱状图
            for (int i = 0; i < 60; i++) {
                float ft = frameTimeHistory[(frameTimeIndex + i) % 60];
                if (ft <= 0) continue;
                float barH = Math.min(ft / 50.0f, 1.0f) * h;
                float x = i * barWidth;

                if (ft < 16.67f) barPaint.setColor(Color.parseColor("#35D0BA"));
                else if (ft < 33.33f) barPaint.setColor(Color.parseColor("#FFB020"));
                else barPaint.setColor(Color.parseColor("#FF5A5A"));

                canvas.drawRect(x, h - barH, x + barWidth - 1, h, barPaint);
            }
        }
    }
}

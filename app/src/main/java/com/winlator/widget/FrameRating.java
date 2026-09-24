package com.winlator.widget;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
 * MangoHud 风格性能 HUD
 * 半透明黑底、可拖动、双击切换紧凑/完整模式
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
    private final TextView extraView;
    private Mode mode = Mode.SIMPLE;
    private ActivityManager activityManager;
    private ActivityManager.MemoryInfo memoryInfo;
    private String cpuInfo = null;
    private byte tick = 0;
    private long startTime = 0;
    private boolean compactMode = false;

    // 拖动相关
    private float downRawX, downRawY;
    private float startTransX, startTransY;
    private boolean isDragging;
    private final GestureDetector gestureDetector;

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

        frameTimeChart = new FrameTimeChart(context);
        batteryView = new TextView(context);
        batteryView.setTextSize(10);
        batteryView.setTextColor(Color.WHITE);
        batteryView.setPadding(4, 2, 4, 2);

        // 新增：电池百分比 + 运行时间
        extraView = new TextView(context);
        extraView.setTextSize(10);
        extraView.setTextColor(Color.parseColor("#AAAAAA"));
        extraView.setPadding(4, 2, 4, 2);

        LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 30);
        frameTimeChart.setLayoutParams(chartParams);
        ((LinearLayout) fpsPanel.getParent()).addView(frameTimeChart);
        ((LinearLayout) fpsPanel.getParent()).addView(batteryView);
        ((LinearLayout) fpsPanel.getParent()).addView(extraView);

        addView(view);

        // MangoHud 风格：半透明黑底圆角
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#80000000"));
        bg.setCornerRadius(8);
        setBackground(bg);
        setPadding(8, 6, 8, 6);

        // 双击切换紧凑/完整
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                compactMode = !compactMode;
                refreshSettings();
                return true;
            }
        });

        setupPanels();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                startTransX = getTranslationX();
                startTransY = getTranslationY();
                isDragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - downRawX;
                float dy = event.getRawY() - downRawY;
                if (Math.abs(dx) > 10 || Math.abs(dy) > 10) isDragging = true;
                if (isDragging) {
                    setTranslationX(startTransX + dx);
                    setTranslationY(startTransY + dy);
                }
                return true;
            case MotionEvent.ACTION_UP:
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void setupPanels() {
        if (mode == Mode.DISABLED) {
            fpsPanel.setVisibility(GONE);
            gpuPanel.setVisibility(GONE);
            ramPanel.setVisibility(GONE);
            cpuPanel.setVisibility(GONE);
            frameTimeChart.setVisibility(GONE);
            batteryView.setVisibility(GONE);
            extraView.setVisibility(GONE);
            activityManager = null;
            memoryInfo = null;
            return;
        }
        refreshSettings();
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        setupPanels();
    }

    /** 按 HUD 设置对话框的勾选 + 紧凑/完整模式，控制每个面板显隐 */
    public void refreshSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean showFps = prefs.getBoolean("hud_fps", true);
        boolean showFrameGraph = prefs.getBoolean("hud_frame_graph", true);
        boolean showGpu = prefs.getBoolean("hud_engine", true);
        boolean showRam = prefs.getBoolean("hud_ram", true);
        boolean showCpu = prefs.getBoolean("hud_cpu_load", true);
        boolean showTemp = prefs.getBoolean("hud_battery", true);

        fpsPanel.setVisibility(showFps ? VISIBLE : GONE);
        frameTimeChart.setVisibility((showFrameGraph && showFps && !compactMode) ? VISIBLE : GONE);
        gpuPanel.setVisibility((showGpu && !compactMode) ? VISIBLE : GONE);
        ramPanel.setVisibility((showRam && !compactMode) ? VISIBLE : GONE);
        cpuPanel.setVisibility((showCpu && !compactMode) ? VISIBLE : GONE);
        batteryView.setVisibility((showTemp && !compactMode) ? VISIBLE : GONE);
        extraView.setVisibility(compactMode ? GONE : VISIBLE);

        if (showRam || showCpu || showTemp || !compactMode) {
            if (activityManager == null) {
                activityManager = (ActivityManager)getContext().getSystemService(Context.ACTIVITY_SERVICE);
                memoryInfo = new ActivityManager.MemoryInfo();
            }
        }
    }

    public void setGPUInfo(String gpuInfo) {
        post(() -> ((TextView)gpuPanel.getChildAt(1)).setText(gpuInfo));
    }

    public void reset() {
        frameCount = 0;
        lastTime = SystemClock.elapsedRealtime();
        lastFPS = 0;
        tick = 2;
        startTime = SystemClock.elapsedRealtime();
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

        int fpsColor;
        if (lastFPS >= 50) fpsColor = Color.parseColor("#35D0BA");
        else if (lastFPS >= 30) fpsColor = Color.parseColor("#FFB020");
        else fpsColor = Color.parseColor("#FF5A5A");

        TextView fpsText = (TextView) fpsPanel.getChildAt(1);
        fpsText.setText(String.format(Locale.ENGLISH, "%.1f", lastFPS));
        fpsText.setTextColor(fpsColor);

        frameTimeChart.invalidate();

        boolean needExtra = !compactMode && (
                ramPanel.getVisibility() == VISIBLE
                || cpuPanel.getVisibility() == VISIBLE
                || batteryView.getVisibility() == VISIBLE);
        if (needExtra && ++tick >= 2) {
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

            android.content.Intent batteryIntent = getContext().registerReceiver(null, new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
            if (batteryIntent != null) {
                int tempTenths = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0);
                float tempC = tempTenths / 10.0f;
                int level = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
                String battText = String.format(Locale.ENGLISH, "%.0f°C", tempC);
                if (level >= 0 && scale > 0) {
                    battText += " · " + Math.round(level * 100f / scale) + "%";
                }
                batteryView.setText(battText);

                // 运行时间
                long elapsed = (SystemClock.elapsedRealtime() - startTime) / 1000;
                long h = elapsed / 3600, m = (elapsed % 3600) / 60, s = elapsed % 60;
                extraView.setText(String.format(Locale.ENGLISH, "运行 %02d:%02d:%02d", h, m, s));
            }
        }
    }

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

            float y60 = h * (16.67f / 50.0f);
            canvas.drawLine(0, y60, w, y60, linePaint);

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

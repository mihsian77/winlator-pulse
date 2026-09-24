package com.winlator.hud;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.TrafficStats;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * WinlatorHUD v2.0 Ultimate —— 自包含单文件性能监控 HUD。
 *
 * <p>集成 6 个 Winlator 项目的优点：</p>
 * <ul>
 *   <li>横屏/竖屏双布局 + 双击切换（上游 Ludashi WinlatorHUD）</li>
 *   <li>GPU 路径自动发现 16+ 路径（Bannerlator HudMetrics）</li>
 *   <li>FPS stale 检测 1.5s 无帧→0（Bannerlator FpsCounter）</li>
 *   <li>VRAM 三模式探测（WinNative MangoHudView）</li>
 *   <li>零 GC 文本格式化（WinNative MangoHudView）</li>
 *   <li>3 种密度模式 精简/标准/详细（融合 Bannerlator + 上游）</li>
 *   <li>Thermal zone 优先级排序（Bannerlator HudMetrics）</li>
 *   <li>显示刷新率 @120Hz（WinNative MangoHudView）</li>
 *   <li>电池剩余时间平滑计算（CronyX SensorReader）</li>
 *   <li>拖拽 + 长按锁定（已有）</li>
 * </ul>
 *
 * <p>使用方法（3 步）：</p>
 * <pre>
 * WinlatorHUD.init(activity);
 * // 在渲染循环中每帧调用
 * WinlatorHUD.recordFrame();
 * // 可选：设置游戏信息
 * WinlatorHUD.setGameInfo("DXVK", "1920x1080", "Wine 9.0");
 * </pre>
 *
 * <p>手势：点击循环密度模式，双击切换横屏/竖屏，拖拽移动，长按 3.5s 锁定。</p>
 */
public class WinlatorHUD extends View {

    // ==================== 版本 ====================
    public static final String VERSION = "2.0.0-Ultimate";

    // ==================== 元素位掩码（参考上游 Ludashi） ====================
    public static final int SHOW_FPS          = 1 << 0;
    public static final int SHOW_FRAMETIME    = 1 << 1;
    public static final int SHOW_AVG_FPS      = 1 << 2;
    public static final int SHOW_1PC_LOW      = 1 << 3;
    public static final int SHOW_01PC_LOW     = 1 << 4;
    public static final int SHOW_GRAPH        = 1 << 5;
    public static final int SHOW_GPU_LOAD     = 1 << 6;
    public static final int SHOW_GPU_TEMP     = 1 << 7;
    public static final int SHOW_GPU_CLOCK    = 1 << 8;
    public static final int SHOW_VRAM         = 1 << 9;
    public static final int SHOW_CPU_LOAD     = 1 << 10;
    public static final int SHOW_CPU_TEMP     = 1 << 11;
    public static final int SHOW_CPU_CLOCK    = 1 << 12;
    public static final int SHOW_CPU_CORES    = 1 << 13;
    public static final int SHOW_RAM          = 1 << 14;
    public static final int SHOW_SWAP         = 1 << 15;
    public static final int SHOW_BATTERY      = 1 << 16;
    public static final int SHOW_NETWORK      = 1 << 17;
    public static final int SHOW_ENGINE       = 1 << 18;
    public static final int SHOW_RESOLUTION   = 1 << 19;
    public static final int SHOW_WINE_VERSION = 1 << 20;
    public static final int SHOW_DURATION     = 1 << 21;
    public static final int SHOW_THROTTLE     = 1 << 22;
    public static final int SHOW_BATTERY_TIME = 1 << 23;

    public static final int SHOW_DEFAULT = SHOW_FPS | SHOW_FRAMETIME | SHOW_AVG_FPS
            | SHOW_1PC_LOW | SHOW_01PC_LOW | SHOW_GRAPH
            | SHOW_GPU_LOAD | SHOW_GPU_TEMP
            | SHOW_CPU_LOAD | SHOW_CPU_TEMP
            | SHOW_RAM | SHOW_BATTERY | SHOW_ENGINE;

    public static final int SHOW_COMPACT = SHOW_FPS | SHOW_GPU_LOAD | SHOW_CPU_LOAD | SHOW_RAM;
    public static final int SHOW_DETAILED = (1 << 24) - 1; // 全部开启

    // ==================== 密度模式 ====================
    public static final int DENSITY_COMPACT  = 0;
    public static final int DENSITY_NORMAL   = 1;
    public static final int DENSITY_DETAILED = 2;
    private static final String[] DENSITY_NAMES = {"精简", "标准", "详细"};

    // ==================== 颜色 ====================
    private static final int C_BG      = 0xCC000000;
    private static final int C_TEXT    = 0xFFFFFFFF;
    private static final int C_GPU     = 0xFF2E9762;
    private static final int C_CPU     = 0xFF2E97CB;
    private static final int C_VRAM    = 0xFFAD64C1;
    private static final int C_RAM     = 0xFFC26693;
    private static final int C_BAT     = 0xFFFF9078;
    private static final int C_ENGINE  = 0xFFEB5B5B;
    private static final int C_NET     = 0xFFE07B85;
    private static final int C_GRAPH   = 0xFF00FF00;
    private static final int C_OUTLINE = 0xFF000000;
    private static final int C_WARN    = 0xFFFDFD09;
    private static final int C_CRIT    = 0xFFB22222;

    // ==================== 尺寸常量 ====================
    private static final float BASE_TEXT_DP = 14f;
    private static final long TICK_MS = 500L;
    private static final int GRAPH_SAMPLES = 200;
    private static final float GRAPH_CEIL_MS = 50f;
    private static final long STALE_FPS_NS = 1_500_000_000L; // 1.5s 无帧→0
    private static final float TAP_SLOP = 20f;
    private static final long DOUBLE_TAP_MS = 300L;
    private static final long LOCK_HOLD_MS = 3500L;

    // ==================== GPU 路径（参考 Bannerlator HudMetrics） ====================
    private static final String[] GPU_USAGE_STATIC_PATHS = {
        "/sys/class/kgsl/kgsl-3d0/gpubusy",
        "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage",
        "/sys/class/kgsl/kgsl-3d0/devfreq/gpu_load",
        "/sys/class/misc/mali0/device/utilisation",
        "/sys/class/misc/mali0/device/utilization",
        "/sys/class/misc/mali0/device/gpuinfo",
        "/sys/devices/platform/mali/utilization",
        "/sys/kernel/gpu/gpu_busy",
        "/sys/devices/platform/gpusysfs/gpu_busy",
        "/sys/class/misc/pvrsrvkm/device/utilisation",
        "/sys/class/pvr/utilisation",
        "/sys/class/pvr/gpu_utilisation",
        "/sys/class/drm/card0/device/gpu_busy_percent",
        "/sys/class/devfreq/gpu/load",
        "/sys/kernel/ged/hal/gpu_utilization",
        "/sys/module/ged/parameters/gpu_loading"
    };
    private static final String[] GPU_USAGE_FILES = {
        "gpu_busy_percentage", "gpu_busy_percent", "gpu_load",
        "utilisation", "utilization", "load", "gpu_busy", "gpuinfo"
    };
    private static final String[] GPU_NODE_TOKENS = {
        "gpu", "mali", "g3d", "kgsl", "panfrost", "pvr", "powervr", "xclipse", "sgpu"
    };

    // ==================== 静态单例 ====================
    private static volatile WinlatorHUD instance;
    private static volatile boolean initialized;

    // ==================== 游戏信息 ====================
    private volatile String engineName = "";
    private volatile String engineVersion = "";
    private volatile String resolution = "";
    private volatile String wineVersion = "";

    // ==================== 配置 ====================
    private int showMask = SHOW_DEFAULT;
    private int density = DENSITY_NORMAL;
    private boolean vertical = false;
    private boolean locked = false;
    private float scaleFactor = 0.75f;
    private float textAlpha = 1.0f;
    private float bgAlpha = 0.7f;
    private int posX = -1, posY = -1;

    // ==================== FPS 追踪 ====================
    private final FrameTracker frameTracker = new FrameTracker();

    // ==================== 系统指标 ====================
    private final SystemMetrics metrics;

    // ==================== 绘制相关 ====================
    private final Paint bgPaint = new Paint();
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint graphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bgRect = new RectF();

    private float textSize, smallSize, rowH, smallRowH, baseline, smallBaseline;
    private float pad, charW, smallCharW, labelColW, graphH;
    private int panelW = 1, panelH = 1;

    // ==================== 零 GC 文本缓冲区（参考 WinNative） ====================
    private final StringBuilder sbFps = new StringBuilder(8);
    private final StringBuilder sbMs = new StringBuilder(8);
    private final StringBuilder sbAvg = new StringBuilder(8);
    private final StringBuilder sbLow1 = new StringBuilder(8);
    private final StringBuilder sbLow01 = new StringBuilder(8);
    private final StringBuilder sbGpuLoad = new StringBuilder(4);
    private final StringBuilder sbGpuTemp = new StringBuilder(4);
    private final StringBuilder sbGpuClk = new StringBuilder(6);
    private final StringBuilder sbVram = new StringBuilder(6);
    private final StringBuilder sbCpuLoad = new StringBuilder(4);
    private final StringBuilder sbCpuTemp = new StringBuilder(4);
    private final StringBuilder sbCpuClk = new StringBuilder(6);
    private final StringBuilder sbRam = new StringBuilder(6);
    private final StringBuilder sbRamPct = new StringBuilder(4);
    private final StringBuilder sbSwap = new StringBuilder(6);
    private final StringBuilder sbBatPct = new StringBuilder(4);
    private final StringBuilder sbBatTemp = new StringBuilder(4);
    private final StringBuilder sbBatW = new StringBuilder(6);
    private final StringBuilder sbBatTime = new StringBuilder(8);
    private final StringBuilder sbNetRx = new StringBuilder(6);
    private final StringBuilder sbNetTx = new StringBuilder(6);
    private final StringBuilder sbDuration = new StringBuilder(12);
    private final StringBuilder sbMinMax = new StringBuilder(20);

    // ==================== 线程 ====================
    private HandlerThread statsThread;
    private Handler statsHandler;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable tickRunnable = new Runnable() {
        @Override public void run() {
            Handler h = statsHandler;
            if (h == null) return;
            if (getVisibility() == VISIBLE) {
                metrics.update();
                formatAll();
                postInvalidate();
            }
            h.postDelayed(this, TICK_MS);
        }
    };

    // ==================== 触摸状态 ====================
    private int activePointerId = -1;
    private float touchOffsetX, touchOffsetY, downRawX, downRawY;
    private long downTime;
    private long lastTapTime = 0;
    private boolean dragging;
    private final Runnable lockRunnable = new Runnable() {
        @Override public void run() {
            locked = true;
            activePointerId = -1;
            try { performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS); } catch (Exception ignored) {}
            postInvalidate();
        }
    };

    private final long sessionStartMs = SystemClock.elapsedRealtime();

    // ==================== 静态 API ====================

    public static WinlatorHUD init(Activity activity) {
        return init(activity, SHOW_DEFAULT, DENSITY_NORMAL);
    }

    public static WinlatorHUD init(Activity activity, int showMask, int density) {
        if (activity == null) throw new IllegalArgumentException("activity must not be null");
        synchronized (WinlatorHUD.class) {
            if (initialized) return instance;
            WinlatorHUD hud = new WinlatorHUD(activity, showMask, density);
            ViewGroup root = (ViewGroup) activity.getWindow().getDecorView();
            root.addView(hud);
            instance = hud;
            initialized = true;
            return hud;
        }
    }

    public static WinlatorHUD getInstance() { return instance; }

    public static void recordFrame() {
        WinlatorHUD h = instance;
        if (h != null) h.frameTracker.recordFrame(System.nanoTime());
    }

    // 实例方法：供 FrameRating 包装器直接调用（不走静态单例）
    public void onFrame() {
        frameTracker.recordFrame(System.nanoTime());
    }

    // 实例方法：设置游戏/引擎信息
    public void setGameInfoDirect(String engine, String engineVersion, String resolution, String wineVersion) {
        this.engineName = engine != null ? engine : "";
        this.engineVersion = engineVersion != null ? engineVersion : "";
        this.resolution = resolution != null ? resolution : "";
        this.wineVersion = wineVersion != null ? wineVersion : "";
        postInvalidate();
    }

    public static void setGameInfo(String engine, String resolution, String wineVersion) {
        setGameInfo(engine, null, resolution, wineVersion);
    }

    public static void setGameInfo(String engine, String engineVersion, String resolution, String wineVersion) {
        WinlatorHUD h = instance;
        if (h != null) {
            h.engineName = engine != null ? engine : "";
            h.engineVersion = engineVersion != null ? engineVersion : "";
            h.resolution = resolution != null ? resolution : "";
            h.wineVersion = wineVersion != null ? wineVersion : "";
            h.postInvalidate();
        }
    }

    public static void show() { setVisible(true); }
    public static void hide() { setVisible(false); }

    public static void setVisible(boolean visible) {
        WinlatorHUD h = instance;
        if (h != null) h.setVisibility(visible ? VISIBLE : GONE);
    }

    public static boolean isInitialized() { return initialized; }

    public static void release() {
        synchronized (WinlatorHUD.class) {
            WinlatorHUD h = instance;
            if (h != null && h.getParent() != null) {
                ((ViewGroup) h.getParent()).removeView(h);
            }
            instance = null;
            initialized = false;
        }
    }

    // ==================== 构造函数 ====================

    public WinlatorHUD(Context context, int showMask, int density) {
        super(context);
        this.showMask = showMask;
        this.density = density;
        this.metrics = new SystemMetrics(context);

        Typeface mono;
        try {
            mono = Typeface.create(Typeface.createFromFile("/system/fonts/DroidSansMono.ttf"), Typeface.BOLD);
        } catch (Exception e) {
            mono = Typeface.create("monospace", Typeface.BOLD);
        }
        valuePaint.setTypeface(mono);
        valuePaint.setColor(C_TEXT);
        labelPaint.setTypeface(mono);
        smallPaint.setTypeface(mono);
        smallPaint.setColor(C_TEXT);
        outlinePaint.setTypeface(mono);
        outlinePaint.setColor(C_OUTLINE);
        outlinePaint.setStyle(Paint.Style.STROKE);
        graphPaint.setColor(C_GRAPH);
        graphPaint.setStyle(Paint.Style.STROKE);

        setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setElevation(1000f);
        applyScale();
        applyAlphas();
        computeLayout();
        setVisibility(VISIBLE);
    }

    // ==================== 配置方法 ====================

    public void setShowMask(int mask) { this.showMask = mask; computeLayout(); postInvalidate(); }
    public int getShowMask() { return showMask; }

    public void setDensity(int density) {
        this.density = Math.max(DENSITY_COMPACT, Math.min(DENSITY_DETAILED, density));
        applyDensityMask();
        computeLayout();
        postInvalidate();
    }
    public int getDensity() { return density; }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
        computeLayout();
        postInvalidate();
    }
    public boolean isVertical() { return vertical; }

    public void setScale(float scale) {
        scaleFactor = Math.max(0.5f, Math.min(1.5f, scale));
        applyScale();
        computeLayout();
        postInvalidate();
    }

    public void setTextAlpha(float alpha) {
        textAlpha = Math.max(0f, Math.min(1f, alpha));
        applyAlphas();
        postInvalidate();
    }

    public void setBackgroundAlpha(float alpha) {
        bgAlpha = Math.max(0f, Math.min(1f, alpha));
        applyAlphas();
        postInvalidate();
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        if (locked) { removeCallbacks(lockRunnable); activePointerId = -1; }
        postInvalidate();
    }

    public void resetStats() { frameTracker.reset(); }

    private void applyDensityMask() {
        switch (density) {
            case DENSITY_COMPACT:  showMask = SHOW_COMPACT; break;
            case DENSITY_NORMAL:   showMask = SHOW_DEFAULT; break;
            case DENSITY_DETAILED: showMask = SHOW_DETAILED; break;
        }
    }

    private void applyScale() {
        float density = getResources().getDisplayMetrics().density;
        textSize = BASE_TEXT_DP * density * scaleFactor;
        smallSize = textSize * 0.55f;
        valuePaint.setTextSize(textSize);
        labelPaint.setTextSize(textSize);
        smallPaint.setTextSize(smallSize);
        outlinePaint.setStrokeWidth(Math.max(1.5f, textSize * 0.08f));
        graphPaint.setStrokeWidth(Math.max(1.5f, textSize * 0.07f));

        Paint.FontMetrics fm = valuePaint.getFontMetrics();
        rowH = (fm.descent - fm.ascent) * 1.02f;
        baseline = -fm.ascent;
        Paint.FontMetrics sfm = smallPaint.getFontMetrics();
        smallRowH = (sfm.descent - sfm.ascent) * 1.1f;
        smallBaseline = -sfm.ascent;
        pad = textSize * 0.25f;
        charW = valuePaint.measureText("0");
        smallCharW = smallPaint.measureText("0");
        graphH = textSize * 2.0f;
        labelColW = charW * 4.2f;
    }

    private void applyAlphas() {
        int bg = Math.round(bgAlpha * 255f);
        bgPaint.setColor((bg << 24) | (C_BG & 0x00FFFFFF));
        int text = Math.round(textAlpha * 255f);
        valuePaint.setAlpha(text);
        labelPaint.setAlpha(text);
        smallPaint.setAlpha(text);
        graphPaint.setAlpha(text);
        outlinePaint.setAlpha(text);
    }

    // ==================== 布局计算 ====================

    private boolean has(int flag) { return (showMask & flag) != 0; }

    private void computeLayout() {
        if (vertical) {
            computeVerticalLayout();
        } else {
            computeHorizontalLayout();
        }
    }

    private void computeHorizontalLayout() {
        float w = 0f;
        int rows = 0;
        int smallRows = 0;

        if (has(SHOW_GPU_LOAD) || has(SHOW_GPU_TEMP) || has(SHOW_GPU_CLOCK)) {
            rows++;
            float rw = labelColW
                + (has(SHOW_GPU_LOAD) ? cellW(3,1) : 0)
                + (has(SHOW_GPU_TEMP) ? cellW(3,2) : 0)
                + (has(SHOW_GPU_CLOCK) ? cellW(4,3) : 0);
            w = Math.max(w, rw);
        }
        if (has(SHOW_CPU_LOAD) || has(SHOW_CPU_TEMP) || has(SHOW_CPU_CLOCK)) {
            rows++;
            float rw = labelColW
                + (has(SHOW_CPU_LOAD) ? cellW(3,1) : 0)
                + (has(SHOW_CPU_TEMP) ? cellW(3,2) : 0)
                + (has(SHOW_CPU_CLOCK) ? cellW(4,3) : 0);
            w = Math.max(w, rw);
        }
        if (has(SHOW_CPU_CORES)) {
            rows += metrics.coreCount;
            w = Math.max(w, labelColW + cellW(3,1) + cellW(4,3));
        }
        if (has(SHOW_VRAM)) { rows++; w = Math.max(w, labelColW + cellW(4,3)); }
        if (has(SHOW_RAM)) { rows++; w = Math.max(w, labelColW + cellW(4,3) + cellW(3,1)); }
        if (has(SHOW_SWAP)) { rows++; w = Math.max(w, labelColW + cellW(4,3)); }
        if (has(SHOW_NETWORK)) { rows++; w = Math.max(w, labelColW + cellW(4,2) + cellW(4,2)); }
        if (has(SHOW_BATTERY) || has(SHOW_BATTERY_TIME)) {
            rows++;
            float rw = labelColW + cellW(3,1) + cellW(3,2);
            if (has(SHOW_BATTERY)) rw += cellW(4,1);
            w = Math.max(w, rw);
        }

        rows++; // FPS 行
        w = Math.max(w, labelColW + cellW(4,3) + cellW(4,2));

        if (has(SHOW_AVG_FPS)) { rows++; w = Math.max(w, labelColW + cellW(4,3)); }
        if (has(SHOW_1PC_LOW)) { rows++; w = Math.max(w, labelColW + cellW(4,3)); }
        if (has(SHOW_01PC_LOW)) { rows++; w = Math.max(w, labelColW + cellW(4,3)); }

        if (has(SHOW_ENGINE) && !engineName.isEmpty()) { smallRows++; w = Math.max(w, smallCharW * (engineName.length() + 8)); }
        if (has(SHOW_RESOLUTION) && !resolution.isEmpty()) { smallRows++; w = Math.max(w, smallCharW * (resolution.length() + 12)); }
        if (has(SHOW_WINE_VERSION) && !wineVersion.isEmpty()) { smallRows++; w = Math.max(w, smallCharW * wineVersion.length()); }
        if (has(SHOW_DURATION)) { smallRows++; w = Math.max(w, smallCharW * 16); }
        if (has(SHOW_THROTTLE) && metrics.throttleStatus > 0) { smallRows++; }

        w = Math.max(w, charW * 13f);
        float h = rows * rowH + smallRows * smallRowH;
        if (has(SHOW_GRAPH)) h += smallRowH + graphH + pad * 0.5f;

        panelW = (int) Math.ceil(w + pad * 2);
        panelH = (int) Math.ceil(h + pad * 2);
    }

    private void computeVerticalLayout() {
        // 竖屏：单列，每个分类占 1-2 行
        float w = 0f;
        int rows = 0;

        if (has(SHOW_GPU_LOAD) || has(SHOW_GPU_TEMP) || has(SHOW_GPU_CLOCK)) {
            rows += 2; // 标签行 + 数据行
            w = Math.max(w, smallCharW * 12);
        }
        if (has(SHOW_CPU_LOAD) || has(SHOW_CPU_TEMP) || has(SHOW_CPU_CLOCK)) {
            rows += 2;
            w = Math.max(w, smallCharW * 12);
        }
        if (has(SHOW_VRAM)) { rows += 2; w = Math.max(w, smallCharW * 10); }
        if (has(SHOW_RAM)) { rows += 2; w = Math.max(w, smallCharW * 12); }
        if (has(SHOW_BATTERY) || has(SHOW_BATTERY_TIME)) { rows += 2; w = Math.max(w, smallCharW * 14); }
        if (has(SHOW_FPS) || has(SHOW_FRAMETIME)) { rows += 2; w = Math.max(w, smallCharW * 12); }
        if (has(SHOW_AVG_FPS)) { rows++; w = Math.max(w, smallCharW * 10); }
        if (has(SHOW_1PC_LOW)) { rows++; w = Math.max(w, smallCharW * 10); }
        if (has(SHOW_01PC_LOW)) { rows++; w = Math.max(w, smallCharW * 10); }
        if (has(SHOW_ENGINE) && !engineName.isEmpty()) { rows++; w = Math.max(w, smallCharW * engineName.length()); }
        if (has(SHOW_DURATION)) { rows++; w = Math.max(w, smallCharW * 12); }

        if (has(SHOW_GRAPH)) rows += 3; // 标签 + 图表

        panelW = (int) Math.ceil(w + pad * 2);
        panelH = (int) Math.ceil(rows * smallRowH + pad * 2);
    }

    private float cellW(int valueChars, int unitChars) {
        return charW * valueChars + smallCharW * (unitChars + 0.2f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(panelW, panelH);
    }

    // ==================== 绘制 ====================

    @Override
    protected void onDraw(Canvas canvas) {
        bgRect.set(0, 0, panelW, panelH);
        canvas.drawRoundRect(bgRect, pad, pad, bgPaint);

        if (vertical) {
            drawVertical(canvas);
        } else {
            drawHorizontal(canvas);
        }

        // 锁定指示器
        if (locked) {
            smallPaint.setColor(C_WARN);
            canvas.drawText("🔒", pad, panelH - pad, smallPaint);
        }
    }

    private void drawHorizontal(Canvas canvas) {
        float y = pad;

        if (has(SHOW_GPU_LOAD) || has(SHOW_GPU_TEMP) || has(SHOW_GPU_CLOCK)) {
            float x = drawLabel(canvas, "GPU", C_GPU, y);
            if (has(SHOW_GPU_LOAD)) x = drawCell(canvas, sbGpuLoad, "%", x, y, 3);
            if (has(SHOW_GPU_TEMP)) x = drawCell(canvas, sbGpuTemp, "°C", x, y, 3);
            if (has(SHOW_GPU_CLOCK)) drawCell(canvas, sbGpuClk, "MHz", x, y, 4);
            y += rowH;
        }
        if (has(SHOW_CPU_LOAD) || has(SHOW_CPU_TEMP) || has(SHOW_CPU_CLOCK)) {
            float x = drawLabel(canvas, "CPU", C_CPU, y);
            if (has(SHOW_CPU_LOAD)) x = drawCell(canvas, sbCpuLoad, "%", x, y, 3);
            if (has(SHOW_CPU_TEMP)) x = drawCell(canvas, sbCpuTemp, "°C", x, y, 3);
            if (has(SHOW_CPU_CLOCK)) drawCell(canvas, sbCpuClk, "MHz", x, y, 4);
            y += rowH;
        }
        if (has(SHOW_CPU_CORES)) {
            for (int i = 0; i < metrics.coreCount; i++) {
                float x = drawLabel(canvas, "C" + i, C_CPU, y);
                x = drawCellInt(canvas, metrics.coreLoad[i], "%", x, y, 3);
                drawCellInt(canvas, metrics.coreClock[i], "MHz", x, y, 4);
                y += rowH;
            }
        }
        if (has(SHOW_VRAM)) {
            float x = drawLabel(canvas, "VRAM", C_VRAM, y);
            drawCell(canvas, sbVram, "GiB", x, y, 4);
            y += rowH;
        }
        if (has(SHOW_RAM)) {
            float x = drawLabel(canvas, "RAM", C_RAM, y);
            x = drawCell(canvas, sbRam, "GiB", x, y, 4);
            drawCell(canvas, sbRamPct, "%", x, y, 3);
            y += rowH;
        }
        if (has(SHOW_SWAP)) {
            float x = drawLabel(canvas, "SWP", C_RAM, y);
            drawCell(canvas, sbSwap, "GiB", x, y, 4);
            y += rowH;
        }
        if (has(SHOW_NETWORK)) {
            float x = drawLabel(canvas, "NET", C_NET, y);
            x = drawCell(canvas, sbNetRx, "K↓", x, y, 4);
            drawCell(canvas, sbNetTx, "K↑", x, y, 4);
            y += rowH;
        }
        if (has(SHOW_BATTERY) || has(SHOW_BATTERY_TIME)) {
            float x = drawLabel(canvas, "BAT", C_BAT, y);
            if (has(SHOW_BATTERY)) {
                x = drawCell(canvas, sbBatPct, "%", x, y, 3);
                x = drawCell(canvas, sbBatTemp, "°C", x, y, 3);
                x = drawCell(canvas, sbBatW, "W", x, y, 4);
            }
            if (has(SHOW_BATTERY_TIME)) {
                drawSmall(canvas, sbBatTime.toString(), x, y + baseline, C_BAT);
            }
            y += rowH;
        }

        // FPS 行
        {
            float x = drawLabel(canvas, "FPS", C_TEXT, y);
            x = drawCell(canvas, sbFps, "FPS", x, y, 4);
            if (has(SHOW_FRAMETIME)) drawCell(canvas, sbMs, "ms", x, y, 4);
            y += rowH;
        }

        if (has(SHOW_AVG_FPS)) { float x = drawLabel(canvas, "AVG", C_TEXT, y); drawCell(canvas, sbAvg, "FPS", x, y, 4); y += rowH; }
        if (has(SHOW_1PC_LOW)) { float x = drawLabel(canvas, "1%", C_TEXT, y); drawCell(canvas, sbLow1, "FPS", x, y, 4); y += rowH; }
        if (has(SHOW_01PC_LOW)) { float x = drawLabel(canvas, "0.1%", C_TEXT, y); drawCell(canvas, sbLow01, "FPS", x, y, 4); y += rowH; }

        if (has(SHOW_ENGINE) && !engineName.isEmpty()) {
            String eng = engineName + (engineVersion.isEmpty() ? "" : " " + engineVersion);
            drawSmall(canvas, eng, pad, y + smallBaseline, C_ENGINE);
            y += smallRowH;
        }
        if (has(SHOW_RESOLUTION) && !resolution.isEmpty()) {
            String res = resolution + getRefreshRateSuffix();
            drawSmall(canvas, res, pad, y + smallBaseline, C_TEXT);
            y += smallRowH;
        }
        if (has(SHOW_WINE_VERSION) && !wineVersion.isEmpty()) {
            drawSmall(canvas, wineVersion, pad, y + smallBaseline, C_ENGINE);
            y += smallRowH;
        }
        if (has(SHOW_DURATION)) {
            drawSmall(canvas, sbDuration.toString(), pad, y + smallBaseline, C_TEXT);
            y += smallRowH;
        }
        if (has(SHOW_THROTTLE) && metrics.throttleStatus > 0) {
            String[] txt = {"", "throttle: light", "throttle: moderate", "throttle: severe",
                "throttle: critical", "throttle: emergency", "throttle: shutdown"};
            int idx = Math.min(metrics.throttleStatus, txt.length - 1);
            int color = metrics.throttleStatus >= 3 ? C_CRIT : C_WARN;
            drawSmall(canvas, txt[idx], pad, y + smallBaseline, color);
            y += smallRowH;
        }

        if (has(SHOW_GRAPH)) {
            drawSmall(canvas, "Frametime", pad, y + smallBaseline, C_ENGINE);
            float mmW = smallPaint.measureText(sbMinMax.toString());
            drawSmall(canvas, sbMinMax.toString(), panelW - pad - mmW, y + smallBaseline, C_TEXT);
            y += smallRowH + pad * 0.5f;
            drawGraph(canvas, pad, y, panelW - pad * 2, graphH);
        }
    }

    private void drawVertical(Canvas canvas) {
        float y = pad;
        float cx = panelW / 2f;

        if (has(SHOW_GPU_LOAD) || has(SHOW_GPU_TEMP) || has(SHOW_GPU_CLOCK)) {
            drawSmallCentered(canvas, "GPU", y + smallBaseline, C_GPU);
            y += smallRowH;
            StringBuilder sb = new StringBuilder();
            if (has(SHOW_GPU_LOAD)) sb.append(sbGpuLoad).append("% ");
            if (has(SHOW_GPU_TEMP)) sb.append(sbGpuTemp).append("°C ");
            if (has(SHOW_GPU_CLOCK)) sb.append(sbGpuClk).append("MHz");
            drawSmallCentered(canvas, sb.toString(), y + smallBaseline, C_TEXT);
            y += smallRowH;
        }
        if (has(SHOW_CPU_LOAD) || has(SHOW_CPU_TEMP) || has(SHOW_CPU_CLOCK)) {
            drawSmallCentered(canvas, "CPU", y + smallBaseline, C_CPU);
            y += smallRowH;
            StringBuilder sb = new StringBuilder();
            if (has(SHOW_CPU_LOAD)) sb.append(sbCpuLoad).append("% ");
            if (has(SHOW_CPU_TEMP)) sb.append(sbCpuTemp).append("°C ");
            if (has(SHOW_CPU_CLOCK)) sb.append(sbCpuClk).append("MHz");
            drawSmallCentered(canvas, sb.toString(), y + smallBaseline, C_TEXT);
            y += smallRowH;
        }
        if (has(SHOW_VRAM)) {
            drawSmallCentered(canvas, "VRAM", y + smallBaseline, C_VRAM);
            y += smallRowH;
            drawSmallCentered(canvas, sbVram + " GiB", y + smallBaseline, C_TEXT);
            y += smallRowH;
        }
        if (has(SHOW_RAM)) {
            drawSmallCentered(canvas, "RAM", y + smallBaseline, C_RAM);
            y += smallRowH;
            drawSmallCentered(canvas, sbRam + "GiB " + sbRamPct + "%", y + smallBaseline, C_TEXT);
            y += smallRowH;
        }
        if (has(SHOW_BATTERY)

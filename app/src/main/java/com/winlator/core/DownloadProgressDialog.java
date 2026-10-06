package com.winlator.core;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.winlator.R;
import com.winlator.math.Mathf;

import java.util.Locale;

/**
 * 现代化下载/安装进度对话框
 * 支持水平进度条、下载速度、已下载/总大小、剩余时间、当前文件名。
 * 保持原有接口兼容，新增带字节数的进度更新方法。
 */
public class DownloadProgressDialog {
    private final Activity activity;
    private Dialog dialog;

    // 速度计算相关
    private long lastUpdateTime = 0;
    private long lastDownloadedBytes = 0;
    private float currentSpeed = 0; // MB/s

    public DownloadProgressDialog(Activity activity) {
        this.activity = activity;
    }

    private void create() {
        if (dialog != null) return;
        dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(R.layout.download_progress_dialog);

        Window window = dialog.getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
    }

    public void show() {
        show(null);
    }

    public void show(int textResId) {
        show(textResId, null);
    }

    public void show(Runnable onCancelCallback) {
        show(0, onCancelCallback);
    }

    public void show(int textResId, final Runnable onCancelCallback) {
        if (isShowing()) return;
        close();
        if (dialog == null) create();

        if (textResId > 0) ((TextView)dialog.findViewById(R.id.TVTitle)).setText(textResId);

        // 重置速度计算
        lastUpdateTime = 0;
        lastDownloadedBytes = 0;
        currentSpeed = 0;

        setProgress(0);
        if (onCancelCallback != null) {
            dialog.findViewById(R.id.BTCancel).setOnClickListener((v) -> onCancelCallback.run());
            dialog.findViewById(R.id.LLBottomBar).setVisibility(View.VISIBLE);
        }
        dialog.show();
    }

    /**
     * 设置进度（兼容原有接口，只更新百分比）
     */
    public void setProgress(int progress) {
        if (dialog == null) return;
        progress = Mathf.clamp(progress, 0, 100);
        ((CircularProgressIndicator)dialog.findViewById(R.id.CircularProgressIndicator)).setProgress(progress);
        ((LinearProgressIndicator)dialog.findViewById(R.id.LinearProgressBar)).setProgress(progress);
        ((TextView)dialog.findViewById(R.id.TVProgressPercent)).setText(progress + "%");
    }

    /**
     * 设置进度（带字节数，自动计算速度和剩余时间）
     * @param progress 百分比 0-100
     * @param downloadedBytes 已下载字节数
     * @param totalBytes 总字节数
     */
    public void setProgress(int progress, long downloadedBytes, long totalBytes) {
        if (dialog == null) return;
        progress = Mathf.clamp(progress, 0, 100);

        // 更新进度条和百分比
        ((CircularProgressIndicator)dialog.findViewById(R.id.CircularProgressIndicator)).setProgress(progress);
        ((LinearProgressIndicator)dialog.findViewById(R.id.LinearProgressBar)).setProgress(progress);
        ((TextView)dialog.findViewById(R.id.TVProgressPercent)).setText(progress + "%");

        // 计算下载速度（每 500ms 更新一次）
        long now = System.currentTimeMillis();
        if (lastUpdateTime > 0 && now - lastUpdateTime >= 500) {
            long deltaBytes = downloadedBytes - lastDownloadedBytes;
            long deltaTime = now - lastUpdateTime;
            if (deltaTime > 0 && deltaBytes >= 0) {
                currentSpeed = (deltaBytes / 1024.0f / 1024.0f) / (deltaTime / 1000.0f);
            }
            lastUpdateTime = now;
            lastDownloadedBytes = downloadedBytes;
        } else if (lastUpdateTime == 0) {
            lastUpdateTime = now;
            lastDownloadedBytes = downloadedBytes;
        }

        // 更新速度显示
        String speedText;
        if (currentSpeed >= 1.0f) {
            speedText = String.format(Locale.US, "%.1f MB/s", currentSpeed);
        } else if (currentSpeed > 0) {
            speedText = String.format(Locale.US, "%.0f KB/s", currentSpeed * 1024);
        } else {
            speedText = "-- MB/s";
        }
        ((TextView)dialog.findViewById(R.id.TVSpeed)).setText(speedText);

        // 更新已下载/总大小
        String downloadedText = formatBytes(downloadedBytes) + " / " + formatBytes(totalBytes);
        ((TextView)dialog.findViewById(R.id.TVDownloaded)).setText(downloadedText);

        // 计算剩余时间
        String remainingText;
        if (currentSpeed > 0 && totalBytes > downloadedBytes) {
            long remainingBytes = totalBytes - downloadedBytes;
            float remainingSeconds = remainingBytes / 1024.0f / 1024.0f / currentSpeed;
            if (remainingSeconds < 60) {
                remainingText = String.format(Locale.US, "0:%02.0f", remainingSeconds);
            } else if (remainingSeconds < 3600) {
                int minutes = (int)(remainingSeconds / 60);
                int seconds = (int)(remainingSeconds % 60);
                remainingText = String.format(Locale.US, "%d:%02d", minutes, seconds);
            } else {
                int hours = (int)(remainingSeconds / 3600);
                int minutes = (int)((remainingSeconds % 3600) / 60);
                remainingText = String.format(Locale.US, "%d:%02d", hours, minutes);
            }
        } else {
            remainingText = "--:--";
        }
        ((TextView)dialog.findViewById(R.id.TVRemaining)).setText(remainingText);
    }

    /**
     * 设置当前下载/安装的文件名
     */
    public void setFileName(String fileName) {
        if (dialog == null || fileName == null) return;
        TextView tvFileName = dialog.findViewById(R.id.TVFileName);
        tvFileName.setText(fileName);
        tvFileName.setVisibility(View.VISIBLE);
    }

    /**
     * 格式化字节数为人类可读格式
     */
    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        if (unitIndex == 0) {
            return String.format(Locale.US, "%.0f %s", size, units[unitIndex]);
        }
        return String.format(Locale.US, "%.1f %s", size, units[unitIndex]);
    }

    public void close() {
        try {
            if (dialog != null) {
                dialog.dismiss();
            }
        }
        catch (Exception e) {}
    }

    public void closeOnUiThread() {
        activity.runOnUiThread(this::close);
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }
}

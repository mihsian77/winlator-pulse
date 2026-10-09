package com.winlator.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.winlator.MainActivity;
import com.winlator.R;
import com.winlator.core.TarCompressorUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 组件安装后台服务
 * 在前台服务中执行组件安装，通知栏实时显示进度，支持取消。
 * 同时通过广播实时回传进度，供组件管理页面的圆形进度对话框更新。
 */
public class InstallService extends Service {
    private static final String TAG = "InstallService";
    private static final String CHANNEL_ID = "winlator_install";
    private static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_START_INSTALL = "com.winlator.action.START_INSTALL";
    public static final String ACTION_CANCEL_INSTALL = "com.winlator.action.CANCEL_INSTALL";
    public static final String ACTION_INSTALL_PROGRESS = "com.winlator.action.INSTALL_PROGRESS";
    public static final String ACTION_INSTALL_COMPLETED = "com.winlator.action.INSTALL_COMPLETED";

    public static final String EXTRA_FILE_URI = "file_uri";
    public static final String EXTRA_FILE_NAME = "file_name";
    public static final String EXTRA_CATEGORY = "category";
    public static final String EXTRA_BASE_FILES_PATH = "base_files_path";
    public static final String EXTRA_PROGRESS = "progress";
    public static final String EXTRA_STAGE = "stage";

    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    private NotificationManager notificationManager;
    private Handler mainHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mainHandler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (ACTION_CANCEL_INSTALL.equals(action)) {
            isCancelled.set(true);
            updateNotification("正在取消...", 0, false);
            sendProgressBroadcast("正在取消...", 0);
            return START_NOT_STICKY;
        }

        if (ACTION_START_INSTALL.equals(action)) {
            Uri fileUri = intent.getParcelableExtra(EXTRA_FILE_URI);
            String fileName = intent.getStringExtra(EXTRA_FILE_NAME);
            String category = intent.getStringExtra(EXTRA_CATEGORY);
            String baseFilesPath = intent.getStringExtra(EXTRA_BASE_FILES_PATH);

            if (fileUri == null || fileName == null || category == null) {
                stopSelf();
                return START_NOT_STICKY;
            }

            startForeground(NOTIFICATION_ID, buildNotification("正在安装: " + fileName, 0));
            sendProgressBroadcast("正在安装", 5);
            isCancelled.set(false);

            // 在后台线程执行安装
            new Thread(() -> {
                try {
                    performInstall(fileUri, fileName, category, baseFilesPath);
                    if (!isCancelled.get()) {
                        mainHandler.post(() -> {
                            updateNotification("安装完成: " + fileName, 100, true);
                            sendProgressBroadcast("安装完成", 100);
                            sendCompletionBroadcast(fileName, true, null);
                        });
                    } else {
                        mainHandler.post(() -> sendCompletionBroadcast(fileName, false, "已取消"));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "安装失败", e);
                    mainHandler.post(() -> {
                        updateNotification("安装失败: " + e.getMessage(), 0, false);
                        sendCompletionBroadcast(fileName, false, e.getMessage());
                    });
                } finally {
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                    stopSelf();
                }
            }).start();
        }

        return START_NOT_STICKY;
    }

    /**
     * 执行安装
     */
    private void performInstall(Uri fileUri, String fileName, String category, String baseFilesPath) throws Exception {
        boolean isWine = category.equalsIgnoreCase("wine") || category.equalsIgnoreCase("proton");
        String storagePath = isWine ? "rootfs/opt/installed-wine" : "installed_components";
        String installPath = isWine ? "" : category;
        File targetDir = new File(baseFilesPath, storagePath + File.separator + installPath);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new Exception("目录创建失败");
        }

        updateNotification("正在安装: " + fileName, 10);
        sendProgressBroadcast("正在安装", 10);

        String baseName = fileName.replaceAll("(?i)\\.whp$", "");
        try (InputStream input = getContentResolver().openInputStream(fileUri)) {
            if (input == null) throw new Exception("无法读取文件");

            if (isWine) {
                TarCompressorUtils.Type type = detectCompressionType(fileUri);
                if (type == null) throw new Exception("不支持的文件格式");
                updateNotification("正在解压: " + fileName, 30);
                sendProgressBroadcast("正在解压", 30);
                if (!TarCompressorUtils.extract(type, this, fileUri, targetDir, null)) {
                    throw new Exception("解压失败");
                }
                updateNotification("正在配置: " + fileName, 80);
                sendProgressBroadcast("正在配置", 80);
            } else {
                File outFile = new File(targetDir, baseName + ".tzst");
                try (OutputStream output = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    long total = 0;
                    while ((len = input.read(buffer)) != -1) {
                        if (isCancelled.get()) throw new Exception("已取消");
                        output.write(buffer, 0, len);
                        total += len;
                        // 模拟进度（实际无法预知总大小，用阶段进度）
                        int progress = (int) Math.min(90, 10 + (total / (1024 * 1024)) * 5);
                        updateNotification("正在复制: " + fileName, progress);
                        sendProgressBroadcast("正在复制", progress);
                    }
                }
            }
        }
        updateNotification("安装完成: " + fileName, 100);
        sendProgressBroadcast("安装完成", 100);
    }

    /**
     * 检测压缩类型
     */
    private TarCompressorUtils.Type detectCompressionType(Uri uri) throws IOException {
        byte[] magic = new byte[6];
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null || is.read(magic) < 4) return null;
            if ((magic[0] & 0xFF) == 0xFD && (magic[1] & 0xFF) == 0x37 &&
                (magic[2] & 0xFF) == 0x7A && (magic[3] & 0xFF) == 0x58) {
                return TarCompressorUtils.Type.XZ;
            }
            if ((magic[0] & 0xFF) == 0x28 && (magic[1] & 0xFF) == 0xB5 &&
                (magic[2] & 0xFF) == 0x2F && (magic[3] & 0xFF) == 0xFD) {
                return TarCompressorUtils.Type.ZSTD;
            }
            return null;
        }
    }

    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "组件安装", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Winlator 组件安装进度通知");
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * 构建通知
     */
    private Notification buildNotification(String text, int progress) {
        Intent cancelIntent = new Intent(this, InstallService.class);
        cancelIntent.setAction(ACTION_CANCEL_INSTALL);
        PendingIntent cancelPendingIntent = PendingIntent.getService(
                this, 0, cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent contentIntent = new Intent(this, MainActivity.class);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                this, 0, contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Winlator 组件安装")
                .setContentText(text)
                .setContentIntent(contentPendingIntent)
                .setOngoing(true)
                .setProgress(100, progress, false)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "取消", cancelPendingIntent);

        return builder.build();
    }

    /**
     * 更新通知（两参数重载，默认未完成）
     */
    private void updateNotification(String text, int progress) {
        updateNotification(text, progress, false);
    }

    /**
     * 更新通知
     */
    private void updateNotification(String text, int progress, boolean completed) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(completed ? android.R.drawable.stat_sys_download_done : android.R.drawable.stat_sys_download)
                .setContentTitle("Winlator 组件安装")
                .setContentText(text)
                .setOngoing(!completed)
                .setProgress(100, progress, false);

        if (!completed) {
            Intent cancelIntent = new Intent(this, InstallService.class);
            cancelIntent.setAction(ACTION_CANCEL_INSTALL);
            PendingIntent cancelPendingIntent = PendingIntent.getService(
                    this, 0, cancelIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "取消", cancelPendingIntent);
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    /**
     * 发送实时进度广播（应用内圆形进度对话框更新）
     */
    private void sendProgressBroadcast(String stage, int progress) {
        Intent intent = new Intent(ACTION_INSTALL_PROGRESS);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_PROGRESS, progress);
        intent.putExtra(EXTRA_STAGE, stage);
        sendBroadcast(intent);
    }

    /**
     * 发送安装完成广播
     */
    private void sendCompletionBroadcast(String fileName, boolean success, String error) {
        Intent intent = new Intent(ACTION_INSTALL_COMPLETED);
        intent.setPackage(getPackageName());
        intent.putExtra("file_name", fileName);
        intent.putExtra("success", success);
        if (error != null) intent.putExtra("error", error);
        sendBroadcast(intent);
    }

    /**
     * 启动安装服务的便捷方法
     */
    public static void startInstall(Context context, Uri fileUri, String fileName, String category, String baseFilesPath) {
        Intent intent = new Intent(context, InstallService.class);
        intent.setAction(ACTION_START_INSTALL);
        intent.putExtra(EXTRA_FILE_URI, fileUri);
        intent.putExtra(EXTRA_FILE_NAME, fileName);
        intent.putExtra(EXTRA_CATEGORY, category);
        intent.putExtra(EXTRA_BASE_FILES_PATH, baseFilesPath);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

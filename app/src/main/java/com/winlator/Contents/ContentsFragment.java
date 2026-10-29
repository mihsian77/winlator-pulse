package com.winlator.contents;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.content.res.TypedArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.winlator.core.HttpUtils;
import com.winlator.core.TarCompressorUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;

public class ContentsFragment extends Fragment {
    private static final String[] FILE_TYPES = {"dxvk", "vkd3d", "box64", "turnip", "wined3d", "wine", "proton"};

    // 有在线仓库的组件类型（可以在线下载）
    private static final java.util.Set<String> ONLINE_TYPES = new java.util.HashSet<>(java.util.Arrays.asList("dxvk", "vkd3d", "box64", "turnip", "wined3d"));
    private static final String TZSD_EXTENSION = ".tzst";
    private static final int CORNER_RADIUS_DP = 12;
    private static final int ELEVATION_DP = 4;

    // 加速节点列表（按 MirrorHub 延迟排序）
    // raw 文件用 jsDelivr CDN 加速
    private static final String[] INDEX_URL_TEMPLATES = {
        "https://cdn.jsdelivr.net/gh/mihsian77/winlator-components@main/%s/index.txt",
        "https://fastly.jsdelivr.net/gh/mihsian77/winlator-components@main/%s/index.txt",
        "https://gcore.jsdelivr.net/gh/mihsian77/winlator-components@main/%s/index.txt",
        "https://raw.githubusercontent.com/mihsian77/winlator-components/main/%s/index.txt"
    };

    // release 文件用通用代理加速
    private static final String[] DOWNLOAD_URL_TEMPLATES = {
        "https://gh-proxy.com/https://github.com/mihsian77/winlator-components/releases/download/%s/%s",
        "https://ghfast.top/https://github.com/mihsian77/winlator-components/releases/download/%s/%s",
        "https://ghproxy.net/https://github.com/mihsian77/winlator-components/releases/download/%s/%s",
        "https://github.com/mihsian77/winlator-components/releases/download/%s/%s"
    };

    private static boolean isWineOrProton(String category) {
        return category.equalsIgnoreCase("wine") || category.equalsIgnoreCase("proton");
    }

    private String baseFilesPath;
    private String currentCategory = FILE_TYPES[0];

    private LinearLayout fileListContainer;
    private TextView categorySelector;
    private ArrayList<String> availableComponents = new ArrayList<>();
    private HashSet<String> installedComponents = new HashSet<>();

    private ActivityResultLauncher<String[]> filePickerLauncher;
    private AlertDialog installProgressDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        baseFilesPath = requireContext().getFilesDir().getAbsolutePath();
        initDirectories();

        // 注册文件选择器
        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) installFromUri(uri);
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null && activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle("组件管理");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dismissInstallProgressDialog();
    }

    private void initDirectories() {
        File imageFsDir = new File(requireContext().getFilesDir(), "imagefs");
        if (!imageFsDir.exists()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    Files.createSymbolicLink(imageFsDir.toPath(), Paths.get("./rootfs"));
                } catch (Exception e) {
                    Log.e("Symlink", "创建符号链接失败", e);
                }
            }
        }

        File wineDir = new File(baseFilesPath, "rootfs/opt/installed-wine");
        if (!wineDir.exists() && wineDir.mkdirs()) {
            try {
                new ProcessBuilder("chmod", "-R", "771", wineDir.getAbsolutePath())
                        .start()
                        .waitFor();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColorFromAttr(android.R.attr.colorBackground, Color.parseColor("#FAFAFA"), Color.parseColor("#121212")));
        int padding = dpToPx(16);
        root.setPadding(padding, padding, padding, padding);

        // === 折叠栏：组件类型选择 ===
        categorySelector = new TextView(requireContext());
        categorySelector.setText("▼ 组件类型：" + currentCategory);
        categorySelector.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        categorySelector.setTypeface(Typeface.DEFAULT_BOLD);
        int cardBgColor = getColorFromAttr(android.R.attr.colorBackground, Color.parseColor("#FFFFFF"), Color.parseColor("#1E1E1E"));
        categorySelector.setBackground(createRoundedBackground(cardBgColor, CORNER_RADIUS_DP));
        categorySelector.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            categorySelector.setElevation(dpToPx(ELEVATION_DP));
        }
        categorySelector.setOnClickListener(v -> showCategoryDialog());
        root.addView(categorySelector, createLayoutParams(12));

        // === 滚动列表 ===
        ScrollView scroll = new ScrollView(requireContext());
        fileListContainer = new LinearLayout(requireContext());
        fileListContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(fileListContainer);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        // === 底部按钮 ===
        LinearLayout btnLayout = new LinearLayout(requireContext());
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);

        // 刷新按钮
        Button refreshBtn = new Button(requireContext());
        refreshBtn.setText("刷新列表");
        refreshBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        refreshBtn.setTextColor(Color.WHITE);
        refreshBtn.setAllCaps(false);
        int primaryColor = getColorFromAttr(android.R.attr.colorPrimary, Color.parseColor("#2196F3"), Color.parseColor("#2196F3"));
        refreshBtn.setBackground(createRoundedBackground(primaryColor, CORNER_RADIUS_DP));
        refreshBtn.setPadding(0, dpToPx(12), 0, dpToPx(12));
        refreshBtn.setOnClickListener(v -> loadAvailableComponents());
        LinearLayout.LayoutParams refreshLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        refreshLp.setMargins(0, 0, dpToPx(8), 0);
        refreshBtn.setLayoutParams(refreshLp);
        btnLayout.addView(refreshBtn);

        // 手动安装按钮
        Button installBtn = new Button(requireContext());
        installBtn.setText("选择安装文件");
        installBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        installBtn.setTextColor(Color.WHITE);
        installBtn.setAllCaps(false);
        int accentColor = getColorFromAttr(android.R.attr.colorAccent, Color.parseColor("#FF9800"), Color.parseColor("#FF9800"));
        installBtn.setBackground(createRoundedBackground(accentColor, CORNER_RADIUS_DP));
        installBtn.setPadding(0, dpToPx(12), 0, dpToPx(12));
        installBtn.setOnClickListener(v -> showFilePicker());
        LinearLayout.LayoutParams installLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        installLp.setMargins(dpToPx(8), 0, 0, 0);
        installBtn.setLayoutParams(installLp);
        btnLayout.addView(installBtn);

        root.addView(btnLayout, createLayoutParams(12));

        // 加载组件列表
        loadAvailableComponents();
        return root;
    }

    private void showCategoryDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("选择组件类型")
                .setItems(FILE_TYPES, (d, which) -> {
                    currentCategory = FILE_TYPES[which];
                    categorySelector.setText("▼ 组件类型：" + currentCategory);
                    loadAvailableComponents();
                })
                .show();
    }

    private void showFilePicker() {
        filePickerLauncher.launch(new String[]{"*/*"});
    }

    private void installFromUri(Uri uri) {
        showInstallProgressDialog();
        new Thread(() -> {
            try {
                // 获取文件名
                String fileName = getFileName(uri);
                if (fileName == null || !fileName.endsWith(".tzst")) {
                    requireActivity().runOnUiThread(() -> {
                        dismissInstallProgressDialog();
                        showToast("请选择 .tzst 格式文件");
                    });
                    return;
                }

                // 复制到目标目录
                boolean isWine = isWineOrProton(currentCategory);
                String storagePath = isWine ? "rootfs/opt/installed-wine" : "installed_components";
                String installPath = isWine ? "" : currentCategory;
                File targetDir = new File(baseFilesPath, storagePath + File.separator + installPath);
                if (!targetDir.exists() && !targetDir.mkdirs()) {
                    throw new Exception("目录创建失败");
                }

                File outFile = new File(targetDir, fileName);
                try (InputStream input = requireContext().getContentResolver().openInputStream(uri);
                     OutputStream output = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = input.read(buffer)) != -1) output.write(buffer, 0, len);
                }

                requireActivity().runOnUiThread(() -> {
                    dismissInstallProgressDialog();
                    showToast("✔ 安装完成");
                    refreshInstalledStatus();
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    dismissInstallProgressDialog();
                    showToast("✘ 安装失败: " + e.getMessage());
                });
            }
        }).start();
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private void loadAvailableComponents() {
        // 没有在线仓库的类型，不尝试在线拉取
        if (!ONLINE_TYPES.contains(currentCategory)) {
            availableComponents.clear();
            refreshInstalledStatus();
            return;
        }

        showInstallProgressDialog();
        tryLoadIndex(0);
    }

    private void tryLoadIndex(int index) {
        if (index >= INDEX_URL_TEMPLATES.length) {
            // 所有节点都失败
            requireActivity().runOnUiThread(() -> {
                dismissInstallProgressDialog();
                showToast("网络错误，请检查网络连接");
            });
            return;
        }

        String url = String.format(INDEX_URL_TEMPLATES[index], currentCategory);
        HttpUtils.download(url, (content) -> {
            if (content != null) {
                handleIndexResult(content);
            } else {
                // 当前节点失败，尝试下一个
                tryLoadIndex(index + 1);
            }
        });
    }

    private void handleIndexResult(String content) {
        requireActivity().runOnUiThread(() -> {
            dismissInstallProgressDialog();
            availableComponents.clear();
            if (content != null && !content.isEmpty()) {
                for (String line : content.split("\n")) {
                    line = line.trim();
                    if (!line.isEmpty()) availableComponents.add(line);
                }
            }
            refreshInstalledStatus();
        });
    }

    private void refreshInstalledStatus() {
        installedComponents.clear();
        boolean isWine = isWineOrProton(currentCategory);
        String storagePath = isWine ? "rootfs/opt/installed-wine" : "installed_components";
        String installPath = isWine ? "" : currentCategory;

        File dir = new File(baseFilesPath, storagePath + File.separator + installPath);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    String name = f.isFile() ? f.getName().replace(TZSD_EXTENSION, "") : f.getName();
                    installedComponents.add(name);
                }
            }
        }

        renderList();
    }

    private void renderList() {
        fileListContainer.removeAllViews();

        if (availableComponents.isEmpty()) {
            showEmptyState("暂无可用组件");
            return;
        }

        for (String filename : availableComponents) {
            String name = filename.replace(TZSD_EXTENSION, "");
            boolean installed = installedComponents.contains(name);
            addComponentEntry(name, filename, installed);
        }
    }

    private void showEmptyState(String message) {
        TextView empty = new TextView(requireContext());
        empty.setText(message);
        empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(0, dpToPx(32), 0, 0);
        fileListContainer.addView(empty);
    }

    private void addComponentEntry(String name, String filename, boolean installed) {
        LinearLayout item = new LinearLayout(requireContext());
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        int cardBgColor = getColorFromAttr(android.R.attr.colorBackground, Color.parseColor("#FFFFFF"), Color.parseColor("#1E1E1E"));
        item.setBackground(createRoundedBackground(cardBgColor, CORNER_RADIUS_DP));
        item.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            item.setElevation(dpToPx(ELEVATION_DP / 2));
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dpToPx(8));
        item.setLayoutParams(lp);

        // 左边：组件名 + 状态
        LinearLayout leftLayout = new LinearLayout(requireContext());
        leftLayout.setOrientation(LinearLayout.VERTICAL);
        leftLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView tv = new TextView(requireContext());
        tv.setText(name);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        leftLayout.addView(tv);

        TextView statusTv = new TextView(requireContext());
        if (installed) {
            statusTv.setText("✅ 已安装");
            statusTv.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            statusTv.setText("⬇️ 可下载");
            statusTv.setTextColor(Color.parseColor("#2196F3"));
        }
        statusTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        statusTv.setPadding(0, dpToPx(2), 0, 0);
        leftLayout.addView(statusTv);

        item.addView(leftLayout);

        // 右边：操作按钮
        if (installed) {
            ImageView deleteBtn = new ImageView(requireContext());
            deleteBtn.setImageResource(android.R.drawable.ic_menu_delete);
            deleteBtn.setColorFilter(Color.parseColor("#F44336"));
            deleteBtn.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
            deleteBtn.setOnClickListener(v -> showDeleteDialog(name));
            item.addView(deleteBtn);
        } else {
            ImageView downloadBtn = new ImageView(requireContext());
            downloadBtn.setImageResource(android.R.drawable.stat_sys_download);
            downloadBtn.setColorFilter(Color.parseColor("#2196F3"));
            downloadBtn.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
            downloadBtn.setOnClickListener(v -> showInstallDialog(filename));
            item.addView(downloadBtn);
        }

        fileListContainer.addView(item);
    }

    private void showInstallDialog(String filename) {
        new AlertDialog.Builder(requireContext())
                .setTitle("下载安装")
                .setMessage("下载并安装：" + filename)
                .setPositiveButton("确认下载", (d, w) -> {
                    showInstallProgressDialog();
                    new Thread(() -> {
                        try {
                            downloadAndInstall(filename);
                            requireActivity().runOnUiThread(() -> {
                                dismissInstallProgressDialog();
                                showToast("✔ 安装完成");
                                refreshInstalledStatus();
                            });
                        } catch (Exception e) {
                            requireActivity().runOnUiThread(() -> {
                                dismissInstallProgressDialog();
                                showToast("✘ 安装失败: " + e.getMessage());
                            });
                        }
                    }).start();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void downloadAndInstall(String filename) throws Exception {
        boolean isWine = isWineOrProton(currentCategory);
        String storagePath = isWine ? "rootfs/opt/installed-wine" : "installed_components";
        String installPath = isWine ? "" : currentCategory;
        File targetDir = new File(baseFilesPath, storagePath + File.separator + installPath);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new Exception("目录创建失败");
        }

        String baseName = filename.replace(TZSD_EXTENSION, "");
        File outFile = new File(targetDir, baseName + TZSD_EXTENSION);

        // 依次尝试多个加速节点
        for (String template : DOWNLOAD_URL_TEMPLATES) {
            String url = String.format(template, currentCategory, filename);
            try {
                tryDownload(url, outFile);
                if (outFile.exists() && outFile.length() > 0) {
                    return; // 下载成功
                }
            } catch (Exception e) {
                // 当前节点失败，尝试下一个
            }
        }

        throw new Exception("所有节点都下载失败");
    }

    private void tryDownload(String url, File outFile) throws Exception {
        java.net.URL downloadUrl = new java.net.URL(url);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) downloadUrl.openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        conn.setInstanceFollowRedirects(true);

        if (conn.getResponseCode() != java.net.HttpURLConnection.HTTP_OK) {
            return;
        }

        try (InputStream input = conn.getInputStream();
             OutputStream output = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = input.read(buffer)) != -1) output.write(buffer, 0, len);
        }
    }

    private void showDeleteDialog(String name) {
        new AlertDialog.Builder(requireContext())
                .setTitle("确认")
                .setMessage("确定删除 " + name + " 吗？")
                .setPositiveButton("确定删除", (d, w) -> deleteFile(name))
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteFile(String name) {
        boolean isWine = isWineOrProton(currentCategory);
        String storagePath = isWine ? "rootfs/opt/installed-wine" : "installed_components";
        String installPath = isWine ? "" : currentCategory;
        File target = new File(baseFilesPath, storagePath + File.separator + installPath + File.separator + name);

        boolean success;
        if (isWine) {
            success = deleteRecursive(target);
        } else {
            success = new File(target.getAbsolutePath() + TZSD_EXTENSION).delete();
        }

        if (success) {
            showToast("✔ 删除成功");
            refreshInstalledStatus();
        } else {
            showToast("✘ 删除失败");
        }
    }

    private boolean deleteRecursive(File file) {
        if (isSymbolicLink(file)) return file.delete();
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursive(child);
            }
        }
        return file.delete();
    }

    private boolean isSymbolicLink(File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Files.isSymbolicLink(file.toPath());
        } else {
            try {
                return !file.getCanonicalPath().equals(file.getAbsolutePath());
            } catch (IOException e) {
                return false;
            }
        }
    }

    private void showInstallProgressDialog() {
        if (installProgressDialog != null && installProgressDialog.isShowing()) return;

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(24));
        layout.setGravity(Gravity.CENTER);

        ProgressBar progressBar = new ProgressBar(requireContext());
        progressBar.setIndeterminate(true);
        layout.addView(progressBar);

        TextView message = new TextView(requireContext());
        message.setText("正在加载，请稍候...");
        message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        message.setPadding(0, dpToPx(16), 0, 0);
        layout.addView(message);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(layout);
        builder.setCancelable(false);
        installProgressDialog = builder.create();
        installProgressDialog.show();
    }

    private void dismissInstallProgressDialog() {
        if (installProgressDialog != null && installProgressDialog.isShowing()) {
            installProgressDialog.dismiss();
            installProgressDialog = null;
        }
    }

    private int getColorFromAttr(int attr, int lightDefault, int darkDefault) {
        TypedArray ta = requireContext().getTheme().obtainStyledAttributes(new int[]{attr});
        int color = ta.getColor(0, 0);
        ta.recycle();
        if (color != 0) return color;

        int nightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES ? darkDefault : lightDefault;
    }

    private GradientDrawable createRoundedBackground(int color, int radiusDP) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(dpToPx(radiusDP));
        gd.setColor(color);
        return gd;
    }

    private LinearLayout.LayoutParams createLayoutParams(int marginDP) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dpToPx(marginDP), 0, dpToPx(marginDP));
        return lp;
    }

    private int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                requireContext().getResources().getDisplayMetrics());
    }

    private void showToast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}

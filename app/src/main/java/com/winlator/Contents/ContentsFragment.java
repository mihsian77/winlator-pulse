package com.winlator.contents;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;      // 使用 AppCompat 版本支持深色模式
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.winlator.core.TarCompressorUtils;
import com.winlator.services.InstallService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class ContentsFragment extends Fragment {
    private static final String[] FILE_TYPES = {"dxvk", "box64", "turnip", "virgl", "vkd3d", "wine", "proton"};
    private static final String WHP_EXTENSION = ".whp";
    private static final String TZSD_EXTENSION = ".tzst";
    private static final int CORNER_RADIUS_DP = 12;
    private static final int ELEVATION_DP = 4;

    private static boolean isWineOrProton(String category) {
        return category.equalsIgnoreCase("wine") || category.equalsIgnoreCase("proton");
    }

    private String baseFilesPath;
    private String currentStoragePath = "installed_components";
    private String currentInstallPath = "";

    private LinearLayout fileListContainer;
    private Spinner categorySpinner;
    private String currentCategory = FILE_TYPES[0];
    private String installCategory;
    private Uri selectedFileUri;

    private AlertDialog installProgressDialog;
    private BroadcastReceiver installCompleteReceiver;
    private BroadcastReceiver installProgressReceiver;
    private CircularProgressIndicator installCircularProgress;
    private TextView installStageView;
    private TextView installPercentView;

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    validateSelectedFile();
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        baseFilesPath = requireContext().getFilesDir().getAbsolutePath();
        initDirectories();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null && activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle("组件管理");
        }
        // 注册安装完成广播接收器
        registerInstallReceiver();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dismissInstallProgressDialog();
        unregisterInstallReceiver();
    }

    /**
     * 注册安装完成广播接收器
     */
    private void registerInstallReceiver() {
        if (installCompleteReceiver != null) return;
        installCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String fileName = intent.getStringExtra("file_name");
                boolean success = intent.getBooleanExtra("success", false);
                String error = intent.getStringExtra("error");
                if (success) {
                    showToast("✔ 安装完成: " + fileName);
                    // 自动切换到对应分类并刷新列表
                    String detected = detectCategory(fileName);
                    if (detected != null) {
                        currentCategory = detected;
                        int pos = Arrays.asList(FILE_TYPES).indexOf(currentCategory);
                        if (pos >= 0 && categorySpinner != null) categorySpinner.setSelection(pos);
                    }
                    refreshFileList();
                } else if (error != null && !error.equals("已取消")) {
                    showToast("✘ 安装失败: " + error);
                }
            }
        };
        IntentFilter filter = new IntentFilter("com.winlator.action.INSTALL_COMPLETED");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(installCompleteReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(installCompleteReceiver, filter);
        }
        registerInstallProgressReceiver();
    }

    /**
     * 注册安装进度广播接收器
     */
    private void registerInstallProgressReceiver() {
        if (installProgressReceiver != null) return;
        installProgressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int progress = intent.getIntExtra(InstallService.EXTRA_PROGRESS, 0);
                String stage = intent.getStringExtra(InstallService.EXTRA_STAGE);
                updateInstallProgress(stage, progress);
            }
        };
        IntentFilter progressFilter = new IntentFilter("com.winlator.action.INSTALL_PROGRESS");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(installProgressReceiver, progressFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(installProgressReceiver, progressFilter);
        }
    }

    /**
     * 注销安装完成广播接收器
     */
    private void unregisterInstallReceiver() {
        if (installCompleteReceiver != null) {
            try {
                requireContext().unregisterReceiver(installCompleteReceiver);
            } catch (Exception ignored) {}
            installCompleteReceiver = null;
        }
        if (installProgressReceiver != null) {
            try {
                requireContext().unregisterReceiver(installProgressReceiver);
            } catch (Exception ignored) {}
            installProgressReceiver = null;
        }
    }

    private void initDirectories() {
        File imageFsDir = new File(requireContext().getFilesDir(), "imagefs");
        if (!imageFsDir.exists()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    Files.createSymbolicLink(imageFsDir.toPath(), Paths.get("./rootfs"));
                    Log.d("Symlink", "符号链接创建成功");
                } catch (Exception e) {
                    Log.e("Symlink", "创建符号链接失败", e);
                }
            } else {
                Log.w("Symlink", "符号链接需要 Android 8+，跳过创建");
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

        TextView title = new TextView(requireContext());
        title.setText("选择附加类型");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, dpToPx(12));
        root.addView(title);

        categorySpinner = new Spinner(requireContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, FILE_TYPES);
        categorySpinner.setAdapter(adapter);
        int cardBgColor = getColorFromAttr(android.R.attr.colorBackground, Color.parseColor("#FFFFFF"), Color.parseColor("#121212"));
        categorySpinner.setBackground(createRoundedBackground(cardBgColor, CORNER_RADIUS_DP));
        categorySpinner.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            categorySpinner.setElevation(dpToPx(ELEVATION_DP));
        }
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategory = FILE_TYPES[position];
                refreshFileList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        root.addView(categorySpinner, createLayoutParams(16));

        // 滚动列表
        ScrollView scroll = new ScrollView(requireContext());
        fileListContainer = new LinearLayout(requireContext());
        fileListContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(fileListContainer);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        // 安装按钮
        Button installBtn = new Button(requireContext());
        installBtn.setText("选择安装文件");
        installBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        installBtn.setTextColor(Color.WHITE);
        installBtn.setTypeface(Typeface.DEFAULT_BOLD);
        installBtn.setAllCaps(false);
        int primaryColor = getColorFromAttr(android.R.attr.colorPrimary, Color.parseColor("#2196F3"), Color.parseColor("#2196F3"));
        installBtn.setBackground(createRoundedBackground(primaryColor, CORNER_RADIUS_DP));
        installBtn.setPadding(0, dpToPx(14), 0, dpToPx(14));
        installBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            filePickerLauncher.launch(intent);
        });
        root.addView(installBtn, createLayoutParams(24));

        refreshFileList();
        return root;
    }

    private int getColorFromAttr(int attr, int lightDefault, int darkDefault) {
        TypedArray ta = requireContext().getTheme().obtainStyledAttributes(new int[]{attr});
        int color = ta.getColor(0, 0);
        ta.recycle();
        if (color != 0) return color;

        int nightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES ? darkDefault : lightDefault;
    }

    private void validateSelectedFile() {
        if (selectedFileUri == null) {
            showToast("无效文件选择");
            return;
        }

        String fileName = extractFileName(selectedFileUri);
        if (fileName == null) {
            showToast("无效文件选择");
            return;
        }

        String detected = detectCategory(fileName);
        if (detected == null) {
            showToast("无法识别文件类别，请确保文件名包含 " + TextUtils.join(", ", FILE_TYPES));
            return;
        }
        if (!fileName.toLowerCase().endsWith(WHP_EXTENSION)) {
            showToast("请选择 .whp 文件");
            return;
        }

        installCategory = detected;
        showInstallDialog(fileName);
    }

    private String detectCategory(String fileName) {
        String lower = fileName.toLowerCase();
        for (String type : FILE_TYPES) {
            if (lower.contains(type.toLowerCase())) return type;
        }
        return null;
    }

    private void showInstallDialog(String fileName) {
        new AlertDialog.Builder(requireContext())
                .setTitle("安全提示")
                .setMessage("即将安装：" + fileName + "\n请确认文件来源可靠")
                .setPositiveButton("确认安装", (d, w) -> {
                    // 显示应用内圆形进度对话框
                    showInstallProgressDialog(fileName);
                    // 启动后台安装服务
                    InstallService.startInstall(
                            requireContext(),
                            selectedFileUri,
                            fileName,
                            installCategory,
                            baseFilesPath
                    );
                })
                .setNegativeButton("取消操作", null)
                .show();
    }

    private void showInstallProgressDialog(String fileName) {
        if (installProgressDialog != null && installProgressDialog.isShowing()) return;

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(28), dpToPx(24), dpToPx(28), dpToPx(24));
        layout.setGravity(Gravity.CENTER);

        // 文件名
        TextView titleView = new TextView(requireContext());
        titleView.setText(fileName);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setMaxLines(1);
        titleView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, dpToPx(16));
        layout.addView(titleView);

        // 圆形进度条
        installCircularProgress = new CircularProgressIndicator(requireContext());
        installCircularProgress.setIndeterminate(false);
        installCircularProgress.setProgress(0);
        LinearLayout.LayoutParams cpLp = new LinearLayout.LayoutParams(dpToPx(72), dpToPx(72));
        cpLp.gravity = Gravity.CENTER;
        installCircularProgress.setLayoutParams(cpLp);
        layout.addView(installCircularProgress);

        // 阶段文字
        installStageView = new TextView(requireContext());
        installStageView.setText("正在安装");
        installStageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        installStageView.setGravity(Gravity.CENTER);
        installStageView.setPadding(0, dpToPx(12), 0, 0);
        layout.addView(installStageView);

        // 百分比
        installPercentView = new TextView(requireContext());
        installPercentView.setText("0%");
        installPercentView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        installPercentView.setGravity(Gravity.CENTER);
        installPercentView.setPadding(0, dpToPx(4), 0, 0);
        layout.addView(installPercentView);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(layout);
        builder.setCancelable(false);
        installProgressDialog = builder.create();
        installProgressDialog.show();
    }

    /**
     * 更新安装进度（圆形进度条）
     */
    private void updateInstallProgress(String stage, int progress) {
        if (installCircularProgress != null) {
            installCircularProgress.setProgress(progress);
        }
        if (installStageView != null && stage != null) {
            installStageView.setText(stage);
        }
        if (installPercentView != null) {
            installPercentView.setText(progress + "%");
        }
        if (progress >= 100 && installProgressDialog != null && installProgressDialog.isShowing()) {
            installProgressDialog.dismiss();
            installProgressDialog = null;
            installCircularProgress = null;
            installStageView = null;
            installPercentView = null;
        }
    }

    private void dismissInstallProgressDialog() {
        if (installProgressDialog != null && installProgressDialog.isShowing()) {
            installProgressDialog.dismiss();
            installProgressDialog = null;
        }
    }

    private void performInstall(String fileName) throws Exception {
        boolean isWine = isWineOrProton(installCategory);
        String storagePath = isWine ? "rootfs/opt/installed-wine" : "installed_components";
        String installPath = isWine ? "" : installCategory;
        File targetDir = new File(baseFilesPath, storagePath + File.separator + installPath);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new Exception("目录创建失败");
        }

        String baseName = fileName.replaceAll("(?i)" + WHP_EXTENSION + "$", "");
        try (InputStream input = requireContext().getContentResolver().openInputStream(selectedFileUri)) {
            if (input == null) throw new Exception("无法读取文件");

            if (isWine) {
                TarCompressorUtils.Type type = detectCompressionType(selectedFileUri);
                if (type == null) throw new Exception("不支持的文件格式，请提供 XZ 或 ZSTD 压缩的 Wine/Proton 包");
                if (!TarCompressorUtils.extract(type, requireContext(), selectedFileUri, targetDir, null)) {
                    throw new Exception("解压失败");
                }
            } else {
                File outFile = new File(targetDir, baseName + TZSD_EXTENSION);
                try (OutputStream output = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = input.read(buffer)) != -1) output.write(buffer, 0, len);
                }
            }
        }
    }

    private TarCompressorUtils.Type detectCompressionType(Uri uri) throws IOException {
        byte[] magic = new byte[6];
        try (InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
            if (is == null || is.read(magic) < 4) return null;
            if ((magic[0] & 0xFF) == 0xFD && (magic[1] & 0xFF) == 0x37 &&
                (magic[2] & 0xFF) == 0x7A && (magic[3] & 0xFF) == 0x58 &&
                (magic[4] & 0xFF) == 0x5A && (magic[5] & 0xFF) == 0x00) {
                return TarCompressorUtils.Type.XZ;
            }
            if ((magic[0] & 0xFF) == 0x28 && (magic[1] & 0xFF) == 0xB5 &&
                (magic[2] & 0xFF) == 0x2F && (magic[3] & 0xFF) == 0xFD) {
                return TarCompressorUtils.Type.ZSTD;
            }
            return null;
        }
    }

    private void sanitizeWineFolderNames(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        File[] folders = dir.listFiles(File::isDirectory);
        if (folders == null) return;

        for (File folder : folders) {
            String oldName = folder.getName();
            if (oldName.endsWith("-")) {
                String newName = oldName.substring(0, oldName.length() - 1);
                File newFolder = new File(folder.getParent(), newName);
                if (folder.renameTo(newFolder)) {
                    Log.d("WineFolder", "重命名文件夹: " + oldName + " -> " + newName);
                    String version = extractVersionFromName(oldName);
                    if (version != null) {
                        File oldPattern = new File(folder.getParent(), "container-pattern-" + version + ".tzst");
                        if (oldPattern.exists()) {
                            String newVersion = extractVersionFromName(newName);
                            if (newVersion != null) {
                                File newPattern = new File(folder.getParent(), "container-pattern-" + newVersion + ".tzst");
                                oldPattern.renameTo(newPattern);
                                Log.d("WineFolder", "重命名 pattern: " + oldPattern.getName() + " -> " + newPattern.getName());
                            }
                        }
                    }
                } else {
                    Log.e("WineFolder", "重命名失败: " + oldName);
                }
            }
        }
    }

    private String extractVersionFromName(String folderName) {
        int dash = folderName.indexOf('-');
        if (dash == -1) return null;
        String version = folderName.substring(dash + 1);
        if (version.endsWith("-")) version = version.substring(0, version.length() - 1);
        return version;
    }

    private void refreshFileList() {
        boolean isWine = isWineOrProton(currentCategory);
        currentStoragePath = isWine ? "rootfs/opt/installed-wine" : "installed_components";
        currentInstallPath = isWine ? "" : currentCategory;

        fileListContainer.removeAllViews();
        File dir = new File(baseFilesPath, currentStoragePath + File.separator + currentInstallPath);
        if (!dir.exists() || !dir.isDirectory()) {
            showEmptyState();
            return;
        }

        if (isWine) {
            sanitizeWineFolderNames(dir);
        }

        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            showEmptyState();
            return;
        }

        boolean hasEntries = false;
        for (File f : files) {
            if (isWine) {
                if (f.isDirectory()) {
                    addFileEntry(f.getName());
                    hasEntries = true;
                }
            } else {
                String name = f.isFile() ? f.getName().replace(TZSD_EXTENSION, "") : f.getName();
                addFileEntry(name);
                hasEntries = true;
            }
        }
        if (!hasEntries) showEmptyState();
    }

    private void showEmptyState() {
        TextView empty = new TextView(requireContext());
        empty.setText("当前无已安装项目");
        empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(0, dpToPx(32), 0, 0);
        fileListContainer.addView(empty);
    }

    private void addFileEntry(String name) {
        LinearLayout item = new LinearLayout(requireContext());
        item.setOrientation(LinearLayout.HORIZONTAL);
        int cardBgColor = getColorFromAttr(android.R.attr.colorBackground, Color.parseColor("#FFFFFF"), Color.parseColor("#121212"));
        item.setBackground(createRoundedBackground(cardBgColor, CORNER_RADIUS_DP));
        item.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            item.setElevation(dpToPx(ELEVATION_DP / 2));
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dpToPx(10));
        item.setLayoutParams(lp);

        // 组件图标（按类型显示首字母+主题色背景）
        TextView iconView = new TextView(requireContext());
        iconView.setText(getCategoryIcon(currentCategory));
        iconView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        iconView.setTextColor(Color.WHITE);
        iconView.setTypeface(Typeface.DEFAULT_BOLD);
        iconView.setGravity(Gravity.CENTER);
        int iconSize = dpToPx(44);
        iconView.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
        iconView.setBackground(createRoundedBackground(getCategoryColor(currentCategory), 12));
        item.addView(iconView);

        // 中间信息区：名称 + 描述/大小
        LinearLayout infoLayout = new LinearLayout(requireContext());
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        infoLayout.setPadding(dpToPx(14), 0, dpToPx(8), 0);

        TextView tv = new TextView(requireContext());
        tv.setText(name);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setTextColor(getColorFromAttr(android.R.attr.textColorPrimary, Color.parseColor("#212121"), Color.parseColor("#FFFFFF")));
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setMaxLines(1);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        infoLayout.addView(tv);

        // 描述行：类型 + 大小
        TextView descView = new TextView(requireContext());
        String fileSize = getFileSizeString(name);
        String categoryName = getCategoryDisplayName(currentCategory);
        descView.setText(categoryName + (fileSize != null ? " · " + fileSize : ""));
        descView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        descView.setTextColor(getColorFromAttr(android.R.attr.textColorSecondary, Color.parseColor("#757575"), Color.parseColor("#B0B0B0")));
        descView.setMaxLines(1);
        descView.setPadding(0, dpToPx(2), 0, 0);
        infoLayout.addView(descView);

        item.addView(infoLayout);

        // 右侧状态标识
        TextView statusView = new TextView(requireContext());
        statusView.setText("已安装");
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        statusView.setTextColor(Color.parseColor("#4CAF50"));
        statusView.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        statusView.setBackground(createRoundedBackground(Color.parseColor("#E8F5E9"), 8));
        item.addView(statusView);

        // 右侧操作按钮：信息 + 删除
        LinearLayout btnLayout = new LinearLayout(requireContext());
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setGravity(Gravity.CENTER_VERTICAL);
        btnLayout.setPadding(dpToPx(6), 0, 0, 0);

        Button infoBtn = new Button(requireContext());
        infoBtn.setText("信息");
        infoBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        infoBtn.setTextColor(Color.parseColor("#2196F3"));
        infoBtn.setAllCaps(false);
        infoBtn.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
        infoBtn.setBackground(createRoundedBackground(Color.parseColor("#E3F2FD"), 8));
        infoBtn.setOnClickListener(v -> showInfoDialog(name));
        btnLayout.addView(infoBtn);

        Button delBtn = new Button(requireContext());
        delBtn.setText("删除");
        delBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        delBtn.setTextColor(Color.parseColor("#F44336"));
        delBtn.setAllCaps(false);
        delBtn.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
        delBtn.setBackground(createRoundedBackground(Color.parseColor("#FFEBEE"), 8));
        LinearLayout.LayoutParams delLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        delLp.setMargins(dpToPx(8), 0, 0, 0);
        delBtn.setLayoutParams(delLp);
        delBtn.setOnClickListener(v -> showDeleteDialog(name));
        btnLayout.addView(delBtn);

        item.addView(btnLayout);
        fileListContainer.addView(item);
    }

    /**
     * 显示组件信息对话框
     */
    private void showInfoDialog(String name) {
        File target = new File(baseFilesPath, currentStoragePath + File.separator + currentInstallPath + File.separator + name);
        StringBuilder info = new StringBuilder();
        info.append("名称：").append(name).append("\n");
        info.append("类型：").append(getCategoryDisplayName(currentCategory)).append("\n");
        if (isWineOrProton(currentCategory)) {
            info.append("路径：rootfs/opt/installed-wine/").append(name).append("\n");
        } else {
            info.append("路径：installed_components/").append(currentCategory).append("/").append(name).append("\n");
        }
        info.append("大小：").append(formatFileSize(getFolderSize(target))).append("\n");
        new AlertDialog.Builder(requireContext())
                .setTitle("组件信息")
                .setMessage(info.toString())
                .setPositiveButton("知道了", null)
                .setNegativeButton("删除", (d, w) -> showDeleteDialog(name))
                .show();
    }

    /**
     * 获取组件类型图标（首字母）
     */
    private String getCategoryIcon(String category) {
        switch (category.toLowerCase()) {
            case "dxvk": return "D";
            case "box64": return "B";
            case "turnip": return "T";
            case "virgl": return "V";
            case "vkd3d": return "K";
            case "wine": return "W";
            case "proton": return "P";
            default: return "?";
        }
    }

    /**
     * 获取组件类型主题色
     */
    private int getCategoryColor(String category) {
        switch (category.toLowerCase()) {
            case "dxvk": return Color.parseColor("#673AB7"); // 紫色
            case "box64": return Color.parseColor("#FF9800"); // 橙色
            case "turnip": return Color.parseColor("#4CAF50"); // 绿色
            case "virgl": return Color.parseColor("#2196F3"); // 蓝色
            case "vkd3d": return Color.parseColor("#E91E63"); // 粉色
            case "wine": return Color.parseColor("#F44336"); // 红色
            case "proton": return Color.parseColor("#00BCD4"); // 青色
            default: return Color.parseColor("#607D8B"); // 灰色
        }
    }

    /**
     * 获取组件类型显示名称
     */
    private String getCategoryDisplayName(String category) {
        switch (category.toLowerCase()) {
            case "dxvk": return "DXVK 翻译层";
            case "box64": return "Box64 模拟器";
            case "turnip": return "Turnip 驱动";
            case "virgl": return "VirGL 渲染器";
            case "vkd3d": return "VKD3D 翻译层";
            case "wine": return "Wine 运行环境";
            case "proton": return "Proton 运行环境";
            default: return category;
        }
    }

    /**
     * 获取文件大小字符串
     */
    private String getFileSizeString(String name) {
        try {
            File file;
            if (isWineOrProton(currentCategory)) {
                file = new File(baseFilesPath, currentStoragePath + File.separator + currentInstallPath + File.separator + name);
            } else {
                file = new File(baseFilesPath, currentStoragePath + File.separator + currentInstallPath + File.separator + name + TZSD_EXTENSION);
            }
            if (!file.exists()) return null;
            long size = getFolderSize(file);
            return formatFileSize(size);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 递归计算文件夹大小
     */
    private long getFolderSize(File file) {
        if (file == null || !file.exists()) return 0;
        if (file.isFile()) return file.length();
        long size = 0;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                size += getFolderSize(child);
            }
        }
        return size;
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double size = bytes;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        if (unitIndex == 0) return String.format(java.util.Locale.US, "%d %s", (long)size, units[unitIndex]);
        return String.format(java.util.Locale.US, "%.1f %s", size, units[unitIndex]);
    }

    private void showDeleteDialog(String name) {
        new AlertDialog.Builder(requireContext())
                .setTitle("确认")
                .setMessage("确定删除 " + name + " 吗？")
                .setPositiveButton("确定删除", (d, w) -> deleteFile(name))
                .setNegativeButton("取消操作", null)
                .show();
    }

    private void deleteFile(String name) {
        File target = new File(baseFilesPath, currentStoragePath + File.separator + currentInstallPath + File.separator + name);
        boolean success;
        if (isWineOrProton(currentCategory)) {
            int dash = name.indexOf('-');
            if (dash == -1) {
                showToast("无法识别版本");
                return;
            }
            String version = name.substring(dash + 1);
            if (version.endsWith("-")) version = version.substring(0, version.length() - 1);
            File pattern = new File(baseFilesPath, currentStoragePath + File.separator + currentInstallPath +
                    File.separator + "container-pattern-" + version + ".tzst");
            pattern.delete();
            success = deleteRecursive(target);
        } else {
            success = new File(target.getAbsolutePath() + TZSD_EXTENSION).delete();
        }
        if (success) {
            showToast("✔ 删除成功");
            refreshFileList();
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

    private String extractFileName(Uri uri) {
        String name = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    name = cursor.getString(idx);
                }
            }
        }
        if (name == null) {
            String path = uri.getPath();
            if (path != null) name = path.substring(path.lastIndexOf('/') + 1);
        }
        return name;
    }

    private int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                requireContext().getResources().getDisplayMetrics());
    }

    private void showToast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
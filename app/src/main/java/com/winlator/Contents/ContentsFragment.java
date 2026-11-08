package com.winlator.contents;

<<<<<<< HEAD
=======
import android.app.Activity;
import android.content.Intent;
>>>>>>> upstream/main
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
<<<<<<< HEAD
=======
import android.text.TextUtils;
>>>>>>> upstream/main
import android.util.Log;
import android.util.TypedValue;
import android.content.res.TypedArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
<<<<<<< HEAD
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
=======
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
>>>>>>> upstream/main
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
<<<<<<< HEAD
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.winlator.core.HttpUtils;
=======
import androidx.appcompat.app.AlertDialog;      // 使用 AppCompat 版本支持深色模式
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

>>>>>>> upstream/main
import com.winlator.core.TarCompressorUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
<<<<<<< HEAD
import java.util.ArrayList;
import java.util.HashSet;

public class ContentsFragment extends Fragment {
    private static final String[] FILE_TYPES = {"dxvk", "vkd3d", "box64", "turnip", "wined3d", "d7vk", "d8vk", "virgl", "wine", "proton"};

    // 有在线仓库的组件类型（可以在线下载）
    private static final java.util.Set<String> ONLINE_TYPES = new java.util.HashSet<>(java.util.Arrays.asList("dxvk", "vkd3d", "box64", "turnip", "wined3d", "d7vk", "d8vk"));
=======
import java.util.Arrays;

public class ContentsFragment extends Fragment {
    private static final String[] FILE_TYPES = {"dxvk", "box64", "turnip", "virgl", "vkd3d", "wine", "proton"};
    private static final String WHP_EXTENSION = ".whp";
>>>>>>> upstream/main
    private static final String TZSD_EXTENSION = ".tzst";
    private static final int CORNER_RADIUS_DP = 12;
    private static final int ELEVATION_DP = 4;

<<<<<<< HEAD
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

=======
>>>>>>> upstream/main
    private static boolean isWineOrProton(String category) {
        return category.equalsIgnoreCase("wine") || category.equalsIgnoreCase("proton");
    }

    private String baseFilesPath;
<<<<<<< HEAD
    private String currentCategory = FILE_TYPES[0];

    private LinearLayout fileListContainer;
    private TextView categorySelector;
    private ArrayList<String> availableComponents = new ArrayList<>();
    private HashSet<String> installedComponents = new HashSet<>();

    private ActivityResultLauncher<String[]> filePickerLauncher;
    private AlertDialog installProgressDialog;

=======
    private String currentStoragePath = "installed_components";
    private String currentInstallPath = "";

    private LinearLayout fileListContainer;
    private Spinner categorySpinner;
    private String currentCategory = FILE_TYPES[0];
    private String installCategory;
    private Uri selectedFileUri;

    private AlertDialog installProgressDialog;

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    validateSelectedFile();
                }
            });

>>>>>>> upstream/main
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        baseFilesPath = requireContext().getFilesDir().getAbsolutePath();
        initDirectories();
<<<<<<< HEAD

        // 注册文件选择器
        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) installFromUri(uri);
        });
=======
>>>>>>> upstream/main
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
<<<<<<< HEAD
                } catch (Exception e) {
                    Log.e("Symlink", "创建符号链接失败", e);
                }
=======
                    Log.d("Symlink", "符号链接创建成功");
                } catch (Exception e) {
                    Log.e("Symlink", "创建符号链接失败", e);
                }
            } else {
                Log.w("Symlink", "符号链接需要 Android 8+，跳过创建");
>>>>>>> upstream/main
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
<<<<<<< HEAD
        root.setBackgroundColor(getColorFromAttr(android.R.attr.colorBackground, Color.parseColor("#F5F5F5"), Color.parseColor("#121212")));
        int padding = dpToPx(16);
        root.setPadding(padding, padding, padding, padding);

        // === 顶部标题区域 ===
        LinearLayout headerLayout = new LinearLayout(requireContext());
        headerLayout.setOrientation(LinearLayout.VERTICAL);
        headerLayout.setPadding(0, dpToPx(8), 0, dpToPx(16));

        TextView title = new TextView(requireContext());
        title.setText("组件管理");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(getColorFromAttr(android.R.attr.textColorPrimary, Color.parseColor("#212121"), Color.parseColor("#FFFFFF")));
        headerLayout.addView(title);

        TextView subtitle = new TextView(requireContext());
        subtitle.setText("下载并安装组件，支持多版本共存");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        subtitle.setTextColor(getColorFromAttr(android.R.attr.textColorSecondary, Color.parseColor("#757575"), Color.parseColor("#B0B0B0")));
        subtitle.setPadding(0, dpToPx(4), 0, 0);
        headerLayout.addView(subtitle);

        root.addView(headerLayout);

        // === 折叠栏：组件类型选择 ===
        categorySelector = new TextView(requireContext());
        categorySelector.setText("▼ 组件类型：" + currentCategory);
        categorySelector.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        categorySelector.setTypeface(Typeface.DEFAULT_BOLD);
        int cardBgColor = getColorFromAttr(android.R.attr.colorBackground, Color.parseColor("#FFFFFF"), Color.parseColor("#1E1E1E"));
        categorySelector.setBackground(createRoundedBackground(cardBgColor, CORNER_RADIUS_DP));
        categorySelector.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            categorySelector.setElevation(dpToPx(ELEVATION_DP));
        }
        categorySelector.setOnClickListener(v -> showCategoryDialog());
        root.addView(categorySelector, createLayoutParams(12));

        // === 滚动列表 ===
=======
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
>>>>>>> upstream/main
        ScrollView scroll = new ScrollView(requireContext());
        fileListContainer = new LinearLayout(requireContext());
        fileListContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(fileListContainer);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

<<<<<<< HEAD
        // === 底部按钮 ===
        LinearLayout btnLayout = new LinearLayout(requireContext());
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);

        // 刷新按钮（图标按钮）
        LinearLayout refreshBtn = createIconButton(
                android.R.drawable.ic_menu_rotate,
                "刷新",
                getColorFromAttr(android.R.attr.colorPrimary, Color.parseColor("#2196F3"), Color.parseColor("#2196F3")),
                v -> loadAvailableComponents()
        );
        LinearLayout.LayoutParams refreshLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        refreshLp.setMargins(0, 0, dpToPx(8), 0);
        refreshBtn.setLayoutParams(refreshLp);
        btnLayout.addView(refreshBtn);

        // 手动安装按钮（图标按钮）
        LinearLayout installBtn = createIconButton(
                android.R.drawable.ic_menu_upload,
                "安装文件",
                getColorFromAttr(android.R.attr.colorAccent, Color.parseColor("#FF9800"), Color.parseColor("#FF9800")),
                v -> showFilePicker()
        );
        LinearLayout.LayoutParams installLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        installLp.setMargins(dpToPx(8), 0, 0, 0);
        installBtn.setLayoutParams(installLp);
        btnLayout.addView(installBtn);

        root.addView(btnLayout, createLayoutParams(12));

        // 加载组件列表
        loadAvailableComponents();
        return root;
    }

    private LinearLayout createIconButton(int iconRes, String label, int bgColor, View.OnClickListener listener) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackground(createRoundedBackground(bgColor, CORNER_RADIUS_DP));
        layout.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            layout.setElevation(dpToPx(ELEVATION_DP));
        }
        layout.setOnClickListener(listener);

        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.WHITE);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dpToPx(20), dpToPx(20));
        iconLp.setMargins(0, 0, dpToPx(8), 0);
        icon.setLayoutParams(iconLp);
        layout.addView(icon);

        TextView tv = new TextView(requireContext());
        tv.setText(label);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setTextColor(Color.WHITE);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        layout.addView(tv);

        return layout;
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
                if (fileName == null) {
                    requireActivity().runOnUiThread(() -> {
                        dismissInstallProgressDialog();
                        showToast("无效文件选择");
                    });
                    return;
                }

                // 支持 .whp 和 .tzst 两种格式
                String lowerName = fileName.toLowerCase();
                if (!lowerName.endsWith(".whp") && !lowerName.endsWith(".tzst")) {
                    requireActivity().runOnUiThread(() -> {
                        dismissInstallProgressDialog();
                        showToast("请选择 .whp 或 .tzst 格式文件");
                    });
                    return;
                }

                boolean isWine = isWineOrProton(currentCategory);
                String storagePath = isWine ? "rootfs/opt/installed-wine" : "installed_components";
                String installPath = isWine ? "" : currentCategory;
                File targetDir = new File(baseFilesPath, storagePath + File.separator + installPath);
                if (!targetDir.exists() && !targetDir.mkdirs()) {
                    throw new Exception("目录创建失败");
                }

                if (isWine) {
                    // wine/proton：直接解压安装
                    TarCompressorUtils.Type type = detectCompressionType(uri);
                    if (type == null) throw new Exception("不支持的文件格式，请提供 XZ 或 ZSTD 压缩的 Wine/Proton 包");
                    if (!TarCompressorUtils.extract(type, requireContext(), uri, targetDir, null)) {
                        throw new Exception("解压失败");
                    }
                } else {
                    // 其他组件：把 .whp 文件重命名为 .tzst 存放
                    String baseName = fileName.replaceAll("(?i)\\.whp$", "").replaceAll("(?i)\\.tzst$", "");
                    File outFile = new File(targetDir, baseName + ".tzst");
                    try (InputStream input = requireContext().getContentResolver().openInputStream(uri);
                         OutputStream output = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = input.read(buffer)) != -1) output.write(buffer, 0, len);
                    }
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

    private TarCompressorUtils.Type detectCompressionType(Uri uri) throws IOException {
        byte[] magic = new byte[6];
        try (InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
            if (is == null || is.read(magic) < 4) return null;
            // XZ magic: FD 37 7A 58 5A 00
            if ((magic[0] & 0xFF) == 0xFD && (magic[1] & 0xFF) == 0x37 &&
                (magic[2] & 0xFF) == 0x7A && (magic[3] & 0xFF) == 0x58 &&
                (magic[4] & 0xFF) == 0x5A && (magic[5] & 0xFF) == 0x00) {
                return TarCompressorUtils.Type.XZ;
            }
            // ZSTD magic: 28 B5 2F FD
            if ((magic[0] & 0xFF) == 0x28 && (magic[1] & 0xFF) == 0xB5 &&
                (magic[2] & 0xFF) == 0x2F && (magic[3] & 0xFF) == 0xFD) {
                return TarCompressorUtils.Type.ZSTD;
            }
            return null;
        }
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
                    // 跳过空行、注释/标题行（部分 index.txt 带 "# xxx" 之类的标题）
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    // 只接受真实的组件文件名，避免把标题文字当成可下载条目
                    String lower = line.toLowerCase();
                    if (!lower.endsWith(TZSD_EXTENSION) && !lower.endsWith(".sf2")) continue;
                    availableComponents.add(line);
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
        item.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            item.setElevation(dpToPx(ELEVATION_DP / 2));
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dpToPx(10));
        item.setLayoutParams(lp);

        // 左边：文件图标（不同类型不同颜色）
        int iconColor = getIconColorForType(currentCategory);
        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(android.R.drawable.ic_menu_save);
        icon.setColorFilter(iconColor);
        icon.setPadding(dpToPx(4), dpToPx(4), dpToPx(12), dpToPx(4));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dpToPx(36), dpToPx(36));
        iconLp.setMargins(0, 0, dpToPx(12), 0);
        icon.setLayoutParams(iconLp);
        item.addView(icon);

        // 中间：组件名 + 状态标签
        LinearLayout middleLayout = new LinearLayout(requireContext());
        middleLayout.setOrientation(LinearLayout.VERTICAL);
        middleLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView tv = new TextView(requireContext());
        tv.setText(name);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(getColorFromAttr(android.R.attr.textColorPrimary, Color.parseColor("#212121"), Color.parseColor("#FFFFFF")));
        middleLayout.addView(tv);

        TextView statusTv = new TextView(requireContext());
        if (installed) {
            statusTv.setText("已安装");
            statusTv.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            statusTv.setText("可下载");
            statusTv.setTextColor(getColorFromAttr(android.R.attr.textColorSecondary, Color.parseColor("#9E9E9E"), Color.parseColor("#757575")));
        }
        statusTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        statusTv.setPadding(0, dpToPx(2), 0, 0);
        middleLayout.addView(statusTv);

        item.addView(middleLayout);

        // 右边：圆形图标按钮
        if (installed) {
            // 删除按钮（圆形红色）
            ImageView deleteBtn = createCircleIconButton(
                    android.R.drawable.ic_menu_delete,
                    Color.parseColor("#F44336"),
                    v -> showDeleteDialog(name)
            );
            item.addView(deleteBtn);
        } else {
            // 下载按钮（圆形蓝色）
            ImageView downloadBtn = createCircleIconButton(
                    android.R.drawable.stat_sys_download,
                    getColorFromAttr(android.R.attr.colorPrimary, Color.parseColor("#2196F3"), Color.parseColor("#1976D2")),
                    v -> showInstallDialog(filename)
            );
            item.addView(downloadBtn);
        }

        fileListContainer.addView(item);
    }

    private int getIconColorForType(String type) {
        switch (type) {
            case "dxvk": return Color.parseColor("#2196F3");
            case "vkd3d": return Color.parseColor("#9C27B0");
            case "box64": return Color.parseColor("#4CAF50");
            case "turnip": return Color.parseColor("#FF9800");
            case "wined3d": return Color.parseColor("#00BCD4");
            case "d7vk": return Color.parseColor("#FF5722");
            case "d8vk": return Color.parseColor("#E91E63");
            default: return getColorFromAttr(android.R.attr.colorPrimary, Color.parseColor("#2196F3"), Color.parseColor("#64B5F6"));
        }
    }

    private ImageView createCircleIconButton(int iconRes, int bgColor, View.OnClickListener listener) {
        ImageView btn = new ImageView(requireContext());
        btn.setImageResource(iconRes);
        btn.setColorFilter(Color.WHITE);
        btn.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        btn.setBackground(createRoundedBackground(bgColor, dpToPx(20)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40));
        lp.setMargins(dpToPx(8), 0, 0, 0);
        btn.setLayoutParams(lp);
        btn.setOnClickListener(listener);
        return btn;
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
=======
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
                    showInstallProgressDialog();
                    new Thread(() -> {
                        try {
                            performInstall(fileName);
                            requireActivity().runOnUiThread(() -> {
                                dismissInstallProgressDialog();
                                showToast("✔ 安装完成");
                                currentCategory = installCategory;
                                int pos = Arrays.asList(FILE_TYPES).indexOf(currentCategory);
                                if (pos >= 0) categorySpinner.setSelection(pos);
                                refreshFileList();
>>>>>>> upstream/main
                            });
                        } catch (Exception e) {
                            requireActivity().runOnUiThread(() -> {
                                dismissInstallProgressDialog();
                                showToast("✘ 安装失败: " + e.getMessage());
                            });
                        }
                    }).start();
                })
<<<<<<< HEAD
                .setNegativeButton("取消", null)
                .show();
    }

    private void downloadAndInstall(String filename) throws Exception {
        boolean isWine = isWineOrProton(currentCategory);
        String storagePath = isWine ? "rootfs/opt/installed-wine" : "installed_components";
        String installPath = isWine ? "" : currentCategory;
=======
                .setNegativeButton("取消操作", null)
                .show();
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
        message.setText("正在安装，请稍候...");
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

    private void performInstall(String fileName) throws Exception {
        boolean isWine = isWineOrProton(installCategory);
        String storagePath = isWine ? "rootfs/opt/installed-wine" : "installed_components";
        String installPath = isWine ? "" : installCategory;
>>>>>>> upstream/main
        File targetDir = new File(baseFilesPath, storagePath + File.separator + installPath);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new Exception("目录创建失败");
        }

<<<<<<< HEAD
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
=======
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
        item.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            item.setElevation(dpToPx(ELEVATION_DP / 2));
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dpToPx(8));
        item.setLayoutParams(lp);

        TextView tv = new TextView(requireContext());
        tv.setText(name);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        item.addView(tv);

        item.setOnLongClickListener(v -> {
            showDeleteDialog(name);
            return true;
        });
        fileListContainer.addView(item);
>>>>>>> upstream/main
    }

    private void showDeleteDialog(String name) {
        new AlertDialog.Builder(requireContext())
                .setTitle("确认")
                .setMessage("确定删除 " + name + " 吗？")
                .setPositiveButton("确定删除", (d, w) -> deleteFile(name))
<<<<<<< HEAD
                .setNegativeButton("取消", null)
=======
                .setNegativeButton("取消操作", null)
>>>>>>> upstream/main
                .show();
    }

    private void deleteFile(String name) {
<<<<<<< HEAD
        boolean isWine = isWineOrProton(currentCategory);
        String storagePath = isWine ? "rootfs/opt/installed-wine" : "installed_components";
        String installPath = isWine ? "" : currentCategory;
        File target = new File(baseFilesPath, storagePath + File.separator + installPath + File.separator + name);

        boolean success;
        if (isWine) {
=======
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
>>>>>>> upstream/main
            success = deleteRecursive(target);
        } else {
            success = new File(target.getAbsolutePath() + TZSD_EXTENSION).delete();
        }
<<<<<<< HEAD

        if (success) {
            showToast("✔ 删除成功");
            refreshInstalledStatus();
=======
        if (success) {
            showToast("✔ 删除成功");
            refreshFileList();
>>>>>>> upstream/main
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

<<<<<<< HEAD
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

=======
>>>>>>> upstream/main
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

<<<<<<< HEAD
=======
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

>>>>>>> upstream/main
    private int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                requireContext().getResources().getDisplayMetrics());
    }

    private void showToast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
<<<<<<< HEAD
}
=======
}
>>>>>>> upstream/main

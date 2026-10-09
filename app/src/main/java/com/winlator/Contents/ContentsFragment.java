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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import com.winlator.R;
import com.winlator.core.TarCompressorUtils;
import com.winlator.services.InstallService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 组件管理（附加内容）页面
 *
 * 界面：横向 Chip 分类 + 卡片列表 + 空态提示 + 底部安装按钮
 * 操作：列表项菜单支持查看信息 / 删除
 * 安装：本地 .whp 安装走 InstallService 后台安装 + 应用内圆形进度对话框（阶段+百分比）
 * 下载：在线资源下载走 DownloadProgressDialog（带速度/已下载/剩余时间）
 */
public class ContentsFragment extends Fragment {
    private static final String[] FILE_TYPES = {"wine", "proton", "dxvk", "box64", "turnip", "virgl", "vkd3d"};
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

    private RecyclerView recyclerView;
    private View emptyState;
    private TextView emptyIconView;
    private TextView emptyTextView;
    private ChipGroup chipGroup;
    private String currentCategory = FILE_TYPES[0];
    private String installCategory;
    private Uri selectedFileUri;

    private AlertDialog installProgressDialog;
    private BroadcastReceiver installCompleteReceiver;
    private BroadcastReceiver installProgressReceiver;
    private CircularProgressIndicator installCircularProgress;
    private TextView installStageView;
    private TextView installPercentView;

    private final List<String> installedItems = new ArrayList<>();

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
                    showToast("安装完成: " + fileName);
                    refreshFileList();
                } else if (error != null && !error.equals("已取消")) {
                    showToast("安装失败: " + error);
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
            }
        }
        for (String type : FILE_TYPES) {
            File dir = new File(baseFilesPath, "installed_components" + File.separator + type);
            if (!dir.exists()) dir.mkdirs();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.contents_fragment, container, false);

        chipGroup = root.findViewById(R.id.ChipGroupCategories);
        buildCategoryChips();

        recyclerView = root.findViewById(R.id.RecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        emptyState = root.findViewById(R.id.LLEmptyState);
        emptyIconView = root.findViewById(R.id.TVEmptyIcon);
        emptyTextView = root.findViewById(R.id.TVEmptyText);

        MaterialButton installBtn = root.findViewById(R.id.BTInstallContent);
        installBtn.setText("选择安装文件");
        installBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            filePickerLauncher.launch(intent);
        });

        refreshFileList();
        return root;
    }

    /**
     * 构建分类 Chip（横向滚动单选）
     */
    private void buildCategoryChips() {
        if (chipGroup == null) return;
        chipGroup.removeAllViews();
        for (String type : FILE_TYPES) {
            Chip chip = new Chip(requireContext());
            chip.setText(getCategoryDisplayName(type));
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(android.R.color.transparent);
            chip.setCheckedIconVisible(false);
            chip.setTextSize(13);
            chip.setChipStrokeWidth(dpToPx(1));
            chip.setOnClickListener(v -> {
                currentCategory = type;
                refreshFileList();
            });
            chipGroup.addView(chip);
            if (type.equals(currentCategory)) {
                chip.setChecked(true);
            }
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

    /**
     * 安装确认对话框
     */
    private void showInstallDialog(String fileName) {
        new AlertDialog.Builder(requireContext())
                .setTitle("安全提示")
                .setMessage("即将安装：" + fileName + "\n请确认文件来源可靠")
                .setPositiveButton("确认安装", (d, w) -> {
                    showInstallProgressDialog(fileName);
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

    /**
     * 应用内安装进度对话框（圆形进度条 + 阶段 + 百分比）
     */
    private void showInstallProgressDialog(String fileName) {
        if (installProgressDialog != null && installProgressDialog.isShowing()) return;

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(28), dpToPx(24), dpToPx(28), dpToPx(24));
        layout.setGravity(Gravity.CENTER);

        TextView titleView = new TextView(requireContext());
        titleView.setText(fileName);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setMaxLines(1);
        titleView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, dpToPx(16));
        layout.addView(titleView);

        installCircularProgress = new CircularProgressIndicator(requireContext());
        installCircularProgress.setIndeterminate(false);
        installCircularProgress.setProgress(0);
        LinearLayout.LayoutParams cpLp = new LinearLayout.LayoutParams(dpToPx(72), dpToPx(72));
        cpLp.gravity = Gravity.CENTER;
        installCircularProgress.setLayoutParams(cpLp);
        layout.addView(installCircularProgress);

        installStageView = new TextView(requireContext());
        installStageView.setText("正在准备");
        installStageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        installStageView.setGravity(Gravity.CENTER);
        installStageView.setPadding(0, dpToPx(12), 0, 0);
        layout.addView(installStageView);

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

    /**
     * 执行安装（旧接口保留，实际走 InstallService）
     */
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
                    while ((len = input.read(buffer)) != -1) {
                        output.write(buffer, 0, len);
                    }
                }
            }
        }
    }

    /**
     * 检测压缩类型
     */
    private TarCompressorUtils.Type detectCompressionType(Uri uri) throws IOException {
        byte[] magic = new byte[6];
        try (InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
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
                            }
                        }
                    }
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

    /**
     * 刷新列表
     */
    private void refreshFileList() {
        boolean isWine = isWineOrProton(currentCategory);
        currentStoragePath = isWine ? "rootfs/opt/installed-wine" : "installed_components";
        currentInstallPath = isWine ? "" : currentCategory;

        File dir = new File(baseFilesPath, currentStoragePath + File.separator + currentInstallPath);
        if (isWine && dir.exists() && dir.isDirectory()) {
            sanitizeWineFolderNames(dir);
        }

        installedItems.clear();
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (isWine) {
                        if (f.isDirectory()) installedItems.add(f.getName());
                    } else {
                        String name = f.isFile() ? f.getName().replace(TZSD_EXTENSION, "") : f.getName();
                        if (name != null && !name.isEmpty()) installedItems.add(name);
                    }
                }
            }
        }

        if (installedItems.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyIconView.setText(getCategoryIcon(currentCategory));
            emptyIconView.setBackground(createRoundedBackground(getCategoryColor(currentCategory), 14));
            emptyTextView.setText("当前分类下没有已安装组件\n点击下方按钮选择 .whp 文件安装");
            emptyTextView.setTextColor(getColorFromAttr(android.R.attr.textColorSecondary, Color.parseColor("#757575"), Color.parseColor("#B0B0B0")));
            return;
        }

        emptyState.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        recyclerView.setAdapter(new ContentItemAdapter(installedItems));
    }

    /**
     * 组件列表适配器（卡片式）
     */
    private class ContentItemAdapter extends RecyclerView.Adapter<ContentItemAdapter.ViewHolder> {
        private final List<String> items;

        ContentItemAdapter(List<String> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contents_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final String name = items.get(position);
            String displayName = isWineOrProton(currentCategory) ? name : name;
            holder.nameView.setText(displayName);

            // 描述行：类型 + 版本 + 大小
            String version = extractVersionFromName(name);
            String size = getFileSizeString(name);
            String desc;
            if (version != null) {
                desc = getCategoryDisplayName(currentCategory) + " · " + version +
                        (size != null ? " · " + size : "");
            } else {
                desc = getCategoryDisplayName(currentCategory) + (size != null ? " · " + size : "");
            }
            holder.versionView.setText(desc);

            holder.iconView.setText(getCategoryIcon(currentCategory));
            holder.iconView.setBackground(createRoundedBackground(getCategoryColor(currentCategory), 12));

            holder.statusView.setText("已安装");
            holder.statusView.setTextColor(Color.parseColor("#4CAF50"));
            holder.statusView.setBackground(createRoundedBackground(Color.parseColor("#E8F5E9"), 8));

            holder.menuBtn.setOnClickListener(v -> showItemMenu(holder.menuBtn, name));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView iconView;
            final TextView nameView;
            final TextView versionView;
            final TextView statusView;
            final ImageButton menuBtn;

            ViewHolder(@NonNull View view) {
                super(view);
                iconView = view.findViewById(R.id.TVIcon);
                nameView = view.findViewById(R.id.TVName);
                versionView = view.findViewById(R.id.TVVersion);
                statusView = view.findViewById(R.id.TVStatus);
                menuBtn = view.findViewById(R.id.BTMenu);
            }
        }
    }

    /**
     * 列表项操作菜单：查看信息 / 删除
     */
    private void showItemMenu(View anchor, final String name) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(0, 1, 0, "查看信息");
        menu.getMenu().add(0, 2, 1, "删除");
        menu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == 1) {
                showInfoDialog(name);
            } else if (itemId == 2) {
                showDeleteDialog(name);
            }
            return true;
        });
        menu.show();
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
            case "dxvk": return Color.parseColor("#673AB7");
            case "box64": return Color.parseColor("#FF9800");
            case "turnip": return Color.parseColor("#4CAF50");
            case "virgl": return Color.parseColor("#2196F3");
            case "vkd3d": return Color.parseColor("#E91E63");
            case "wine": return Color.parseColor("#F44336");
            case "proton": return Color.parseColor("#00BCD4");
            default: return Color.parseColor("#607D8B");
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
            showToast("删除成功");
            refreshFileList();
        } else {
            showToast("删除失败");
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

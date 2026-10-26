package com.winlator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

/**
 * 游戏商店入口（当前仅支持 Steam）。
 * 显示登录状态，点击进入 Steam 主界面（登录/游戏库/下载/创建快捷方式）。
 */
public class StoresFragment extends Fragment {
    private LinearLayout cardsContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context ctx = requireContext();
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) actionBar.setTitle(R.string.game_stores);

        ScrollView scrollView = new ScrollView(ctx);
        scrollView.setFillViewport(true);
        cardsContainer = new LinearLayout(ctx);
        cardsContainer.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(ctx, 16);
        cardsContainer.setPadding(pad, pad, pad, pad);
        scrollView.addView(cardsContainer,
            new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        refreshCards();
        return scrollView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 一次性数据迁移：旧版本将商店游戏安装在 filesDir/imagefs 下，
        // Wine 无法访问（Z: 盘映射到 RootFS 根目录），导致快捷方式报"路径未找到"。
        // 将数据迁移到 Wine 可见的 rootfs 目录，重写存储路径，修复过期的快捷方式目标。
        new Thread(() -> {
            try {
                android.content.Context ctx = getContext();
                if (ctx == null) return;
                int moved = com.winlator.store.StorePaths.migrateLegacyStores(ctx);
                int fixed = com.winlator.store.StorePaths.repairStoreShortcuts(ctx);
                if ((moved > 0 || fixed > 0) && getActivity() != null) {
                    getActivity().runOnUiThread(this::refreshCards);
                }
            }
            catch (Exception ignored) {}
        }, "store-path-migration").start();
        refreshCards();
    }

    private void refreshCards() {
        if (cardsContainer == null) return;
        Context ctx = requireContext();
        cardsContainer.removeAllViews();
        cardsContainer.addView(makeHeader(ctx));
        cardsContainer.addView(makeCard(ctx, "Steam",
            getString(R.string.stores_desc_steam),
            isSteamLoggedIn(ctx), new Intent(ctx, com.winlator.store.SteamMainActivity.class)));
    }

    private View makeHeader(Context ctx) {
        TextView tv = new TextView(ctx);
        tv.setText(getString(R.string.stores_header));
        tv.setTextSize(13f);
        tv.setTextColor(0xFFAAAAAA);
        tv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 12);
        tv.setLayoutParams(lp);
        return tv;
    }

    private View makeCard(Context ctx, String name, String desc, boolean loggedIn, Intent intent) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFF1E1E1E);
        int pad = dp(ctx, 16);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(ctx, 12);
        card.setLayoutParams(cardLp);

        TextView title = new TextView(ctx);
        title.setText(name);
        title.setTextSize(20f);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(0xFFFFFFFF);
        card.addView(title);

        TextView sub = new TextView(ctx);
        sub.setText(desc);
        sub.setTextSize(13f);
        sub.setTextColor(0xFFAAAAAA);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(ctx, 4);
        card.addView(sub, subLp);

        TextView status = new TextView(ctx);
        status.setText(loggedIn ? getString(R.string.stores_status_in) : getString(R.string.stores_status_out));
        status.setTextSize(13f);
        status.setTextColor(loggedIn ? 0xFF7BC47F : 0xFFFFB74D);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stLp.topMargin = dp(ctx, 8);
        card.addView(status, stLp);

        Button open = new Button(ctx);
        open.setText(loggedIn ? getString(R.string.stores_open_library) : getString(R.string.store_sign_in));
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLp.topMargin = dp(ctx, 8);
        open.setOnClickListener(v -> startActivity(intent));
        card.addView(open, btnLp);
        return card;
    }

    private static boolean isSteamLoggedIn(Context ctx) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences("steam_prefs", Context.MODE_PRIVATE);
            String token = sp.getString("refresh_token", null);
            String user = sp.getString("username", null);
            return token != null && !token.isEmpty() && user != null && !user.isEmpty();
        }
        catch (Exception e) {
            return false;
        }
    }

    private static int dp(Context ctx, int v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density);
    }
}

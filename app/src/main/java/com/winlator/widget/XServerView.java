package com.winlator.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.winlator.renderer.GLRenderer;
import com.winlator.xserver.XServer;

@SuppressLint("ViewConstructor")
public class XServerView extends GLSurfaceView {
    private final GLRenderer renderer;
    // 待设置的刷新率，0 表示自动匹配系统最高刷新率，-1 表示未设置
    private int pendingRefreshRate = -1;

    public XServerView(Context context, XServer xServer) {
        super(context);
        setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setEGLContextClientVersion(3);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        setPreserveEGLContextOnPause(true);
        renderer = new GLRenderer(this, xServer);
        setRenderer(renderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        // Surface 创建后应用待设置的刷新率
        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (pendingRefreshRate >= 0 && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    applyRefreshRate(holder.getSurface(), pendingRefreshRate);
                    pendingRefreshRate = -1;
                }
            }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {}
        });
    }

    public GLRenderer getRenderer() {
        return renderer;
    }

    // 设置 Surface 刷新率（Android API 31+），0 表示自动匹配系统最高刷新率
    public void setRefreshRate(int refreshRate) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return;
        Surface surface = getHolder().getSurface();
        if (surface != null && surface.isValid()) {
            applyRefreshRate(surface, refreshRate);
        } else {
            // Surface 尚未创建，暂存待创建后应用
            pendingRefreshRate = refreshRate;
        }
    }

    private void applyRefreshRate(Surface surface, int refreshRate) {
        float rate = refreshRate > 0 ? (float)refreshRate : getContext().getDisplay().getRefreshRate();
        surface.setFrameRate(rate, Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE);
    }
}

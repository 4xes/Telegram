package com.blackfox.surface.renderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blackfox.surface.renderer.particle.DustRequest;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.Cells.ChatMessageCell;

public class DustSurfaceView extends FrameLayout {

    private final EglEffectThread thread = new EglEffectThread();

    public DustSurfaceView(@NonNull Context context) {
        super(context);
        initView();
    }

    public DustSurfaceView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public DustSurfaceView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        init();
    }

    private int[] locationView = new int[2];
    private int[] locationThis = new int[2];

    public void addViewRequest(View view, DustEffectDelegate delegate) {
        if (view.getMeasuredWidth() != 0 && view.getMeasuredHeight() != 0) {
            thread.addRequest(createRequest(view, delegate));
        } else {
            view.addOnLayoutChangeListener(new OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    thread.addRequest(createRequest(view, delegate));
                    view.removeOnLayoutChangeListener(this);
                    view.setVisibility(View.INVISIBLE);
                }
            });
        }
    }

    public static Bitmap loadBitmapFromView(View v) {
        Bitmap b = Bitmap.createBitmap(v.getMeasuredWidth(), v.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        if (v instanceof ChatMessageCell) {
            ChatMessageCell messageCell = (ChatMessageCell) v;
            messageCell.drawBackgroundInternal(c, true);
        }
        v.draw(c);
        return b;
    }

    private DustRequest createRequest(View view, DustEffectDelegate delegate) {
        view.getLocationOnScreen(locationView);
        getLocationOnScreen(locationThis);
        float x = locationView[0] - locationThis[0];
        float y = locationView[1] - locationThis[1];
        return new DustRequest(
                view,
                loadBitmapFromView(view),
                delegate,
                view.getMeasuredWidth(),
                view.getMeasuredHeight(),
                (int) x,
                (int) y,
                getSizeParticle(),
                3000
        );
    }

    private static float getSizeParticle() {
        switch (SharedConfig.getDevicePerformanceClass()) {
            case SharedConfig.PERFORMANCE_CLASS_HIGH:
                return AndroidUtilities.dpf2(1.0f);
            case SharedConfig.PERFORMANCE_CLASS_AVERAGE:
                return AndroidUtilities.dpf2(1.2f);
            default:
            case SharedConfig.PERFORMANCE_CLASS_LOW:
                return AndroidUtilities.dpf2(1.5f);
        }
    }

    public void init() {
        TextureView textureView = new TextureView(getContext());
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                thread.onSurfaceTextureAvailable(surface);
                thread.setSize(width, height);
                thread.setTimeConfig(new TimeConfig((int) AndroidUtilities.screenRefreshRate));
                thread.start();
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
                thread.setSize(width, height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                thread.onSurfaceTextureDestroyed();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

            }
        });

        textureView.setOpaque(false);
        addView(textureView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

}

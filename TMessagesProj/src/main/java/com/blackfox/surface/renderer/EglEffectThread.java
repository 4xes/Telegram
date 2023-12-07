package com.blackfox.surface.renderer;

import android.graphics.SurfaceTexture;
import android.opengl.GLES31;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blackfox.surface.renderer.particle.DustRenderer;
import com.blackfox.surface.renderer.particle.DustRequest;

import java.util.ArrayList;
import java.util.Iterator;

/** @noinspection NonAsciiCharacters*/
public class EglEffectThread extends Thread {

    @Nullable
    private EglEffectContext eglContext;
    @NonNull
    private TimeConfig timeConfig;
    private int width, height;

    private volatile boolean running = true;
    private volatile boolean paused = false;
    private volatile boolean finish = false;

    private final Object lock = new Object();
    public volatile ArrayList<DustRequest> requests = new ArrayList();

    public EglEffectThread() {
        super();
        this.timeConfig = TimeConfig.FPS60Config();
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture) {
        this.eglContext = new EglEffectContext(surfaceTexture);
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setTimeConfig(@NonNull TimeConfig timeConfig) {
        this.timeConfig = timeConfig;
    }

    public void onSurfaceTextureDestroyed() {
        running = false;
    }

    public void addRequest(@Nullable DustRequest request) {
        synchronized (lock) {
            requests.add(request);
        }
    }

    @Override
    public void run() {
        if (eglContext != null) {
            eglContext.init();
        }
        long lastTime = System.nanoTime();
        while (running && !finish) {
            final long now = System.nanoTime();
            float Δt = (float)((now - lastTime) / 1_000_000_000.);
            lastTime = now;

            if (Δt < timeConfig.min_delta) {
                double wait = timeConfig.min_delta - Δt;
                try {
                    long milli = (long) (wait * 1000L);
                    int nano = (int) ((wait - milli / 1000.) * 1_000_000_000);
                    //noinspection BusyWait
                    sleep(milli, nano);
                } catch (Exception ignore) {}
                Δt = timeConfig.min_delta;
            } else if (Δt > timeConfig.max_delta) {
                Δt = timeConfig.max_delta;
            }

            running = eglContext.makeCurrent();
            GLES31.glViewport(0, 0, width, height);
            GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT);
            if (running) {
                for (int i = 0; i < requests.size(); i++) {
                    DustRequest request = requests.get(i);
                    if (!request.isFinish()) {
                        DustRenderer renderer = request.getRendererAndInitIfNeeded(width, height);
                        renderer.drawParticles(request.timeUs / request.getDurationUs(), Δt / request.getDurationUs());
                        request.timeUs += Δt;
                        if (request.isFinish()) {
                            request.finishAndCleanup();
                        }

                    }
                }
            }

            eglContext.swapBuffer();

            synchronized (lock) {
                Iterator<DustRequest> iterator = requests.iterator();
                while ((iterator.hasNext())) {
                    DustRequest renderer = iterator.next();
                    renderer.notifyStart();
                    if (renderer.isFinish()) {
                        iterator.remove();
                    }
                }
            }

        }
        eglContext.cleanup();
    }
}

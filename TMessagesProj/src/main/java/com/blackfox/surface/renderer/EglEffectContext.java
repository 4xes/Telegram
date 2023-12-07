package com.blackfox.surface.renderer;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLExt;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

public class EglEffectContext {

    private EGL10 egl;
    private EGLDisplay eglDisplay;
    private EGLSurface eglSurface;
    private EGLConfig eglConfig;
    private EGLContext eglContext;

    private final SurfaceTexture surfaceTexture;

    private boolean isInitialized = false;

    public EglEffectContext(SurfaceTexture surfaceTexture) {
        this.surfaceTexture = surfaceTexture;
    }

    public void init() {
        egl = (EGL10) EGLContext.getEGL();
        eglDisplay = egl.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        EglUtils.checkGlErrors("eglGetDisplay");
        if (eglDisplay == egl.EGL_NO_DISPLAY) {
            return;
        }

        // initialize egDisplay
        int[] version = new int[2];
        if (!egl.eglInitialize(eglDisplay, version)) {
            return;
        }

        // choose Config
        int[] configAttributes = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
                EGL14.EGL_NONE
        };
        EGLConfig[] eglConfigs = new EGLConfig[1];
        int[] numConfigs = new int[1];

        boolean chooseConfig = egl.eglChooseConfig(eglDisplay, configAttributes, eglConfigs, 1, numConfigs);
        EglUtils.checkGlErrors("eglChooseConfig");
        if (!chooseConfig) {
            return;
        }
        eglConfig = eglConfigs[0];

        // create context
        int[] contextAttributes = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                EGL14.EGL_NONE
        };
        eglContext = egl.eglCreateContext(eglDisplay, eglConfig, egl.EGL_NO_CONTEXT, contextAttributes);
        EglUtils.checkGlErrors("eglCreateContext");
        if (eglContext == null) {
            return;
        }


        eglSurface = egl.eglCreateWindowSurface(eglDisplay, eglConfig, surfaceTexture, null);
        EglUtils.checkGlErrors("createWindowSurface");
        if (eglSurface == null) {
            return;
        }
        isInitialized = makeCurrent();
    }

    public boolean makeCurrent() {
        boolean isCurrent = egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);
        EglUtils.checkGlErrors("makeCurrent");
        return isCurrent;
    }

    public void swapBuffer() {
        egl.eglSwapBuffers(eglDisplay, eglSurface);
    }

    public void cleanup() {
        if (egl != null) {
            try {
                egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
                EglUtils.checkGlErrors("cleanup makeCurrent");
            } catch (Exception e) {
                EglUtils.log(e);
            }
            if (eglSurface != null) {
                try {
                    egl.eglDestroySurface(eglDisplay, eglSurface);
                    EglUtils.checkGlErrors("destroySurface");
                } catch (Exception e) {
                    EglUtils.log(e);
                }
            }
            if (eglContext != null) {
                try {
                    egl.eglDestroyContext(eglDisplay, eglContext);
                    EglUtils.checkGlErrors("destroyContext");
                } catch (Exception e) {
                    EglUtils.log(e);
                }
            }
        }
        if (surfaceTexture != null) {
            try {
                surfaceTexture.release();
                EglUtils.checkGlErrors("destroyTexture");
            } catch (Exception e) {
                EglUtils.log(e);
            }
        }
        isInitialized = false;
    }

    public boolean isInitialized() {
        return isInitialized;
    }
}

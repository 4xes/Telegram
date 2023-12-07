package com.blackfox.surface.renderer;

public class TimeConfig {
    public final int maxFps;
    public final float min_delta;
    public final float max_delta;

    public TimeConfig(int maxFps) {
        this.maxFps = maxFps;
        this.min_delta = 1.0f / this.maxFps;
        this.max_delta = this.min_delta * 4;
    }

    public static TimeConfig FPS60Config() {
        return new TimeConfig(60);
    }
}

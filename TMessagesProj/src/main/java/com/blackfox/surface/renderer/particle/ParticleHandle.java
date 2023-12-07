package com.blackfox.surface.renderer.particle;

import android.opengl.GLES31;

public class ParticleHandle {

    public static final String VAR_OUT_OFFSET = "outOffset";
    public static final String VAR_OUT_VELOCITY = "outVelocity";
    public static final String VAR_OUT_LIFE_TIME = "outLifetime";
    public static final String[] FEEDBACK_VARYING = {VAR_OUT_OFFSET, VAR_OUT_VELOCITY, VAR_OUT_LIFE_TIME};

    private int resetHandle;
    private int timeHandle;
    private int deltaTimeHandle;
    private int dustCountHandle;
    private int dustOffsetHandle;
    private int dustSizeHandle;
    private int radiusHandle;
    private int seedHandle;

    public void getHandlers(int program) {
        resetHandle = GLES31.glGetUniformLocation(program, "reset");
        timeHandle = GLES31.glGetUniformLocation(program, "time");
        deltaTimeHandle = GLES31.glGetUniformLocation(program, "deltaTime");
        radiusHandle = GLES31.glGetUniformLocation(program, "r");
        seedHandle = GLES31.glGetUniformLocation(program, "seed");

        //my realisation
        dustCountHandle = GLES31.glGetUniformLocation(program, "dustCount");
        dustOffsetHandle = GLES31.glGetUniformLocation(program, "dustOffset");
        dustSizeHandle = GLES31.glGetUniformLocation(program, "dustSize");
    }

    public void setDustCount(int countVertical, int countHorizontal) {
        GLES31.glUniform2f(dustCountHandle, (float) countVertical, (float) countHorizontal);
    }

    public void setDustOffset(float offsetX, float offsetY) {
        GLES31.glUniform2f(dustOffsetHandle, offsetX, offsetY);
    }

    public void setDustSize(float width, float height) {
        GLES31.glUniform2f(dustSizeHandle, width, height);
    }

    public void setTime(float time) {
        GLES31.glUniform1f(timeHandle, time);
    }

    public void setDeltaTime(float deltaTime) {
        GLES31.glUniform1f(deltaTimeHandle, deltaTime);
    }

    public void setReset(boolean reset) {
        GLES31.glUniform1f(resetHandle, reset ? 1.0f : 0.0f);
    }

    public void setRadius(float radius) {
        GLES31.glUniform1f(radiusHandle, radius);
    }

    public void setSeeds(float seeds) {
        GLES31.glUniform1f(seedHandle, seeds);
    }

}

package org.telegram.ui.Animations;

import android.content.Context;
import android.content.SharedPreferences;

public class AnimationPreferences {
    final SharedPreferences sharedPreferences;

    public AnimationPreferences(Context context) {
        this.sharedPreferences = context.getSharedPreferences("animation", Context.MODE_PRIVATE);
    }

    public void putInterpolator(String key,
                     float progressionTop,
                     float progressionBottom,
                     float timeStart,
                     float timeEnd) {
        sharedPreferences.edit()
                .putFloat(key + keyProgressionTop, progressionTop)
                .putFloat(key + keyProgressionBottom, progressionBottom)
                .putFloat(key + keyTimeStart, timeStart)
                .putFloat(key + keyTimeEnd, timeEnd).apply();
    }

    public void putProgressionTop(String key, float value) {
        sharedPreferences.edit()
                .putFloat(key + keyProgressionTop, value).apply();
    }

    public void putProgressionBottom(String key, float value) {
        sharedPreferences.edit()
                .putFloat(key + keyProgressionBottom, value).apply();
    }

    public void putTimeStart(String key, float value) {
        sharedPreferences.edit()
                .putFloat(key + keyTimeStart, value).apply();
    }

    public void putTimeEnd(String key, float value) {
        sharedPreferences.edit()
                .putFloat(key + keyTimeEnd, value).apply();
    }

    public InterpolatorData getInterpolator(String key) {
        float progressionTop = sharedPreferences.getFloat(key + keyProgressionTop, InterpolatorData.DEFAULT_PROGRESSION_TOP);
        float progressionBottom = sharedPreferences.getFloat(key + keyProgressionBottom, InterpolatorData.DEFAULT_PROGRESSION_BOTTOM);
        float timeStart = sharedPreferences.getFloat(key + keyTimeStart, InterpolatorData.DEFAULT_TIME_START);
        float timeEnd = sharedPreferences.getFloat(key + keyTimeEnd, InterpolatorData.DEFAULT_TIME_END);
        return new InterpolatorData(
                progressionTop,
                progressionBottom,
                timeStart,
                timeEnd
        );
    }

    public void putDuration(String key,
                     long duration) {
        sharedPreferences.edit()
                .putLong(key + keyDuration, duration).apply();
    }

    public long getDuration(String key) {
        return sharedPreferences.getLong(key + keyDuration, 500L);
    }

    private static final String keyProgressionTop = "_pt";
    private static final String keyProgressionBottom = "_pb";
    private static final String keyTimeStart = "_ts";
    private static final String keyTimeEnd = "_te";
    private static final String keyDuration = "_d";
}
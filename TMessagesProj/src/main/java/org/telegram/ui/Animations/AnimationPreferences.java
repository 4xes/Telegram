package org.telegram.ui.Animations;

import android.content.Context;
import android.content.SharedPreferences;

public class AnimationPreferences {
    final SharedPreferences sharedPreferences;

    public AnimationPreferences(Context context) {
        this.sharedPreferences = context.getSharedPreferences("animation", Context.MODE_PRIVATE);
    }

    public void putInterpolator(String key,
                     float progressionStart,
                     float progressionEnd,
                     float timeStart,
                     float timeEnd) {
        sharedPreferences.edit()
                .putFloat(key + keyProgressionStart, progressionStart)
                .putFloat(key + keyProgressionEnd, progressionEnd)
                .putFloat(key + keyTimeStart, timeStart)
                .putFloat(key + keyTimeEnd, timeEnd).apply();
    }

    public InterpolatorData getInterpolator(String key) {
        float progressionStart = sharedPreferences.getFloat(key + keyProgressionStart, 0f);
        float progressionEnd = sharedPreferences.getFloat(key + keyProgressionEnd, 0.33f);
        float timeStart = sharedPreferences.getFloat(key + keyTimeStart, 0f);
        float timeEnd = sharedPreferences.getFloat(key + keyTimeEnd, 0.5f);
        return new InterpolatorData(
                key,
                progressionStart,
                progressionEnd,
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

    private static final String keyProgressionStart = "_ps";
    private static final String keyProgressionEnd = "_pe";
    private static final String keyTimeStart = "_ts";
    private static final String keyTimeEnd = "_te";
    private static final String keyDuration = "_d";
}
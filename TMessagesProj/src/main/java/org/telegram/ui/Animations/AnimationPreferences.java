package org.telegram.ui.Animations;

import android.content.Context;
import android.content.SharedPreferences;

import static com.microsoft.appcenter.utils.storage.SharedPreferencesManager.putInt;

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
                .putFloat(key + prefixProgressionTop, progressionTop)
                .putFloat(key + prefixProgressionBottom, progressionBottom)
                .putFloat(key + prefixTimeStart, timeStart)
                .putFloat(key + prefixTimeEnd, timeEnd).apply();
    }

    public void putProgressionTop(String key, float value) {
        sharedPreferences.edit()
                .putFloat(key + prefixProgressionTop, value).apply();
    }

    public void putProgressionBottom(String key, float value) {
        sharedPreferences.edit()
                .putFloat(key + prefixProgressionBottom, value).apply();
    }

    public void putTimeStart(String key, float value) {
        sharedPreferences.edit()
                .putFloat(key + prefixTimeStart, value).apply();
    }

    public void putTimeEnd(String key, float value) {
        sharedPreferences.edit()
                .putFloat(key + prefixTimeEnd, value).apply();
    }

    public InterpolatorData getInterpolator(String key) {
        float progressionTop = sharedPreferences.getFloat(key + prefixProgressionTop, InterpolatorData.DEFAULT_PROGRESSION_TOP);
        float progressionBottom = sharedPreferences.getFloat(key + prefixProgressionBottom, InterpolatorData.DEFAULT_PROGRESSION_BOTTOM);
        float timeStart = sharedPreferences.getFloat(key + prefixTimeStart, InterpolatorData.DEFAULT_TIME_START);
        float timeEnd = sharedPreferences.getFloat(key + prefixTimeEnd, InterpolatorData.DEFAULT_TIME_END);
        return new InterpolatorData(
                progressionTop,
                progressionBottom,
                timeStart,
                timeEnd
        );
    }

    public void putBackgroundIndexes(int[] indexes) {
        sharedPreferences.edit()
                .putInt(keyBackgroundIndexes + 0, indexes[0])
                .apply();
    }

    public int[] getBackgroundIndexes() {
        int i0 = sharedPreferences.getInt(keyBackgroundIndexes + 0, 0);
        int i1 = (i0 + 6) % 8;
        int i2 = (i0 + 4) % 8;
        int i3 = (i0 + 2) % 8;
        return new int[] {i0, i1, i2, i3};
    }

    public void putDuration(String key,
                     long duration) {
        sharedPreferences.edit()
                .putLong(key + prefixDuration, duration).apply();
    }

    public long getDuration(String key) {
        return sharedPreferences.getLong(key + prefixDuration, 500L);
    }

    private static final String prefixProgressionTop = "_pt";
    private static final String prefixProgressionBottom = "_pb";
    private static final String prefixTimeStart = "_ts";
    private static final String prefixTimeEnd = "_te";
    private static final String prefixDuration = "_d";
    private static final String keyBackgroundIndexes = "bg_i";
}
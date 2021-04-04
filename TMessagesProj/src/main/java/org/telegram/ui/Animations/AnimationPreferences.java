package org.telegram.ui.Animations;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class AnimationPreferences {
    final SharedPreferences sharedPreferences;

    public AnimationPreferences(Context context) {
        this.sharedPreferences = context.getSharedPreferences("animation", Context.MODE_PRIVATE);
    }

    public void putProgressionTop(AnimationType animationType, Parameter parameter, float value) {
        String key = key(animationType, parameter);
        putProgressionTop(key, value);
    }

    public void putProgressionBottom(AnimationType animationType, Parameter parameter, float value) {
        String key = key(animationType, parameter);
        putProgressionBottom(key, value);
    }

    public void putTimeStart(AnimationType animationType, Parameter parameter, float value) {
        String key = key(animationType, parameter);
        putTimeStart(key, value);
    }

    public void putTimeEnd(AnimationType animationType, Parameter parameter, float value) {
        String key = key(animationType, parameter);
        putTimeEnd(key, value);
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

    public InterpolatorData getInterpolatorData(AnimationType animationType, Parameter parameter) {
        String key = key(animationType, parameter);
        float progressionTop = sharedPreferences.getFloat(key + prefixProgressionTop, parameter.defaultProgressionTop(animationType));
        float progressionBottom = sharedPreferences.getFloat(key + prefixProgressionBottom, parameter.defaultProgressionBottom(animationType));
        float timeStart = sharedPreferences.getFloat(key + prefixTimeStart, parameter.defaultTimeStart(animationType));
        float timeEnd = sharedPreferences.getFloat(key + prefixTimeEnd, parameter.defaultTimeEnd(animationType));
        return new InterpolatorData(
                progressionTop,
                progressionBottom,
                timeStart,
                timeEnd
        );
    }

    public void putBackgroundIndexes(int[] indexes) {
        sharedPreferences.edit()
                .putInt(keyBackgroundIndexes, indexes[0])
                .apply();
    }

    public int[] getBackgroundIndexes() {
        int i0 = sharedPreferences.getInt(keyBackgroundIndexes, 0);
        int i1 = (i0 + 6) % 8;
        int i2 = (i0 + 4) % 8;
        int i3 = (i0 + 2) % 8;
        return new int[] {i0, i1, i2, i3};
    }

    public int getColor(int i) {
        return sharedPreferences.getInt(keyBackgroundColor + i, colorsDefault[i]);
    }

    public int[] getColors() {
        int[] colors = new int[4];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = getColor(i);
        }
        return colors;
    }

    @SuppressLint("ApplySharedPref")
    public void putColor(int i, int color) {
        sharedPreferences.edit()
                .putInt(keyBackgroundColor + i, color)
                .commit();
    }
    @SuppressLint("ApplySharedPref")
    public void putColors(int[] colors) {
        sharedPreferences.edit()
                .putInt(keyBackgroundColor + 0, colors[0])
                .putInt(keyBackgroundColor + 1, colors[1])
                .putInt(keyBackgroundColor + 2, colors[2])
                .putInt(keyBackgroundColor + 3, colors[3])
                .commit();
    }

    public long getDuration(String key) {
        return sharedPreferences.getLong(key + prefixDuration, 500L);
    }

    public void resetSettings() {
        sharedPreferences.edit().clear().apply();
    }

    public long getDuration(AnimationType animationType, @Nullable Parameter parameter) {
        String key = key(animationType, parameter);
        long defaultDuration = animationType.defaultDuration(parameter);
        return sharedPreferences.getLong(key + prefixDuration, defaultDuration);
    }

    public void putDuration(AnimationType animationType, @Nullable Parameter parameter, long duration) {
        String key = key(animationType, parameter);
        sharedPreferences.edit()
                .putLong(key + prefixDuration, duration).apply();
    }

    public static String key(AnimationType type, @Nullable Parameter parameter) {
        String key;
        if (parameter == null) {
            key = (type.name()).replace(" ", "").toLowerCase(Locale.ENGLISH);
        } else {
            key = (type.name() + "_" + parameter.name()).replace(" ", "").toLowerCase(Locale.ENGLISH);;
        }
        return key;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for(Map.Entry<String, ?> entry: sharedPreferences.getAll().entrySet()) {
            String key = entry.getKey();
            if (key.equals(keyBackgroundIndexes)) {
                continue;
            }
            builder.append(key);
            builder.append("=");
            builder.append(entry.getValue().toString());
            builder.append("\n");
        }
        return builder.toString();
    }

    public boolean setSettingsFromString(String settings) {
        if (settings == null) {
            return false;
        }
        settings = settings.trim().toLowerCase(Locale.ENGLISH);
        if (settings.isEmpty()) {
            resetSettings();
            return true;
        }
        if (settings.length() > 0 ) {
            resetSettings();
            String[] params = settings.split("\n");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length < 2) {
                    continue;
                }
                String key = keyValue[0];
                String strValue = keyValue[1];
                if (key.equals(keyBackgroundIndexes)) {
                    continue;
                }
                if (key.endsWith(prefixProgressionTop) || key.endsWith(prefixProgressionBottom) || key.endsWith(prefixTimeStart) || key.endsWith(prefixTimeEnd)) {
                    try {
                        float value = Float.parseFloat(strValue);
                        if (value < 0f && value > 1f) {
                            sharedPreferences.edit().remove(key).apply();
                        } else {
                            sharedPreferences.edit().putFloat(key, value).apply();
                        }

                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                if (key.endsWith(prefixDuration)) {
                    try {
                        long value = Long.parseLong(strValue);
                        sharedPreferences.edit().putLong(key, value).apply();
                        if (value < 0) {
                            sharedPreferences.edit().remove(key).apply();
                        } else {
                            sharedPreferences.edit().putLong(key, value).apply();
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                if (key.endsWith("c0") || key.endsWith("c1") || key.endsWith("c2") || key.endsWith("c3")) {
                    try {
                        int value = Integer.parseInt(strValue);
                        sharedPreferences.edit().putInt(key, value).apply();
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
        return true;
    }

    private static final String prefixProgressionTop = "_pt";
    private static final String prefixProgressionBottom = "_pb";
    private static final String prefixTimeStart = "_ts";
    private static final String prefixTimeEnd = "_te";
    private static final String prefixDuration = "_d";
    private static final String keyBackgroundIndexes = "bg";
    private static final String keyBackgroundColor = "c";

    private static final int[] colorsDefault = new int[]{0xfffff6bf, 0xff76a076, 0xfff6e477, 0xff316b4d};
}
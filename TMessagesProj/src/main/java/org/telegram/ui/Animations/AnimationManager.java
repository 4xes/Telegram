package org.telegram.ui.Animations;
import android.content.Context;

import androidx.annotation.Nullable;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.Animations.Components.AnimationCubicBezierInterpolator;

import java.util.Locale;

public class AnimationManager {

    AnimationPreferences preferences;

    private AnimationManager(Context context) {
        this.preferences = new AnimationPreferences(context);
    }

    public static AnimationPreferences getPreferences() {
        return getInstance().preferences;
    }

    private static volatile AnimationManager Instance = null;
    public static AnimationManager getInstance() {
        AnimationManager localInstance = Instance;
        if (localInstance == null) {
            synchronized (AnimationManager.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new AnimationManager(ApplicationLoader.applicationContext);
                }
            }
        }
        return localInstance;
    }

    public InterpolatorData getInterpolatorData(AnimationType animationType, Parameter parameter) {
        return preferences.getInterpolator(key(animationType, parameter));
    }

    public AnimationCubicBezierInterpolator getInterpolator(AnimationType animationType, Parameter parameter) {
        InterpolatorData data = getInterpolatorData(animationType, parameter);
        AnimationCubicBezierInterpolator interpolator = new AnimationCubicBezierInterpolator(data);
        return interpolator;
    }

    public long getDuration(AnimationType animationType, @Nullable Parameter parameter) {
        String key;
        if (animationType == AnimationType.Background) {
            key = key(animationType, parameter);
        } else {
            key = key(animationType, null);
        }
        return preferences.getDuration(key);
    }

    public void setDuration(AnimationType animationType, @Nullable Parameter parameter, long duration) {
        String key;
        if (animationType == AnimationType.Background) {
            key = key(animationType, parameter);
        } else {
            key = key(animationType, null);
        }
        preferences.putDuration(key, duration);
    }

    public static String key(AnimationType type, Parameter parameter) {
        if (parameter != null) {
            return (type.name() + "_" + parameter.name()).replace(" ", "").toLowerCase(Locale.ENGLISH);
        } else {
            return (type.name()).replace(" ", "").toLowerCase(Locale.ENGLISH);
        }
    }
}
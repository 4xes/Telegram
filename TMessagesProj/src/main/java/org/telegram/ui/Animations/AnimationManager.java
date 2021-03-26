package org.telegram.ui.Animations;
import android.content.Context;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import org.telegram.messenger.ApplicationLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    public InterpolatorData getInterpolator(AnimationType animationType, Interpolator interpolator) {
        return preferences.getInterpolator(key(animationType, interpolator));
    }

    public long getDuration(AnimationType animationType, @Nullable Interpolator interpolator) {
        String key;
        if (animationType == AnimationType.Background) {
            key = key(animationType, interpolator);
        } else {
            key = key(animationType, null);
        }
        return preferences.getDuration(key);
    }

    public void setDuration(AnimationType animationType, @Nullable Interpolator interpolator, long duration) {
        String key;
        if (animationType == AnimationType.Background) {
            key = key(animationType, interpolator);
        } else {
            key = key(animationType, null);
        }
        preferences.putDuration(key, duration);
    }

    public static String key(AnimationType type, Interpolator interpolator) {
        if (interpolator != null) {
            return (type.name() + "_" + interpolator.name()).replace(" ", "").toLowerCase(Locale.ENGLISH);
        } else {
            return (type.name()).replace(" ", "").toLowerCase(Locale.ENGLISH);
        }
    }
}
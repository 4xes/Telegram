package org.telegram.ui.Animations;
import android.content.Context;
import android.view.View;

import androidx.collection.ArrayMap;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AnimationManager {
    Map<String, List<AnimationDataListener>> mapListeners = new ArrayMap<>();

    AnimationPreferences preferences;

    private AnimationManager(Context context) {
        this.preferences = new AnimationPreferences(context);
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

    public interface AnimationDataListener {
        void onAnimationDataUpdate(View view, String key, Object value);
    }

    public void notify(View view, String key, Object value) {
        List<AnimationDataListener> listeners = mapListeners.get(key);
        if (listeners != null) {
            for (AnimationDataListener listener: listeners) {
                listener.onAnimationDataUpdate(view, key, value);
            }
        }
    }

    private void addListener(String key, AnimationDataListener listener) {
        List<AnimationDataListener> listeners = mapListeners.get(key);
        if (listeners == null) {
            listeners = new ArrayList<>();
            mapListeners.put(key, listeners);
        }
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    private void removeListener(String key, AnimationDataListener listener) {
        List<AnimationDataListener> listeners = mapListeners.get(key);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

}
package org.telegram.ui.Reactions;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.Nullable;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;

import java.util.concurrent.TimeUnit;

public class ReactionsPreferences {

    private static final String KEY_AVAILABLE_REACTIONS = "reactions";
    private static final String KEY_EXPIRED_CACHE = "reactions_expired";

    final SharedPreferences preferences;

    public ReactionsPreferences() {
        preferences = ApplicationLoader.applicationContext.getSharedPreferences("reactions", Context.MODE_PRIVATE);
    }

    public void save(@Nullable TLRPC.TL_messages_availableReactions availableReactions) {
        if (availableReactions != null) {
            SerializedData data = new SerializedData();
            availableReactions.serializeToStream(data);
            String string = Base64.encodeToString(data.toByteArray(), Base64.DEFAULT);
            preferences.edit()
                    .putString(KEY_AVAILABLE_REACTIONS, string)
                    .putLong(KEY_EXPIRED_CACHE, System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(3600))
                    .apply();
            data.cleanup();
        } else {
            resetReactions();
        }
    }

    public boolean isCacheExpired() {
        long expiredTime = preferences.getLong(KEY_EXPIRED_CACHE, 0);
        return expiredTime < System.currentTimeMillis();
    }

    @Nullable
    public TLRPC.TL_messages_availableReactions getAvailableReactions() {
        TLRPC.TL_messages_availableReactions availableReactions = null;
        String string = preferences.getString(KEY_AVAILABLE_REACTIONS, null);
        if (string != null) {
            byte[] bytes = Base64.decode(string, Base64.DEFAULT);
            if (bytes != null) {
                SerializedData data = new SerializedData(bytes);
                availableReactions = (TLRPC.TL_messages_availableReactions) TLRPC.TL_messages_availableReactions.TLdeserialize(data, data.readInt32(false), false);
                data.cleanup();
            }
            if (availableReactions == null) {
                resetReactions();
            }
        }
        return availableReactions;
    }

    private void resetReactions() {
        preferences.edit()
                .remove(KEY_AVAILABLE_REACTIONS)
                .remove(KEY_EXPIRED_CACHE)
                .apply();
    }
}

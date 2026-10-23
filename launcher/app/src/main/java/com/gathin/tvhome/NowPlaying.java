package com.gathin.tvhome;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.provider.Settings;

import java.util.List;

/** Read-only lookup for the active media session, available only after the owner enables Notification Access. */
final class NowPlaying {
    static final String ACTION_CHANGED = "com.gathin.tvhome.action.MEDIA_SESSION_CHANGED";
    static final String SETTING_KEY = "now_playing_enabled";
    static final String SETTING_ACCESS_KEY = "enabled_notification_listeners";

    private NowPlaying() { }

    static boolean listenerEnabled(Context context) {
        String enabled = Settings.Secure.getString(context.getContentResolver(), SETTING_ACCESS_KEY);
        ComponentName component = new ComponentName(context, NowPlayingListener.class);
        return enabled != null && enabled.contains(component.flattenToString());
    }

    static MediaController activeController(Context context) {
        if (!listenerEnabled(context)) return null;
        MediaSessionManager manager = context.getSystemService(MediaSessionManager.class);
        if (manager == null) return null;
        try {
            List<MediaController> controllers = manager.getActiveSessions(
                    new ComponentName(context, NowPlayingListener.class));
            MediaController fallback = null;
            for (MediaController controller : controllers) {
                PlaybackState state = controller.getPlaybackState();
                if (state != null && (state.getState() == PlaybackState.STATE_PLAYING
                        || state.getState() == PlaybackState.STATE_BUFFERING)) return controller;
                MediaMetadata metadata = controller.getMetadata();
                if (fallback == null && metadata != null) fallback = controller;
            }
            return fallback;
        } catch (SecurityException unavailable) {
            return null;
        }
    }
}

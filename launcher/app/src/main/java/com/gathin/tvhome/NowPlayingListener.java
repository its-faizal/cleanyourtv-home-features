package com.gathin.tvhome;

import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/** Android binds this listener only after the owner explicitly enables Notification Access. */
public final class NowPlayingListener extends NotificationListenerService {
    @Override
    public void onListenerConnected() {
        changed();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification notification) {
        changed();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification notification) {
        changed();
    }

    private void changed() {
        if (getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE).getBoolean(NowPlaying.SETTING_KEY, false)) {
            sendBroadcast(new Intent(NowPlaying.ACTION_CHANGED).setPackage(getPackageName()));
        }
    }
}

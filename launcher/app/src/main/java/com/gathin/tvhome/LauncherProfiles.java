package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Launcher-local profiles. They organise this launcher's tile order and wallpaper choice; Android users stay untouched. */
final class LauncherProfiles {
    private static final String PREFS = "launcher_profiles";
    private static final String IDS = "ids";
    private static final String ACTIVE = "active";
    private static final String PREFIX = "name_";
    static final String DEFAULT = "default";

    private LauncherProfiles() { }

    static List<String> ids(Context context) {
        SharedPreferences p = prefs(context);
        List<String> ids = new ArrayList<>(p.getStringSet(IDS, Collections.singleton(DEFAULT)));
        if (!ids.contains(DEFAULT)) ids.add(0, DEFAULT);
        return ids;
    }

    static String active(Context context) {
        String active = prefs(context).getString(ACTIVE, DEFAULT);
        return ids(context).contains(active) ? active : DEFAULT;
    }

    static String name(Context context, String id) {
        return prefs(context).getString(PREFIX + id, id.equals(DEFAULT) ? "Home" : "Profile");
    }

    static String create(Context context, String name) {
        SharedPreferences p = prefs(context);
        String id = UUID.randomUUID().toString().substring(0, 8);
        List<String> ids = new ArrayList<>(ids(context));
        ids.add(id);
        p.edit().putStringSet(IDS, new java.util.HashSet<>(ids)).putString(PREFIX + id, name.trim()).apply();
        return id;
    }

    static void activate(Context context, String id) {
        if (ids(context).contains(id)) prefs(context).edit().putString(ACTIVE, id).apply();
    }

    static File orderFile(Context context) {
        String id = active(context);
        return new File(context.getFilesDir(), id.equals(DEFAULT) ? Order.FILE_NAME : "order_" + id + ".txt");
    }

    static String wallpaperChoiceKey(Context context) {
        String id = active(context);
        return id.equals(DEFAULT) ? Config.WALLPAPER_CHOICE_KEY : Config.WALLPAPER_CHOICE_KEY + "_" + id;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}

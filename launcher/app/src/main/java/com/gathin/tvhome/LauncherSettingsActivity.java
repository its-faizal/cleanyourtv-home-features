package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

/** Remote-friendly settings owned by the launcher; system settings stay one row away. */
public final class LauncherSettingsActivity extends Activity {
    static final String EXTRA_REORDER = "com.gathin.tvhome.extra.REORDER_FROM_SETTINGS";
    private static final int VISUALIZER_PERMISSION_REQUEST = 71;
    private SharedPreferences choices;
    private final TextView[] columnChoices = new TextView[4];
    private final TextView[] rowChoices = new TextView[4];
    private static final int[] DOCK_OPACITIES = {100, 70, 50, 30};
    private final TextView[] dockChoices = new TextView[DOCK_OPACITIES.length];
    private TextView wallpaperChoice;
    private TextView animationsChoice;
    private TextView advancedChoice;
    private TextView followThemeChoice;
    private TextView musicVisualizerChoice;
    private TextView nowPlayingChoice;
    private TextView homeStyleChoice, homePaletteChoice, tileShapeChoice;
    private int columns;
    private int rowsBelowDock;
    private int dockOpacity;
    private boolean animations;
    private boolean advancedEffects;
    private boolean musicVisualizer;
    private boolean nowPlayingEnabled;
    private boolean awaitingNotificationAccess;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        Config config = Config.load(this);
        View reorderChoice = findViewById(R.id.reorderChoice);
        reorderChoice.setVisibility(config.launcherSettingsButton ? View.VISIBLE : View.GONE);
        reorderChoice.setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_REORDER, true));
            finish();
        });
        columns = config.columns;
        rowsBelowDock = config.rowsBelowDock;
        dockOpacity = config.dockOpacity;
        animations = config.animations;
        advancedEffects = config.animations && config.advancedEffects;
        musicVisualizer = choices.getBoolean(Config.MUSIC_VISUALIZER_KEY, config.musicVisualizer);
        nowPlayingEnabled = choices.getBoolean(NowPlaying.SETTING_KEY, false);

        int[] ids = {R.id.columns4, R.id.columns5, R.id.columns6, R.id.columns7};
        for (int i = 0; i < ids.length; i++) {
            final int value = i + 4;
            columnChoices[i] = findViewById(ids[i]);
            columnChoices[i].setOnClickListener(v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                changed();
                refresh();
            });
        }
        int[] rowIds = {R.id.rows0, R.id.rows1, R.id.rows2, R.id.rows3};
        for (int i = 0; i < rowIds.length; i++) {
            final int value = i;
            rowChoices[i] = findViewById(rowIds[i]);
            rowChoices[i].setOnClickListener(v -> {
                if (rowsBelowDock == value) return;
                rowsBelowDock = value;
                choices.edit().putInt(Config.ROWS_BELOW_DOCK_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] dockIds = {R.id.dock100, R.id.dock70, R.id.dock50, R.id.dock30};
        for (int i = 0; i < dockIds.length; i++) {
            final int value = DOCK_OPACITIES[i];
            dockChoices[i] = findViewById(dockIds[i]);
            dockChoices[i].setOnClickListener(v -> {
                if (dockOpacity == value) return;
                dockOpacity = value;
                choices.edit().putInt(Config.DOCK_OPACITY_KEY, value).apply();
                changed();
                refresh();
            });
        }
        // Keep the numeric choices increasing in the Hebrew reading direction. Reorder the
        // actual views so D-pad focus follows their positions without changing the saved values.
        if (getResources().getBoolean(R.bool.rtl_app_grid)) {
            reverseRow(columnChoices);
            reverseRow(rowChoices);
            reverseRow(dockChoices);
        }
        wallpaperChoice = findViewById(R.id.wallpaperChoice);
        wallpaperChoice.setOnClickListener(v -> showWallpaperPicker());
        homeStyleChoice = findViewById(R.id.homeStyleChoice);
        homeStyleChoice.setOnClickListener(v -> chooseHomeStyle());
        homePaletteChoice = findViewById(R.id.homePaletteChoice);
        homePaletteChoice.setOnClickListener(v -> choosePalette());
        tileShapeChoice = findViewById(R.id.tileShapeChoice);
        tileShapeChoice.setOnClickListener(v -> chooseTileShape());
        animationsChoice = findViewById(R.id.animationsChoice);
        animationsChoice.setOnClickListener(v -> {
            animations = !animations;
            if (!animations) advancedEffects = false;
            choices.edit().putBoolean("animations", animations)
                    .putBoolean("advanced_effects", advancedEffects).apply();
            changed();
            refresh();
        });
        advancedChoice = findViewById(R.id.advancedChoice);
        advancedChoice.setOnClickListener(v -> {
            if (advancedEffects) {
                setAdvanced(false);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.advanced_effects)
                        .setMessage(R.string.advanced_effects_warning)
                        .setPositiveButton(R.string.enable, (dialog, which) -> setAdvanced(true))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        followThemeChoice = findViewById(R.id.followThemeChoice);
        musicVisualizerChoice = findViewById(R.id.musicVisualizerChoice);
        musicVisualizerChoice.setOnClickListener(v -> toggleMusicVisualizer());
        nowPlayingChoice = findViewById(R.id.nowPlayingChoice);
        nowPlayingChoice.setOnClickListener(v -> toggleNowPlaying());
        followThemeChoice.setOnClickListener(v -> {
            if (!hasOnTvChoices()) return;
            choices.edit().remove("columns").remove(Config.ROWS_BELOW_DOCK_KEY).remove("animations")
                    .remove("advanced_effects").remove(Config.WALLPAPER_CHOICE_KEY)
                    .remove(Config.DOCK_OPACITY_KEY).remove(Config.MUSIC_VISUALIZER_KEY)
                    .remove("home_style").remove("home_palette")
                    .remove("tile_shape").commit();
            Config current = Config.load(this);
            columns = current.columns;
            rowsBelowDock = current.rowsBelowDock;
            dockOpacity = current.dockOpacity;
            animations = current.animations;
            advancedEffects = current.animations && current.advancedEffects;
            musicVisualizer = current.musicVisualizer;
            changed();
            refresh();
        });
        findViewById(R.id.defaultHomeChoice).setOnClickListener(v -> {
            Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
            open(new Intent(Settings.ACTION_HOME_SETTINGS));
        });
        findViewById(R.id.tvSettingsChoice).setOnClickListener(v ->
                open(SystemSettings.intent(this, Settings.ACTION_SETTINGS)));
        refresh();
        if (config.launcherSettingsButton) reorderChoice.requestFocus();
        else columnChoices[Math.max(0, Math.min(columns - 4, 3))].requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!awaitingNotificationAccess) return;
        awaitingNotificationAccess = false;
        nowPlayingEnabled = NowPlaying.listenerEnabled(this);
        if (nowPlayingEnabled) {
            choices.edit().putBoolean(NowPlaying.SETTING_KEY, true).apply();
            changed();
        } else {
            choices.edit().putBoolean(NowPlaying.SETTING_KEY, false).apply();
            Toast.makeText(this, R.string.now_playing_access_denied, Toast.LENGTH_LONG).show();
        }
        refresh();
    }

    private void toggleNowPlaying() {
        if (nowPlayingEnabled) {
            nowPlayingEnabled = false;
            choices.edit().putBoolean(NowPlaying.SETTING_KEY, false).apply();
            changed();
            refresh();
            return;
        }
        if (NowPlaying.listenerEnabled(this)) {
            nowPlayingEnabled = true;
            choices.edit().putBoolean(NowPlaying.SETTING_KEY, true).apply();
            changed();
            refresh();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.now_playing_access)
                .setMessage(R.string.now_playing_access_message)
                .setPositiveButton(R.string.enable, (dialog, which) -> {
                    awaitingNotificationAccess = true;
                    try {
                        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    } catch (ActivityNotFoundException | SecurityException unavailable) {
                        awaitingNotificationAccess = false;
                        Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void toggleMusicVisualizer() {
        if (musicVisualizer && hasVisualizerPermission()) {
            saveMusicVisualizer(false);
            return;
        }
        if (hasVisualizerPermission()) {
            saveMusicVisualizer(true);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.music_visualizer)
                .setMessage(R.string.music_visualizer_permission_message)
                .setPositiveButton(R.string.enable, (dialog, which) -> requestPermissions(
                        new String[]{Manifest.permission.RECORD_AUDIO}, VISUALIZER_PERMISSION_REQUEST))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private boolean hasVisualizerPermission() {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void saveMusicVisualizer(boolean enabled) {
        musicVisualizer = enabled;
        choices.edit().putBoolean(Config.MUSIC_VISUALIZER_KEY, enabled).apply();
        changed();
        refresh();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != VISUALIZER_PERMISSION_REQUEST) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            saveMusicVisualizer(true);
        } else {
            Toast.makeText(this, R.string.music_visualizer_permission_denied, Toast.LENGTH_LONG).show();
            refresh();
        }
    }

    private void reverseRow(TextView[] views) {
        LinearLayout row = (LinearLayout) views[0].getParent();
        row.removeAllViews();
        for (int i = views.length - 1; i >= 0; i--) row.addView(views[i]);
    }

    private void showWallpaperPicker() {
        File dir = Config.dir(this);
        List<File> images = new java.util.ArrayList<>(WallpaperRotation.images(
                dir == null ? null : new File(dir, "wallpapers")));
        // A theme with a single wallpaper.jpg offers that picture too (Config maps the choice back to it).
        File single = dir == null ? null : new File(dir, Config.WALLPAPER_NAME);
        if (single != null && single.isFile()) images.add(0, single);
        String[] labels = new String[images.size() + 1];
        for (int i = 0; i < images.size(); i++) labels[i] = images.get(i).getName();
        labels[images.size()] = getString(R.string.wallpaper_none);
        String current = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        int checked = "none".equals(current) ? images.size()
                : indexOf(images, current);
        new AlertDialog.Builder(this)
                .setTitle(R.string.wallpaper_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == images.size()) {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY, "none").apply();
                    } else {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY,
                                images.get(which).getName()).apply();
                    }
                    changed();
                    Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNeutralButton(R.string.wallpaper_theme_default, (dialog, which) -> {
                    choices.edit().remove(Config.WALLPAPER_CHOICE_KEY).apply();
                    changed();
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void chooseHomeStyle() {
        chooseValue("home_style", R.string.home_style,
                new String[]{"theme", "cinema", "glow", "minimal"},
                new int[]{R.string.theme_default, R.string.style_cinema, R.string.style_glow, R.string.style_minimal});
    }

    private void choosePalette() {
        chooseValue("home_palette", R.string.home_palette,
                new String[]{"theme", "midnight", "ocean", "ember"},
                new int[]{R.string.theme_default, R.string.palette_midnight, R.string.palette_ocean, R.string.palette_ember});
    }

    private void chooseTileShape() {
        chooseValue("tile_shape", R.string.tile_shape,
                new String[]{"square", "soft", "round"},
                new int[]{R.string.shape_square, R.string.shape_soft, R.string.shape_round});
    }

    private void chooseValue(String key, int title, String[] values, int[] labels) {
        String selected = choices.getString(key, key.equals("tile_shape") ? "soft" : "theme");
        int checked = 0;
        CharSequence[] names = new CharSequence[labels.length];
        for (int i = 0; i < labels.length; i++) {
            names[i] = getString(labels[i]);
            if (values[i].equals(selected)) checked = i;
        }
        new AlertDialog.Builder(this).setTitle(title).setSingleChoiceItems(names, checked, (dialog, which) -> {
            String value = values[which];
            if (value.equals(choices.getString(key, key.equals("tile_shape") ? "soft" : "theme"))) {
                dialog.dismiss();
                return;
            }
            choices.edit().putString(key, value).apply();
            changed();
            refresh();
            dialog.dismiss();
        }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private int homeStyleLabel(String value) {
        switch (value) {
            case "cinema": return R.string.style_cinema;
            case "glow": return R.string.style_glow;
            case "minimal": return R.string.style_minimal;
            default: return R.string.theme_default;
        }
    }

    private int paletteLabel(String value) {
        switch (value) {
            case "midnight": return R.string.palette_midnight;
            case "ocean": return R.string.palette_ocean;
            case "ember": return R.string.palette_ember;
            default: return R.string.theme_default;
        }
    }

    private int tileShapeLabel(String value) {
        switch (value) {
            case "square": return R.string.shape_square;
            case "round": return R.string.shape_round;
            default: return R.string.shape_soft;
        }
    }

    private static int indexOf(List<File> images, String name) {
        if (name == null) return -1;
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getName().equals(name)) return i;
        }
        return -1;
    }

    /**
     * A choice was saved. Home rebuilds when it next resumes because the change count moved, whether these
     * settings are left with Back or with the remote's Home button (which cancels the activity result).
     */
    private void changed() {
        setResult(RESULT_OK);
        choices.edit().putLong(Config.SETTINGS_REVISION_KEY,
                choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
    }

    private void setAdvanced(boolean enabled) {
        advancedEffects = enabled;
        choices.edit().putBoolean("advanced_effects", enabled).apply();
        changed();
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < columnChoices.length; i++) {
            columnChoices[i].setSelected(columns == i + 4);
        }
        for (int i = 0; i < rowChoices.length; i++) {
            rowChoices[i].setSelected(rowsBelowDock == i);
        }
        for (int i = 0; i < dockChoices.length; i++) {
            dockChoices[i].setSelected(dockOpacity == DOCK_OPACITIES[i]);
        }
        animationsChoice.setText(getString(R.string.animations_state,
                getString(animations ? R.string.on : R.string.off)));
        advancedChoice.setText(getString(R.string.advanced_effects_state,
                getString(advancedEffects ? R.string.on : R.string.off)));
        advancedChoice.setEnabled(animations);
        advancedChoice.setAlpha(animations ? 1f : 0.5f);
        String wallpaperCurrent = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        wallpaperChoice.setText(getString(R.string.wallpaper_state,
                "none".equals(wallpaperCurrent) ? getString(R.string.wallpaper_none)
                        : wallpaperCurrent != null ? wallpaperCurrent
                        : getString(R.string.wallpaper_theme_default)));
        String visualizerState = getString(musicVisualizer ? R.string.on : R.string.off);
        if (musicVisualizer && !hasVisualizerPermission()) {
            visualizerState = getString(R.string.music_visualizer_needs_permission);
        }
        musicVisualizerChoice.setText(getString(R.string.music_visualizer_state, visualizerState));
        String mediaState = getString(nowPlayingEnabled ? R.string.on : R.string.off);
        if (nowPlayingEnabled && !NowPlaying.listenerEnabled(this)) {
            mediaState = getString(R.string.now_playing_access_needed);
        }
        nowPlayingChoice.setText(getString(R.string.now_playing_state, mediaState));
        homeStyleChoice.setText(getString(R.string.home_style_state, getString(homeStyleLabel(
                choices.getString("home_style", "theme")))));
        homePaletteChoice.setText(getString(R.string.home_palette_state, getString(paletteLabel(
                choices.getString("home_palette", "theme")))));
        tileShapeChoice.setText(getString(R.string.tile_shape_state, getString(tileShapeLabel(
                choices.getString("tile_shape", "soft")))));
        followThemeChoice.setSelected(!hasOnTvChoices());
    }

    private boolean hasOnTvChoices() {
        return choices.contains("columns") || choices.contains(Config.ROWS_BELOW_DOCK_KEY)
                || choices.contains("animations") || choices.contains("advanced_effects")
                || choices.contains(Config.WALLPAPER_CHOICE_KEY) || choices.contains(Config.DOCK_OPACITY_KEY)
                || choices.contains(Config.MUSIC_VISUALIZER_KEY) || choices.contains("home_style")
                || choices.contains("home_palette") || choices.contains("tile_shape");
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }
}

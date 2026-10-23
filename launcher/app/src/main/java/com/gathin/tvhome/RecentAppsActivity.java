package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** A small app switcher backed only by apps the owner has opened from this launcher. */
public final class RecentAppsActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_recent_apps);
        findViewById(R.id.dismiss).setOnClickListener(v -> finish());
        LinearLayout rows = findViewById(R.id.rows);
        TextView empty = findViewById(R.id.empty);
        PackageManager pm = getPackageManager();
        for (String pkg : LastOpened.recentPackages(this, 8)) {
            try {
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                if (pm.getLeanbackLaunchIntentForPackage(pkg) == null
                        && pm.getLaunchIntentForPackage(pkg) == null) continue;
                View row = getLayoutInflater().inflate(R.layout.item_input, rows, false);
                ((ImageView) row.findViewById(R.id.icon)).setImageDrawable(pm.getApplicationIcon(info));
                ((TextView) row.findViewById(R.id.title)).setText(pm.getApplicationLabel(info));
                row.findViewById(R.id.dot).setVisibility(View.GONE);
                ((TextView) row.findViewById(R.id.status)).setText(
                        LastOpened.describe(this, LastOpened.get(this, pkg)));
                row.setOnClickListener(v -> {
                    RemoteKeys.launch(this, pkg);
                    finish();
                });
                rows.addView(row);
            } catch (PackageManager.NameNotFoundException | SecurityException ignored) {
                // The app was removed or became unavailable since its last launch.
            }
        }
        empty.setVisibility(rows.getChildCount() == 0 ? View.VISIBLE : View.GONE);
        if (rows.getChildCount() > 0) rows.getChildAt(0).requestFocus();
        else empty.requestFocus();
    }
}

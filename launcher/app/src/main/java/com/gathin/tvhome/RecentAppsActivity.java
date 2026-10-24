package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
        View clear = findViewById(R.id.clearHistory);
        clear.setOnClickListener(v -> {
            LastOpened.clear(this);
            rows.removeAllViews();
            empty.setVisibility(View.VISIBLE);
            clear.setVisibility(View.GONE);
            empty.requestFocus();
        });
        PackageManager pm = getPackageManager();
        for (String pkg : LastOpened.recentPackages(this, 8)) {
            try {
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                if (pm.getLeanbackLaunchIntentForPackage(pkg) == null
                        && pm.getLaunchIntentForPackage(pkg) == null) continue;
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setGravity(Gravity.CENTER_VERTICAL);
                card.setPadding(dp(18), dp(16), dp(18), dp(16));
                card.setBackgroundResource(R.drawable.input_row_bg);
                card.setFocusable(true);
                card.setClickable(true);
                card.setContentDescription(String.valueOf(pm.getApplicationLabel(info)));
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(dp(224), dp(148));
                cardParams.setMarginEnd(dp(12));
                card.setLayoutParams(cardParams);
                ImageView icon = new ImageView(this);
                icon.setImageDrawable(pm.getApplicationIcon(info));
                card.addView(icon, new LinearLayout.LayoutParams(dp(50), dp(50)));
                TextView title = new TextView(this);
                title.setText(pm.getApplicationLabel(info));
                title.setTextColor(0xFFFFFFFF);
                title.setTextSize(17);
                title.setSingleLine(true);
                title.setEllipsize(android.text.TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                titleParams.topMargin = dp(12);
                card.addView(title, titleParams);
                TextView when = new TextView(this);
                when.setText(LastOpened.describe(this, LastOpened.get(this, pkg)));
                when.setTextColor(0xAFFFFFFF);
                when.setTextSize(12);
                when.setSingleLine(true);
                card.addView(when);
                card.setOnClickListener(v -> {
                    RemoteKeys.launch(this, pkg);
                    finish();
                });
                rows.addView(card);
            } catch (PackageManager.NameNotFoundException | SecurityException ignored) {
                // The app was removed or became unavailable since its last launch.
            }
        }
        empty.setVisibility(rows.getChildCount() == 0 ? View.VISIBLE : View.GONE);
        clear.setVisibility(rows.getChildCount() == 0 ? View.GONE : View.VISIBLE);
        if (rows.getChildCount() > 0) rows.getChildAt(0).requestFocus();
        else empty.requestFocus();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

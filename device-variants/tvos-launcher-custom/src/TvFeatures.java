package com.sid.tvos.custom;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.ComponentName;
import android.provider.Settings;
import android.os.Handler;
import android.os.Looper;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public final class TvFeatures {
  private static final String PREFS = "tvOS_user_settings";
  private static final String KEY = "hidden_apps";
  private static final Handler UI = new Handler(Looper.getMainLooper());
  private static android.widget.TextView priorDockName;
  private static DockTitleView dockFocusTitle;
  private static android.view.View dockFocusedTile;
  private static Activity dockTitleActivity;
  private static final WeakHashMap<android.view.View,android.view.ViewTreeObserver.OnScrollChangedListener> dockScrollHooks=new WeakHashMap<>();
  private TvFeatures() {}

  public static void setDockTitleVisible(Activity activity,boolean visible){if(activity==null||dockFocusTitle==null)return;if(!visible){hideDockTitle();return;}updateDockTitleVisibility();}
  private static void hideDockTitle(){if(dockFocusTitle!=null)dockFocusTitle.setVisibility(android.view.View.GONE);if(priorDockName!=null){priorDockName.setVisibility(android.view.View.VISIBLE);priorDockName=null;}dockFocusedTile=null;}
  private static boolean isValidDockFocus(Activity a,android.view.View tile,int position){if(a==null||tile==null||position<0||position>=5||!tile.isAttachedToWindow()||!tile.hasFocus())return false;int recent=a.getResources().getIdentifier("recentsOverlay","id",a.getPackageName());android.view.View recentView=a.findViewById(recent);if(recentView!=null&&recentView.getVisibility()==android.view.View.VISIBLE)return false;try{java.lang.reflect.Field f=a.getClass().getDeclaredField("settingsPanel");f.setAccessible(true);if(f.get(a)!=null)return false;}catch(Throwable ignored){}try{java.lang.reflect.Field f=a.getClass().getDeclaredField("controlCenter");f.setAccessible(true);Object o=f.get(a);if(o instanceof android.widget.PopupWindow&&((android.widget.PopupWindow)o).isShowing())return false;}catch(Throwable ignored){}return true;}
  private static void updateDockTitleVisibility(){if(dockFocusTitle==null)return;if(dockTitleActivity!=null&&dockFocusedTile!=null&&dockFocusedTile.isAttachedToWindow()&&dockFocusedTile.hasFocus()&&dockFocusTitle.getTextLength()>0)dockFocusTitle.setVisibility(android.view.View.VISIBLE);else hideDockTitle();}
  private static void hookDockScroll(Activity a,android.view.View root){if(dockScrollHooks.containsKey(root))return;android.view.ViewTreeObserver.OnScrollChangedListener l=()->hideDockTitle();dockScrollHooks.put(root,l);root.getViewTreeObserver().addOnScrollChangedListener(l);}
  public static void setHomeInfoVisible(Activity activity, boolean visible) {
    int id=activity.getResources().getIdentifier("homeInfoGlass","id",activity.getPackageName());
    android.view.View view=activity.findViewById(id);if(view!=null)view.setVisibility(visible?android.view.View.VISIBLE:android.view.View.GONE);
  }
  public static void ensureInfoStatusBar(Activity activity) {
    String[] textIds={"homeGreeting","clock","homePeriod","homeDate","weatherPillText"};
    for(String name:textIds){int id=activity.getResources().getIdentifier(name,"id",activity.getPackageName());android.view.View v=activity.findViewById(id);if(v instanceof android.widget.TextView)((android.widget.TextView)v).setShadowLayer(0f,0f,0f,Color.TRANSPARENT);}
    android.content.SharedPreferences p=activity.getSharedPreferences("appearance",0);
    if(!p.getBoolean("info_header_setup_v1",false))p.edit().putBoolean("status_minimal",false).putBoolean("info_header_setup_v1",true).apply();
    if(!p.getBoolean("header_cleanup_v13",false))p.edit().putBoolean("show_weather",false).putBoolean("show_assistant",false).putBoolean("header_cleanup_v13",true).apply();
    alignWeatherUnderDate(activity);
  }
  private static void alignWeatherUnderDate(Activity a){int topId=a.getResources().getIdentifier("topBar","id",a.getPackageName()),infoId=a.getResources().getIdentifier("homeInfoGlass","id",a.getPackageName()),dateId=a.getResources().getIdentifier("homeDate","id",a.getPackageName()),weatherId=a.getResources().getIdentifier("weatherPill","id",a.getPackageName());android.view.View top=a.findViewById(topId),info=a.findViewById(infoId),date=a.findViewById(dateId),weather=a.findViewById(weatherId);if(top==null||info==null||date==null||weather==null)return;top.post(()->{ViewGroup.LayoutParams raw=weather.getLayoutParams();if(raw instanceof FrameLayout.LayoutParams){FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)raw;lp.setMarginStart(Math.round(info.getX()+date.getX()));lp.topMargin=(int)(82*a.getResources().getDisplayMetrics().density);weather.setLayoutParams(lp);}});}
  /** Put the existing live weather pill in the profile header while the control centre is open. */
  public static void setProfileWeather(Activity a,boolean open){
    if(a==null)return;
    int wid=a.getResources().getIdentifier("weatherPill","id",a.getPackageName());
    int topId=a.getResources().getIdentifier("topBar","id",a.getPackageName());
    android.view.View pill=a.findViewById(wid),top=a.findViewById(topId);
    if(pill==null||!(top instanceof ViewGroup))return;
    pill.setVisibility(android.view.View.GONE);
    android.view.ViewGroup destination=null;boolean showing=false;
    try{java.lang.reflect.Field f=a.getClass().getDeclaredField("controlCenter");f.setAccessible(true);Object popup=f.get(a);if(popup instanceof android.widget.PopupWindow){showing=((android.widget.PopupWindow)popup).isShowing();android.view.View content=((android.widget.PopupWindow)popup).getContentView();int hid=a.getResources().getIdentifier("ccHeader","id",a.getPackageName());android.view.View header=content==null?null:content.findViewById(hid);if(header instanceof android.view.ViewGroup)destination=(android.view.ViewGroup)header;}}catch(Throwable ignored){}
    android.view.View old=destination==null?null:destination.findViewWithTag("tvos_profile_weather");
    if(!open||!showing||destination==null){if(old!=null&&old.getParent() instanceof ViewGroup)((ViewGroup)old.getParent()).removeView(old);return;}
    android.view.View sourceIcon=pill.findViewById(a.getResources().getIdentifier("weatherPillIcon","id",a.getPackageName()));
    android.view.View sourceText=pill.findViewById(a.getResources().getIdentifier("weatherPillText","id",a.getPackageName()));
    android.widget.LinearLayout copy;
    if(old instanceof android.widget.LinearLayout)copy=(android.widget.LinearLayout)old;
    else{
      copy=new android.widget.LinearLayout(a);copy.setTag("tvos_profile_weather");copy.setGravity(android.view.Gravity.CENTER_VERTICAL);copy.setOrientation(android.widget.LinearLayout.HORIZONTAL);copy.setPadding(dp(a,7),0,dp(a,7),0);
      android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(0x33262A35);bg.setCornerRadius(dp(a,16));bg.setStroke(dp(a,1),0x44FFFFFF);copy.setBackground(bg);
      android.widget.ImageView image=new android.widget.ImageView(a);image.setTag("profile_weather_icon");copy.addView(image,new android.widget.LinearLayout.LayoutParams(dp(a,16),dp(a,16)));
      android.widget.TextView text=new android.widget.TextView(a);text.setTag("profile_weather_text");text.setTextSize(11);text.setTextColor(Color.WHITE);text.setSingleLine(true);text.setEllipsize(android.text.TextUtils.TruncateAt.END);text.setMaxWidth(dp(a,82));android.widget.LinearLayout.LayoutParams tp=new android.widget.LinearLayout.LayoutParams(-2,-2);tp.leftMargin=dp(a,5);copy.addView(text,tp);
      android.widget.LinearLayout.LayoutParams lp=new android.widget.LinearLayout.LayoutParams(-2,dp(a,30));lp.setMargins(dp(a,5),0,dp(a,5),0);destination.addView(copy,Math.min(1,destination.getChildCount()),lp);
    }
    android.view.View image=copy.findViewWithTag("profile_weather_icon"),text=copy.findViewWithTag("profile_weather_text");
    if(image instanceof android.widget.ImageView&&sourceIcon instanceof android.widget.ImageView)((android.widget.ImageView)image).setImageDrawable(((android.widget.ImageView)sourceIcon).getDrawable());
    if(text instanceof android.widget.TextView&&sourceText instanceof android.widget.TextView)((android.widget.TextView)text).setText(((android.widget.TextView)sourceText).getText());
  }

  private static final class DockTitleView extends android.view.View {
    private final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG|android.graphics.Paint.SUBPIXEL_TEXT_FLAG);
    private CharSequence value="";private CharSequence display="";private int maxWidth=Integer.MAX_VALUE;
    DockTitleView(Activity a){super(a);paint.setColor(Color.WHITE);paint.setStyle(android.graphics.Paint.Style.FILL);paint.setStrokeWidth(0f);paint.setTypeface(android.graphics.Typeface.create("sans-serif-light",android.graphics.Typeface.NORMAL));paint.setTextSize(32f*a.getResources().getDisplayMetrics().scaledDensity);setLayerType(android.view.View.LAYER_TYPE_SOFTWARE,null);}
    int getTextLength(){return value==null?0:value.length();}
    void setMaxWidth(int width){maxWidth=Math.max(1,width);requestLayout();}
    void setTitle(CharSequence title){value=title==null?"":title;display=android.text.TextUtils.ellipsize(value,new android.text.TextPaint(paint),maxWidth,android.text.TextUtils.TruncateAt.END);requestLayout();invalidate();}
    @Override protected void onMeasure(int ws,int hs){android.graphics.Paint.FontMetrics fm=paint.getFontMetrics();int wantW=Math.min(maxWidth,(int)Math.ceil(paint.measureText(display.toString())));int wantH=(int)Math.ceil(fm.bottom-fm.top);setMeasuredDimension(resolveSize(wantW,ws),resolveSize(wantH,hs));}
    @Override protected void onDraw(android.graphics.Canvas canvas){super.onDraw(canvas);android.graphics.Paint.FontMetrics fm=paint.getFontMetrics();canvas.drawText(display.toString(),0f,-fm.top,paint);}
  }

  private static int dp(Activity a,int n){return (int)(n*a.getResources().getDisplayMetrics().density+.5f);}
  public static boolean ambientEnabled(android.content.Context context) {
    return context.getSharedPreferences("appearance",0).getBoolean("ambient_glow",true);
  }
  public static void setAmbientEnabled(android.content.Context context, boolean enabled) {
    context.getSharedPreferences("appearance",0).edit().putBoolean("ambient_glow",enabled).apply();
  }
  private static int tint(int color, int amount) {
    int r=(Color.red(color)*amount+8*(255-amount))/255;
    int g=(Color.green(color)*amount+9*(255-amount))/255;
    int b=(Color.blue(color)*amount+13*(255-amount))/255;
    return Color.rgb(r,g,b);
  }
  private static int mix(int from,int to,int amount) {
    int inverse=255-amount;
    return Color.rgb((Color.red(from)*inverse+Color.red(to)*amount)/255,
      (Color.green(from)*inverse+Color.green(to)*amount)/255,
      (Color.blue(from)*inverse+Color.blue(to)*amount)/255);
  }
  public static void applyFocusGradient(Activity a, FrameLayout root, android.view.View tile, int color, int position) {
    updateHeaderForFocus(root,position);
    int hintId=a.getResources().getIdentifier("fullHint","id",a.getPackageName());android.view.View hint=root.findViewById(hintId);
    if(hint instanceof ViewGroup&&((ViewGroup)hint).getChildCount()>1)((ViewGroup)hint).getChildAt(1).setVisibility(android.view.View.GONE);
    if(dockFocusTitle==null){dockFocusTitle=new DockTitleView(a);dockFocusTitle.setFocusable(false);dockFocusTitle.setClickable(false);dockFocusTitle.setVisibility(android.view.View.GONE);root.addView(dockFocusTitle,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT));}
    hookDockScroll(a,root);
    android.widget.TextView name=tile.getParent() instanceof ViewGroup?(android.widget.TextView)((ViewGroup)tile.getParent()).findViewById(a.getResources().getIdentifier("name","id",a.getPackageName())):null;
    if(name!=null&&isValidDockFocus(a,tile,position)){if(priorDockName!=null&&priorDockName!=name)priorDockName.setVisibility(android.view.View.VISIBLE);priorDockName=name;name.setVisibility(android.view.View.INVISIBLE);dockTitleActivity=a;dockFocusedTile=tile;android.graphics.Rect titleRect=new android.graphics.Rect(0,0,tile.getWidth(),tile.getHeight());root.offsetDescendantRectToMyCoords(tile,titleRect);FrameLayout.LayoutParams titleLp=(FrameLayout.LayoutParams)dockFocusTitle.getLayoutParams();titleLp.gravity=android.view.Gravity.TOP|android.view.Gravity.LEFT;titleLp.leftMargin=Math.round(root.getWidth()*0.07f);titleLp.topMargin=Math.max(dp(a,190),titleRect.top-dp(a,56));dockFocusTitle.setMaxWidth(Math.max(dp(a,260),root.getWidth()-titleLp.leftMargin-dp(a,48)));dockFocusTitle.setTitle(name.getText());dockFocusTitle.setLayoutParams(titleLp);dockFocusTitle.setVisibility(android.view.View.VISIBLE);dockFocusTitle.bringToFront();}else hideDockTitle();

    android.widget.FrameLayout content=(android.widget.FrameLayout)a.findViewById(android.R.id.content);
    MusicCardView music=ensureMusicCard(a,root,content);
    if(music==null)return;
    music.setHomeSlotActive(position<5);
    music.bringToFront();
    music.setElevation(64f*a.getResources().getDisplayMetrics().density);
    if(music.getVisibility()==android.view.View.VISIBLE&&tile.hasFocus())music.routeFocusFrom(tile);
    // The stock cinematic shelf paints the focused app's poster/logo behind Home.
    // Keep the focus colour and ambient glow, but use a clean gradient as the backdrop.
    int shelfId=a.getResources().getIdentifier("shelfGroup","id",a.getPackageName());
    android.view.View shelf=a.findViewById(shelfId);
    if(shelf!=null)shelf.setVisibility(android.view.View.GONE);
    boolean dark=a.getSharedPreferences("appearance",0).getBoolean("dark",false);
    int[] gradient=dark
      ?new int[]{tint(color,40),tint(color,18),tint(color,9)}
      :new int[]{mix(color,Color.WHITE,32),mix(color,Color.WHITE,112),mix(color,Color.WHITE,184)};
    GradientDrawable backdrop=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,gradient);backdrop.setDither(true);
    android.graphics.drawable.Drawable old=root.getBackground();
    if(old==null) root.setBackground(backdrop);
    else {
      android.graphics.drawable.TransitionDrawable transition=new android.graphics.drawable.TransitionDrawable(new android.graphics.drawable.Drawable[]{old,backdrop});
      transition.setCrossFadeEnabled(true); root.setBackground(transition); transition.startTransition(520);
    }
    AmbientGlowView glow=(AmbientGlowView)root.findViewWithTag("tvos_ambient_glow");
    music.refreshGlassBackdrop();
    if (glow==null) {
      glow=new AmbientGlowView(a); glow.setTag("tvos_ambient_glow");
      glow.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);
      glow.setFocusable(false); glow.setClickable(false);
      root.addView(glow,2,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
    }
    if (!ambientEnabled(a) || root.getWidth()==0 || tile.getWidth()==0) { glow.setVisibility(android.view.View.GONE); return; }
    Rect r=new Rect(0,0,tile.getWidth(),tile.getHeight());
    root.offsetDescendantRectToMyCoords(tile,r);
    glow.update(color,r.exactCenterX(),r.exactCenterY(),Math.max(root.getWidth()*0.62f,tile.getWidth()*2.2f));
    glow.setVisibility(android.view.View.VISIBLE);
  }
  private static Drawable homeWallpaper(Activity a,FrameLayout root){try{Class<?> type=Class.forName("com.sid.tvos.CwBackground");Method mode=type.getDeclaredMethod("mode",android.content.Context.class);mode.setAccessible(true);if(((Integer)mode.invoke(null,a))!=4)return null;Method get=type.getDeclaredMethod("get",android.content.Context.class,int.class,int.class);get.setAccessible(true);android.graphics.Bitmap bitmap=(android.graphics.Bitmap)get.invoke(null,a,Math.max(1,root.getWidth()),Math.max(1,root.getHeight()));if(bitmap==null)return null;BitmapDrawable photo=new BitmapDrawable(a.getResources(),bitmap);photo.setGravity(android.view.Gravity.FILL);GradientDrawable shade=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x70000000,0x28000000,0x88000000});return new LayerDrawable(new Drawable[]{photo,shade});}catch(Throwable ignored){return null;}}

  public static void showMusicAccessPrompt(Activity a) {
    int id=a.getResources().getIdentifier("root","id",a.getPackageName());
    android.view.View v=a.findViewById(id);
    if(v instanceof FrameLayout){maybePromptMusicAccess(a,(FrameLayout)v);android.widget.FrameLayout content=(android.widget.FrameLayout)a.findViewById(android.R.id.content);ensureMusicCard(a,(FrameLayout)v,content);}
  }

  private static MusicCardView ensureMusicCard(Activity a,FrameLayout root,FrameLayout content){
    android.content.SharedPreferences appearance=a.getSharedPreferences("appearance",0);if(!appearance.getBoolean("clean_home_migration_v15",false))appearance.edit().putBoolean("cw_on",false).putBoolean("cw_art",false).putBoolean("clean_home_migration_v15",true).apply();
    if(content==null)return null;MusicCardView music=(MusicCardView)content.findViewWithTag("tvos_music_home");if(music!=null)return music;
    music=new MusicCardView(a);music.setTag("tvos_music_home");music.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);content.addView(music,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT,android.view.Gravity.TOP));music.setElevation(64f*a.getResources().getDisplayMetrics().density);return music;
  }

  public static boolean focusMusicControl(Activity a,int keyCode) {
    if(keyCode!=android.view.KeyEvent.KEYCODE_DPAD_UP)return false;
    int rootId=a.getResources().getIdentifier("root","id",a.getPackageName());
    android.view.View v=a.findViewById(rootId);
    if(!(v instanceof FrameLayout))return false;
    android.widget.FrameLayout content=(android.widget.FrameLayout)a.findViewById(android.R.id.content);
    MusicCardView music=content==null?null:(MusicCardView)content.findViewWithTag("tvos_music_home");
    if(music==null||music.getVisibility()!=android.view.View.VISIBLE)return false;
    music.focusPlay();return true;
  }

  public static void prepareProfile(Activity a) {
    android.widget.FrameLayout content=(android.widget.FrameLayout)a.findViewById(android.R.id.content);
    MusicCardView music=content==null?null:(MusicCardView)content.findViewWithTag("tvos_music_home");
    if(music!=null)music.setProfileMode(true);
    try { Method m=a.getClass().getDeclaredMethod("showBackdrop"); m.setAccessible(true); m.invoke(a); }
    catch(Exception ignored) { }
  }
  public static void profileOpened(Activity a){setHomeInfoVisible(a,false);android.widget.FrameLayout content=(android.widget.FrameLayout)a.findViewById(android.R.id.content);MusicCardView music=content==null?null:(MusicCardView)content.findViewWithTag("tvos_music_home");if(music!=null)music.setProfilePinned(true);else setProfileWeather(a,true);}
  public static void profileClosed(Activity a){setHomeInfoVisible(a,true);android.widget.FrameLayout content=(android.widget.FrameLayout)a.findViewById(android.R.id.content);MusicCardView music=content==null?null:(MusicCardView)content.findViewWithTag("tvos_music_home");if(music!=null)music.setProfilePinned(false);else setProfileWeather(a,false);}
  public static void showProfileBackdrop(Activity a){UI.postDelayed(()->{if(a==null||a.isFinishing()||a.isDestroyed())return;try{Method m=a.getClass().getDeclaredMethod("showBackdrop");m.setAccessible(true);m.invoke(a);}catch(Throwable ignored){}},100L);}

  private static void updateHeaderForFocus(FrameLayout root,int position) {
    int id=root.getResources().getIdentifier("topBar","id",root.getContext().getPackageName());
    final android.view.View bar=root.findViewById(id); if(bar==null)return;
    boolean atTop=position<5;
    bar.animate().cancel();bar.setVisibility(android.view.View.VISIBLE);bar.animate().alpha(atTop?1f:0f).setDuration(180).start();
  }

  private static boolean hasMusicAccess(Activity a) {
    String listeners=Settings.Secure.getString(a.getContentResolver(),"enabled_notification_listeners");
    return listeners!=null&&listeners.contains(a.getPackageName()+"/");
  }

  private static void maybePromptMusicAccess(Activity a, FrameLayout root) {
    android.content.SharedPreferences p=a.getSharedPreferences("appearance",0);
    if(p.getBoolean("music_access_prompt_seen_v15",false))return;
    if(hasMusicAccess(a)){p.edit().putBoolean("music_access_prompt_seen_v15",true).apply();return;}
    p.edit().putBoolean("music_access_prompt_seen_v15",true).apply();
    root.postDelayed(()->{
      if(a.isFinishing()||a.isDestroyed())return;
      new AlertDialog.Builder(a,android.R.style.Theme_DeviceDefault_Dialog_Alert)
        .setTitle("Home music controls")
        .setMessage("Allow notification access so the launcher can detect and control music. Android also makes other app notifications available to a notification listener.")
        .setPositiveButton("Enable",(dialog,which)->{
          try { a.startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")); }
          catch(Exception unavailable) {
            new AlertDialog.Builder(a,android.R.style.Theme_DeviceDefault_Dialog_Alert)
              .setTitle("Enable music access in TV Settings")
              .setMessage("Open Settings → Apps → Special app access → Notification access, then enable tvOS Launcher Custom. This TV may place Notification access elsewhere.")
              .setPositiveButton("OK",null).show();
          }
        })
        .setNegativeButton("Not now",null).show();
    },900L);
  }

  public static void showHideApps(final Activity a) {
    Intent query = new Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.LEANBACK_LAUNCHER");
    List<ResolveInfo> resolved = a.getPackageManager().queryIntentActivities(query, 0);
    final java.util.Map<String,String> labelsByPackage = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    for (ResolveInfo r : resolved) if (r.activityInfo != null && !r.activityInfo.packageName.equals(a.getPackageName()))
      labelsByPackage.put(r.activityInfo.packageName, String.valueOf(r.loadLabel(a.getPackageManager())));
    final List<String> packages = new ArrayList<>(labelsByPackage.keySet());
    final CharSequence[] labels = new CharSequence[packages.size()];
    for (int i=0;i<packages.size();i++) labels[i]=labelsByPackage.get(packages.get(i));
    final Set<String> selected = new HashSet<>();
    java.util.Set<String> saved = a.getSharedPreferences(PREFS,0).getStringSet(KEY,null);
    if (saved != null) selected.addAll(saved);
    else {
      int id=a.getResources().getIdentifier("hidden_apps","array",a.getPackageName());
      if(id!=0) Collections.addAll(selected,a.getResources().getStringArray(id));
    }
    final boolean[] checked = new boolean[packages.size()];
    for(int i=0;i<checked.length;i++) checked[i]=selected.contains(packages.get(i));
    AlertDialog dialog=new AlertDialog.Builder(a,android.R.style.Theme_DeviceDefault_Dialog_Alert)
      .setTitle("Hide apps · tick to hide")
      .setMultiChoiceItems(labels,checked,(d,which,on)->{String p=packages.get(which);if(on)selected.add(p);else selected.remove(p);})
      .setPositiveButton("Save",null).setNegativeButton("Cancel",null).create();
    dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
      a.getSharedPreferences(PREFS,0).edit().putStringSet(KEY,new HashSet<>(selected)).apply(); dialog.dismiss();
      try { Method m=a.getClass().getMethod("reloadApps"); m.invoke(a); } catch(Exception ignored) {}
    })); dialog.show();
  }
}


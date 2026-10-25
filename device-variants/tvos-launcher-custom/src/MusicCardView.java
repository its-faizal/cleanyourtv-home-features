package com.sid.tvos.custom;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.view.PixelCopy;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/** Glass media card hosted above the launcher content; controls remain remote-focusable. */
public final class MusicCardView extends FrameLayout {
  private final Handler handler=new Handler(Looper.getMainLooper());
  private final TextView title,artist,album,open,previous,play,next,close;
  private final ImageView art;
  private final LinearLayout shell,controls;
  private final View divider;
  private MediaController active;
  private boolean homeSlotActive=true;
  private static final Set<String> dismissed=new HashSet<>();
  private View returnFocus;
  private android.graphics.drawable.Drawable glassDrawable;
  private final Runnable glassRefresh=()->captureGlassBackdrop();
  private boolean profileMode;
  private boolean profilePinned;
  private long profileModeChangedAt;
  private final Runnable profileMonitor=new Runnable(){public void run(){updateProfilePlacement();if(isAttachedToWindow())handler.postDelayed(this,220L);}};
  private final Runnable poll=new Runnable(){public void run(){refresh();handler.postDelayed(this,1000);}};

  public static void dismissPackage(Context context,String pkg){dismissed.add(pkg);try{MediaSessionManager m=(MediaSessionManager)context.getSystemService(Context.MEDIA_SESSION_SERVICE);for(MediaController c:m.getActiveSessions(new ComponentName(context.getPackageName(),"com.sid.tvos.NotifListener")))if(pkg.equals(c.getPackageName()))c.getTransportControls().stop();}catch(Exception ignored){}}

  public MusicCardView(Context c){
    super(c);setVisibility(GONE);setFocusable(false);setClickable(false);setClipChildren(false);setClipToPadding(false);
    shell=new LinearLayout(c);shell.setOrientation(LinearLayout.VERTICAL);shell.setPadding(dp(18),dp(12),dp(18),dp(12));
    GradientDrawable glass=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0x784A5366,0x70303443,0x781A1D27});glass.setCornerRadius(dp(28));glass.setStroke(dp(1),0x88FFFFFF);shell.setBackground(glass);shell.setElevation(dp(24));shell.setClipToOutline(true);
    LinearLayout info=new LinearLayout(c);info.setGravity(Gravity.CENTER_VERTICAL);
    art=new ImageView(c);art.setScaleType(ImageView.ScaleType.CENTER_CROP);GradientDrawable artBg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF544B70,0xFF252936});artBg.setCornerRadius(dp(18));art.setBackground(artBg);art.setClipToOutline(true);info.addView(art,new LinearLayout.LayoutParams(dp(140),dp(140)));
    LinearLayout words=new LinearLayout(c);words.setOrientation(LinearLayout.VERTICAL);words.setGravity(Gravity.CENTER_VERTICAL);words.setPadding(dp(24),0,0,0);info.addView(words,new LinearLayout.LayoutParams(0,-1,1));
    TextView eyebrow=text(c,12,0xBFFFFFFF,true);eyebrow.setText("NOW PLAYING");words.addView(eyebrow);
    title=text(c,22,0xFFFFFFFF,true);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.topMargin=dp(8);words.addView(title,tp);
    artist=text(c,15,0xE6FFFFFF,false);artist.setSingleLine(true);artist.setEllipsize(android.text.TextUtils.TruncateAt.END);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.topMargin=dp(5);words.addView(artist,ap);
    album=text(c,12,0xBFFFFFFF,false);album.setSingleLine(true);album.setEllipsize(android.text.TextUtils.TruncateAt.END);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.topMargin=dp(4);words.addView(album,bp);
    shell.addView(info,new LinearLayout.LayoutParams(-1,0,1));
    divider=new View(c);divider.setVisibility(GONE);
    controls=new LinearLayout(c);controls.setGravity(Gravity.CENTER);controls.setOrientation(LinearLayout.HORIZONTAL);
    open=button(c,"▣","Open music app");previous=button(c,"|◀","Previous track");play=button(c,"▶","Play");next=button(c,"▶|","Next track");close=button(c,"×","Close music player");
    TextView[] all={open,previous,play,next,close};for(int i=0;i<all.length;i++){if(i>0){all[i].setNextFocusLeftId(all[i-1].getId());all[i-1].setNextFocusRightId(all[i].getId());}LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(42),1);if(i>0)p.leftMargin=dp(8);controls.addView(all[i],p);}
    shell.addView(controls,new LinearLayout.LayoutParams(-1,dp(46)));
    FrameLayout.LayoutParams slp=new FrameLayout.LayoutParams(dp(620),dp(230),Gravity.TOP|Gravity.CENTER_HORIZONTAL);slp.topMargin=dp(150);addView(shell,slp);installLiquidGlass(c);
    open.setOnClickListener(v->launchPlayer());previous.setOnClickListener(v->control(-1));play.setOnClickListener(v->control(0));next.setOnClickListener(v->control(1));close.setOnClickListener(v->closePlayer());
  }
  private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  private void installLiquidGlass(Context c){try{Class<?> type=Class.forName("com.sid.tvos.LiquidGlassDrawable");java.lang.reflect.Constructor<?> ctor=type.getDeclaredConstructor(float.class,float.class);ctor.setAccessible(true);float density=getResources().getDisplayMetrics().density;Object glass=ctor.newInstance(density,density*28f);java.lang.reflect.Method liquid=type.getDeclaredMethod("setLiquid",boolean.class);liquid.setAccessible(true);liquid.invoke(glass,true);glassDrawable=(android.graphics.drawable.Drawable)glass;shell.setBackground(glassDrawable);shell.setLayerType(View.LAYER_TYPE_HARDWARE,null);}catch(Throwable ignored){}}
  public void refreshGlassBackdrop(){handler.removeCallbacks(glassRefresh);handler.postDelayed(glassRefresh,260L);}
  public void setProfileMode(boolean open){if(profileMode!=open){profileMode=open;profileModeChangedAt=System.currentTimeMillis();if(getContext() instanceof android.app.Activity){TvFeatures.setHomeInfoVisible((android.app.Activity)getContext(),!open);TvFeatures.setDockTitleVisible((android.app.Activity)getContext(),!open);}if(open){animate().cancel();setAlpha(0f);setVisibility(GONE);}else if(active!=null&&homeSlotActive){animate().cancel();setVisibility(VISIBLE);setAlpha(0f);setScaleX(.985f);setScaleY(.985f);showSmooth();}FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)shell.getLayoutParams();lp.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;lp.setMarginStart(0);lp.topMargin=dp(150);shell.setLayoutParams(lp);shell.setTranslationX(0);shell.requestLayout();refreshGlassBackdrop();}if(getContext() instanceof android.app.Activity)TvFeatures.setProfileWeather((android.app.Activity)getContext(),open);}
  public void setProfilePinned(boolean open){profilePinned=open;if(!open){homeSlotActive=true;reserveGridSpace(true);setProfileMode(false);handler.removeCallbacks(poll);handler.postDelayed(this::refresh,220L);}else setProfileMode(true);}
  private void reserveGridSpace(boolean reserve){try{android.app.Activity a=(android.app.Activity)getContext();int id=getResources().getIdentifier("grid","id",getContext().getPackageName());View grid=a.findViewById(id);if(grid==null)return;ViewGroup parent=(ViewGroup)grid.getParent();if(parent==null)return;int top=reserve?dp(400):0;int height=reserve?Math.max(dp(160),parent.getHeight()-top):ViewGroup.LayoutParams.MATCH_PARENT;FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)grid.getLayoutParams();if(lp.topMargin!=top||lp.height!=height){lp.topMargin=top;lp.height=height;grid.setLayoutParams(lp);}}catch(Throwable ignored){}}
  private boolean isRecentsOpen(){try{android.app.Activity a=(android.app.Activity)getContext();int id=a.getResources().getIdentifier("recentsOverlay","id",a.getPackageName());View v=a.findViewById(id);return v!=null&&v.getVisibility()==View.VISIBLE;}catch(Throwable ignored){return false;}}
  private void updateProfilePlacement(){if(profilePinned){if(getContext() instanceof android.app.Activity)TvFeatures.setProfileWeather((android.app.Activity)getContext(),true);return;}if(isRecentsOpen()){reserveGridSpace(false);hideSmooth();if(getContext() instanceof android.app.Activity)TvFeatures.setDockTitleVisible((android.app.Activity)getContext(),false);return;}try{Class<?> activity=getContext().getClass();java.lang.reflect.Field f=activity.getDeclaredField("settingsPanel");f.setAccessible(true);boolean open=f.get(getContext())!=null;if(!open&&profileMode&&System.currentTimeMillis()-profileModeChangedAt<300L)return;setProfileMode(open);}catch(Throwable ignored){}}
  private void captureGlassBackdrop(){if(Build.VERSION.SDK_INT<26||glassDrawable==null||getVisibility()!=VISIBLE)return;try{android.app.Activity a=(android.app.Activity)getContext();android.view.Window window=a.getWindow();View decor=window.getDecorView();int width=decor.getWidth(),height=decor.getHeight();if(width<2||height<2)return;android.graphics.Bitmap full=android.graphics.Bitmap.createBitmap(width,height,android.graphics.Bitmap.Config.ARGB_8888);float oldAlpha=shell.getAlpha();shell.setAlpha(0f);PixelCopy.request(window,full,copyResult->{shell.setAlpha(oldAlpha);if(copyResult!=PixelCopy.SUCCESS||shell.getWidth()<2||shell.getHeight()<2){full.recycle();return;}int[] pos=new int[2];shell.getLocationInWindow(pos);int x=Math.max(0,Math.min(width-1,pos[0])),y=Math.max(0,Math.min(height-1,pos[1]));int w=Math.max(1,Math.min(shell.getWidth(),width-x)),h=Math.max(1,Math.min(shell.getHeight(),height-y));android.graphics.Bitmap crop=android.graphics.Bitmap.createBitmap(full,x,y,w,h);full.recycle();int sw=Math.max(1,w/3),sh=Math.max(1,h/3);android.graphics.Bitmap small=android.graphics.Bitmap.createScaledBitmap(crop,sw,sh,true);crop.recycle();int[] pixels=new int[sw*sh];small.getPixels(pixels,0,sw,0,0,sw,sh);try{Class<?> blur=Class.forName("com.sid.tvos.Blur");java.lang.reflect.Method box=blur.getDeclaredMethod("boxBlur",int[].class,int.class,int.class,int.class);box.setAccessible(true);box.invoke(null,pixels,sw,sh,2);}catch(Throwable ignored){}small.setPixels(pixels,0,sw,0,0,sw,sh);android.graphics.Bitmap backdrop=android.graphics.Bitmap.createScaledBitmap(small,w,h,true);small.recycle();try{java.lang.reflect.Method set=glassDrawable.getClass().getDeclaredMethod("setBackdrop",android.graphics.Bitmap.class,long.class);set.setAccessible(true);set.invoke(glassDrawable,backdrop,System.currentTimeMillis());}catch(Throwable ignored){}shell.invalidate();},handler);}catch(Throwable ignored){}}
  @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);if(w<=0||h<=0)return;int cw=Math.min(dp(620),w-dp(48)),ch=Math.min(dp(250),h-dp(20));FrameLayout.LayoutParams fp=(FrameLayout.LayoutParams)shell.getLayoutParams();if(fp.width!=cw||fp.height!=ch){fp.width=cw;fp.height=ch;shell.setLayoutParams(fp);}LinearLayout.LayoutParams cp=(LinearLayout.LayoutParams)controls.getLayoutParams();if(cp.height!=dp(46)){cp.height=dp(46);controls.setLayoutParams(cp);}int image=Math.max(dp(48),Math.min(dp(132),ch-dp(24)-dp(46)-dp(18)));LinearLayout.LayoutParams ip=(LinearLayout.LayoutParams)art.getLayoutParams();if(ip.width!=image||ip.height!=image){ip.width=image;ip.height=image;art.setLayoutParams(ip);}}
  private TextView text(Context c,int sp,int color,boolean medium){TextView t=new TextView(c);t.setTextSize(sp);t.setTextColor(color);t.setTypeface(Typeface.create(medium?"sans-serif-medium":"sans-serif",0));t.setIncludeFontPadding(false);return t;}
  private TextView button(Context c,String icon,String label){TextView t=text(c,22,0xFFFFFFFF,true);t.setText(icon);t.setGravity(Gravity.CENTER);t.setContentDescription(label);t.setFocusable(true);t.setClickable(true);t.setId(View.generateViewId());t.setBackground(buttonBg(false));t.setOnFocusChangeListener((v,f)->v.setBackground(buttonBg(f)));return t;}
  private GradientDrawable buttonBg(boolean focus){GradientDrawable d=new GradientDrawable();d.setCornerRadius(dp(13));d.setColor(focus?0xFF7158A6:0x443E4350);d.setStroke(dp(1),focus?0xFFFFFFFF:0x44FFFFFF);return d;}
  public void routeFocusFrom(View tile){if(tile==null||tile==open||tile==previous||tile==play||tile==next||tile==close)return;if(tile.getId()==View.NO_ID)tile.setId(View.generateViewId());returnFocus=tile;tile.setNextFocusUpId(play.getId());open.setNextFocusDownId(tile.getId());previous.setNextFocusDownId(tile.getId());play.setNextFocusDownId(tile.getId());next.setNextFocusDownId(tile.getId());close.setNextFocusDownId(tile.getId());}
  public void focusPlay(){play.requestFocus();}
  public void setHomeSlotActive(boolean active){homeSlotActive=active;if(profileMode){reserveGridSpace(this.active!=null);return;}if(active&&this.active!=null){reserveGridSpace(true);showSmooth();}else{reserveGridSpace(false);hideSmooth();}}
  private void showSmooth(){if(profileMode)return;animate().cancel();if(getVisibility()!=VISIBLE){setVisibility(VISIBLE);setAlpha(0f);setScaleX(.975f);setScaleY(.975f);}animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(280).setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f)).start();}
  private void hideSmooth(){if(getVisibility()!=VISIBLE)return;animate().cancel();animate().alpha(0f).scaleX(.985f).scaleY(.985f).setDuration(220).withEndAction(()->{if(!homeSlotActive)setVisibility(GONE);}).start();}
  private void launchPlayer(){if(active==null)return;try{Intent i=getContext().getPackageManager().getLaunchIntentForPackage(active.getPackageName());if(i!=null){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);getContext().startActivity(i);}}catch(Exception ignored){}}
  private void closePlayer(){if(active==null)return;String pkg=active.getPackageName();dismissPackage(getContext(),pkg);active=null;reserveGridSpace(false);animate().cancel();animate().alpha(0f).scaleX(.985f).scaleY(.985f).setDuration(180).withEndAction(()->setVisibility(GONE)).start();}
  private void control(int which){if(active==null)return;MediaController.TransportControls t=active.getTransportControls();if(which==-1)t.skipToPrevious();else if(which==1)t.skipToNext();else{PlaybackState s=active.getPlaybackState();if(s!=null&&s.getState()==PlaybackState.STATE_PLAYING)t.pause();else t.play();}}
  @Override protected void onAttachedToWindow(){super.onAttachedToWindow();handler.removeCallbacks(poll);poll.run();handler.removeCallbacks(profileMonitor);handler.post(profileMonitor);postDelayed(this::refreshGlassBackdrop,350L);}
  @Override protected void onDetachedFromWindow(){handler.removeCallbacks(poll);handler.removeCallbacks(glassRefresh);handler.removeCallbacks(profileMonitor);super.onDetachedFromWindow();}
  private void refresh(){try{if(isRecentsOpen()){reserveGridSpace(false);hideSmooth();return;}MediaSessionManager m=(MediaSessionManager)getContext().getSystemService(Context.MEDIA_SESSION_SERVICE);List<MediaController> sessions=m.getActiveSessions(new ComponentName("com.sid.tvos.custom","com.sid.tvos.NotifListener"));Set<String> present=new HashSet<>();MediaController chosen=null,fallback=null;for(MediaController c:sessions){String pkg=c.getPackageName();PlaybackState s=c.getPlaybackState();if(pkg==null||!pkg.toLowerCase(java.util.Locale.ROOT).contains("music"))continue;present.add(pkg);if(dismissed.contains(pkg)){if(s!=null&&s.getState()==PlaybackState.STATE_PLAYING)dismissed.remove(pkg);else continue;}if(fallback==null)fallback=c;if(s!=null&&s.getState()==PlaybackState.STATE_PLAYING){chosen=c;break;}}dismissed.retainAll(present);if(chosen==null)chosen=fallback;if(chosen==null){active=null;reserveGridSpace(false);hideSmooth();return;}active=chosen;MediaMetadata md=chosen.getMetadata();String t=md==null?null:md.getString(MediaMetadata.METADATA_KEY_TITLE);String a=md==null?null:md.getString(MediaMetadata.METADATA_KEY_ARTIST);String al=md==null?null:md.getString(MediaMetadata.METADATA_KEY_ALBUM);title.setText(t==null||t.trim().isEmpty()?"Playing audio":t);artist.setText(a==null||a.trim().isEmpty()?chosen.getPackageName():a);album.setText(al==null||al.trim().isEmpty()?"":al);Bitmap b=null;if(md!=null){b=md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);if(b==null)b=md.getBitmap(MediaMetadata.METADATA_KEY_ART);if(b==null)b=md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);}if(b!=null)art.setImageBitmap(b);else try{art.setImageDrawable(getContext().getPackageManager().getApplicationIcon(chosen.getPackageName()));}catch(Exception ignored){art.setImageResource(android.R.drawable.ic_media_play);}PlaybackState s=chosen.getPlaybackState();boolean playing=s!=null&&s.getState()==PlaybackState.STATE_PLAYING;play.setText(playing?"Ⅱ":"▶");play.setContentDescription(playing?"Pause":"Play");if(homeSlotActive){reserveGridSpace(true);showSmooth();}else{reserveGridSpace(false);hideSmooth();}if(homeSlotActive&&returnFocus==null){int id=getResources().getIdentifier("grid","id",getContext().getPackageName());View grid=((android.app.Activity)getContext()).findViewById(id);if(grid!=null&&grid.findFocus()!=null)routeFocusFrom(grid.findFocus());}}catch(Exception ignored){active=null;reserveGridSpace(false);hideSmooth();}}
}

package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.audiofx.Visualizer;
import android.util.AttributeSet;
import android.view.View;

/** Lightweight output-mix spectrum bars. Capture exists only while the Home activity is resumed. */
public final class MusicVisualizerView extends View {
    private static final int BAR_COUNT = 28;
    private static final int CAPTURE_SIZE = 256;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] levels = new float[BAR_COUNT];
    private final RectF bar = new RectF();
    private Visualizer visualizer;

    public MusicVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        paint.setColor(0xFF9AD8FF);
        for (int i = 0; i < levels.length; i++) levels[i] = 0.035f;
    }

    void start() {
        if (visualizer != null || getVisibility() != VISIBLE) return;
        Visualizer capture = null;
        try {
            int[] range = Visualizer.getCaptureSizeRange();
            int size = Math.max(range[0], Math.min(CAPTURE_SIZE, range[1]));
            capture = new Visualizer(0);
            if (capture.setCaptureSize(size) != Visualizer.SUCCESS) {
                release(capture);
                return;
            }
            int rate = Math.min(20_000, Visualizer.getMaxCaptureRate());
            int listening = capture.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override public void onWaveFormDataCapture(Visualizer effect, byte[] waveform, int samplingRate) { }

                @Override
                public void onFftDataCapture(Visualizer effect, byte[] fft, int samplingRate) {
                    updateLevels(fft);
                    postInvalidateOnAnimation();
                }
            }, rate, false, true);
            if (listening != Visualizer.SUCCESS || capture.setEnabled(true) != Visualizer.SUCCESS) {
                release(capture);
                return;
            }
            visualizer = capture;
        } catch (RuntimeException unavailable) {
            release(capture);
        }
    }

    void stop() {
        Visualizer active = visualizer;
        visualizer = null;
        release(active);
        for (int i = 0; i < levels.length; i++) levels[i] = 0.035f;
        invalidate();
    }

    private static void release(Visualizer capture) {
        if (capture == null) return;
        try { capture.setEnabled(false); } catch (RuntimeException ignored) { }
        try { capture.setDataCaptureListener(null, 0, false, false); } catch (RuntimeException ignored) { }
        try { capture.release(); } catch (RuntimeException ignored) { }
    }

    private synchronized void updateLevels(byte[] fft) {
        int bins = fft.length / 2;
        if (bins < 3) return;
        for (int i = 0; i < BAR_COUNT; i++) {
            int from = Math.max(1, (int) Math.pow(bins - 1, i / (double) BAR_COUNT));
            int to = Math.max(from + 1, (int) Math.pow(bins - 1, (i + 1) / (double) BAR_COUNT));
            to = Math.min(bins, to);
            double peak = 0;
            for (int bin = from; bin < to; bin++) {
                double real = fft[bin * 2];
                double imaginary = fft[bin * 2 + 1];
                peak = Math.max(peak, Math.hypot(real, imaginary));
            }
            float target = Math.max(0.035f, Math.min(1f, (float) Math.sqrt(peak) / 10f));
            levels[i] = target >= levels[i] ? target : Math.max(target, levels[i] * 0.72f);
        }
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float slot = getWidth() / (float) BAR_COUNT;
        float barWidth = Math.max(1f, slot * 0.58f);
        float height = getHeight();
        float radius = barWidth / 2f;
        for (int i = 0; i < BAR_COUNT; i++) {
            float barHeight = Math.max(2f, height * levels[i]);
            float left = i * slot + (slot - barWidth) / 2f;
            bar.set(left, height - barHeight, left + barWidth, height);
            canvas.drawRoundRect(bar, radius, radius, paint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }
}

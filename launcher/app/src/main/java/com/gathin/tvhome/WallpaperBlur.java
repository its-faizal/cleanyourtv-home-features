package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/** Small, cached wallpaper blur for glass on devices below API 31. */
final class WallpaperBlur {
    private static final int DOWNSAMPLE = 4;
    private static final int RADIUS = 3;
    private static final int PASSES = 3;

    private WallpaperBlur() { }

    /** Returns a software bitmap cropped to the screen aspect ratio and blurred off the UI thread. */
    static Bitmap create(Bitmap source, int targetWidth, int targetHeight) {
        if (source == null || source.isRecycled() || targetWidth <= 0 || targetHeight <= 0) return null;
        int width = Math.max(1, (targetWidth + DOWNSAMPLE - 1) / DOWNSAMPLE);
        int height = Math.max(1, (targetHeight + DOWNSAMPLE - 1) / DOWNSAMPLE);
        Bitmap result = null;
        try {
            result = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(result);
            canvas.drawColor(0xFF000000);
            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
            drawCover(canvas, source, width, height, 0f, 0f, paint);
            blur(result);
            return result;
        } catch (RuntimeException | OutOfMemoryError failure) {
            if (result != null && !result.isRecycled()) result.recycle();
            return null;
        }
    }

    /** Draws the same CENTER_CROP image used by WallpaperPlayer at the supplied viewport origin. */
    static void drawCover(Canvas canvas, Bitmap source, int width, int height,
            float left, float top, Paint paint) {
        if (source == null || width <= 0 || height <= 0) return;
        float scale = Math.max(width / (float) source.getWidth(), height / (float) source.getHeight());
        float scaledWidth = source.getWidth() * scale;
        float scaledHeight = source.getHeight() * scale;
        float x = left + (width - scaledWidth) / 2f;
        float y = top + (height - scaledHeight) / 2f;
        canvas.drawBitmap(source, null, new RectF(x, y, x + scaledWidth, y + scaledHeight), paint);
    }

    private static void blur(Bitmap bitmap) {
        int width = bitmap.getWidth(), height = bitmap.getHeight();
        int count = width * height;
        int[] pixels = new int[count];
        int[] scratch = new int[count];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int pass = 0; pass < PASSES; pass++) {
            horizontal(pixels, scratch, width, height);
            vertical(scratch, pixels, width, height);
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    }

    private static void horizontal(int[] source, int[] target, int width, int height) {
        int span = RADIUS * 2 + 1;
        for (int y = 0; y < height; y++) {
            int row = y * width;
            int red = 0, green = 0, blue = 0;
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                int color = source[row + Math.max(0, Math.min(width - 1, dx))];
                red += (color >> 16) & 0xFF;
                green += (color >> 8) & 0xFF;
                blue += color & 0xFF;
            }
            for (int x = 0; x < width; x++) {
                target[row + x] = 0xFF000000 | ((red / span) << 16) | ((green / span) << 8) | (blue / span);
                int remove = source[row + Math.max(0, x - RADIUS)];
                int add = source[row + Math.min(width - 1, x + RADIUS + 1)];
                red += ((add >> 16) & 0xFF) - ((remove >> 16) & 0xFF);
                green += ((add >> 8) & 0xFF) - ((remove >> 8) & 0xFF);
                blue += (add & 0xFF) - (remove & 0xFF);
            }
        }
    }

    private static void vertical(int[] source, int[] target, int width, int height) {
        int span = RADIUS * 2 + 1;
        for (int x = 0; x < width; x++) {
            int red = 0, green = 0, blue = 0;
            for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                int color = source[Math.max(0, Math.min(height - 1, dy)) * width + x];
                red += (color >> 16) & 0xFF;
                green += (color >> 8) & 0xFF;
                blue += color & 0xFF;
            }
            for (int y = 0; y < height; y++) {
                target[y * width + x] = 0xFF000000 | ((red / span) << 16) | ((green / span) << 8) | (blue / span);
                int remove = source[Math.max(0, y - RADIUS) * width + x];
                int add = source[Math.min(height - 1, y + RADIUS + 1) * width + x];
                red += ((add >> 16) & 0xFF) - ((remove >> 16) & 0xFF);
                green += ((add >> 8) & 0xFF) - ((remove >> 8) & 0xFF);
                blue += (add & 0xFF) - (remove & 0xFF);
            }
        }
    }
}

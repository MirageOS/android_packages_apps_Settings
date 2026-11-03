/*
 * Copyright (C) 2026 The MirageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.deviceinfo.aboutphone;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.android.settings.R;

import java.util.Arrays;

/**
 * Puts the current wallpaper behind the "MirageOS / Android" text of the About phone banner and
 * picks a scrim strong enough to keep that text readable.
 *
 * <p>The wallpaper is drawn into a small bitmap with the same center-crop the banner uses, and the
 * area under the text (bottom left) is measured. The scrim alpha is then the smallest one that
 * brings the bright end of that area down to the luminance where white text still has a 4.5:1
 * contrast ratio.
 */
final class AboutBannerWallpaper {

    private static final String TAG = "AboutBannerWallpaper";

    /** Same values as the layout: 16dp page padding on each side, 200dp tall banner. */
    private static final int PAGE_PADDING_DP = 16;
    private static final int BANNER_HEIGHT_DP = 200;

    private static final int SAMPLE_WIDTH = 96;
    /** Share of the banner, measured from the left and from the bottom, that holds the text. */
    private static final float TEXT_AREA_WIDTH = 0.75f;
    private static final float TEXT_AREA_HEIGHT = 0.45f;
    private static final float PERCENTILE = 0.85f;

    /** Relative luminance a background can have for white text to reach 4.5:1. */
    private static final float MAX_BACKGROUND_LUMINANCE = 1.05f / 4.5f - 0.05f;
    private static final float MIN_SCRIM_ALPHA = 0.35f;
    private static final float MAX_SCRIM_ALPHA = 0.85f;

    private AboutBannerWallpaper() {}

    /** Loads the wallpaper off the UI thread. If it can't be read, the gradient stays. */
    static void bind(Context context, View root) {
        final ImageView image = root.findViewById(R.id.about_banner_image);
        final View scrim = root.findViewById(R.id.about_banner_scrim);
        if (image == null || scrim == null) {
            return;
        }
        final Context appContext = context.getApplicationContext();
        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        final int bannerWidth = metrics.widthPixels - Math.round(2 * PAGE_PADDING_DP
                * metrics.density);
        final int bannerHeight = Math.round(BANNER_HEIGHT_DP * metrics.density);

        new Thread(() -> {
            Drawable wallpaper = null;
            try {
                final WallpaperManager manager = WallpaperManager.getInstance(appContext);
                if (manager.isWallpaperSupported()) {
                    wallpaper = manager.getDrawable();
                }
            } catch (RuntimeException | OutOfMemoryError e) {
                Log.w(TAG, "Unable to load wallpaper", e);
            }
            if (wallpaper == null) {
                return;
            }
            final Drawable result = wallpaper;
            final float alpha = scrimAlpha(result, bannerWidth, bannerHeight);
            root.post(() -> {
                image.setImageDrawable(result);
                scrim.setBackground(buildScrim(alpha));
            });
        }, TAG).start();
    }

    /** Smallest scrim alpha that keeps white text readable on top of the drawable. */
    static float scrimAlpha(Drawable drawable, int bannerWidth, int bannerHeight) {
        final int dw = drawable.getIntrinsicWidth();
        final int dh = drawable.getIntrinsicHeight();
        if (dw <= 0 || dh <= 0 || bannerWidth <= 0 || bannerHeight <= 0) {
            return MIN_SCRIM_ALPHA;
        }
        final int sw = SAMPLE_WIDTH;
        final int sh = Math.max(1, Math.round(sw * (bannerHeight / (float) bannerWidth)));
        final Bitmap sample = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888);
        try {
            // Center crop, like the ImageView does.
            final float scale = Math.max(sw / (float) dw, sh / (float) dh);
            final Canvas canvas = new Canvas(sample);
            canvas.translate((sw - dw * scale) / 2f, (sh - dh * scale) / 2f);
            canvas.scale(scale, scale);
            final Drawable copy = drawable.getConstantState() != null
                    ? drawable.getConstantState().newDrawable().mutate() : drawable;
            copy.setBounds(0, 0, dw, dh);
            copy.draw(canvas);

            final int x1 = Math.round(sw * TEXT_AREA_WIDTH);
            final int y0 = Math.round(sh * (1f - TEXT_AREA_HEIGHT));
            final float[] values = new float[x1 * (sh - y0)];
            int n = 0;
            for (int y = y0; y < sh; y++) {
                for (int x = 0; x < x1; x++) {
                    values[n++] = luminance(sample.getPixel(x, y));
                }
            }
            Arrays.sort(values, 0, n);
            final float bright = values[Math.min(n - 1, (int) (n * PERCENTILE))];
            if (bright <= MAX_BACKGROUND_LUMINANCE) {
                return MIN_SCRIM_ALPHA;
            }
            final float needed = 1f - MAX_BACKGROUND_LUMINANCE / bright;
            return Math.max(MIN_SCRIM_ALPHA, Math.min(MAX_SCRIM_ALPHA, needed));
        } catch (RuntimeException e) {
            Log.w(TAG, "Unable to measure wallpaper", e);
            return MAX_SCRIM_ALPHA;
        } finally {
            sample.recycle();
        }
    }

    /** Dark at the bottom, where the text is, fading out toward the top of the banner. */
    private static GradientDrawable buildScrim(float alpha) {
        final float bottom = Math.min(MAX_SCRIM_ALPHA, alpha * 1.1f);
        return new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[] {
                black(bottom), black(bottom * 0.8f), black(bottom * 0.15f)});
    }

    private static int black(float alpha) {
        return Color.argb(Math.round(255 * alpha), 0, 0, 0);
    }

    /** Relative luminance (WCAG) of an sRGB pixel. */
    private static float luminance(int color) {
        return 0.2126f * linear(Color.red(color))
                + 0.7152f * linear(Color.green(color))
                + 0.0722f * linear(Color.blue(color));
    }

    private static float linear(int channel) {
        final float c = channel / 255f;
        return c <= 0.03928f ? c / 12.92f : (float) Math.pow((c + 0.055f) / 1.055f, 2.4);
    }
}

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

package com.android.settings.display;

import static android.view.CrossWindowBlurListeners.CROSS_WINDOW_BLUR_SUPPORTED;

import android.app.WallpaperManager;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.lifecycle.LifecycleObserver;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.LayoutPreference;

/**
 * Drives the live banner above the blur options: a mock Quick Settings panel sitting on top of the
 * current wallpaper, with the same wallpaper blurred behind it. The banner redraws as soon as the
 * slider moves or the "Reduce blur effects" switch flips, because it listens to both settings.
 *
 * <p>The panel is a second copy of the scene, as big as the whole banner and shifted back by the
 * panel's own offset, so the blurred copy lines up exactly with the sharp one underneath.
 */
public class BlurPreviewPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private static final String TAG = "BlurPreviewController";

    /** Radius of the stock QS blur in dp, which is what 100% means in the preview. */
    private static final float STOCK_RADIUS_DP = 23f;
    private static final float TINT_ALPHA_BLURRED = 0.5f;
    private static final float TINT_ALPHA_NO_BLUR = 0.94f;
    private static final long TINT_FADE_MS = 180;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ContentObserver mObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            update();
        }
    };

    private View mRoot;
    private View mPanel;
    private View mTint;
    private TextView mChip;
    private View mBlurredScene;
    private boolean mObserving;
    private boolean mWallpaperRequested;

    public BlurPreviewPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return CROSS_WINDOW_BLUR_SUPPORTED ? AVAILABLE_UNSEARCHABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final LayoutPreference preference = screen.findPreference(getPreferenceKey());
        if (preference == null) {
            return;
        }
        mRoot = preference.findViewById(R.id.blur_banner_root);
        if (mRoot == null) {
            return;
        }
        mPanel = mRoot.findViewById(R.id.blur_panel);
        mTint = mRoot.findViewById(R.id.blur_panel_tint);
        mChip = mRoot.findViewById(R.id.blur_banner_chip);
        mBlurredScene = mRoot.findViewById(R.id.blur_scene_blurred);
        mRoot.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> alignBlurredScene());
    }

    @Override
    public void onStart() {
        if (mRoot == null || mObserving) {
            return;
        }
        final Context context = mContext;
        context.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(BlurLevelSettings.KEY_QS_BLUR_LEVEL), false, mObserver);
        context.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.DISABLE_WINDOW_BLURS), false,
                mObserver);
        mObserving = true;
        loadWallpaper();
        update();
    }

    @Override
    public void onStop() {
        if (mObserving) {
            mContext.getContentResolver().unregisterContentObserver(mObserver);
            mObserving = false;
        }
    }

    /** Applies the current level and switch state to the banner. */
    private void update() {
        if (mRoot == null || mBlurredScene == null) {
            return;
        }
        final boolean blurOff = isBlurDisabled();
        final int level = BlurLevelSettings.getLevel(mContext.getContentResolver());
        final float radius = blurOff ? 0f
                : STOCK_RADIUS_DP * mContext.getResources().getDisplayMetrics().density
                        * level / 100f;

        // A radius of zero is not a valid blur, so drop the effect instead.
        mBlurredScene.setRenderEffect(radius < 0.5f ? null
                : RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP));

        // Without blur the shade falls back to a much more opaque background.
        final float tint = (blurOff || level == 0) ? TINT_ALPHA_NO_BLUR : TINT_ALPHA_BLURRED;
        mTint.animate().alpha(tint).setDuration(TINT_FADE_MS).start();

        mChip.setText(blurOff
                ? mContext.getString(R.string.blur_preview_off)
                : mContext.getString(R.string.blur_preview_level, level));
    }

    private boolean isBlurDisabled() {
        final PowerManager pm = mContext.getSystemService(PowerManager.class);
        if (pm != null && pm.isPowerSaveMode()) {
            return true;
        }
        final int fallback = android.os.SystemProperties.getBoolean(
                "ro.surface_flinger.blur_disabled_by_default", false) ? 1 : 0;
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.DISABLE_WINDOW_BLURS, fallback) == 1;
    }

    /** Makes the blurred copy as big as the banner and puts it back where the sharp one is. */
    private void alignBlurredScene() {
        if (mRoot == null || mPanel == null || mBlurredScene == null) {
            return;
        }
        final ViewGroup.LayoutParams params = mBlurredScene.getLayoutParams();
        final int width = mRoot.getWidth();
        final int height = mRoot.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        if (params.width != width || params.height != height) {
            params.width = width;
            params.height = height;
            mBlurredScene.setLayoutParams(params);
        }
        mBlurredScene.setTranslationX(-mPanel.getLeft());
        mBlurredScene.setTranslationY(-mPanel.getTop());
    }

    /** Puts the current wallpaper in both copies of the scene. Falls back to the drawn shapes. */
    private void loadWallpaper() {
        if (mWallpaperRequested) {
            return;
        }
        mWallpaperRequested = true;
        final Context appContext = mContext.getApplicationContext();
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
            mHandler.post(() -> showWallpaper(result));
        }, TAG).start();
    }

    private void showWallpaper(Drawable wallpaper) {
        if (mRoot == null) {
            return;
        }
        final ImageView sharp = mRoot.findViewById(R.id.blur_scene_sharp)
                .findViewById(R.id.blur_scene_wallpaper);
        final ImageView blurred = mBlurredScene.findViewById(R.id.blur_scene_wallpaper);
        final Drawable.ConstantState state = wallpaper.getConstantState();
        sharp.setImageDrawable(wallpaper);
        blurred.setImageDrawable(state != null ? state.newDrawable().mutate() : wallpaper);
        sharp.setVisibility(View.VISIBLE);
        blurred.setVisibility(View.VISIBLE);
    }
}

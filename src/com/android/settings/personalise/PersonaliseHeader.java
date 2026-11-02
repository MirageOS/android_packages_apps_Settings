/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.personalise;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.android.settings.DisplaySettings;
import com.android.settings.R;
import com.android.settings.accessibility.TextReadingPreferenceFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.display.TopLevelWallpaperPreferenceController;
import com.android.settings.display.darkmode.DarkModeSettingsFragment;
import com.android.settings.security.LockscreenDashboardFragment;

import java.util.Locale;

/**
 * Binds the Motorola-style header of the Personalise screen: the wallpaper / lock screen preview
 * and the shortcuts to wallpaper and style, dark mode, display size and text, lock screen and
 * display.
 */
final class PersonaliseHeader {

    private static final String TAG = "PersonaliseHeader";

    private PersonaliseHeader() {}

    /** Fills the header views and wires the shortcuts. Safe to call again from onResume. */
    static void bind(Fragment fragment, View root, int metricsCategory) {
        final Context context = root.getContext();

        // Date on the preview: use the locale's best pattern instead of a fixed one.
        final TextClock date = root.findViewById(R.id.personalise_date_clock);
        if (date != null) {
            final String pattern =
                    DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEdMMM");
            date.setFormat12Hour(pattern);
            date.setFormat24Hour(pattern);
        }

        // Dark mode summary
        final boolean dark = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        text(root, R.id.personalise_dark_summary,
                context.getString(dark ? R.string.switch_on_text : R.string.switch_off_text));

        // Wallpaper picker (preview + wide card). Hidden when the device has no picker.
        final TopLevelWallpaperPreferenceController wallpaper =
                new TopLevelWallpaperPreferenceController(context, "top_level_wallpaper");
        final View wallpaperCard = root.findViewById(R.id.personalise_tile_wallpaper);
        final View preview = root.findViewById(R.id.personalise_preview);
        if (wallpaper.isAvailable()) {
            final View.OnClickListener openPicker = v -> {
                try {
                    context.startActivity(wallpaper.buildLaunchIntent());
                } catch (RuntimeException e) {
                    Log.w(TAG, "Unable to open wallpaper picker", e);
                }
            };
            wallpaperCard.setOnClickListener(openPicker);
            preview.setOnClickListener(openPicker);
            text(root, R.id.personalise_wallpaper_title, wallpaper.getTitle());
        } else {
            wallpaperCard.setVisibility(View.GONE);
            preview.setClickable(false);
        }

        // Shortcuts to other Settings screens
        link(fragment, root, R.id.personalise_tile_dark,
                DarkModeSettingsFragment.class.getName(), R.string.dark_ui_title, metricsCategory);
        link(fragment, root, R.id.personalise_tile_text,
                TextReadingPreferenceFragment.class.getName(),
                R.string.accessibility_text_reading_options_title, metricsCategory);
        link(fragment, root, R.id.personalise_tile_lockscreen,
                LockscreenDashboardFragment.class.getName(), R.string.lockscreen_settings_title,
                metricsCategory);
        link(fragment, root, R.id.personalise_tile_display,
                DisplaySettings.class.getName(), R.string.display_settings, metricsCategory);

        loadWallpaperPreview(context, root);
    }

    private static void link(Fragment fragment, View root, int tileId, String destination,
            int titleRes, int metricsCategory) {
        final View tile = root.findViewById(tileId);
        if (tile == null) {
            return;
        }
        tile.setOnClickListener(v -> new SubSettingLauncher(fragment.requireContext())
                .setDestination(destination)
                .setTitleRes(titleRes)
                .setSourceMetricsCategory(metricsCategory)
                .launch());
    }

    /**
     * Loads the current wallpaper into the preview off the UI thread. If the wallpaper cannot be
     * read (missing permission, live wallpaper, ...) the gradient background is kept.
     */
    private static void loadWallpaperPreview(Context context, View root) {
        final ImageView image = root.findViewById(R.id.personalise_preview_image);
        if (image == null) {
            return;
        }
        final Context appContext = context.getApplicationContext();
        new Thread(() -> {
            Drawable wallpaper = null;
            try {
                final WallpaperManager manager = WallpaperManager.getInstance(appContext);
                if (manager.isWallpaperSupported()) {
                    wallpaper = manager.getDrawable();
                }
            } catch (RuntimeException | OutOfMemoryError e) {
                Log.w(TAG, "Unable to load wallpaper preview", e);
            }
            final Drawable result = wallpaper;
            if (result != null) {
                image.post(() -> image.setImageDrawable(result));
            }
        }, "PersonalisePreview").start();
    }

    private static void text(View root, int id, CharSequence value) {
        final TextView view = root.findViewById(id);
        if (view != null) {
            view.setText(value);
        }
    }
}

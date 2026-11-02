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

package com.android.settings.personalise.dynamicisland;

import android.content.ContentResolver;
import android.provider.Settings;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Keys and helpers for the Dynamic Island options, stored in {@link Settings.System}.
 *
 * <p>These keys are the contract with SystemUI (frameworks/base): the island reads the very same
 * names, so do not rename them without changing the other side. Every boolean defaults to
 * enabled.
 */
public final class DynamicIslandSettings {

    /** Master switch of the island. */
    public static final String KEY_ENABLED = "dynamic_island_enabled";
    /** Tapping the island expands it into the media player, recording, alarm card, ... */
    public static final String KEY_EXPAND_ON_TAP = "dynamic_island_expand_on_tap";

    /** System live updates. */
    public static final String KEY_RECORDER = "dynamic_island_live_recorder";
    public static final String KEY_SCREEN_RECORDER = "dynamic_island_live_screen_recorder";
    public static final String KEY_FLASHLIGHT = "dynamic_island_live_flashlight";
    public static final String KEY_CALLS = "dynamic_island_live_calls";
    public static final String KEY_MEDIA = "dynamic_island_live_media";
    public static final String KEY_ALARMS = "dynamic_island_live_alarms";

    /** Comma separated list of packages whose live updates must not use the island. */
    public static final String KEY_BLOCKED_PACKAGES = "dynamic_island_blocked_packages";

    /**
     * Position and size. With {@link #KEY_AUTO_POSITION} on (the default) the island fits itself
     * to the camera cutout and the other four are ignored. The position is in dp, the size in
     * percent. Ranges and defaults must match DynamicIslandLayoutSettings.kt in SystemUI.
     */
    public static final String KEY_AUTO_POSITION = "dynamic_island_auto_position";
    /** Horizontal shift from the center of the screen, in dp. */
    public static final String KEY_POSITION_X = "dynamic_island_position_x";
    /** Distance from the top edge of the screen, in dp. */
    public static final String KEY_POSITION_Y = "dynamic_island_position_y";
    /** Size of the island, in percent of the normal size. */
    public static final String KEY_SIZE = "dynamic_island_size";
    /** Space left in the middle of the island for the front camera, in dp. */
    public static final String KEY_CAMERA_GAP = "dynamic_island_camera_gap";

    /** The sliders of the manual adjustment. */
    public static final String[] POSITION_KEYS = {
            KEY_POSITION_X, KEY_POSITION_Y, KEY_SIZE, KEY_CAMERA_GAP
    };

    private static final String SEPARATOR = ",";

    private DynamicIslandSettings() {}

    public static boolean getBoolean(ContentResolver resolver, String key) {
        return Settings.System.getInt(resolver, key, 1 /* default */) != 0;
    }

    public static boolean putBoolean(ContentResolver resolver, String key, boolean value) {
        return Settings.System.putInt(resolver, key, value ? 1 : 0);
    }

    /** The smallest value of the position / size option {@code key}. */
    public static int getMin(String key) {
        switch (key) {
            case KEY_POSITION_X:
                return -100;
            case KEY_POSITION_Y:
                return 0;
            case KEY_SIZE:
                return 70;
            case KEY_CAMERA_GAP:
                return 32;
            default:
                throw new IllegalArgumentException("Not a position option: " + key);
        }
    }

    /** The biggest value of the position / size option {@code key}. */
    public static int getMax(String key) {
        switch (key) {
            case KEY_POSITION_X:
                return 100;
            case KEY_POSITION_Y:
                return 24;
            case KEY_SIZE:
                return 130;
            case KEY_CAMERA_GAP:
                return 120;
            default:
                throw new IllegalArgumentException("Not a position option: " + key);
        }
    }

    /** The value of the position / size option {@code key} when the user never changed it. */
    public static int getDefault(String key) {
        switch (key) {
            case KEY_POSITION_X:
                return 0;
            case KEY_POSITION_Y:
                return 4;
            case KEY_SIZE:
                return 100;
            case KEY_CAMERA_GAP:
                return 56;
            default:
                throw new IllegalArgumentException("Not a position option: " + key);
        }
    }

    /** How much one step of the slider of the option {@code key} changes its value. */
    public static int getStep(String key) {
        switch (key) {
            case KEY_SIZE:
                return 5;
            case KEY_CAMERA_GAP:
                return 2;
            default:
                return 1;
        }
    }

    /** Reads a position / size option, kept inside its range. */
    public static int getInt(ContentResolver resolver, String key) {
        final int value = Settings.System.getInt(resolver, key, getDefault(key));
        return Math.max(getMin(key), Math.min(getMax(key), value));
    }

    public static boolean putInt(ContentResolver resolver, String key, int value) {
        return Settings.System.putInt(resolver, key, value);
    }

    /** Puts the position and the size back to the defaults. Automatic adjustment is not touched. */
    public static void resetPosition(ContentResolver resolver) {
        for (String key : POSITION_KEYS) {
            putInt(resolver, key, getDefault(key));
        }
    }

    public static Set<String> getBlockedPackages(ContentResolver resolver) {
        final String value = Settings.System.getString(resolver, KEY_BLOCKED_PACKAGES);
        final Set<String> result = new HashSet<>();
        if (!TextUtils.isEmpty(value)) {
            result.addAll(Arrays.asList(value.split(SEPARATOR)));
        }
        return result;
    }

    public static boolean setPackageBlocked(ContentResolver resolver, String packageName,
            boolean blocked) {
        final Set<String> packages = getBlockedPackages(resolver);
        final boolean changed = blocked ? packages.add(packageName) : packages.remove(packageName);
        if (!changed) {
            return true;
        }
        return Settings.System.putString(resolver, KEY_BLOCKED_PACKAGES,
                TextUtils.join(SEPARATOR, packages));
    }
}

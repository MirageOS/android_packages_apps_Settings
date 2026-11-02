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

    private static final String SEPARATOR = ",";

    private DynamicIslandSettings() {}

    public static boolean getBoolean(ContentResolver resolver, String key) {
        return Settings.System.getInt(resolver, key, 1 /* default */) != 0;
    }

    public static boolean putBoolean(ContentResolver resolver, String key, boolean value) {
        return Settings.System.putInt(resolver, key, value ? 1 : 0);
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

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

package com.android.settings.personalise.statusbar;

import android.content.ContentResolver;
import android.provider.Settings;

/**
 * Keys of the status bar options, stored in {@link Settings.System}. Contract with SystemUI: it
 * reads the very same names, so do not rename them without changing the other side.
 */
public final class StatusBarSettings {
    /** Show the Wi-Fi generation (4, 5, 6, 7) next to the Wi-Fi icon. Defaults to 1. */
    public static final String KEY_WIFI_STANDARD = "status_bar_wifi_standard";

    private StatusBarSettings() {}

    public static boolean getBoolean(ContentResolver resolver, String key) {
        return Settings.System.getInt(resolver, key, 1 /* default */) != 0;
    }

    public static boolean putBoolean(ContentResolver resolver, String key, boolean value) {
        return Settings.System.putInt(resolver, key, value ? 1 : 0);
    }
}

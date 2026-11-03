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

package com.android.settings.personalise.qs;

import android.content.ContentResolver;
import android.provider.Settings;

/** Keys of the QS style options, stored in Settings.System. Contract with SystemUI. */
public final class QsStyleSettings {
    /** 0 = default layout, 1 = Motorola style. Defaults to 0. */
    public static final String KEY_STYLE_MOTOROLA = "qs_style_motorola";
    /** Show tile labels. Defaults to 1. */
    public static final String KEY_SHOW_LABELS = "qs_show_labels";

    private QsStyleSettings() {}

    public static int getDefault(String key) {
        return KEY_SHOW_LABELS.equals(key) ? 1 : 0;
    }

    public static boolean getBoolean(ContentResolver r, String key) {
        return Settings.System.getInt(r, key, getDefault(key)) != 0;
    }

    public static boolean putBoolean(ContentResolver r, String key, boolean value) {
        return Settings.System.putInt(r, key, value ? 1 : 0);
    }
}

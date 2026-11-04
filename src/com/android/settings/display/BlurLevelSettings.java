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

import android.content.ContentResolver;
import android.provider.Settings;

/**
 * Key and limits of the Quick Settings blur level, stored in {@link Settings.System}.
 *
 * <p>This is the contract with SystemUI (frameworks/base): the shade multiplies its blur radius by
 * {@code level / 100}, so 100 is the stock look, 0 is no blur and 200 is twice as strong. Keep the
 * name, range and default in sync with the SystemUI side. The "Reduce blur effects" switch
 * ({@link Settings.Global#DISABLE_WINDOW_BLURS}) still wins over this level.
 */
public final class BlurLevelSettings {

    public static final String KEY_QS_BLUR_LEVEL = "qs_blur_level";

    public static final int MIN = 0;
    public static final int MAX = 200;
    public static final int DEFAULT = 100;
    public static final int STEP = 10;

    private BlurLevelSettings() {}

    public static int getLevel(ContentResolver resolver) {
        return clamp(Settings.System.getInt(resolver, KEY_QS_BLUR_LEVEL, DEFAULT));
    }

    public static boolean putLevel(ContentResolver resolver, int level) {
        return Settings.System.putInt(resolver, KEY_QS_BLUR_LEVEL, clamp(level));
    }

    private static int clamp(int level) {
        return Math.max(MIN, Math.min(MAX, level));
    }
}

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

import android.content.Context;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * "Reduce blur effects", moved here from Accessibility > Color and motion. The switch is on when
 * window blurs are disabled, so it reads and writes {@link Settings.Global#DISABLE_WINDOW_BLURS}
 * directly, the same key {@link BlurSwitchPreference} uses.
 */
// LINT.IfChange
public class BlurPreferenceController extends TogglePreferenceController {

    private static final String PROP_BLUR_DISABLED_BY_DEFAULT =
            "ro.surface_flinger.blur_disabled_by_default";

    public BlurPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!CROSS_WINDOW_BLUR_SUPPORTED) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return isPowerSaveMode() ? DISABLED_DEPENDENT_SETTING : AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        final int fallback =
                SystemProperties.getBoolean(PROP_BLUR_DISABLED_BY_DEFAULT, false) ? 1 : 0;
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.DISABLE_WINDOW_BLURS, fallback) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DISABLE_WINDOW_BLURS, isChecked ? 1 : 0);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setSummary(isPowerSaveMode()
                ? R.string.blur_switch_disabled_summary
                : R.string.blur_switch_summary);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

    private boolean isPowerSaveMode() {
        final PowerManager pm = mContext.getSystemService(PowerManager.class);
        return pm != null && pm.isPowerSaveMode();
    }
}
// LINT.ThenChange(BlurSwitchPreference.kt)

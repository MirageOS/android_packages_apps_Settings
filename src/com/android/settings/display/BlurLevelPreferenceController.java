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

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.SliderPreferenceController;
import com.android.settingslib.widget.SliderPreference;

/**
 * Slider that sets how strong the Quick Settings blur is, from 0% to 200% of the stock radius. It
 * follows the "Reduce blur effects" switch: while blurs are off (or Battery Saver is on) the slider
 * is dimmed, because the system ignores the level anyway.
 */
public class BlurLevelPreferenceController extends SliderPreferenceController {

    private SliderPreference mPreference;

    public BlurLevelPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return CROSS_WINDOW_BLUR_SUPPORTED ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference == null) {
            return;
        }
        mPreference.setUpdatesContinuously(true);
        mPreference.setMin(getMin());
        mPreference.setMax(getMax());
        mPreference.setSliderIncrement(BlurLevelSettings.STEP);
        mPreference.setHapticFeedbackMode(SliderPreference.HAPTIC_FEEDBACK_MODE_ON_ENDS);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setSummary(mContext.getString(R.string.blur_level_value, getSliderPosition()));
        if (isPowerSaveMode()) {
            // The switch can't tell the user why the slider is dimmed, so say it here.
            preference.setEnabled(false);
            preference.setSummary(R.string.blur_switch_disabled_summary);
        }
    }

    @Override
    public int getSliderPosition() {
        return BlurLevelSettings.getLevel(mContext.getContentResolver());
    }

    @Override
    public boolean setSliderPosition(int position) {
        final boolean saved = BlurLevelSettings.putLevel(mContext.getContentResolver(), position);
        if (saved && mPreference != null) {
            mPreference.setSummary(mContext.getString(R.string.blur_level_value, position));
        }
        return saved;
    }

    @Override
    public int getMax() {
        return BlurLevelSettings.MAX;
    }

    @Override
    public int getMin() {
        return BlurLevelSettings.MIN;
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

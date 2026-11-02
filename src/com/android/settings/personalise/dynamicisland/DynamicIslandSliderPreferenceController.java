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

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.SliderPreferenceController;
import com.android.settingslib.widget.SliderPreference;

/**
 * Slider of the manual position and size of the Dynamic Island. The preference key is the {@link
 * android.provider.Settings.System} key it controls (see {@link DynamicIslandSettings}); the
 * island in SystemUI follows it while the slider is dragged.
 */
public class DynamicIslandSliderPreferenceController extends SliderPreferenceController {

    private SliderPreference mPreference;

    public DynamicIslandSliderPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
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
        mPreference.setSliderIncrement(DynamicIslandSettings.getStep(getPreferenceKey()));
        mPreference.setHapticFeedbackMode(SliderPreference.HAPTIC_FEEDBACK_MODE_ON_ENDS);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setSummary(formatValue(mContext, getPreferenceKey(), getSliderPosition()));
    }

    @Override
    public int getSliderPosition() {
        return DynamicIslandSettings.getInt(mContext.getContentResolver(), getPreferenceKey());
    }

    @Override
    public boolean setSliderPosition(int position) {
        final boolean saved = DynamicIslandSettings.putInt(
                mContext.getContentResolver(), getPreferenceKey(), position);
        if (saved && mPreference != null) {
            mPreference.setSummary(formatValue(mContext, getPreferenceKey(), position));
        }
        return saved;
    }

    @Override
    public int getMax() {
        return DynamicIslandSettings.getMax(getPreferenceKey());
    }

    @Override
    public int getMin() {
        return DynamicIslandSettings.getMin(getPreferenceKey());
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_personalise;
    }

    /** The value as shown under the title of the slider: "12 dp" or "110%". */
    static CharSequence formatValue(Context context, String key, int value) {
        return context.getString(DynamicIslandSettings.KEY_SIZE.equals(key)
                ? R.string.dynamic_island_value_percent
                : R.string.dynamic_island_value_dp, value);
    }
}

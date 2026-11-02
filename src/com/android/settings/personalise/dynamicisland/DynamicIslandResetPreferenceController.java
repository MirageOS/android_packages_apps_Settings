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
import android.content.Context;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.SliderPreference;

/**
 * "Restore defaults" row of the Dynamic Island position screen: puts the manual position and size
 * back to the defaults and moves the sliders along.
 */
public class DynamicIslandResetPreferenceController extends BasePreferenceController {

    private PreferenceScreen mScreen;

    public DynamicIslandResetPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mScreen = screen;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        final ContentResolver resolver = mContext.getContentResolver();
        DynamicIslandSettings.resetPosition(resolver);
        if (mScreen != null) {
            for (String key : DynamicIslandSettings.POSITION_KEYS) {
                final Preference slider = mScreen.findPreference(key);
                if (slider instanceof SliderPreference) {
                    final int value = DynamicIslandSettings.getInt(resolver, key);
                    ((SliderPreference) slider).setValue(value);
                    slider.setSummary(DynamicIslandSliderPreferenceController.formatValue(
                            mContext, key, value));
                }
            }
        }
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_personalise;
    }
}

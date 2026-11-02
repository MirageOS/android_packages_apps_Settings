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

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Generic switch for the Dynamic Island options. The preference key is the {@link
 * android.provider.Settings.System} key it controls (see {@link DynamicIslandSettings}).
 */
public class DynamicIslandTogglePreferenceController extends TogglePreferenceController {

    public DynamicIslandTogglePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return DynamicIslandSettings.getBoolean(mContext.getContentResolver(), getPreferenceKey());
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return DynamicIslandSettings.putBoolean(
                mContext.getContentResolver(), getPreferenceKey(), isChecked);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        // Everything except the master switch only makes sense while the island is on.
        if (!DynamicIslandSettings.KEY_ENABLED.equals(getPreferenceKey())) {
            preference.setEnabled(DynamicIslandSettings.getBoolean(
                    mContext.getContentResolver(), DynamicIslandSettings.KEY_ENABLED));
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_personalise;
    }
}

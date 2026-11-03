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

package com.android.settings.personalise;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.LayoutPreference;

/**
 * "Personalise": a Motorola-style screen that groups wallpaper, style and display shortcuts and
 * hosts the Integrity section (certified properties spoofing, keybox XML and PIF JSON), which used
 * to live in Developer options.
 */
@SearchIndexable
public class PersonaliseFragment extends DashboardFragment {

    private static final String TAG = "PersonaliseFragment";

    private static final String KEY_HEADER = "personalise_header";
    private static final String KEY_BANNER_ISLAND = "personalise_banner_island";
    private static final String KEY_BANNER_QS = "personalise_banner_qs";
    private static final String KEY_BANNER_INTEGRITY = "personalise_banner_integrity";
    private static final String KEY_KEYBOX = "keybox_data_setting";
    private static final String KEY_PIF = "pif_data_setting";

    private ActivityResultLauncher<Intent> mKeyboxFilePickerLauncher;
    private ActivityResultLauncher<Intent> mPifFilePickerLauncher;
    private final PersonaliseBanners mBanners = new PersonaliseBanners();

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DISPLAY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.personalise_settings;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        // Launchers must be registered before the fragment is started.
        mKeyboxFilePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    final Uri uri = getResultUri(result.getResultCode(), result.getData());
                    final Preference pref = findPreference(KEY_KEYBOX);
                    if (uri != null && pref instanceof KeyboxDataPreference) {
                        ((KeyboxDataPreference) pref).handleFileSelected(uri);
                    }
                });
        mPifFilePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    final Uri uri = getResultUri(result.getResultCode(), result.getData());
                    final Preference pref = findPreference(KEY_PIF);
                    if (uri != null && pref instanceof PifDataPreference) {
                        ((PifDataPreference) pref).handleFileSelected(uri);
                    }
                });
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Preference keybox = findPreference(KEY_KEYBOX);
        if (keybox instanceof KeyboxDataPreference) {
            ((KeyboxDataPreference) keybox).setFilePickerLauncher(mKeyboxFilePickerLauncher);
        }
        final Preference pif = findPreference(KEY_PIF);
        if (pif instanceof PifDataPreference) {
            ((PifDataPreference) pif).setFilePickerLauncher(mPifFilePickerLauncher);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Rebind on every resume so the dark mode summary and the wallpaper preview stay fresh
        // after coming back from the pickers.
        final LayoutPreference header = findPreference(KEY_HEADER);
        if (header == null) {
            return;
        }
        final View root = header.findViewById(R.id.personalise_header_root);
        if (root != null) {
            PersonaliseHeader.bind(this, root, getMetricsCategory());
        }
        mBanners.start(findBanner(KEY_BANNER_ISLAND, R.id.banner_island_root),
                findBanner(KEY_BANNER_QS, R.id.banner_qs_root),
                findBanner(KEY_BANNER_INTEGRITY, R.id.banner_integrity_root));
    }

    @Override
    public void onPause() {
        mBanners.stop();
        super.onPause();
    }

    private View findBanner(String key, int rootId) {
        final LayoutPreference banner = findPreference(key);
        return banner == null ? null : banner.findViewById(rootId);
    }

    private static Uri getResultUri(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return null;
        }
        return data.getData();
    }

    /** For Search. */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.personalise_settings);
}

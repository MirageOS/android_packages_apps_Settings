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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.utils.ThreadUtils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Fills the "App live updates" category with one switch per app that can post live (promoted)
 * notifications. Apps are allowed by default; turning one off adds it to {@link
 * DynamicIslandSettings#KEY_BLOCKED_PACKAGES}.
 */
public class DynamicIslandAppsPreferenceController extends BasePreferenceController {

    private static final String TAG = "DynamicIslandApps";
    private static final String PERMISSION_POST_PROMOTED =
            "android.permission.POST_PROMOTED_NOTIFICATIONS";
    private static final String KEY_PREFIX_APP = "dynamic_island_app:";

    private PreferenceCategory mCategory;
    private boolean mLoading;

    public DynamicIslandAppsPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mCategory = screen.findPreference(getPreferenceKey());
        loadApps();
    }

    private void loadApps() {
        if (mCategory == null || mLoading) {
            return;
        }
        mLoading = true;
        ThreadUtils.postOnBackgroundThread(() -> {
            final List<AppEntry> apps = queryApps();
            ThreadUtils.postOnMainThread(() -> {
                mLoading = false;
                populate(apps);
            });
        });
    }

    private List<AppEntry> queryApps() {
        final PackageManager pm = mContext.getPackageManager();
        final List<AppEntry> result = new ArrayList<>();
        try {
            final List<PackageInfo> packages = pm.getInstalledPackages(
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS));
            for (PackageInfo info : packages) {
                if (info.applicationInfo == null || !info.applicationInfo.enabled
                        || !requestsPromoted(info)) {
                    continue;
                }
                final boolean launchable = pm.getLaunchIntentForPackage(info.packageName) != null;
                final boolean system =
                        (info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                if (!launchable && !system) {
                    continue;
                }
                result.add(new AppEntry(info.packageName,
                        info.applicationInfo.loadLabel(pm),
                        info.applicationInfo.loadIcon(pm)));
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Unable to list apps", e);
        }
        final Collator collator = Collator.getInstance();
        Collections.sort(result, (a, b) -> collator.compare(a.label.toString(),
                b.label.toString()));
        return result;
    }

    private static boolean requestsPromoted(PackageInfo info) {
        if (info.requestedPermissions == null) {
            return false;
        }
        for (String permission : info.requestedPermissions) {
            if (PERMISSION_POST_PROMOTED.equals(permission)) {
                return true;
            }
        }
        return false;
    }

    private void populate(List<AppEntry> apps) {
        // The screen may have been closed while the list was loading.
        if (mCategory == null || mCategory.getPreferenceManager() == null) {
            return;
        }
        mCategory.removeAll();
        final Context context = mCategory.getContext();
        if (apps.isEmpty()) {
            final Preference empty = new Preference(context);
            empty.setTitle(R.string.dynamic_island_no_apps);
            empty.setSelectable(false);
            empty.setPersistent(false);
            mCategory.addPreference(empty);
            return;
        }
        final Set<String> blocked =
                DynamicIslandSettings.getBlockedPackages(mContext.getContentResolver());
        for (AppEntry app : apps) {
            final SwitchPreferenceCompat pref = new SwitchPreferenceCompat(context);
            pref.setKey(KEY_PREFIX_APP + app.packageName);
            pref.setTitle(app.label);
            pref.setIcon(app.icon);
            pref.setPersistent(false);
            pref.setChecked(!blocked.contains(app.packageName));
            pref.setOnPreferenceChangeListener((p, newValue) ->
                    DynamicIslandSettings.setPackageBlocked(mContext.getContentResolver(),
                            app.packageName, !((Boolean) newValue)));
            mCategory.addPreference(pref);
        }
    }

    private static final class AppEntry {
        final String packageName;
        final CharSequence label;
        final Drawable icon;

        AppEntry(String packageName, CharSequence label, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }
    }
}

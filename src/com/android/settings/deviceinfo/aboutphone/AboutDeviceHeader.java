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

package com.android.settings.deviceinfo.aboutphone;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.View;
import android.widget.TextView;

import com.android.settings.R;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Binds the Motorola-style header (banner, tiles, maintainer and spec card) shown at the top of
 * "About phone".
 *
 * <p>The maintainer name comes from the {@code ro.build.mirage.maintainer} system property,
 * normally set at build time with {@code MIRAGE_MAINTAINER := Name}.
 */
public final class AboutDeviceHeader {

    private static final String TAG = "AboutDeviceHeader";

    /** System property holding the maintainer name. */
    static final String PROP_MAINTAINER = "ro.build.mirage.maintainer";

    private static final int[] RAM_STEPS_GB = {1, 2, 3, 4, 6, 8, 10, 12, 16, 18, 24, 32};

    private AboutDeviceHeader() {}

    /** Fills the header views. {@code onDetails} opens the full device details screen. */
    public static void bind(Context context, View root, Runnable onDetails) {
        // Banner
        text(root, R.id.about_android_version,
                context.getString(R.string.about_device_android_version,
                        Build.VERSION.RELEASE));

        // Device name tile
        String name = Settings.Global.getString(context.getContentResolver(),
                Settings.Global.DEVICE_NAME);
        text(root, R.id.about_device_name_summary, TextUtils.isEmpty(name) ? Build.MODEL : name);
        root.findViewById(R.id.about_tile_device).setOnClickListener(v -> {
            if (onDetails != null) {
                onDetails.run();
            }
        });

        // Storage tile
        root.findViewById(R.id.about_tile_storage).setOnClickListener(v -> {
            try {
                context.startActivity(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception e) {
                Log.w(TAG, "Unable to open storage settings", e);
            }
        });

        // Maintainer
        String maintainer = SystemProperties.get(PROP_MAINTAINER, "").trim();
        text(root, R.id.about_maintainer_summary, TextUtils.isEmpty(maintainer)
                ? context.getString(R.string.about_device_maintainer_unknown) : maintainer);

        // Cheap specs on the main thread
        text(root, R.id.about_ram_summary,
                context.getString(R.string.about_device_ram_summary, getRamGb(context)));
        text(root, R.id.about_display_summary, getDisplayResolution(context));

        // Storage and camera touch the disk / camera service: do it off the UI thread
        final Context appContext = context.getApplicationContext();
        new Thread(() -> {
            final String storage = getStorageSummary(appContext);
            final String camera = getCameraSummary(appContext);
            root.post(() -> {
                text(root, R.id.about_storage_summary, storage);
                final View cameraView = root.findViewById(R.id.about_camera_summary);
                if (cameraView != null) {
                    ((TextView) cameraView).setText(camera);
                }
            });
        }, "AboutDeviceHeader").start();
    }

    private static void text(View root, int id, CharSequence value) {
        final TextView view = root.findViewById(id);
        if (view != null) {
            view.setText(value);
        }
    }

    private static int getRamGb(Context context) {
        final ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        context.getSystemService(ActivityManager.class).getMemoryInfo(info);
        final double gb = info.totalMem / (1024.0 * 1024.0 * 1024.0);
        for (int step : RAM_STEPS_GB) {
            if (gb <= step) {
                return step;
            }
        }
        return (int) Math.ceil(gb);
    }

    private static String getDisplayResolution(Context context) {
        final Display display = context.getSystemService(DisplayManager.class)
                .getDisplay(Display.DEFAULT_DISPLAY);
        if (display == null) {
            return "";
        }
        final Display.Mode mode = display.getMode();
        final int w = Math.max(mode.getPhysicalWidth(), mode.getPhysicalHeight());
        final int h = Math.min(mode.getPhysicalWidth(), mode.getPhysicalHeight());
        final NumberFormat nf = NumberFormat.getIntegerInstance();
        return nf.format(w) + " \u00D7 " + nf.format(h);
    }

    private static String getStorageSummary(Context context) {
        try {
            final long total = context.getSystemService(StorageManager.class)
                    .getPrimaryStorageSize();
            final long free = new StatFs(Environment.getDataDirectory().getPath())
                    .getAvailableBytes();
            final long used = Math.max(0, Math.min(total, total - free));
            return context.getString(R.string.about_device_storage_summary,
                    Math.round(used / 1_000_000_000.0), Math.round(total / 1_000_000_000.0));
        } catch (Exception e) {
            Log.w(TAG, "Unable to read storage size", e);
            return "";
        }
    }

    private static String getCameraSummary(Context context) {
        final List<Integer> front = new ArrayList<>();
        final List<Integer> rear = new ArrayList<>();
        try {
            final CameraManager cm = context.getSystemService(CameraManager.class);
            for (String id : cm.getCameraIdList()) {
                final CameraCharacteristics cc = cm.getCameraCharacteristics(id);
                final Integer facing = cc.get(CameraCharacteristics.LENS_FACING);
                if (facing == null) {
                    continue;
                }
                final List<Integer> target;
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    target = front;
                } else if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    target = rear;
                } else {
                    continue;
                }
                final java.util.Set<String> physical = cc.getPhysicalCameraIds();
                if (physical != null && !physical.isEmpty()) {
                    for (String pid : physical) {
                        addMegapixels(target, cm.getCameraCharacteristics(pid));
                    }
                } else {
                    addMegapixels(target, cc);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to read camera info", e);
        }
        Collections.sort(front, Collections.reverseOrder());
        Collections.sort(rear, Collections.reverseOrder());

        if (!front.isEmpty() && !rear.isEmpty()) {
            return context.getString(R.string.about_device_camera_summary,
                    join(front), join(rear));
        } else if (!rear.isEmpty()) {
            return context.getString(R.string.about_device_camera_rear_only, join(rear));
        } else if (!front.isEmpty()) {
            return context.getString(R.string.about_device_camera_front_only, join(front));
        }
        return "";
    }

    private static void addMegapixels(List<Integer> out, CameraCharacteristics cc) {
        final int[] caps = cc.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
        if (caps != null) {
            boolean compat = false;
            for (int cap : caps) {
                if (cap == CameraCharacteristics
                        .REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE) {
                    compat = true;
                    break;
                }
            }
            if (!compat) {
                return; // depth / IR / monochrome-only helper sensors
            }
        }
        final Size size = cc.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
        if (size == null) {
            return;
        }
        final int mp = Math.round(size.getWidth() * (float) size.getHeight() / 1_000_000f);
        if (mp > 0) {
            out.add(mp);
        }
    }

    private static String join(List<Integer> mps) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mps.size(); i++) {
            if (i > 0) {
                sb.append(" + ");
            }
            sb.append(mps.get(i)).append(" MP");
        }
        return sb.toString();
    }
}

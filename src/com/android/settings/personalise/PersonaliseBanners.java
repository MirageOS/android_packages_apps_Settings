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

package com.android.settings.personalise;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the small looping animations of the banners at the top of each Personalise section:
 * the Dynamic Island pill opening and closing, the Quick Settings tiles lighting up one after the
 * other, and a slow spin behind the Integrity shield.
 *
 * <p>Call {@link #start} when the screen is resumed and {@link #stop} when it is paused. If the
 * user turned animations off, the banners are left in a still, readable state instead.
 */
final class PersonaliseBanners {

    private static final int[] ISLAND_ICONS = {
            R.drawable.ic_dynamic_island_media,
            R.drawable.ic_dynamic_island_alarm,
            R.drawable.ic_dynamic_island_call,
    };
    private static final int ISLAND_COLLAPSED_DP = 92;
    private static final int ISLAND_EXPANDED_DP = 220;
    private static final long ISLAND_HOLD_MS = 1900;

    private static final int[] QS_TILES = {
            R.id.banner_qs_tile_1, R.id.banner_qs_tile_2,
            R.id.banner_qs_tile_3, R.id.banner_qs_tile_4,
    };
    private static final int[] QS_ICONS = {
            R.id.banner_qs_icon_1, R.id.banner_qs_icon_2,
            R.id.banner_qs_icon_3, R.id.banner_qs_icon_4,
    };

    private static final PathInterpolator EMPHASIZED = new PathInterpolator(0.05f, 0.7f, 0.1f, 1f);

    private final List<Animator> mRunning = new ArrayList<>();
    private boolean mStarted;

    /** Starts the animations. Any of the views can be null if its banner isn't on screen. */
    void start(View island, View qs, View integrity) {
        stop();
        mStarted = true;
        final boolean animate = ValueAnimator.areAnimatorsEnabled();
        if (island != null) {
            startIsland(island, animate);
        }
        if (qs != null) {
            startQs(qs, animate);
        }
        if (integrity != null && animate) {
            startIntegrity(integrity);
        }
    }

    /** Cancels everything that is running. */
    void stop() {
        mStarted = false;
        for (Animator animator : new ArrayList<>(mRunning)) {
            animator.cancel();
        }
        mRunning.clear();
    }

    // Dynamic Island

    private void startIsland(View root, boolean animate) {
        final View pill = root.findViewById(R.id.banner_island_pill);
        final View content = root.findViewById(R.id.banner_island_content);
        final ImageView icon = root.findViewById(R.id.banner_island_icon);
        if (pill == null || content == null || icon == null) {
            return;
        }
        final float density = root.getResources().getDisplayMetrics().density;
        final int collapsed = Math.round(ISLAND_COLLAPSED_DP * density);
        final int expanded = Math.round(ISLAND_EXPANDED_DP * density);

        if (!animate) {
            icon.setImageResource(ISLAND_ICONS[0]);
            setWidth(pill, expanded);
            content.setAlpha(1f);
            return;
        }
        setWidth(pill, collapsed);
        content.setAlpha(0f);

        final int[] bars = {R.id.banner_island_bar_1, R.id.banner_island_bar_2,
                R.id.banner_island_bar_3};
        for (int i = 0; i < bars.length; i++) {
            final View bar = root.findViewById(bars[i]);
            if (bar == null) {
                continue;
            }
            final ObjectAnimator pulse = ObjectAnimator.ofFloat(bar, View.SCALE_Y, 0.3f, 1f);
            pulse.setDuration(360 + 130 * i);
            pulse.setRepeatCount(ValueAnimator.INFINITE);
            pulse.setRepeatMode(ValueAnimator.REVERSE);
            run(pulse);
        }
        runIslandCycle(pill, content, icon, collapsed, expanded, 0);
    }

    private void runIslandCycle(View pill, View content, ImageView icon, int collapsed,
            int expanded, int step) {
        if (!mStarted) {
            return;
        }
        icon.setImageResource(ISLAND_ICONS[step % ISLAND_ICONS.length]);

        final ValueAnimator open = widthAnimator(pill, collapsed, expanded, 420);
        final ObjectAnimator fadeIn = ObjectAnimator.ofFloat(content, View.ALPHA, 0f, 1f);
        fadeIn.setStartDelay(140);
        fadeIn.setDuration(220);

        final ValueAnimator hold = ValueAnimator.ofFloat(0f, 1f);
        hold.setDuration(ISLAND_HOLD_MS);

        final ValueAnimator close = widthAnimator(pill, expanded, collapsed, 360);
        final ObjectAnimator fadeOut = ObjectAnimator.ofFloat(content, View.ALPHA, 1f, 0f);
        fadeOut.setDuration(140);

        final AnimatorSet opening = new AnimatorSet();
        opening.playTogether(open, fadeIn);
        final AnimatorSet closing = new AnimatorSet();
        closing.playTogether(close, fadeOut);

        final AnimatorSet cycle = new AnimatorSet();
        cycle.playSequentially(opening, hold, closing);
        cycle.setStartDelay(step == 0 ? 500 : 700);
        cycle.addListener(new AnimatorListenerAdapter() {
            private boolean mCancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                mCancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mRunning.remove(animation);
                if (!mCancelled) {
                    runIslandCycle(pill, content, icon, collapsed, expanded, step + 1);
                }
            }
        });
        mRunning.add(cycle);
        cycle.start();
    }

    private static ValueAnimator widthAnimator(View view, int from, int to, long duration) {
        final ValueAnimator animator = ValueAnimator.ofInt(from, to);
        animator.setDuration(duration);
        animator.setInterpolator(EMPHASIZED);
        animator.addUpdateListener(a -> setWidth(view, (int) a.getAnimatedValue()));
        return animator;
    }

    private static void setWidth(View view, int width) {
        final ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params.width != width) {
            params.width = width;
            view.setLayoutParams(params);
        }
    }

    // Quick Settings

    private void startQs(View root, boolean animate) {
        if (!animate) {
            return;
        }
        runQsStep(root, 0);
    }

    private void runQsStep(View root, int step) {
        if (!mStarted) {
            return;
        }
        final View tile = root.findViewById(QS_TILES[step % QS_TILES.length]);
        final ImageView icon = root.findViewById(QS_ICONS[step % QS_ICONS.length]);
        if (tile == null || icon == null) {
            return;
        }
        final int idle = root.getContext().getColor(
                R.color.settingslib_materialColorSurfaceContainerHigh);
        final int idleIcon = root.getContext().getColor(
                R.color.settingslib_materialColorOnSurfaceVariant);
        final int active = root.getContext().getColor(R.color.settingslib_materialColorPrimary);
        final int activeIcon = root.getContext().getColor(
                R.color.settingslib_materialColorOnPrimary);
        final ArgbEvaluator argb = new ArgbEvaluator();

        // Light up quickly, stay on for a moment, then fade back.
        final ValueAnimator pulse = ValueAnimator.ofPropertyValuesHolder(
                PropertyValuesHolder.ofKeyframe("t",
                        Keyframe.ofFloat(0f, 0f),
                        Keyframe.ofFloat(0.15f, 1f),
                        Keyframe.ofFloat(0.8f, 1f),
                        Keyframe.ofFloat(1f, 0f)));
        pulse.setDuration(2000);
        pulse.setInterpolator(new LinearInterpolator());
        pulse.setStartDelay(step == 0 ? 400 : 250);
        pulse.addUpdateListener(a -> {
            final float t = (float) a.getAnimatedValue("t");
            tile.setBackgroundTintList(
                    ColorStateList.valueOf((int) argb.evaluate(t, idle, active)));
            icon.setImageTintList(
                    ColorStateList.valueOf((int) argb.evaluate(t, idleIcon, activeIcon)));
            final float scale = 1f + 0.12f * t;
            tile.setScaleX(scale);
            tile.setScaleY(scale);
        });
        pulse.addListener(new AnimatorListenerAdapter() {
            private boolean mCancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                mCancelled = true;
                // Leave the tile as it was before the banner started.
                tile.setBackgroundTintList(ColorStateList.valueOf(idle));
                icon.setImageTintList(ColorStateList.valueOf(idleIcon));
                tile.setScaleX(1f);
                tile.setScaleY(1f);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mRunning.remove(animation);
                if (!mCancelled) {
                    runQsStep(root, step + 1);
                }
            }
        });
        mRunning.add(pulse);
        pulse.start();
    }

    // Integrity

    private void startIntegrity(View root) {
        final View shape = root.findViewById(R.id.banner_integrity_shape);
        if (shape == null) {
            return;
        }
        final ObjectAnimator spin = ObjectAnimator.ofFloat(shape, View.ROTATION, 0f, 360f);
        spin.setDuration(20000);
        spin.setInterpolator(new LinearInterpolator());
        spin.setRepeatCount(ValueAnimator.INFINITE);
        run(spin);
    }

    private void run(Animator animator) {
        mRunning.add(animator);
        animator.start();
    }
}

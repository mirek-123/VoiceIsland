package com.mirecek.siriisland;

import android.animation.TimeInterpolator;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.os.Build;
import android.window.BackEvent;
import android.window.OnBackAnimationCallback;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * Settings pages stacked inside one screen, with iOS-style push / pop slides and back handling.
 * On Android 14+ the back gesture is interactive: the page follows the swipe (predictive back) and
 * either finishes the pop from where the finger let go or slides back if the swipe is cancelled.
 */
final class SettingsNav {
    interface Page {
        String title();

        View view();

        default void refresh() {}
    }

    interface Listener {
        void onDepthChanged(Page top, int depth);
    }

    private static final long DURATION_MS = 350;
    private final TimeInterpolator ease = new PathInterpolator(0.32f, 0.72f, 0f, 1f);
    private final Activity activity;
    private final FrameLayout container;
    private final Listener listener;
    private final ArrayDeque<Page> stack = new ArrayDeque<>();
    private final OnBackInvokedCallback back = Build.VERSION.SDK_INT >= 34 ? animatedBack() : this::pop;
    /** A back swipe is in progress: the two pages follow it. */
    private boolean gesture;
    private View gestureIn, gestureOut;
    private boolean backRegistered;

    SettingsNav(Activity activity, FrameLayout container, Listener listener) {
        this.activity = activity;
        this.container = container;
        this.listener = listener;
    }

    Page top() { return stack.peek(); }

    int depth() { return stack.size() - 1; }

    void setRoot(Page root) {
        stack.clear();
        container.removeAllViews();
        stack.push(root);
        container.addView(detached(root.view()), new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        changed();
    }

    void push(Page page) {
        if (stack.contains(page)) return;
        View out = stack.peek().view();
        stack.push(page);
        View in = detached(page.view());
        container.addView(in, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        float w = container.getWidth();
        in.setTranslationX(w);
        in.setAlpha(1f);
        in.setVisibility(View.VISIBLE);
        in.animate().translationX(0f).setDuration(DURATION_MS).setInterpolator(ease).start();
        out.animate().translationX(-0.3f * w).alpha(0.6f).setDuration(DURATION_MS).setInterpolator(ease)
                .withEndAction(() -> out.setVisibility(View.INVISIBLE)).start();
        page.refresh();
        changed();
    }

    boolean pop() {
        if (stack.size() <= 1) return false;
        Page leaving = stack.pop();
        Page showing = stack.peek();
        View out = leaving.view();
        View in = showing.view();
        float w = container.getWidth();
        if (!gesture) {
            // A tap on back: start from the resting positions. A swipe continues from where it is.
            in.animate().cancel();
            in.setVisibility(View.VISIBLE);
            in.setTranslationX(-0.3f * w);
            in.setAlpha(0.6f);
        }
        gesture = false;
        in.animate().translationX(0f).alpha(1f).setDuration(DURATION_MS).setInterpolator(ease).start();
        out.animate().translationX(w).setDuration(DURATION_MS).setInterpolator(ease)
                .withEndAction(() -> {
                    container.removeView(out);
                    out.setTranslationX(0f);
                }).start();
        showing.refresh();
        changed();
        return true;
    }

    private static View detached(View v) {
        if (v.getParent() instanceof ViewGroup) ((ViewGroup) v.getParent()).removeView(v);
        return v;
    }

    private void changed() {
        boolean want = stack.size() > 1;
        if (want != backRegistered) {
            OnBackInvokedDispatcher d = activity.getOnBackInvokedDispatcher();
            if (want) d.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, back);
            else d.unregisterOnBackInvokedCallback(back);
            backRegistered = want;
        }
        listener.onDepthChanged(stack.peek(), stack.size() - 1);
    }

    @android.annotation.TargetApi(34)
    private OnBackInvokedCallback animatedBack() {
        return new OnBackAnimationCallback() {
            @Override public void onBackStarted(BackEvent e) { gestureStart(); }

            @Override public void onBackProgressed(BackEvent e) { gestureProgress(e.getProgress()); }

            @Override public void onBackInvoked() { pop(); }

            @Override public void onBackCancelled() { gestureCancel(); }
        };
    }

    private void gestureStart() {
        if (stack.size() <= 1) return;
        Iterator<Page> it = stack.iterator();
        gestureOut = it.next().view();
        gestureIn = it.next().view();
        gestureOut.animate().cancel();
        gestureIn.animate().cancel();
        gestureIn.setVisibility(View.VISIBLE);
        gestureIn.setTranslationX(-0.3f * container.getWidth());
        gestureIn.setAlpha(0.6f);
        gesture = true;
    }

    /** {@code p} 0..1: how far the swipe has gone (already eased by the system). */
    private void gestureProgress(float p) {
        if (!gesture) return;
        float w = container.getWidth();
        gestureOut.setTranslationX(p * w * 0.9f);
        gestureIn.setTranslationX(-0.3f * w * (1f - p));
        gestureIn.setAlpha(0.6f + 0.4f * p);
    }

    private void gestureCancel() {
        if (!gesture) return;
        gesture = false;
        View in = gestureIn;
        gestureOut.animate().translationX(0f).setDuration(DURATION_MS).setInterpolator(ease).start();
        in.animate().translationX(-0.3f * container.getWidth()).alpha(0.6f).setDuration(DURATION_MS)
                .setInterpolator(ease).withEndAction(() -> in.setVisibility(View.INVISIBLE)).start();
    }
}

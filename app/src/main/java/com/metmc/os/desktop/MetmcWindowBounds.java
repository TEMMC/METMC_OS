package com.metmc.os.desktop;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public final class MetmcWindowBounds {

    private static final int DEFAULT_TASKBAR_HEIGHT = 72;
    private static final int MIN_WINDOW_WIDTH = 280;
    private static final int MIN_WINDOW_HEIGHT = 180;

    private MetmcWindowBounds() {}

    public static Rect getWorkspaceBounds(View desktop, View taskbar) {
        if (desktop == null) return new Rect();

        int width = desktop.getWidth();
        int height = desktop.getHeight();

        if (width <= 0 || height <= 0) return new Rect();

        int top = 0;
        int bottom = height;

        if (taskbar != null && taskbar.getVisibility() == View.VISIBLE) {
            int taskbarHeight = taskbar.getHeight();

            if (taskbarHeight <= 0)
                taskbarHeight = DEFAULT_TASKBAR_HEIGHT;

            if (taskbar.getTop() >= height / 2) {
                bottom = Math.max(top, taskbar.getTop());
            } else {
                top = Math.min(height, taskbar.getBottom());
            }
        }

        return new Rect(0, top, width, Math.max(top, bottom));
    }

    public static void constrainWindow(
            View window,
            View desktop,
            View taskbar
    ) {
        if (window == null || desktop == null) return;

        Rect bounds = getWorkspaceBounds(desktop, taskbar);
        if (bounds.isEmpty()) return;

        int width = window.getWidth();
        int height = window.getHeight();

        ViewGroup.LayoutParams current = window.getLayoutParams();

        if (width <= 0 && current != null)
            width = current.width > 0 ? current.width : MIN_WINDOW_WIDTH;

        if (height <= 0 && current != null)
            height = current.height > 0 ? current.height : MIN_WINDOW_HEIGHT;

        width = Math.max(1, Math.min(width, bounds.width()));
        height = Math.max(1, Math.min(height, bounds.height()));

        int x = window.getLeft();
        int y = window.getTop();

        int maxX = bounds.right - width;
        int maxY = bounds.bottom - height;

        x = Math.max(bounds.left, Math.min(x, maxX));
        y = Math.max(bounds.top, Math.min(y, maxY));

        FrameLayout.LayoutParams lp;

        if (window.getLayoutParams() instanceof FrameLayout.LayoutParams) {
            lp = (FrameLayout.LayoutParams) window.getLayoutParams();
        } else {
            lp = new FrameLayout.LayoutParams(width, height);
        }

        lp.width = width;
        lp.height = height;
        lp.leftMargin = x;
        lp.topMargin = y;

        window.setLayoutParams(lp);
    }

    public static Rect constrainRect(
            Rect requested,
            View desktop,
            View taskbar
    ) {
        Rect bounds = getWorkspaceBounds(desktop, taskbar);

        if (bounds.isEmpty())
            return new Rect(requested);

        int width = Math.min(
                Math.max(MIN_WINDOW_WIDTH, requested.width()),
                bounds.width()
        );

        int height = Math.min(
                Math.max(MIN_WINDOW_HEIGHT, requested.height()),
                bounds.height()
        );

        int left = Math.max(
                bounds.left,
                Math.min(requested.left, bounds.right - width)
        );

        int top = Math.max(
                bounds.top,
                Math.min(requested.top, bounds.bottom - height)
        );

        return new Rect(left, top, left + width, top + height);
    }

    public static boolean isInsideWorkspace(
            View window,
            View desktop,
            View taskbar
    ) {
        if (window == null || desktop == null) return false;

        Rect bounds = getWorkspaceBounds(desktop, taskbar);

        return window.getLeft() >= bounds.left
                && window.getTop() >= bounds.top
                && window.getRight() <= bounds.right
                && window.getBottom() <= bounds.bottom;
    }

    public static void constrainAllChildren(
            ViewGroup desktop,
            View taskbar
    ) {
        if (desktop == null) return;

        for (int i = 0; i < desktop.getChildCount(); i++) {
            View child = desktop.getChildAt(i);

            if (child == taskbar) continue;

            constrainWindow(child, desktop, taskbar);
        }
    }
}

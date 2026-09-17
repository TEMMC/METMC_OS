package com.metmc.os.ipc;

import android.view.SurfaceControlViewHost.SurfacePackage;
import android.os.IBinder;

interface IFeatureWindowService {
    // Called by core when a window of this feature should open.
    // width/height are the initial window content size in px.
    SurfacePackage requestWindow(String windowId, int width, int height, IBinder hostToken);

    // Called when core resizes the window.
    void resizeWindow(String windowId, int width, int height);

    // Called when core closes the window.
    void closeWindow(String windowId);

    // Human-readable title/icon for the taskbar.
    String getFeatureTitle();
}

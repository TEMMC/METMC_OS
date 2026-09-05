package com.metmc.os.shell;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.view.SurfaceControlViewHost;
import android.view.SurfaceView;
import com.metmc.os.ipc.IFeatureWindowService;

public class FeatureWindowHost {

    public interface OnWindowReadyListener {
        void onReady(SurfaceView surfaceView);
    }

    private IFeatureWindowService featureService;
    private ServiceConnection connection;

    public void connectAndOpen(Context context, String packageName, String serviceClass,
                                String windowId, int width, int height,
                                SurfaceView targetSurfaceView,
                                OnWindowReadyListener listener) {
        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                featureService = IFeatureWindowService.Stub.asInterface(binder);
                try {
                    SurfaceControlViewHost.SurfacePackage pkg = featureService.requestWindow(
                        windowId, width, height, targetSurfaceView.getHostToken());
                    targetSurfaceView.setChildSurfacePackage(pkg);
                    listener.onReady(targetSurfaceView);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                featureService = null;
            }
        };

        Intent intent = new Intent();
        intent.setComponent(new ComponentName(packageName, serviceClass));
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    public void close(Context context, String windowId) {
        try {
            if (featureService != null) featureService.closeWindow(windowId);
        } catch (Exception ignored) {}
        if (connection != null) context.unbindService(connection);
    }
}

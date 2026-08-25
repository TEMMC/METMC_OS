package com.metmc.os.desktop;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.widget.Toast;

public final class AndroidWindowLauncher {

    private AndroidWindowLauncher() {
    }

    public static boolean launch(
            Activity activity,
            String packageName
    ) {
        if (activity == null || packageName == null || packageName.isEmpty()) {
            return false;
        }

        try {
            PackageManager pm = activity.getPackageManager();

            Intent launch = pm.getLaunchIntentForPackage(packageName);

            if (launch == null) {
                Toast.makeText(
                        activity,
                        "No launcher activity: " + packageName,
                        Toast.LENGTH_SHORT
                ).show();
                return false;
            }

            /*
             * Ask Android to place the external application in a
             * desktop-style bounded window.
             *
             * Android's WindowManager ultimately decides whether
             * freeform bounds are allowed on the current device.
             */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                int width = dp(activity, 700);
                int height = dp(activity, 500);

                Rect bounds = new Rect(
                        dp(activity, 40),
                        dp(activity, 70),
                        dp(activity, 40) + width,
                        dp(activity, 70) + height
                );

                ActivityOptions options =
                        ActivityOptions.makeBasic();

                options.setLaunchBounds(bounds);

                launch.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                activity.startActivity(
                        launch,
                        options.toBundle()
                );

            } else {

                launch.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                activity.startActivity(launch);
            }

            return true;

        } catch (Exception e) {

            Toast.makeText(
                    activity,
                    "Window launch error: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

            return false;
        }
    }

    private static int dp(
            Activity activity,
            int value
    ) {
        return (int) (
                value *
                activity.getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}

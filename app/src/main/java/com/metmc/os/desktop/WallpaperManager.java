package com.metmc.os.desktop;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

public class WallpaperManager {

    public static final int REQUEST_WALLPAPER = 9001;

    private final Activity activity;
    private final View desktop;

    public WallpaperManager(Activity activity, View desktop) {
        this.activity = activity;
        this.desktop = desktop;
    }

    public void chooseWallpaper() {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        activity.startActivityForResult(
                intent,
                REQUEST_WALLPAPER
        );
    }

    public void apply(Uri image) {

        if (image == null)
            return;

        try {
            java.io.InputStream stream =
                    activity.getContentResolver()
                            .openInputStream(image);

            if (stream == null)
                return;

            android.graphics.Bitmap bitmap =
                    android.graphics.BitmapFactory
                            .decodeStream(stream);

            stream.close();

            if (bitmap == null)
                return;

            android.graphics.drawable.BitmapDrawable drawable =
                    new android.graphics.drawable.BitmapDrawable(
                            activity.getResources(),
                            bitmap
                    );

            drawable.setGravity(
                    android.view.Gravity.CENTER
            );

            desktop.setBackground(drawable);

        } catch (Exception e) {

            android.widget.Toast.makeText(
                    activity,
                    "Wallpaper error: " + e.getMessage(),
                    android.widget.Toast.LENGTH_LONG
            ).show();
        }
    }
}

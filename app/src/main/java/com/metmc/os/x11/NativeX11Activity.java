package com.metmc.os.x11;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** Compatibility entry point kept for existing METMC launchers. */
public class NativeX11Activity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        startActivity(new Intent(this, EmbeddedLinuxX11Activity.class));
        finish();
    }
}

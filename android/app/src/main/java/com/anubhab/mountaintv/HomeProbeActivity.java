package com.anubhab.mountaintv;

import android.app.Activity;
import android.os.Bundle;

/** A transparent, separate task briefly restores a window event at boot. It reads no UI.
 * This lets the helper distinguish stock home from an HDMI input or another resumed app. */
public final class HomeProbeActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().post(() -> { finish(); overridePendingTransition(0,0); });
    }
}

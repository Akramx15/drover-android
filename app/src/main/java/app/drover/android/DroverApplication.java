package app.drover.android;

import android.app.Application;

/** Clears UI state that can be left behind when Android terminates the VPN process. */
public final class DroverApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        TunnelState.set(this, TunnelState.STOPPED, "");
    }
}

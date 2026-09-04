package app.drover.quest;

import android.app.Application;

/** Clears a persisted UI state left behind if Android killed the VPN process. */
public final class DroverApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        TunnelState.set(this, TunnelState.STOPPED, "");
    }
}

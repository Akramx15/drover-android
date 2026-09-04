package app.drover.quest;

import android.content.Context;
import android.content.SharedPreferences;

final class TunnelState {
    static final String STOPPED = "stopped";
    static final String STARTING = "starting";
    static final String RUNNING = "running";
    static final String STOPPING = "stopping";
    static final String ERROR = "error";

    private static final String PREFS = "drover_tunnel_state";
    private static final String KEY_STATE = "state";
    private static final String KEY_DETAIL = "detail";

    private TunnelState() {
    }

    static void set(Context context, String state, String detail) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_STATE, state)
                .putString(KEY_DETAIL, detail == null ? "" : detail)
                .apply();
    }

    static Snapshot get(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new Snapshot(
                preferences.getString(KEY_STATE, STOPPED),
                preferences.getString(KEY_DETAIL, "")
        );
    }

    static final class Snapshot {
        final String state;
        final String detail;

        Snapshot(String state, String detail) {
            this.state = state;
            this.detail = detail;
        }
    }
}

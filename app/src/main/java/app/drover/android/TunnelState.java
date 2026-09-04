package app.drover.android;

import android.content.Context;
import android.content.SharedPreferences;

final class TunnelState {
    static final String STOPPED = "stopped";
    static final String STARTING = "starting";
    static final String RESTARTING = "restarting";
    static final String RUNNING = "running";
    static final String STOPPING = "stopping";
    static final String ERROR = "error";

    private static final String PREFS = "drover_tunnel_state";
    private static final String KEY_STATE = "state";
    private static final String KEY_DETAIL = "detail";
    private static final String KEY_ACTIVE_PACKAGE = "active_package";

    private TunnelState() {
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static void set(Context context, String state, String detail) {
        SharedPreferences.Editor editor = preferences(context)
                .edit()
                .putString(KEY_STATE, state)
                .putString(KEY_DETAIL, detail == null ? "" : detail);
        if (!RUNNING.equals(state)) {
            editor.remove(KEY_ACTIVE_PACKAGE);
        }
        editor.apply();
    }

    static void setRunning(Context context, String activePackage) {
        preferences(context)
                .edit()
                .putString(KEY_STATE, RUNNING)
                .putString(KEY_DETAIL, "")
                .putString(KEY_ACTIVE_PACKAGE, activePackage)
                .apply();
    }

    static Snapshot get(Context context) {
        SharedPreferences preferences = preferences(context);
        return new Snapshot(
                preferences.getString(KEY_STATE, STOPPED),
                preferences.getString(KEY_DETAIL, ""),
                preferences.getString(KEY_ACTIVE_PACKAGE, null)
        );
    }

    static void registerListener(
            Context context,
            SharedPreferences.OnSharedPreferenceChangeListener listener
    ) {
        preferences(context).registerOnSharedPreferenceChangeListener(listener);
    }

    static void unregisterListener(
            Context context,
            SharedPreferences.OnSharedPreferenceChangeListener listener
    ) {
        preferences(context).unregisterOnSharedPreferenceChangeListener(listener);
    }

    static final class Snapshot {
        final String state;
        final String detail;
        final String activePackage;

        Snapshot(String state, String detail, String activePackage) {
            this.state = state;
            this.detail = detail;
            this.activePackage = activePackage;
        }
    }
}

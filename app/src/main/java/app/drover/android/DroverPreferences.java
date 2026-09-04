package app.drover.android;

import android.content.Context;
import android.content.SharedPreferences;

/** Small, synchronous preference facade for choices that affect service startup. */
final class DroverPreferences {
    private static final String PREFS = "drover_preferences";
    private static final String KEY_SELECTED_DISCORD_PACKAGE = "selected_discord_package";
    private static final String KEY_AUTO_START_ON_BOOT = "auto_start_on_boot";
    private static final String KEY_RUN_DESIRED = "run_desired";
    private static final String KEY_NOTIFICATION_PERMISSION_REQUESTED =
            "notification_permission_requested";

    private DroverPreferences() {
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String getSelectedDiscordPackage(Context context) {
        return preferences(context).getString(KEY_SELECTED_DISCORD_PACKAGE, null);
    }

    static void setSelectedDiscordPackage(Context context, String packageName) {
        SharedPreferences.Editor editor = preferences(context).edit();
        if (packageName == null) {
            editor.remove(KEY_SELECTED_DISCORD_PACKAGE);
        } else {
            editor.putString(KEY_SELECTED_DISCORD_PACKAGE, packageName);
        }
        editor.apply();
    }

    static boolean isAutoStartOnBootEnabled(Context context) {
        return preferences(context).getBoolean(KEY_AUTO_START_ON_BOOT, false);
    }

    static void setAutoStartOnBootEnabled(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(KEY_AUTO_START_ON_BOOT, enabled).apply();
    }

    static boolean isRunDesired(Context context) {
        return preferences(context).getBoolean(KEY_RUN_DESIRED, false);
    }

    static void setRunDesired(Context context, boolean desired) {
        preferences(context).edit().putBoolean(KEY_RUN_DESIRED, desired).apply();
    }

    static boolean wasNotificationPermissionRequested(Context context) {
        return preferences(context).getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false);
    }

    static void markNotificationPermissionRequested(Context context) {
        preferences(context)
                .edit()
                .putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, true)
                .apply();
    }
}

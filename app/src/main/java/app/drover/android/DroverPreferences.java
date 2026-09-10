package app.drover.android;

import android.content.Context;
import android.content.SharedPreferences;

/** Small, synchronous preference facade for choices that affect service startup. */
final class DroverPreferences {
    private static final String PREFS = "drover_preferences";
    private static final String KEY_SELECTED_APP_KIND = "selected_app_kind";
    private static final String KEY_SELECTED_DISCORD_PACKAGE = "selected_discord_package";
    private static final String KEY_SELECTED_WHATSAPP_PACKAGE = "selected_whatsapp_package";
    private static final String KEY_AUTO_START_ON_BOOT = "auto_start_on_boot";
    private static final String KEY_RUN_DESIRED = "run_desired";
    private static final String KEY_NOTIFICATION_PERMISSION_REQUESTED =
            "notification_permission_requested";

    private DroverPreferences() {
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static AppKind getSelectedAppKind(Context context) {
        return AppKind.fromPreference(
                preferences(context).getString(KEY_SELECTED_APP_KIND, AppKind.DISCORD.preferenceValue)
        );
    }

    static void setSelectedAppKind(Context context, AppKind kind) {
        preferences(context)
                .edit()
                .putString(KEY_SELECTED_APP_KIND, kind.preferenceValue)
                .apply();
    }

    static String getSelectedPackage(Context context, AppKind kind) {
        String key = kind == AppKind.WHATSAPP
                ? KEY_SELECTED_WHATSAPP_PACKAGE
                : KEY_SELECTED_DISCORD_PACKAGE;
        return preferences(context).getString(key, null);
    }

    static void setSelectedPackage(Context context, AppKind kind, String packageName) {
        String key = kind == AppKind.WHATSAPP
                ? KEY_SELECTED_WHATSAPP_PACKAGE
                : KEY_SELECTED_DISCORD_PACKAGE;
        SharedPreferences.Editor editor = preferences(context).edit();
        if (packageName == null) {
            editor.remove(key);
        } else {
            editor.putString(key, packageName);
        }
        editor.apply();
    }

    static String getSelectedDiscordPackage(Context context) {
        return getSelectedPackage(context, AppKind.DISCORD);
    }

    static void setSelectedDiscordPackage(Context context, String packageName) {
        setSelectedPackage(context, AppKind.DISCORD, packageName);
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

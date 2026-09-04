package app.drover.android;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class DiscordPackages {
    static final String STABLE = "com.discord";
    static final String PTB = "com.discord.ptb";
    static final String CANARY = "com.discord.canary";

    private static final String[] CANDIDATES = {STABLE, PTB, CANARY};

    private DiscordPackages() {
    }

    static List<String> findInstalled(Context context) {
        PackageManager packageManager = context.getPackageManager();
        List<String> installed = new ArrayList<>();
        for (String packageName : CANDIDATES) {
            try {
                ApplicationInfo info = packageManager.getApplicationInfo(packageName, 0);
                if (info.enabled && !packageName.equals(context.getPackageName())) {
                    installed.add(packageName);
                }
            } catch (PackageManager.NameNotFoundException ignored) {
                // Candidate not installed.
            }
        }
        return Collections.unmodifiableList(installed);
    }

    /**
     * Returns a valid saved choice. A sole installation is selected automatically;
     * multiple installations require an explicit choice.
     */
    static String resolveSelected(Context context, List<String> installed) {
        String selected = DroverPreferences.getSelectedDiscordPackage(context);
        if (selected != null && installed.contains(selected)) {
            return selected;
        }

        if (installed.size() == 1) {
            selected = installed.get(0);
            DroverPreferences.setSelectedDiscordPackage(context, selected);
            return selected;
        }

        if (selected != null) {
            DroverPreferences.setSelectedDiscordPackage(context, null);
        }
        return null;
    }

    static String displayName(Context context, String packageName) {
        if (PTB.equals(packageName)) {
            return context.getString(R.string.discord_ptb);
        }
        if (CANARY.equals(packageName)) {
            return context.getString(R.string.discord_canary);
        }
        return context.getString(R.string.discord_stable);
    }

    static boolean launch(Context context, String packageName) {
        if (packageName == null) {
            return false;
        }

        PackageManager packageManager = context.getPackageManager();
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                IntentSender sender = packageManager.getLaunchIntentSenderForPackage(packageName);
                if (Build.VERSION.SDK_INT >= 34) {
                    ActivityOptions options = ActivityOptions.makeBasic();
                    if (Build.VERSION.SDK_INT >= 36) {
                        options.setPendingIntentBackgroundActivityStartMode(
                                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE
                        );
                    } else {
                        options.setPendingIntentBackgroundActivityStartMode(
                                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                        );
                    }
                    context.startIntentSender(sender, null, 0, 0, 0, options.toBundle());
                } else {
                    context.startIntentSender(sender, null, 0, 0, 0);
                }
                return true;
            }

            Intent intent = packageManager.getLaunchIntentForPackage(packageName);
            if (intent == null) {
                return false;
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (IntentSender.SendIntentException | RuntimeException ignored) {
            return false;
        }
    }
}

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

final class SupportedApps {
    static final String DISCORD_STABLE = "com.discord";
    static final String DISCORD_PTB = "com.discord.ptb";
    static final String DISCORD_CANARY = "com.discord.canary";
    static final String WHATSAPP = "com.whatsapp";
    static final String WHATSAPP_BUSINESS = "com.whatsapp.w4b";

    private static final String[] DISCORD_CANDIDATES = {
            DISCORD_STABLE,
            DISCORD_PTB,
            DISCORD_CANARY
    };
    private static final String[] WHATSAPP_CANDIDATES = {
            WHATSAPP,
            WHATSAPP_BUSINESS
    };

    private SupportedApps() {
    }

    static List<String> findInstalled(Context context, AppKind kind) {
        PackageManager packageManager = context.getPackageManager();
        List<String> installed = new ArrayList<>();
        for (String packageName : candidates(kind)) {
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
    static String resolveSelected(Context context, AppKind kind, List<String> installed) {
        String selected = DroverPreferences.getSelectedPackage(context, kind);
        if (selected != null && installed.contains(selected)) {
            return selected;
        }

        if (installed.size() == 1) {
            selected = installed.get(0);
            DroverPreferences.setSelectedPackage(context, kind, selected);
            return selected;
        }

        if (selected != null) {
            DroverPreferences.setSelectedPackage(context, kind, null);
        }
        return null;
    }

    static String displayName(Context context, String packageName) {
        if (DISCORD_PTB.equals(packageName)) {
            return context.getString(R.string.discord_ptb);
        }
        if (DISCORD_CANARY.equals(packageName)) {
            return context.getString(R.string.discord_canary);
        }
        if (WHATSAPP.equals(packageName)) {
            return context.getString(R.string.whatsapp_messenger);
        }
        if (WHATSAPP_BUSINESS.equals(packageName)) {
            return context.getString(R.string.whatsapp_business);
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

    private static String[] candidates(AppKind kind) {
        return kind == AppKind.WHATSAPP ? WHATSAPP_CANDIDATES : DISCORD_CANDIDATES;
    }
}

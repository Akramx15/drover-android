package app.drover.quest;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class DiscordPackages {
    private static final String[] CANDIDATES = {
            "com.discord",
            "com.discord.ptb",
            "com.discord.canary"
    };

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
}

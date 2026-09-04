package app.drover.android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;

import java.util.List;

/** Starts only the foreground VPN service after boot; it never opens an activity. */
public final class BootReceiver extends BroadcastReceiver {
    private static final String ATTENTION_CHANNEL_ID = "drover_attention";
    private static final int ATTENTION_NOTIFICATION_ID = 4208;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !isSupportedAction(intent.getAction())) {
            return;
        }
        if (!DroverPreferences.isAutoStartOnBootEnabled(context)) {
            return;
        }

        List<String> installedPackages = DiscordPackages.findInstalled(context);
        String selectedPackage = DiscordPackages.resolveSelected(context, installedPackages);
        if (selectedPackage == null) {
            DroverPreferences.setRunDesired(context, false);
            postAttention(context, R.string.boot_discord_selection_required);
            return;
        }

        if (VpnService.prepare(context) != null) {
            DroverPreferences.setRunDesired(context, false);
            postAttention(context, R.string.boot_vpn_permission_required);
            return;
        }

        DroverPreferences.setRunDesired(context, true);
        Intent serviceIntent = new Intent(context, DroverVpnService.class)
                .setAction(DroverVpnService.ACTION_START);
        try {
            context.startForegroundService(serviceIntent);
        } catch (RuntimeException error) {
            DroverPreferences.setRunDesired(context, false);
            TunnelState.set(context, TunnelState.ERROR, safeMessage(error));
            postAttention(context, R.string.boot_start_failed);
        }
    }

    private static boolean isSupportedAction(String action) {
        return Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action);
    }

    private static void postAttention(Context context, int messageResource) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(
                ATTENTION_CHANNEL_ID,
                context.getString(R.string.attention_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        manager.createNotificationChannel(channel);

        Intent openIntent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                context,
                3,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new Notification.Builder(context, ATTENTION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_drover)
                .setContentTitle(context.getString(R.string.attention_notification_title))
                .setContentText(context.getString(messageResource))
                .setContentIntent(openPendingIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_ERROR)
                .build();
        manager.notify(ATTENTION_NOTIFICATION_ID, notification);
    }

    private static String safeMessage(Throwable error) {
        String detail = error.getMessage();
        if (detail == null || detail.trim().isEmpty()) {
            return error.getClass().getSimpleName();
        }
        return detail;
    }
}

package app.drover.quest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Icon;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.github.shadowsocks.bg.Tun2proxy;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DroverVpnService extends VpnService {
    public static final String ACTION_START = "app.drover.quest.action.START";
    public static final String ACTION_STOP = "app.drover.quest.action.STOP";

    private static final String TAG = "DroverQuest";
    private static final String CHANNEL_ID = "drover_vpn";
    private static final int NOTIFICATION_ID = 4207;
    private static final int MTU = 1500;

    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private volatile ParcelFileDescriptor tunInterface;
    private volatile Thread tunnelThread;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            requestStop();
            return Service.START_NOT_STICKY;
        }

        startInForeground();
        startTunnelIfNeeded();
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onRevoke() {
        requestStop();
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        requestStop();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    private synchronized void startTunnelIfNeeded() {
        if (tunnelThread != null && tunnelThread.isAlive()) {
            return;
        }

        stopRequested.set(false);
        TunnelState.set(this, TunnelState.STARTING, "");
        tunnelThread = new Thread(this::runTunnel, "drover-tunnel");
        tunnelThread.start();
    }

    private void runTunnel() {
        try {
            List<String> discordPackages = DiscordPackages.findInstalled(this);
            if (discordPackages.isEmpty()) {
                throw new IllegalStateException(getString(R.string.discord_missing));
            }

            Builder builder = new Builder()
                    .setSession(getString(R.string.app_name))
                    .setMtu(MTU)
                    .addAddress("10.77.0.1", 32)
                    .addRoute("0.0.0.0", 0)
                    .addAddress("fd42:4242:4242::1", 128)
                    .addRoute("::", 0)
                    .addDnsServer("1.1.1.1");

            for (String discordPackage : discordPackages) {
                // Never establish the VPN unless at least one explicit Discord
                // package has been added. An empty allow-list would capture all apps.
                builder.addAllowedApplication(discordPackage);
            }

            builder.setMetered(false);

            tunInterface = builder.establish();
            if (tunInterface == null) {
                throw new IllegalStateException("تعذّر إنشاء واجهة VPN");
            }

            if (stopRequested.get()) {
                return;
            }

            TunnelState.set(this, TunnelState.RUNNING, "");
            String arguments = "tun2proxy-bin"
                    + " --tun-fd " + tunInterface.getFd()
                    + " --close-fd-on-drop false"
                    + " --proxy none"
                    + " --dns direct"
                    + " --dns-addr 1.1.1.1"
                    + " --ipv6-enabled"
                    + " --mtu " + MTU
                    + " --tcp-mss 1460"
                    + " --udp-timeout 120"
                    + " --verbosity info";

            int result = Tun2proxy.run(arguments, (char) MTU);
            if (!stopRequested.get()) {
                throw new IllegalStateException("توقف محرك الشبكة بشكل غير متوقع (رمز " + result + ")");
            }
        } catch (Throwable error) {
            Log.e(TAG, "Tunnel failed", error);
            if (!stopRequested.get()) {
                String detail = error.getMessage();
                if (detail == null || detail.trim().isEmpty()) {
                    detail = error.getClass().getSimpleName();
                }
                TunnelState.set(this, TunnelState.ERROR, detail);
            }
        } finally {
            closeTunInterface();
            if (stopRequested.get()) {
                TunnelState.set(this, TunnelState.STOPPED, "");
            }
            // Prevent onDestroy() from replacing a useful error state with a
            // transient stopping state after this worker has already finished.
            stopRequested.set(true);
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            synchronized (this) {
                tunnelThread = null;
            }
        }
    }

    private void requestStop() {
        if (!stopRequested.compareAndSet(false, true)) {
            return;
        }
        TunnelState.set(this, TunnelState.STOPPING, "");
        try {
            int result = Tun2proxy.stop();
            if (result != 0) {
                closeTunInterface();
            }
        } catch (Throwable error) {
            Log.w(TAG, "Native tunnel was not running", error);
            closeTunInterface();
        }

        Thread thread = tunnelThread;
        if (thread == null || !thread.isAlive()) {
            closeTunInterface();
            TunnelState.set(this, TunnelState.STOPPED, "");
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        }
    }

    private synchronized void closeTunInterface() {
        if (tunInterface == null) {
            return;
        }
        try {
            tunInterface.close();
        } catch (IOException error) {
            Log.w(TAG, "Failed to close TUN interface", error);
        } finally {
            tunInterface = null;
        }
    }

    private void startInForeground() {
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            );
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification() {
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                this,
                1,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent stopIntent = new Intent(this, DroverVpnService.class).setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                2,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_drover)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setContentIntent(openPendingIntent)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .addAction(
                        new Notification.Action.Builder(
                                Icon.createWithResource(this, R.drawable.ic_drover),
                                getString(R.string.stop_action),
                                stopPendingIntent
                        ).build()
                )
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription(getString(R.string.notification_text));
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }
}

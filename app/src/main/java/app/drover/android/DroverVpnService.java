package app.drover.android;

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
import java.util.concurrent.atomic.AtomicLong;

public final class DroverVpnService extends VpnService {
    public static final String ACTION_START = "app.drover.android.action.START";
    public static final String ACTION_STOP = "app.drover.android.action.STOP";
    public static final String ACTION_RESTART = "app.drover.android.action.RESTART";
    public static final String ACTION_REFRESH_POLICY =
            "app.drover.android.action.REFRESH_POLICY";

    private static final String TAG = "DroverAndroid";
    private static final String CHANNEL_ID = "drover_vpn";
    private static final int NOTIFICATION_ID = 4207;
    private static final int MTU = 1500;
    private static final AtomicLong NEXT_SERVICE_GENERATION = new AtomicLong();
    private static final AtomicLong ACTIVE_SERVICE_GENERATION = new AtomicLong();

    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private volatile ParcelFileDescriptor tunInterface;
    private volatile Thread tunnelThread;
    // Local worker transitions are guarded by this service instance's monitor.
    // Android can deliver commands on the main thread while the native worker
    // is unwinding on another thread.
    private boolean restartRequested;
    private boolean explicitStopRequested;
    private boolean destroyed;
    private int latestStartId;
    private long serviceGeneration;

    @Override
    public void onCreate() {
        super.onCreate();
        serviceGeneration = NEXT_SERVICE_GENERATION.incrementAndGet();
        ACTIVE_SERVICE_GENERATION.set(serviceGeneration);
        createNotificationChannel();
    }

    @Override
    public synchronized int onStartCommand(Intent intent, int flags, int startId) {
        latestStartId = startId;
        String action = intent == null ? null : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            restartRequested = false;
            requestStop(true);
            return Service.START_NOT_STICKY;
        }

        if (ACTION_RESTART.equals(action)) {
            DroverPreferences.setRunDesired(this, true);
            startInForeground();
            requestRestart();
            return restartMode();
        }

        if (ACTION_REFRESH_POLICY.equals(action)) {
            Thread thread = tunnelThread;
            if (thread == null || !thread.isAlive()) {
                stopSelf(startId);
                return Service.START_NOT_STICKY;
            }
            return restartMode();
        }

        boolean restartedByAndroid = intent == null;
        boolean restartWasRequested = isAlwaysOn()
                || (DroverPreferences.isAutoStartOnBootEnabled(this)
                && DroverPreferences.isRunDesired(this));
        if (restartedByAndroid && !restartWasRequested) {
            stopSelf(startId);
            return Service.START_NOT_STICKY;
        }

        if (ACTION_START.equals(action) || isAlwaysOn()) {
            DroverPreferences.setRunDesired(this, true);
        }

        startInForeground();
        startTunnelIfNeeded();
        return restartMode();
    }

    @Override
    public void onRevoke() {
        synchronized (this) {
            restartRequested = false;
        }
        requestStop(true);
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        // Preserve the desired state if Android is reclaiming a sticky service;
        // only an explicit stop or VPN revocation clears it. Never let this
        // destroyed instance create another local worker; START_STICKY or
        // Always-on must create a fresh service instance instead.
        synchronized (this) {
            destroyed = true;
            restartRequested = false;
        }
        requestStop(false);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    private synchronized void startTunnelIfNeeded() {
        if (destroyed) {
            return;
        }
        if (tunnelThread != null && tunnelThread.isAlive()) {
            if (stopRequested.get() && DroverPreferences.isRunDesired(this)) {
                restartRequested = true;
            }
            return;
        }

        stopRequested.set(false);
        explicitStopRequested = false;
        TunnelState.set(this, TunnelState.STARTING, "");
        tunnelThread = new Thread(this::runTunnel, "drover-tunnel");
        tunnelThread.start();
    }

    private void runTunnel() {
        boolean unexpectedFailure = false;
        try {
            List<String> installedPackages = DiscordPackages.findInstalled(this);
            String selectedPackage = DiscordPackages.resolveSelected(this, installedPackages);
            if (selectedPackage == null || !installedPackages.contains(selectedPackage)) {
                throw new IllegalStateException(getString(R.string.discord_selection_missing));
            }

            Builder builder = new Builder()
                    .setSession(getString(R.string.app_name))
                    .setMtu(MTU)
                    .addAddress("10.77.0.1", 32)
                    .addRoute("0.0.0.0", 0)
                    .addAddress("fd42:4242:4242::1", 128)
                    .addRoute("::", 0)
                    .addDnsServer("1.1.1.1");

            // An empty allow-list would capture every app. Establish the VPN only
            // after adding the single explicitly selected Discord installation.
            builder.addAllowedApplication(selectedPackage);
            builder.setMetered(false);

            tunInterface = builder.establish();
            if (tunInterface == null) {
                throw new IllegalStateException(getString(R.string.vpn_establish_failed));
            }

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
                    + " --verbosity warn";

            if (stopRequested.get()) {
                return;
            }

            int result = Tun2proxy.run(
                    arguments,
                    (char) MTU,
                    () -> markTunnelReady(selectedPackage)
            );
            if (!stopRequested.get()) {
                throw new IllegalStateException(getString(R.string.engine_stopped, result));
            }
        } catch (Throwable error) {
            Log.e(TAG, "Tunnel failed", error);
            synchronized (this) {
                if (!stopRequested.get() && ownsServiceGeneration()) {
                    // Mark this worker as exiting before publishing ERROR. A START
                    // delivered during unwind will then queue a real restart.
                    unexpectedFailure = true;
                    stopRequested.set(true);
                    DroverPreferences.setRunDesired(this, false);
                    TunnelState.set(this, TunnelState.ERROR, safeMessage(error));
                }
            }
        } finally {
            closeTunInterface();
            synchronized (this) {
                tunnelThread = null;
                boolean shouldRestart = !destroyed
                        && ownsServiceGeneration()
                        && stopRequested.get()
                        && restartRequested
                        && DroverPreferences.isRunDesired(this);
                restartRequested = false;
                if (shouldRestart) {
                    startTunnelIfNeeded();
                    return;
                }

                if (ownsServiceGeneration()
                        && stopRequested.get()
                        && (!unexpectedFailure || explicitStopRequested)) {
                    TunnelState.set(this, TunnelState.STOPPED, "");
                }
                // Prevent onDestroy() from replacing a useful error state after
                // this worker has already finished.
                stopRequested.set(true);
                if (ownsServiceGeneration()) {
                    stopForeground(STOP_FOREGROUND_REMOVE);
                    stopSelfResult(latestStartId);
                }
            }
        }
    }

    private synchronized void markTunnelReady(String selectedPackage) {
        // The callback is emitted by Rust only after the async TUN/ipstack loop is
        // initialized. Keep the readiness transition ordered with STOP/RESTART.
        if (destroyed || stopRequested.get() || !ownsServiceGeneration()) {
            // STOP can race with a replacement service waiting for the previous
            // native generation to unwind. Once this callback proves that the
            // replacement registered, cancel that generation as well.
            try {
                Tun2proxy.stop();
            } catch (Throwable error) {
                Log.w(TAG, "Failed to cancel a superseded native tunnel", error);
            }
            return;
        }
        TunnelState.setRunning(this, selectedPackage);
    }

    private synchronized void requestRestart() {
        if (destroyed) {
            return;
        }
        Thread thread = tunnelThread;
        if (thread == null || !thread.isAlive()) {
            restartRequested = false;
            startTunnelIfNeeded();
            return;
        }
        restartRequested = true;
        requestStop(false, true);
    }

    private synchronized void requestStop(boolean clearDesiredState) {
        requestStop(clearDesiredState, false);
    }

    /** Caller holds this service instance's monitor. */
    private void requestStop(boolean clearDesiredState, boolean forRestart) {
        if (clearDesiredState) {
            DroverPreferences.setRunDesired(this, false);
            explicitStopRequested = true;
        }
        Thread thread = tunnelThread;
        if (!stopRequested.compareAndSet(false, true)) {
            if (clearDesiredState && thread != null && thread.isAlive()) {
                TunnelState.set(this, TunnelState.STOPPING, "");
            } else if (clearDesiredState && (thread == null || !thread.isAlive())) {
                closeTunInterface();
                if (ownsServiceGeneration()) {
                    TunnelState.set(this, TunnelState.STOPPED, "");
                    stopForeground(STOP_FOREGROUND_REMOVE);
                    stopSelfResult(latestStartId);
                }
            }
            return;
        }

        if (thread != null && thread.isAlive()) {
            TunnelState.set(
                    this,
                    forRestart ? TunnelState.RESTARTING : TunnelState.STOPPING,
                    ""
            );
        }
        try {
            int result = Tun2proxy.stop();
            if (result != 0) {
                Log.w(TAG, "Native tunnel stop returned " + result);
            }
        } catch (Throwable error) {
            Log.w(TAG, "Native tunnel was not running", error);
        }

        if (thread == null || !thread.isAlive()) {
            // There is no native borrower in this branch, so Java can close its
            // descriptor immediately. While a worker is alive, runTunnel's finally
            // block closes it only after Tun2proxy.run() has returned: tun2proxy uses
            // the borrowed raw fd without dup(), and closing it earlier could let a
            // replacement VPN reuse the same descriptor number during native unwind.
            closeTunInterface();
            if (ownsServiceGeneration()) {
                TunnelState.set(this, TunnelState.STOPPED, "");
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelfResult(latestStartId);
            }
        }
    }

    private int restartMode() {
        boolean restartRequested = isAlwaysOn()
                || (DroverPreferences.isAutoStartOnBootEnabled(this)
                && DroverPreferences.isRunDesired(this));
        return restartRequested ? Service.START_STICKY : Service.START_NOT_STICKY;
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
        Intent openIntent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
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
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private static String safeMessage(Throwable error) {
        String detail = error.getMessage();
        if (detail == null || detail.trim().isEmpty()) {
            return error.getClass().getSimpleName();
        }
        return detail;
    }

    private boolean ownsServiceGeneration() {
        return ACTIVE_SERVICE_GENERATION.get() == serviceGeneration;
    }
}

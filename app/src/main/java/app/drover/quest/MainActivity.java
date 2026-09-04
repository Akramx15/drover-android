package app.drover.quest;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public final class MainActivity extends Activity {
    private static final int REQUEST_VPN = 7001;
    private static final int REQUEST_NOTIFICATIONS = 7002;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshTask = new Runnable() {
        @Override
        public void run() {
            refreshUi();
            handler.postDelayed(this, 500);
        }
    };

    private TextView discordStatus;
    private TextView tunnelStatus;
    private Button startButton;
    private Button stopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        discordStatus = findViewById(R.id.discord_status);
        tunnelStatus = findViewById(R.id.tunnel_status);
        startButton = findViewById(R.id.start_button);
        stopButton = findViewById(R.id.stop_button);

        startButton.setOnClickListener(view -> requestVpnAndStart());
        stopButton.setOnClickListener(view -> stopDrover());

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(refreshTask);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refreshTask);
        super.onPause();
    }

    private void requestVpnAndStart() {
        if (DiscordPackages.findInstalled(this).isEmpty()) {
            Toast.makeText(this, R.string.discord_missing, Toast.LENGTH_LONG).show();
            return;
        }

        Intent permissionIntent = VpnService.prepare(this);
        if (permissionIntent != null) {
            startActivityForResult(permissionIntent, REQUEST_VPN);
        } else {
            startDroverService();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_VPN) {
            return;
        }
        if (resultCode == RESULT_OK) {
            startDroverService();
        } else {
            Toast.makeText(this, R.string.vpn_permission_denied, Toast.LENGTH_LONG).show();
        }
    }

    private void startDroverService() {
        Intent intent = new Intent(this, DroverVpnService.class)
                .setAction(DroverVpnService.ACTION_START);
        startForegroundService(intent);
        TunnelState.set(this, TunnelState.STARTING, "");
        refreshUi();
    }

    private void stopDrover() {
        Intent intent = new Intent(this, DroverVpnService.class)
                .setAction(DroverVpnService.ACTION_STOP);
        startService(intent);
        TunnelState.set(this, TunnelState.STOPPING, "");
        refreshUi();
    }

    private void refreshUi() {
        List<String> packages = DiscordPackages.findInstalled(this);
        if (packages.isEmpty()) {
            discordStatus.setText(R.string.discord_missing);
        } else {
            discordStatus.setText(getString(R.string.discord_found, String.join(", ", packages)));
        }

        TunnelState.Snapshot snapshot = TunnelState.get(this);
        switch (snapshot.state) {
            case TunnelState.STARTING:
                tunnelStatus.setText(R.string.status_starting);
                break;
            case TunnelState.RUNNING:
                tunnelStatus.setText(R.string.status_running);
                break;
            case TunnelState.STOPPING:
                tunnelStatus.setText(R.string.status_stopping);
                break;
            case TunnelState.ERROR:
                tunnelStatus.setText(getString(R.string.status_error, snapshot.detail));
                break;
            case TunnelState.STOPPED:
            default:
                tunnelStatus.setText(R.string.status_stopped);
                break;
        }

        boolean busy = TunnelState.STARTING.equals(snapshot.state)
                || TunnelState.STOPPING.equals(snapshot.state);
        boolean active = TunnelState.RUNNING.equals(snapshot.state)
                || TunnelState.STARTING.equals(snapshot.state);
        startButton.setEnabled(!busy && !active && !packages.isEmpty());
        stopButton.setEnabled(!busy && active);
        startButton.setAlpha(startButton.isEnabled() ? 1.0f : 0.45f);
        stopButton.setAlpha(stopButton.isEnabled() ? 1.0f : 0.45f);
    }
}

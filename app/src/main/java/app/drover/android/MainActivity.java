package app.drover.android;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Insets;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MainActivity extends Activity {
    private static final String TAG = "DroverAndroid";
    private static final int REQUEST_VPN = 7001;
    private static final int REQUEST_NOTIFICATIONS = 7002;
    private static final int REQUEST_EXPORT_TRACE = 7003;

    private static final int VPN_ACTION_NONE = 0;
    private static final int VPN_ACTION_START = 1;
    private static final int VPN_ACTION_ENABLE_AUTO_START = 2;

    private static final String STATE_PENDING_OPEN = "pending_open";
    private static final String STATE_PENDING_VPN_ACTION = "pending_vpn_action";
    private static final String STATE_PENDING_NOTIFICATION_START =
            "pending_notification_start";
    private static final String STATE_SERVICE_START_DISPATCHED =
            "service_start_dispatched";

    private final SharedPreferences.OnSharedPreferenceChangeListener stateListener =
            (preferences, key) -> runOnUiThread(this::onTunnelStateChanged);

    private TextView appStatus;
    private TextView tunnelStatus;
    private Button primaryButton;
    private Button stopButton;
    private Button targetChoiceButton;
    private Button appChoiceButton;
    private Button exportTraceButton;
    private Button vpnSettingsButton;
    private Button licensesButton;
    private Switch autoStartSwitch;

    private List<String> installedPackages = Collections.emptyList();
    private AppKind selectedKind = AppKind.DISCORD;
    private String selectedPackage;
    private boolean syncingAutoStartSwitch;
    private boolean pendingOpenAfterStart;
    private boolean pendingStartAfterNotificationPermission;
    private boolean pendingServiceStartDispatched;
    private int pendingVpnAction = VPN_ACTION_NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        applySystemBarInsets(findViewById(R.id.root_scroll));

        appStatus = findViewById(R.id.app_status);
        tunnelStatus = findViewById(R.id.tunnel_status);
        primaryButton = findViewById(R.id.primary_button);
        stopButton = findViewById(R.id.stop_button);
        targetChoiceButton = findViewById(R.id.target_choice_button);
        appChoiceButton = findViewById(R.id.app_choice_button);
        exportTraceButton = findViewById(R.id.export_trace_button);
        vpnSettingsButton = findViewById(R.id.vpn_settings_button);
        licensesButton = findViewById(R.id.licenses_button);
        autoStartSwitch = findViewById(R.id.auto_start_switch);

        if (savedInstanceState != null) {
            pendingOpenAfterStart = savedInstanceState.getBoolean(STATE_PENDING_OPEN, false);
            pendingVpnAction = savedInstanceState.getInt(
                    STATE_PENDING_VPN_ACTION,
                    VPN_ACTION_NONE
            );
            pendingStartAfterNotificationPermission = savedInstanceState.getBoolean(
                    STATE_PENDING_NOTIFICATION_START,
                    false
            );
            pendingServiceStartDispatched = savedInstanceState.getBoolean(
                    STATE_SERVICE_START_DISPATCHED,
                    false
            );
        }

        primaryButton.setOnClickListener(view -> onPrimaryAction());
        stopButton.setOnClickListener(view -> stopDrover());
        targetChoiceButton.setOnClickListener(view -> showTargetChooser());
        appChoiceButton.setOnClickListener(view -> showAppChooser(false));
        exportTraceButton.setOnClickListener(view -> exportTrace());
        vpnSettingsButton.setOnClickListener(view -> openVpnSettings());
        licensesButton.setOnClickListener(view -> startActivity(
                new Intent(this, ThirdPartyLicensesActivity.class)
        ));
        autoStartSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (!syncingAutoStartSwitch) {
                onAutoStartChanged(checked);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        TunnelState.registerListener(this, stateListener);
        refreshSelectedPackages();
        refreshUi();
        handlePendingState(TunnelState.get(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Package installation state can change while Drover is in the background.
        refreshSelectedPackages();
        refreshUi();
    }

    @Override
    protected void onStop() {
        TunnelState.unregisterListener(this, stateListener);
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_PENDING_OPEN, pendingOpenAfterStart);
        outState.putInt(STATE_PENDING_VPN_ACTION, pendingVpnAction);
        outState.putBoolean(
                STATE_PENDING_NOTIFICATION_START,
                pendingStartAfterNotificationPermission
        );
        outState.putBoolean(STATE_SERVICE_START_DISPATCHED, pendingServiceStartDispatched);
        super.onSaveInstanceState(outState);
    }

    private void onPrimaryAction() {
        refreshSelectedPackages();
        if (installedPackages.isEmpty()) {
            showMissingAppToast();
            return;
        }
        if (selectedPackage == null) {
            showAppChooser(true);
            return;
        }

        TunnelState.Snapshot snapshot = TunnelState.get(this);
        if (TunnelState.RUNNING.equals(snapshot.state)) {
            if (!selectedPackage.equals(snapshot.activePackage)) {
                restartDrover(true);
            } else {
                launchSelectedApp();
            }
        } else if (TunnelState.STARTING.equals(snapshot.state)
                || TunnelState.RESTARTING.equals(snapshot.state)) {
            pendingOpenAfterStart = true;
        } else if (!TunnelState.STOPPING.equals(snapshot.state)) {
            ensureNotificationPermissionThenStart(true);
        }
    }

    private void ensureNotificationPermissionThenStart(boolean openAfterStart) {
        pendingOpenAfterStart = pendingOpenAfterStart || openAfterStart;
        if (requestNotificationPermissionIfNeeded(true)) {
            return;
        }
        ensureVpnPermissionAndStart(openAfterStart);
    }

    private void ensureVpnPermissionAndStart(boolean openAfterStart) {
        pendingOpenAfterStart = pendingOpenAfterStart || openAfterStart;
        Intent permissionIntent = VpnService.prepare(this);
        if (permissionIntent != null) {
            if (!launchVpnPermission(permissionIntent, VPN_ACTION_START)) {
                pendingOpenAfterStart = false;
                TunnelState.set(
                        this,
                        TunnelState.ERROR,
                        getString(R.string.vpn_permission_screen_unavailable)
                );
            }
        } else {
            startDroverService();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EXPORT_TRACE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                copyTraceTo(data.getData());
            }
            return;
        }
        if (requestCode != REQUEST_VPN) {
            return;
        }

        int completedAction = pendingVpnAction;
        pendingVpnAction = VPN_ACTION_NONE;
        if (resultCode != RESULT_OK) {
            pendingOpenAfterStart = false;
            if (completedAction == VPN_ACTION_ENABLE_AUTO_START) {
                setAutoStartSwitchChecked(false);
            }
            Toast.makeText(this, R.string.vpn_permission_denied, Toast.LENGTH_LONG).show();
            return;
        }

        if (completedAction == VPN_ACTION_ENABLE_AUTO_START) {
            DroverPreferences.setAutoStartOnBootEnabled(this, true);
            setAutoStartSwitchChecked(true);
            requestNotificationPermissionIfNeeded(false);
        } else if (completedAction == VPN_ACTION_START) {
            startDroverService();
        }
        refreshUi();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_NOTIFICATIONS
                || !pendingStartAfterNotificationPermission) {
            return;
        }

        pendingStartAfterNotificationPermission = false;
        // Notification denial must not disable the local VPN. Android still
        // exposes the foreground service in its active-apps UI, and the user
        // can enable drawer notifications later in system settings.
        ensureVpnPermissionAndStart(pendingOpenAfterStart);
    }

    private void startDroverService() {
        if (selectedPackage == null || !installedPackages.contains(selectedPackage)) {
            refreshSelectedPackages();
        }
        if (selectedPackage == null) {
            pendingOpenAfterStart = false;
            refreshUi();
            return;
        }

        Intent intent = new Intent(this, DroverVpnService.class)
                .setAction(DroverVpnService.ACTION_START);
        pendingServiceStartDispatched = true;
        try {
            startForegroundService(intent);
        } catch (RuntimeException error) {
            pendingServiceStartDispatched = false;
            pendingOpenAfterStart = false;
            TunnelState.set(this, TunnelState.ERROR, safeMessage(error));
        }
    }

    private void stopDrover() {
        pendingOpenAfterStart = false;
        pendingServiceStartDispatched = false;
        Intent intent = new Intent(this, DroverVpnService.class)
                .setAction(DroverVpnService.ACTION_STOP);
        try {
            startService(intent);
        } catch (RuntimeException error) {
            TunnelState.set(this, TunnelState.ERROR, safeMessage(error));
        }
    }

    private void restartDrover(boolean openAfterStart) {
        pendingOpenAfterStart = openAfterStart;
        Intent intent = new Intent(this, DroverVpnService.class)
                .setAction(DroverVpnService.ACTION_RESTART);
        try {
            startService(intent);
        } catch (RuntimeException error) {
            pendingOpenAfterStart = false;
            TunnelState.set(this, TunnelState.ERROR, safeMessage(error));
        }
    }

    private void onAutoStartChanged(boolean enabled) {
        if (!enabled) {
            DroverPreferences.setAutoStartOnBootEnabled(this, false);
            refreshServiceRestartPolicy();
            return;
        }

        Intent permissionIntent = VpnService.prepare(this);
        if (permissionIntent != null) {
            setAutoStartSwitchChecked(false);
            launchVpnPermission(permissionIntent, VPN_ACTION_ENABLE_AUTO_START);
            return;
        }

        DroverPreferences.setAutoStartOnBootEnabled(this, true);
        requestNotificationPermissionIfNeeded(false);
        refreshServiceRestartPolicy();
    }

    private boolean launchVpnPermission(Intent permissionIntent, int action) {
        pendingVpnAction = action;
        try {
            startActivityForResult(permissionIntent, REQUEST_VPN);
            return true;
        } catch (RuntimeException error) {
            Log.w(TAG, "Android VPN permission screen is unavailable", error);
            pendingVpnAction = VPN_ACTION_NONE;
            pendingStartAfterNotificationPermission = false;
            pendingServiceStartDispatched = false;
            Toast.makeText(
                    this,
                    R.string.vpn_permission_screen_unavailable,
                    Toast.LENGTH_LONG
            ).show();
            return false;
        }
    }

    private void refreshServiceRestartPolicy() {
        TunnelState.Snapshot snapshot = TunnelState.get(this);
        if (!TunnelState.RUNNING.equals(snapshot.state)
                && !TunnelState.STARTING.equals(snapshot.state)
                && !TunnelState.RESTARTING.equals(snapshot.state)) {
            return;
        }
        Intent intent = new Intent(this, DroverVpnService.class)
                .setAction(DroverVpnService.ACTION_REFRESH_POLICY);
        startService(intent);
    }

    private boolean requestNotificationPermissionIfNeeded(boolean continueStart) {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
                && !DroverPreferences.wasNotificationPermissionRequested(this)) {
            DroverPreferences.markNotificationPermissionRequested(this);
            pendingStartAfterNotificationPermission = continueStart;
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS
            );
            return true;
        }
        return false;
    }

    private void refreshSelectedPackages() {
        selectedKind = DroverPreferences.getSelectedAppKind(this);
        installedPackages = SupportedApps.findInstalled(this, selectedKind);
        selectedPackage = SupportedApps.resolveSelected(
                this,
                selectedKind,
                installedPackages
        );
    }

    private void showTargetChooser() {
        AppKind[] choices = {AppKind.DISCORD, AppKind.WHATSAPP};
        CharSequence[] labels = new CharSequence[choices.length];
        int checkedItem = 0;
        for (int index = 0; index < choices.length; index++) {
            labels[index] = getString(choices[index].displayNameResource);
            if (choices[index] == selectedKind) {
                checkedItem = index;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_target_title)
                .setSingleChoiceItems(labels, checkedItem, (dialog, which) -> {
                    dialog.dismiss();
                    selectAppKind(choices[which]);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void selectAppKind(AppKind kind) {
        AppKind previousKind = selectedKind;
        String previousPackage = selectedPackage;
        DroverPreferences.setSelectedAppKind(this, kind);
        if (kind == AppKind.WHATSAPP) {
            // A diagnostic trace must always begin with an explicit tap.
            DroverPreferences.setAutoStartOnBootEnabled(this, false);
        }
        refreshSelectedPackages();
        refreshUi();

        TunnelState.Snapshot snapshot = TunnelState.get(this);
        boolean active = TunnelState.RUNNING.equals(snapshot.state)
                || TunnelState.STARTING.equals(snapshot.state);
        if (!active || (kind == previousKind && same(selectedPackage, previousPackage))) {
            return;
        }
        if (kind == AppKind.WHATSAPP) {
            // Selecting an observe-only target is not consent to begin a new
            // diagnostic session. Stop any Discord tunnel and require the
            // user to tap the explicit start action for WhatsApp.
            stopDrover();
            return;
        }
        if (selectedPackage == null) {
            stopDrover();
        } else {
            restartDrover(false);
        }
    }

    private void showAppChooser(boolean continuePrimaryAction) {
        refreshSelectedPackages();
        if (installedPackages.isEmpty()) {
            showMissingAppToast();
            return;
        }
        if (installedPackages.size() == 1) {
            selectPackage(installedPackages.get(0), continuePrimaryAction);
            return;
        }

        final List<String> choices = new ArrayList<>(installedPackages);
        CharSequence[] labels = new CharSequence[choices.size()];
        int checkedItem = -1;
        for (int index = 0; index < choices.size(); index++) {
            String packageName = choices.get(index);
            labels[index] = SupportedApps.displayName(this, packageName);
            if (packageName.equals(selectedPackage)) {
                checkedItem = index;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(
                        R.string.choose_app_title,
                        getString(selectedKind.displayNameResource)
                ))
                .setSingleChoiceItems(labels, checkedItem, (dialog, which) -> {
                    dialog.dismiss();
                    selectPackage(choices.get(which), continuePrimaryAction);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void selectPackage(String packageName, boolean continuePrimaryAction) {
        String previousPackage = selectedPackage;
        DroverPreferences.setSelectedPackage(this, selectedKind, packageName);
        selectedPackage = packageName;
        refreshUi();

        TunnelState.Snapshot snapshot = TunnelState.get(this);
        boolean active = TunnelState.RUNNING.equals(snapshot.state)
                || TunnelState.STARTING.equals(snapshot.state);
        if (active && !packageName.equals(previousPackage)) {
            if (selectedKind == AppKind.WHATSAPP && !continuePrimaryAction) {
                // Changing the observed installation while a trace is active
                // ends the session. A fresh one needs another explicit tap.
                stopDrover();
            } else {
                restartDrover(continuePrimaryAction);
            }
        } else if (continuePrimaryAction) {
            onPrimaryAction();
        }
    }

    private void onTunnelStateChanged() {
        TunnelState.Snapshot snapshot = TunnelState.get(this);
        refreshUi(snapshot);
        handlePendingState(snapshot);
    }

    private void handlePendingState(TunnelState.Snapshot snapshot) {
        boolean awaitingPermissionResult = pendingVpnAction != VPN_ACTION_NONE
                || pendingStartAfterNotificationPermission;
        if (pendingOpenAfterStart && TunnelState.RUNNING.equals(snapshot.state)) {
            pendingServiceStartDispatched = false;
            pendingOpenAfterStart = false;
            launchSelectedApp();
        } else if (TunnelState.STARTING.equals(snapshot.state)
                || TunnelState.RESTARTING.equals(snapshot.state)) {
            // The service has acknowledged the command. A later STOPPED state is
            // now final rather than the pre-command state seen during rotation.
            pendingServiceStartDispatched = false;
        } else if (pendingOpenAfterStart && !awaitingPermissionResult
                && (TunnelState.ERROR.equals(snapshot.state)
                || TunnelState.STOPPING.equals(snapshot.state)
                || (TunnelState.STOPPED.equals(snapshot.state)
                && !pendingServiceStartDispatched))) {
            pendingServiceStartDispatched = false;
            pendingOpenAfterStart = false;
        }
    }

    private void launchSelectedApp() {
        refreshSelectedPackages();
        if (selectedPackage == null) {
            showAppChooser(true);
            return;
        }
        TunnelState.Snapshot snapshot = TunnelState.get(this);
        if (TunnelState.RUNNING.equals(snapshot.state)
                && !selectedPackage.equals(snapshot.activePackage)) {
            restartDrover(true);
            return;
        }
        if (!SupportedApps.launch(this, selectedPackage)) {
            refreshSelectedPackages();
            Toast.makeText(
                    this,
                    getString(
                            R.string.app_launch_failed,
                            getString(selectedKind.displayNameResource)
                    ),
                    Toast.LENGTH_LONG
            ).show();
            refreshUi();
        }
    }

    private void exportTrace() {
        File traceFile = DroverVpnService.labTraceFile(this);
        if (!traceFile.isFile() || traceFile.length() == 0) {
            Toast.makeText(this, R.string.trace_not_ready, Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/x-ndjson")
                .putExtra(Intent.EXTRA_TITLE, DroverVpnService.LAB_TRACE_FILE_NAME);
        try {
            startActivityForResult(intent, REQUEST_EXPORT_TRACE);
        } catch (RuntimeException error) {
            Log.w(TAG, "Android document picker is unavailable", error);
            Toast.makeText(this, R.string.trace_export_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    private void copyTraceTo(Uri destination) {
        File traceFile = DroverVpnService.labTraceFile(this);
        try (FileInputStream input = new FileInputStream(traceFile);
             OutputStream output = getContentResolver().openOutputStream(destination, "wt")) {
            if (output == null) {
                throw new IOException("The selected destination could not be opened");
            }
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            output.flush();
            Toast.makeText(this, R.string.trace_exported, Toast.LENGTH_LONG).show();
        } catch (IOException | RuntimeException error) {
            Log.w(TAG, "Could not export lab trace", error);
            Toast.makeText(this, R.string.trace_export_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void showMissingAppToast() {
        Toast.makeText(
                this,
                getString(
                        R.string.app_missing,
                        getString(selectedKind.displayNameResource)
                ),
                Toast.LENGTH_LONG
        ).show();
    }

    private void openVpnSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_VPN_SETTINGS));
        } catch (RuntimeException error) {
            Toast.makeText(this, R.string.vpn_settings_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    private boolean areVpnSettingsAvailable() {
        return new Intent(Settings.ACTION_VPN_SETTINGS)
                .resolveActivity(getPackageManager()) != null;
    }

    private void refreshUi() {
        refreshUi(TunnelState.get(this));
    }

    private void refreshUi(TunnelState.Snapshot snapshot) {
        String appName = getString(selectedKind.displayNameResource);
        targetChoiceButton.setText(getString(R.string.selected_target, appName));
        if (installedPackages.isEmpty()) {
            appStatus.setText(getString(R.string.app_missing, appName));
        } else if (selectedPackage == null) {
            appStatus.setText(getString(R.string.app_choose_required, appName));
        } else {
            appStatus.setText(getString(
                    R.string.app_selected,
                    SupportedApps.displayName(this, selectedPackage)
            ));
        }

        switch (snapshot.state) {
            case TunnelState.STARTING:
                tunnelStatus.setText(R.string.status_starting);
                primaryButton.setText(R.string.status_starting);
                break;
            case TunnelState.RESTARTING:
                tunnelStatus.setText(R.string.status_restarting);
                primaryButton.setText(R.string.status_restarting);
                break;
            case TunnelState.RUNNING:
                if (selectedPackage == null
                        || !selectedPackage.equals(snapshot.activePackage)) {
                    tunnelStatus.setText(R.string.status_restart_required);
                    primaryButton.setText(getString(R.string.restart_and_open_app, appName));
                } else {
                    tunnelStatus.setText(getString(R.string.status_running_app, appName));
                    primaryButton.setText(getString(R.string.open_app, appName));
                }
                break;
            case TunnelState.STOPPING:
                tunnelStatus.setText(R.string.status_stopping);
                primaryButton.setText(R.string.status_stopping);
                break;
            case TunnelState.ERROR:
                tunnelStatus.setText(getString(R.string.status_error, snapshot.detail));
                primaryButton.setText(getString(R.string.start_and_open_app, appName));
                break;
            case TunnelState.STOPPED:
            default:
                tunnelStatus.setText(R.string.status_stopped);
                primaryButton.setText(getString(R.string.start_and_open_app, appName));
                break;
        }

        boolean busy = TunnelState.STARTING.equals(snapshot.state)
                || TunnelState.RESTARTING.equals(snapshot.state)
                || TunnelState.STOPPING.equals(snapshot.state);
        boolean stoppable = TunnelState.RUNNING.equals(snapshot.state)
                || TunnelState.STARTING.equals(snapshot.state)
                || TunnelState.RESTARTING.equals(snapshot.state);
        primaryButton.setEnabled(!busy && !installedPackages.isEmpty());
        // STARTING/RESTARTING are deliberately cancellable. This is also the
        // only in-app escape hatch when notification permission was declined.
        stopButton.setEnabled(stoppable);
        targetChoiceButton.setEnabled(!busy);
        appChoiceButton.setVisibility(
                installedPackages.size() > 1 ? View.VISIBLE : View.GONE
        );
        appChoiceButton.setEnabled(!busy);
        File traceFile = DroverVpnService.labTraceFile(this);
        exportTraceButton.setVisibility(
                selectedKind == AppKind.WHATSAPP ? View.VISIBLE : View.GONE
        );
        exportTraceButton.setEnabled(
                selectedKind == AppKind.WHATSAPP
                        && !busy
                        && !stoppable
                        && traceFile.isFile()
                        && traceFile.length() > 0
        );
        vpnSettingsButton.setVisibility(
                areVpnSettingsAvailable() ? View.VISIBLE : View.GONE
        );

        primaryButton.setAlpha(primaryButton.isEnabled() ? 1.0f : 0.45f);
        stopButton.setAlpha(stopButton.isEnabled() ? 1.0f : 0.45f);
        exportTraceButton.setAlpha(exportTraceButton.isEnabled() ? 1.0f : 0.45f);
        appChoiceButton.setText(selectedPackage == null
                ? R.string.choose_app_button
                : R.string.change_app_button);

        boolean automaticStartAllowed = selectedKind != AppKind.WHATSAPP;
        if (!automaticStartAllowed) {
            DroverPreferences.setAutoStartOnBootEnabled(this, false);
        }
        autoStartSwitch.setEnabled(automaticStartAllowed);
        autoStartSwitch.setAlpha(automaticStartAllowed ? 1.0f : 0.45f);
        autoStartSwitch.setText(automaticStartAllowed
                ? R.string.auto_start_on_boot
                : R.string.auto_start_disabled_for_lab);
        setAutoStartSwitchChecked(automaticStartAllowed
                && DroverPreferences.isAutoStartOnBootEnabled(this));
    }

    private void setAutoStartSwitchChecked(boolean checked) {
        syncingAutoStartSwitch = true;
        autoStartSwitch.setChecked(checked);
        syncingAutoStartSwitch = false;
    }

    private static String safeMessage(Throwable error) {
        String detail = error.getMessage();
        if (detail == null || detail.trim().isEmpty()) {
            return error.getClass().getSimpleName();
        }
        return detail;
    }

    private static boolean same(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private static void applySystemBarInsets(View root) {
        root.setOnApplyWindowInsetsListener((view, windowInsets) -> {
            int left;
            int top;
            int right;
            int bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                Insets insets = windowInsets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()
                );
                left = insets.left;
                top = insets.top;
                right = insets.right;
                bottom = insets.bottom;
            } else {
                left = windowInsets.getSystemWindowInsetLeft();
                top = windowInsets.getSystemWindowInsetTop();
                right = windowInsets.getSystemWindowInsetRight();
                bottom = windowInsets.getSystemWindowInsetBottom();
            }
            view.setPadding(left, top, right, bottom);
            return windowInsets;
        });
        root.requestApplyInsets();
    }
}

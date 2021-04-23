package com.monitorapp.view;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import com.monitorapp.BuildConfig;
import com.monitorapp.R;
import com.monitorapp.services.MonitoringNotificationService;
import com.monitorapp.utils.ScreenRecorderHelper;
import com.monitorapp.utils.ZipUtils;


import org.jetbrains.annotations.NotNull;

import static com.monitorapp.services.MonitoringNotificationService.ACTION_START_SERVICE;
import static com.monitorapp.services.MonitoringNotificationService.ACTION_STOP_SERVICE;
import static com.monitorapp.utils.StorageUtils.csvMkdir;
import static com.monitorapp.utils.StorageUtils.getDataStoragePath;
import static com.monitorapp.utils.StorageUtils.getExternalStoragePath;
import static com.monitorapp.utils.StorageUtils.getZipStoragePath;
import static com.monitorapp.utils.StorageUtils.isCsvStorageReadable;
import static com.monitorapp.utils.StorageUtils.isCsvStorageWritable;
import static com.monitorapp.utils.StorageUtils.isZipStorageReadable;
import static com.monitorapp.utils.StorageUtils.isZipStorageWritable;
import static com.monitorapp.utils.StorageUtils.zipMkdir;
import static com.monitorapp.utils.ZipUtils.zipFileAtPath;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS_ALL = 101;

    private static final String[] PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private static final String TAG = "MainActivity";

    public static SwitchCompat switchSoundLevelMeter;
    public static SwitchCompat switchGyroscope;
    public static SwitchCompat switchAccelerometer;
    public static SwitchCompat switchGravity;
    public static SwitchCompat switchLight;
    public static SwitchCompat switchMagneticField;
    public static SwitchCompat switchScreenOnOff;
    public static SwitchCompat switchSms;
    public static SwitchCompat switchCall;
    public static SwitchCompat switchBattery;
    public static SwitchCompat switchAirplaneMode;
    public static SwitchCompat switchNetwork;
    public static SwitchCompat switchForegroundApp;

    public static MediaProjection mediaProjection;
    private static final int PERMISSION_CODE = 1;
    private MediaProjectionManager mProjectionManager;

    private static Button buttonStartStop;

    private static boolean buttonStartStopStatus = false;

    public static String PACKAGE_NAME;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PACKAGE_NAME = getApplicationContext().getPackageName();

        csvMkdir(this);
        zipMkdir(this);

        initUIComponents();

        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestPermissions();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            batteryOptimizations(getApplicationContext());
        }
    }

    private void initUIComponents() {
        Button buttonZip = findViewById(R.id.Button_zip);
        buttonZip.setOnClickListener(view -> {
                zipFileAtPath(getDataStoragePath(this), getZipStoragePath(this) + ScreenRecorderHelper.getCurSysDate() + ".zip", this);
            }
        );

        buttonStartStop = findViewById(R.id.Button_start_stop);
        buttonStartStop.setText(R.string.button_start);
        buttonStartStop.setOnClickListener(view -> {
            if (!buttonStartStopStatus) {
                startActivityForResult(mProjectionManager.createScreenCaptureIntent(), 1);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent startIntent = new Intent(getApplicationContext(), MonitoringNotificationService.class);
                        startIntent.setAction(ACTION_START_SERVICE);
                        startService(startIntent);
                    }
                }, 3000);
            } else {
                Intent stopIntent = new Intent(getApplicationContext(), MonitoringNotificationService.class);
                stopIntent.setAction(ACTION_STOP_SERVICE);
                startService(stopIntent);


            }
            changeButtonText();
        });

        Button buttonSend = findViewById(R.id.Button_send);
        buttonSend.setOnClickListener(view -> startActivity(new Intent(getApplicationContext(), DriveActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void batteryOptimizations(Context context) {
        Intent intent = new Intent();
        String packageName = getPackageName();
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PERMISSION_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this,
                    "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            return;
        }
        mediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
    }


    private void requestPermissions() {

        if (!hasSensorPermissions(PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE_PERMISSIONS_ALL);
        } else {
            checkMonitoringPermissions();
        }

        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission();
        }

        if (!hasReadWriteStorageAccess()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Cannot read/write data");
            builder.setMessage("Cannot read/write data from " + getExternalStoragePath(this)
                    + " which may cause problems with functioning of this app. Please contact app supplier for further help.");

            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            });
            builder.show();
        }
    }

    private boolean hasReadWriteStorageAccess() {
        return isCsvStorageReadable()
                && isCsvStorageWritable()
                && isZipStorageReadable()
                && isZipStorageWritable();
    }

    private void requestUsageStatsPermission() {
        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
    }

    private boolean hasPermissions() {
        return hasSensorPermissions(PERMISSIONS) && hasUsageStatsPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS_ALL) {
            checkMonitoringPermissions();
        }
    }

    public void changeButtonText() {

        if (!buttonStartStopStatus) {
            buttonStartStop.setText(R.string.button_stop);
            buttonStartStopStatus = true;
        } else {
            buttonStartStop.setText(R.string.button_start);
            buttonStartStopStatus = false;
        }
    }

    private boolean hasSensorPermissions(@NonNull String[] perms) {

        for (String permission : perms) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasUsageStatsPermission() {
        final AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());

        if (mode == AppOpsManager.MODE_ALLOWED) {
            return true;
        }

        appOpsManager.startWatchingMode(AppOpsManager.OPSTR_GET_USAGE_STATS,
                getApplicationContext().getPackageName(),
                new AppOpsManager.OnOpChangedListener() {
                    @Override
                    public void onOpChanged(String s, String s1) {
                        int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
                        if (mode != AppOpsManager.MODE_ALLOWED) {
                            return;
                        }
                        appOpsManager.stopWatchingMode(this);
                        Intent intent = new Intent(MainActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        getApplicationContext().startActivity(intent);
                    }
                });
        requestUsageStatsPermission();
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    public static boolean isAppRunning(@NotNull Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo task : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (context.getPackageName().equalsIgnoreCase(task.service.getPackageName()))
                return true;
        }
        return false;
    }

    private void checkMonitoringPermissions() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, ": onMonitoringPermissionsCheck");
        }

    }
}
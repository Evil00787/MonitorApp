package com.monitorapp.services;

import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.monitorapp.BuildConfig;
import com.monitorapp.R;
import com.monitorapp.db_utils.SQLExporter;
import com.monitorapp.view.MainActivity;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.monitorapp.enums.SensorType.TYPE_ACCELEROMETER;
import static com.monitorapp.enums.SensorType.TYPE_GRAVITY;
import static com.monitorapp.enums.SensorType.TYPE_GYROSCOPE;
import static com.monitorapp.enums.SensorType.TYPE_LIGHT;
import static com.monitorapp.enums.SensorType.TYPE_MAGNETIC_FIELD;
import static com.monitorapp.view.MainActivity.PACKAGE_NAME;
import static com.monitorapp.view.MainActivity.isAppRunning;
import static com.monitorapp.view.MainActivity.switchAccelerometer;
import static com.monitorapp.view.MainActivity.switchAirplaneMode;
import static com.monitorapp.view.MainActivity.switchBattery;
import static com.monitorapp.view.MainActivity.switchCall;
import static com.monitorapp.view.MainActivity.switchForegroundApp;
import static com.monitorapp.view.MainActivity.switchGravity;
import static com.monitorapp.view.MainActivity.switchGyroscope;
import static com.monitorapp.view.MainActivity.switchLight;
import static com.monitorapp.view.MainActivity.switchMagneticField;
import static com.monitorapp.view.MainActivity.switchNetwork;
import static com.monitorapp.view.MainActivity.switchScreenOnOff;
import static com.monitorapp.view.MainActivity.switchSms;
import static com.monitorapp.view.MainActivity.switchSoundLevelMeter;

public class MonitoringNotificationService extends Service {

    private static final int NOTIFICATION_ID = 999;

    public static final String ACTION_START_SERVICE = PACKAGE_NAME + ".ACTION_START_SERVICE";
    public static final String ACTION_STOP_SERVICE = PACKAGE_NAME + ".ACTION_STOP_SERVICE";
    private static final String TAG = "MonitoringNotifService";

    public static boolean isServiceRunning = false;

    private boolean ifSoundMonitoring;
    private boolean ifGyroMonitoring;
    private boolean ifAccelMonitoring;
    private boolean ifGravityMonitoring;
    private boolean ifLightMonitoring;
    private boolean ifMagnMonitoring;
    private boolean ifScreenMonitoring;
    private boolean ifSmsMonitoring;
    private boolean ifCallMonitoring;
    private boolean ifBattMonitoring;
    private boolean ifAirplMonitoring;
    private boolean ifNetwMonitoring;
    private boolean ifAppMonitoring;
    private boolean isAppRunning;
    private String delayString;

    Notification notification;



    @Override
    public void onCreate() {
        super.onCreate();
        loadUiState(this);
        startServiceWithNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && Objects.equals(intent.getAction(), ACTION_START_SERVICE)) {
            startServiceWithNotification();
        } else stopMyService();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        isServiceRunning = false;
        if (BuildConfig.DEBUG) {
            Log.d(TAG, ": destroyed");
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startServiceWithNotification() {
        if (isServiceRunning) {
            return;
        }

        isServiceRunning = true;

        onMonitoringStart();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setContentTitle("App is running in background")
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();

            startForeground(NOTIFICATION_ID, notification);
        } else {
            String NOTIFICATION_CHANNEL_ID = "com.MonitorApp";
            String channelName = "Background Service";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_stat_name))
                    .setContentTitle("App is running in background")
                    .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();

            startForeground(NOTIFICATION_ID, notification);
        }
    }



    private void stopMyService() {
        onMonitoringStop();
        stopForeground(true);
        stopSelf();
        isServiceRunning = false;
    }

    private void onMonitoringStart() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, ": onMonitoringStart");
        }

        isAppRunning = isAppRunning(this);

        /* SOUND LEVEL */
        if (ifSoundMonitoring) {
            startService(new Intent(getApplicationContext(), NoiseDetectorService.class));
        }

        /* GYROSCOPE */
        if (ifGyroMonitoring) {
            startService(new Intent(getApplicationContext(), SensorsService.class).putExtra("SENSOR_TYPE", TYPE_GYROSCOPE.getValue()));
        }

        /* ACCELEROMETER */
        if (ifAccelMonitoring) {
            startService(new Intent(getApplicationContext(), SensorsService.class).putExtra("SENSOR_TYPE", TYPE_ACCELEROMETER.getValue()));
        }

        /* GRAVITY */
        if (ifGravityMonitoring) {
            startService(new Intent(getApplicationContext(), SensorsService.class).putExtra("SENSOR_TYPE", TYPE_GRAVITY.getValue()));
        }

        /* LIGHT METER */
        if (ifLightMonitoring) {
            startService(new Intent(getApplicationContext(), SensorsService.class).putExtra("SENSOR_TYPE", TYPE_LIGHT.getValue()));
        }

        /* MAGNETIC FIELD */
        if (ifMagnMonitoring) {
            startService(new Intent(getApplicationContext(), SensorsService.class).putExtra("SENSOR_TYPE", TYPE_MAGNETIC_FIELD.getValue()));
        }

        /* SCREEN ON/OFF */
        if (ifScreenMonitoring) {
            startService(new Intent(getApplicationContext(), ScreenOnOffService.class));
        }

        /* SMS */
        if (ifSmsMonitoring) {
            startService(new Intent(getApplicationContext(), SmsService.class));
        }

        /* CALL */
        if (ifCallMonitoring) {
            startService(new Intent(getApplicationContext(), CallService.class));
        }

        /* BATTERY */
        if (ifBattMonitoring) {
            startService(new Intent(getApplicationContext(), BatteryService.class));
        }

        /* AIRPLANE MODE */
        if (ifAirplMonitoring) {
            startService(new Intent(getApplicationContext(), AirplaneModeService.class));
        }

        /* NETWORK */
        if (ifNetwMonitoring) {
            startService(new Intent(getApplicationContext(), NetworkService.class));
        }


        final Intent intent = new Intent(this, ScreenRecorderService.class);
        intent.addCategory("START");
        startService(intent);


        if (ifAppMonitoring) {
            long delay;
            if (delayString.isEmpty()) {
                if (isServiceRunning) {
                    postToastFromService("Empty delay field: app check started with delay default value of 5 seconds.");
                }
                startService(new Intent(getApplicationContext(), ForegroundAppService.class).putExtra("DELAY", 5));
            } else if (delayString.startsWith("-")) {
                if (isServiceRunning) {
                    postToastFromService("Negative delay specified: app check started with delay default value of 5 seconds.");
                }
                startService(new Intent(getApplicationContext(), ForegroundAppService.class).putExtra("DELAY", 5));
            } else if (delayString.startsWith("+")) {
                if (isServiceRunning) {
                    postToastFromService("Redundant plus character: app check started with delay value of " + delayString.substring(1) + " seconds.");
                }
                delay = Long.parseLong(delayString.substring(1));
                startService(new Intent(getApplicationContext(), ForegroundAppService.class).putExtra("DELAY", delay));
            } else if (delayString.equals("0") || delayString.equals("00")) {
                if (isServiceRunning) {
                    postToastFromService("Delay equals zero: app check started with delay default value of 5 seconds.");
                }
                startService(new Intent(getApplicationContext(), ForegroundAppService.class).putExtra("DELAY", 5));
            } else if (delayString.startsWith("0")) {
                delay = Long.parseLong(delayString);
                if (isServiceRunning) {
                    postToastFromService("App check started with delay value of " + delayString.substring(1) + " seconds.");
                }
                startService(new Intent(getApplicationContext(), ForegroundAppService.class).putExtra("DELAY", delay));
            } else {
                delay = Long.parseLong(delayString);
                if (isServiceRunning) {
                    postToastFromService("App check started with delay value of " + delayString + " seconds.");
                }
                startService(new Intent(getApplicationContext(), ForegroundAppService.class).putExtra("DELAY", delay));
            }
        }
    }

    private void onMonitoringStop() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, ": onMonitoringStop");
        }

        stopService(new Intent(getApplicationContext(), ScreenRecorderService.class));



        /* SOUND LEVEL */
        if (ifSoundMonitoring) {
            stopService(new Intent(getApplicationContext(), NoiseDetectorService.class));
        }

        /* GYROSCOPE */
        if (ifGyroMonitoring) {
            stopService(new Intent(getApplicationContext(), SensorsService.class).putExtra("SENSOR_TYPE", TYPE_GYROSCOPE.getValue()));
        }

        /* ACCELEROMETER */
        if (ifAccelMonitoring) {
            stopService(new Intent(getApplicationContext(), SensorsService.class).putExtra("SENSOR_TYPE", TYPE_ACCELEROMETER.getValue()));
        }

        /* GRAVITY */
        if (ifGravityMonitoring) {
            stopService(new Intent(getApplicationContext(), SensorsService.class).putExtra("SENSOR_TYPE", TYPE_GRAVITY.getValue()));
        }

        /* LIGHT METER */
        if (ifLightMonitoring) {
            stopService(new Intent(getApplicationContext(), SensorsService.class).putExtra("SENSOR_TYPE", TYPE_LIGHT.getValue()));
        }

        /* MAGNETIC FIELD */
        if (ifMagnMonitoring) {
            stopService(new Intent(getApplicationContext(), SensorsService.class).putExtra("SENSOR_TYPE", TYPE_MAGNETIC_FIELD.getValue()));
        }

        /* SCREEN ON/OFF */
        if (ifScreenMonitoring) {
            stopService(new Intent(getApplicationContext(), ScreenOnOffService.class));
        }

        /* SMS */
        if (ifSmsMonitoring) {
            stopService(new Intent(getApplicationContext(), SmsService.class));
        }

        /* CALL */
        if (ifCallMonitoring) {
            stopService(new Intent(getApplicationContext(), CallService.class));
        }

        /* BATTERY */
        if (ifBattMonitoring) {
            stopService(new Intent(getApplicationContext(), BatteryService.class));
        }

        /* AIRPLANE MODE */
        if (ifAirplMonitoring) {
            stopService(new Intent(getApplicationContext(), AirplaneModeService.class));
        }

        /* NETWORK */
        if (ifNetwMonitoring) {
            stopService(new Intent(getApplicationContext(), NetworkService.class));
        }



        if (ifAppMonitoring) {
            stopService(new Intent(getApplicationContext(), ForegroundAppService.class));
        }



        /* EXPORT TO CSV */
        try {
            Intent intent = new Intent(this, SQLExporter.class);
            startService(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void requestUsageStatsPermission() {
        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
    }

    private boolean hasUsageStatsPermission() {
        final AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());

        if (mode == AppOpsManager.MODE_ALLOWED) {
            return true;
        }

        appOpsManager.startWatchingMode(AppOpsManager.OPSTR_GET_USAGE_STATS, getApplicationContext().getPackageName(), new AppOpsManager.OnOpChangedListener() {
            @Override
            public void onOpChanged(String s, String s1) {
                int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
                if (mode != AppOpsManager.MODE_ALLOWED) {
                    return;
                }
                appOpsManager.stopWatchingMode(this);
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(intent);
            }
        });
        requestUsageStatsPermission();
        return false;
    }

    private void loadUiState(@NotNull Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PACKAGE_NAME, MODE_PRIVATE);
        ifSoundMonitoring = false;
        ifGyroMonitoring = true;
        ifAccelMonitoring = true;
        ifGravityMonitoring = true;
        ifLightMonitoring = false;
        ifMagnMonitoring = true;
        ifScreenMonitoring = true;
        ifSmsMonitoring = false;
        ifCallMonitoring = false;
        ifBattMonitoring = false;
        ifAirplMonitoring = false;
        ifNetwMonitoring = true;
        ifAppMonitoring = true;
        delayString = "2";
    }

    private void postToastFromService(final String message) {
        Handler handler = new Handler(getApplicationContext().getMainLooper());
        handler.post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show());
    }
}

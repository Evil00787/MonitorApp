package com.monitorapp.services;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import com.monitorapp.utils.ScreenRecorderHelper;

import androidx.annotation.Nullable;

import java.io.IOException;

import  com.monitorapp.view.MainActivity;

import static android.webkit.ConsoleMessage.MessageLevel.LOG;
import static com.monitorapp.services.MonitoringNotificationService.ACTION_START_SERVICE;


/**
 * Created by uzias on 10/3/16.
 */

public class ScreenRecorderService extends Service {

    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaProjectionCallback mediaProjectionCallback;
    private MediaRecorder mediaRecorder;
    private int displayWidth;
    private int mDisplayHeight;
    private int mDensityDpi;
    private WindowManager windowManager;
    private boolean isStarted = false;
    private static final int DISPLAY_WIDTH = 480;
    private static final int DISPLAY_HEIGHT = 640;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isStarted = false;
        mediaRecorder = new MediaRecorder();
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mDensityDpi = metrics.densityDpi;
        displayWidth = metrics.widthPixels;
        mDisplayHeight = metrics.heightPixels;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.hasCategory("START") && !isStarted) {
            initRecorder();
            isStarted = true;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRecorderAndScreen();
    }

    private void initRecorder() {
        try {
            if (mediaProjectionCallback == null){
                mediaProjectionCallback = new MediaProjectionCallback();
            }
            mediaProjection = MainActivity.mediaProjection;
            if (mediaProjection == null){
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                       initRecorder();
                    }
                }, 3000);
                hideRecordingViewStopServicesShowError();
                return;
            }
            mediaProjection.registerCallback(mediaProjectionCallback, null);
            mediaRecorder = ScreenRecorderHelper.configureRecorder(mediaRecorder, windowManager, DISPLAY_WIDTH, DISPLAY_HEIGHT, true, null, getApplicationContext());
            mediaRecorder.prepare();
            virtualDisplay = ScreenRecorderHelper.createVirtualDisplay(mediaProjection, mediaRecorder, this, DISPLAY_WIDTH, DISPLAY_HEIGHT, mDensityDpi);
            mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
            hideRecordingViewStopServicesShowError();
        }
    }

    private void destroyMediaProjection() {
        if (mediaProjection != null) {
            mediaProjection.unregisterCallback(mediaProjectionCallback);
            mediaProjection.stop();
            mediaProjection = null;
        }
    }

    private void stopRecorderAndScreen() {
        try {
            mediaRecorder.stop();
        }catch (Exception e){
            e.printStackTrace();
            hideRecordingViewStopServicesShowError();
        }
        mediaRecorder.reset();
        destroyMediaProjection();
        if (virtualDisplay != null) {
            virtualDisplay.release();
        }
    }

    private void hideRecordingViewStopServicesShowError(){
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            stopRecorderAndScreen();
            Log.println(Log.INFO, "ok,", "Ending");
        }
    }

}
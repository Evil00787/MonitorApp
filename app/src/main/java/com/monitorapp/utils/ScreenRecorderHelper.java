package com.monitorapp.utils;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.WindowManager;
import com.monitorapp.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenRecorderHelper {

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    public static String getFilePath(Context context) {
        final String directory = context.getExternalFilesDir(null).toString() + "/data/recordings/";
        final File folder = new File(directory);
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdir();
        }
        String filePath;
        if (success) {
            String videoName = ("capture_" + getCurSysDate() + ".mp4");
            filePath = directory + File.separator + videoName;
        } else {
            return null;
        }
        return filePath;
    }

    public static String getCurSysDate() {
        return new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
    }


    public static MediaRecorder configureRecorder(MediaRecorder mediaRecorder, WindowManager windowManager, int displayWidth, int displayHeight, boolean enableAudio, Integer sensorOrientation, Context context){
        if (enableAudio){
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        }
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(getFilePath(context));
        mediaRecorder.setVideoSize(displayWidth, displayHeight);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        if (enableAudio){
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        }
        mediaRecorder.setVideoEncodingBitRate(512 * 2000);
        mediaRecorder.setVideoFrameRate(10);


        int rotation = windowManager.getDefaultDisplay().getRotation();

        if (sensorOrientation != null){
            switch (sensorOrientation) {
                case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                    mediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                    break;
                case SENSOR_ORIENTATION_INVERSE_DEGREES:
                    mediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                    break;
            }
        }else{
            int orientation = DEFAULT_ORIENTATIONS.get(rotation + 90);
            mediaRecorder.setOrientationHint(orientation);
        }

        return mediaRecorder;
    }

    public static VirtualDisplay createVirtualDisplay(MediaProjection mediaProjection, MediaRecorder mediaRecorder, Context context, int displayWidth, int displayHeight, int density) {
        return mediaProjection.createVirtualDisplay(context.getString(R.string.app_name), displayWidth, displayHeight, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.getSurface(), null, null);
    }
}
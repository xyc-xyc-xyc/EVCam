package com.test.cam.camera;

import android.media.MediaRecorder;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;

/**
 * 视频录制管理类
 */
public class VideoRecorder {
    private static final String TAG = "VideoRecorder";

    private final String cameraId;
    private MediaRecorder mediaRecorder;
    private RecordCallback callback;
    private boolean isRecording = false;
    private String currentFilePath;

    public VideoRecorder(String cameraId) {
        this.cameraId = cameraId;
    }

    public void setCallback(RecordCallback callback) {
        this.callback = callback;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public Surface getSurface() {
        if (mediaRecorder != null) {
            return mediaRecorder.getSurface();
        }
        return null;
    }

    /**
     * 准备录制器
     */
    private void prepareMediaRecorder(String filePath, int width, int height) throws IOException {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(filePath);
        mediaRecorder.setVideoEncodingBitRate(1000000); // 降低到1Mbps以减少资源消耗
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(width, height);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.prepare();
    }

    /**
     * 开始录制
     */
    public boolean startRecording(String filePath, int width, int height) {
        try {
            prepareMediaRecorder(filePath, width, height);
            mediaRecorder.start();
            isRecording = true;
            currentFilePath = filePath;

            Log.d(TAG, "Camera " + cameraId + " started recording to: " + filePath);
            if (callback != null) {
                callback.onRecordStart(cameraId);
            }
            return true;

        } catch (IOException e) {
            Log.e(TAG, "Failed to start recording for camera " + cameraId, e);
            releaseMediaRecorder();
            if (callback != null) {
                callback.onRecordError(cameraId, e.getMessage());
            }
            return false;
        }
    }

    /**
     * 停止录制
     */
    public void stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "Camera " + cameraId + " is not recording");
            return;
        }

        try {
            mediaRecorder.stop();
            isRecording = false;

            Log.d(TAG, "Camera " + cameraId + " stopped recording: " + currentFilePath);
            if (callback != null) {
                callback.onRecordStop(cameraId);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to stop recording for camera " + cameraId, e);
        } finally {
            releaseMediaRecorder();
            currentFilePath = null;
        }
    }

    /**
     * 释放录制器
     */
    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        if (isRecording) {
            stopRecording();
        }
        releaseMediaRecorder();
    }
}

package com.test.cam.camera;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.view.TextureView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 四路摄像头管理器
 */
public class MultiCameraManager {
    private static final String TAG = "MultiCameraManager";

    private static final int DEFAULT_MAX_OPEN_CAMERAS = 4;
    private static final int RECORD_WIDTH = 1280;
    private static final int RECORD_HEIGHT = 800;

    private final Context context;
    private final Map<String, SingleCamera> cameras = new LinkedHashMap<>();
    private final Map<String, VideoRecorder> recorders = new LinkedHashMap<>();
    private final List<String> activeCameraKeys = new ArrayList<>();
    private int maxOpenCameras = DEFAULT_MAX_OPEN_CAMERAS;

    private boolean isRecording = false;
    private StatusCallback statusCallback;

    public interface StatusCallback {
        void onCameraStatusUpdate(String cameraId, String status);
    }

    public MultiCameraManager(Context context) {
        this.context = context;
    }

    public void setStatusCallback(StatusCallback callback) {
        this.statusCallback = callback;
    }

    public void setMaxOpenCameras(int maxOpenCameras) {
        this.maxOpenCameras = Math.max(1, maxOpenCameras);
    }

    /**
     * 初始化摄像头
     */
    public void initCameras(String frontId, TextureView frontView,
                           String backId, TextureView backView,
                           String leftId, TextureView leftView,
                           String rightId, TextureView rightView) {

        // 创建四个摄像头实例
        cameras.put("front", new SingleCamera(context, frontId, frontView));
        cameras.put("back", new SingleCamera(context, backId, backView));
        cameras.put("left", new SingleCamera(context, leftId, leftView));
        cameras.put("right", new SingleCamera(context, rightId, rightView));

        // 为每个摄像头设置回调
        CameraCallback callback = new CameraCallback() {
            @Override
            public void onCameraOpened(String cameraId) {
                Log.d(TAG, "Callback: Camera " + cameraId + " opened");
                if (statusCallback != null) {
                    statusCallback.onCameraStatusUpdate(cameraId, "已打开");
                }
            }

            @Override
            public void onCameraConfigured(String cameraId) {
                Log.d(TAG, "Callback: Camera " + cameraId + " configured");
                if (statusCallback != null) {
                    statusCallback.onCameraStatusUpdate(cameraId, "预览已启动");
                }
            }

            @Override
            public void onCameraClosed(String cameraId) {
                Log.d(TAG, "Callback: Camera " + cameraId + " closed");
                if (statusCallback != null) {
                    statusCallback.onCameraStatusUpdate(cameraId, "已关闭");
                }
            }

            @Override
            public void onCameraError(String cameraId, int errorCode) {
                String errorMsg = getErrorMessage(errorCode);
                Log.e(TAG, "Callback: Camera " + cameraId + " error: " + errorCode + " - " + errorMsg);
                if (statusCallback != null) {
                    statusCallback.onCameraStatusUpdate(cameraId, "错误: " + errorMsg);
                }
            }
        };

        cameras.get("front").setCallback(callback);
        cameras.get("back").setCallback(callback);
        cameras.get("left").setCallback(callback);
        cameras.get("right").setCallback(callback);

        // 创建四个录制器实例
        recorders.put("front", new VideoRecorder(frontId));
        recorders.put("back", new VideoRecorder(backId));
        recorders.put("left", new VideoRecorder(leftId));
        recorders.put("right", new VideoRecorder(rightId));

        Log.d(TAG, "Cameras initialized");
    }

    /**
     * 获取错误信息描述
     */
    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case 1: // ERROR_CAMERA_IN_USE
                return "摄像头正在被使用";
            case 2: // ERROR_MAX_CAMERAS_IN_USE
                return "已达到最大摄像头数量";
            case 3: // ERROR_CAMERA_DISABLED
                return "摄像头被禁用";
            case 4: // ERROR_CAMERA_DEVICE
                return "摄像头设备错误(资源不足?)";
            case 5: // ERROR_CAMERA_SERVICE
                return "摄像头服务错误";
            case -1:
                return "访问失败";
            case -2:
                return "权限不足";
            case -3:
                return "会话配置失败";
            case -4:
                return "摄像头断开连接(资源耗尽)";
            default:
                return "未知错误(" + errorCode + ")";
        }
    }

    /**
     * 打开所有摄像头
     */
    public void openAllCameras() {
        Log.d(TAG, "Opening all cameras...");

        activeCameraKeys.clear();
        int opened = 0;
        Set<String> openedIds = new HashSet<>();
        for (Map.Entry<String, SingleCamera> entry : cameras.entrySet()) {
            if (opened >= maxOpenCameras) {
                break;
            }
            SingleCamera camera = entry.getValue();
            String id = camera.getCameraId();
            if (!openedIds.add(id)) {
                continue;
            }
            activeCameraKeys.add(entry.getKey());
            camera.openCamera();
            opened++;
        }

        Log.d(TAG, "Requested open cameras: " + activeCameraKeys);
    }

    /**
     * 关闭所有摄像头
     */
    public void closeAllCameras() {
        for (SingleCamera camera : cameras.values()) {
            camera.closeCamera();
        }
        Log.d(TAG, "All cameras closed");
    }

    /**
     * 开始录制所有摄像头
     */
    public boolean startRecording() {
        if (isRecording) {
            Log.w(TAG, "Already recording");
            return false;
        }

        File saveDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "MultiCam");
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());

        List<String> keys = getActiveCameraKeys();
        if (keys.isEmpty()) {
            Log.e(TAG, "No active cameras for recording");
            return false;
        }

        boolean success = true;
        for (String key : keys) {
            VideoRecorder recorder = recorders.get(key);
            if (recorder == null) {
                continue;
            }
            String path = new File(saveDir, key + "_" + timestamp + ".mp4").getAbsolutePath();
            success &= recorder.startRecording(path, RECORD_WIDTH, RECORD_HEIGHT);
        }

        if (success) {
            for (String key : keys) {
                SingleCamera camera = cameras.get(key);
                VideoRecorder recorder = recorders.get(key);
                if (camera == null || recorder == null) {
                    continue;
                }
                camera.setRecordSurface(recorder.getSurface());
                camera.recreateSession();
            }

            isRecording = true;
            Log.d(TAG, "All cameras started recording");
        } else {
            Log.e(TAG, "Failed to start recording on some cameras");
            stopRecording();
        }

        return success;
    }

    /**
     * 停止录制所有摄像头
     */
    public void stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "Not recording");
            return;
        }

        List<String> keys = getActiveCameraKeys();
        for (String key : keys) {
            VideoRecorder recorder = recorders.get(key);
            if (recorder != null && recorder.isRecording()) {
                recorder.stopRecording();
            }
        }

        for (String key : keys) {
            SingleCamera camera = cameras.get(key);
            if (camera != null) {
                camera.clearRecordSurface();
                camera.recreateSession();
            }
        }

        isRecording = false;
        Log.d(TAG, "All cameras stopped recording");
    }

    /**
     * 释放所有资源
     */
    public void release() {
        stopRecording();
        closeAllCameras();

        for (VideoRecorder recorder : recorders.values()) {
            recorder.release();
        }

        cameras.clear();
        recorders.clear();
        Log.d(TAG, "All resources released");
    }

    /**
     * 是否正在录制
     */
    public boolean isRecording() {
        return isRecording;
    }

    private List<String> getActiveCameraKeys() {
        if (!activeCameraKeys.isEmpty()) {
            return new ArrayList<>(activeCameraKeys);
        }
        List<String> keys = new ArrayList<>();
        int opened = 0;
        Set<String> openedIds = new HashSet<>();
        for (Map.Entry<String, SingleCamera> entry : cameras.entrySet()) {
            if (opened >= maxOpenCameras) {
                break;
            }
            SingleCamera camera = entry.getValue();
            String id = camera.getCameraId();
            if (!openedIds.add(id)) {
                continue;
            }
            keys.add(entry.getKey());
            opened++;
        }
        return keys;
    }
}

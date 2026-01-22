package com.test.cam.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;

import java.util.Arrays;

/**
 * 单个摄像头管理类
 */
public class SingleCamera {
    private static final String TAG = "SingleCamera";

    private final Context context;
    private final String cameraId;
    private final TextureView textureView;
    private CameraCallback callback;

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private Size previewSize;
    private Surface recordSurface;  // 录制Surface

    public SingleCamera(Context context, String cameraId, TextureView textureView) {
        this.context = context;
        this.cameraId = cameraId;
        this.textureView = textureView;
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    public void setCallback(CameraCallback callback) {
        this.callback = callback;
    }

    public String getCameraId() {
        return cameraId;
    }

    /**
     * 设置录制Surface
     */
    public void setRecordSurface(Surface surface) {
        this.recordSurface = surface;
        Log.d(TAG, "Record surface set for camera " + cameraId);
    }

    /**
     * 清除录制Surface
     */
    public void clearRecordSurface() {
        this.recordSurface = null;
        Log.d(TAG, "Record surface cleared for camera " + cameraId);
    }

    public Surface getSurface() {
        if (textureView != null && textureView.isAvailable()) {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            if (surfaceTexture != null) {
                return new Surface(surfaceTexture);
            }
        }
        return null;
    }

    /**
     * 选择最优分辨率（参考guardapp使用1280x800）
     */
    private Size chooseOptimalSize(Size[] sizes) {
        // 目标分辨率：1280x800 (guardapp使用的分辨率)
        final int TARGET_WIDTH = 1280;
        final int TARGET_HEIGHT = 800;

        // 首先尝试找到精确匹配 1280x800
        for (Size size : sizes) {
            if (size.getWidth() == TARGET_WIDTH && size.getHeight() == TARGET_HEIGHT) {
                Log.d(TAG, "Camera " + cameraId + " found exact 1280x800 match");
                return size;
            }
        }

        // 找到最接近 1280x800 的分辨率
        Size bestSize = null;
        int minDiff = Integer.MAX_VALUE;

        for (Size size : sizes) {
            int width = size.getWidth();
            int height = size.getHeight();

            // 计算与目标分辨率的差距
            int diff = Math.abs(TARGET_WIDTH - width) + Math.abs(TARGET_HEIGHT - height);
            if (diff < minDiff) {
                minDiff = diff;
                bestSize = size;
            }
        }

        if (bestSize == null) {
            // 如果还是没找到，使用第一个可用分辨率
            bestSize = sizes[0];
            Log.d(TAG, "Camera " + cameraId + " using first available size");
        }

        return bestSize;
    }

    /**
     * 启动后台线程
     */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("Camera-" + cameraId);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    /**
     * 停止后台线程
     */
    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping background thread", e);
            }
        }
    }

    /**
     * 打开摄像头
     */
    public void openCamera() {
        try {
            Log.d(TAG, "openCamera: Starting for camera " + cameraId);
            startBackgroundThread();

            // 获取摄像头特性
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map != null) {
                // 优先使用 SurfaceTexture 的输出尺寸
                Size[] sizes = map.getOutputSizes(ImageFormat.PRIVATE);
                if (sizes == null || sizes.length == 0) {
                    sizes = map.getOutputSizes(SurfaceTexture.class);
                    if (sizes != null && sizes.length > 0) {
                        Log.w(TAG, "Camera " + cameraId + " no PRIVATE sizes, fallback to SurfaceTexture sizes");
                    }
                }
                if (sizes == null || sizes.length == 0) {
                    Log.e(TAG, "Camera " + cameraId + " has no output sizes for PRIVATE/SurfaceTexture");
                    return;
                }

                // 打印所有可用分辨率
                Log.d(TAG, "Camera " + cameraId + " available sizes:");
                for (int i = 0; i < Math.min(sizes.length, 10); i++) {
                    Log.d(TAG, "  [" + i + "] " + sizes[i].getWidth() + "x" + sizes[i].getHeight());
                }

                // 选择合适的分辨率
                previewSize = chooseOptimalSize(sizes);
                Log.d(TAG, "Camera " + cameraId + " selected preview size: " + previewSize);
            } else {
                Log.e(TAG, "Camera " + cameraId + " StreamConfigurationMap is null!");
            }

            // 检查 TextureView 状态
            Log.d(TAG, "Camera " + cameraId + " TextureView available: " + textureView.isAvailable());
            if (textureView.getSurfaceTexture() != null) {
                Log.d(TAG, "Camera " + cameraId + " SurfaceTexture exists");
            } else {
                Log.e(TAG, "Camera " + cameraId + " SurfaceTexture is NULL!");
            }

            // 打开摄像头
            Log.d(TAG, "Camera " + cameraId + " calling openCamera...");
            cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to open camera " + cameraId, e);
            if (callback != null) {
                callback.onCameraError(cameraId, -1);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "No camera permission", e);
            if (callback != null) {
                callback.onCameraError(cameraId, -2);
            }
        }
    }

    /**
     * 摄像头状态回调
     */
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            Log.d(TAG, "Camera " + cameraId + " opened");
            if (callback != null) {
                callback.onCameraOpened(cameraId);
            }
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDevice = null;
            Log.e(TAG, "Camera " + cameraId + " DISCONNECTED - this usually means resource exhaustion!");
            if (callback != null) {
                callback.onCameraError(cameraId, -4); // 自定义错误码：断开连接
            }
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
            String errorMsg = "UNKNOWN";
            switch (error) {
                case CameraDevice.StateCallback.ERROR_CAMERA_IN_USE:
                    errorMsg = "ERROR_CAMERA_IN_USE (1)";
                    break;
                case CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE:
                    errorMsg = "ERROR_MAX_CAMERAS_IN_USE (2)";
                    break;
                case CameraDevice.StateCallback.ERROR_CAMERA_DISABLED:
                    errorMsg = "ERROR_CAMERA_DISABLED (3)";
                    break;
                case CameraDevice.StateCallback.ERROR_CAMERA_DEVICE:
                    errorMsg = "ERROR_CAMERA_DEVICE (4) - Fatal device error!";
                    break;
                case CameraDevice.StateCallback.ERROR_CAMERA_SERVICE:
                    errorMsg = "ERROR_CAMERA_SERVICE (5)";
                    break;
            }
            Log.e(TAG, "Camera " + cameraId + " error: " + errorMsg);
            if (callback != null) {
                callback.onCameraError(cameraId, error);
            }
        }
    };

    /**
     * 创建预览会话
     */
    private void createCameraPreviewSession() {
        try {
            Log.d(TAG, "createCameraPreviewSession: Starting for camera " + cameraId);

            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            if (surfaceTexture == null) {
                Log.e(TAG, "Surface not available for camera " + cameraId);
                Log.e(TAG, "TextureView available: " + textureView.isAvailable());
                Log.e(TAG, "SurfaceTexture: " + textureView.getSurfaceTexture());
                return;
            }


            // 设置预览尺寸为最小值以减少资源消耗
            if (previewSize != null) {
                // 使用最小的预览尺寸 (例如 320x240)
                surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
                Log.d(TAG, "Camera " + cameraId + " buffer size set to: " + previewSize);
            } else {
                Log.e(TAG, "Camera " + cameraId + " Cannot set buffer size - previewSize: " + previewSize + ", SurfaceTexture: " + surfaceTexture);
            }

            // 创建预览请求
            Surface surface = new Surface(surfaceTexture);
            Log.d(TAG, "Camera " + cameraId + " Surface obtained: " + surface);

            Log.d(TAG, "Camera " + cameraId + " Creating capture request...");
            int template = (recordSurface != null) ? CameraDevice.TEMPLATE_RECORD : CameraDevice.TEMPLATE_PREVIEW;
            final CaptureRequest.Builder previewRequestBuilder = cameraDevice.createCaptureRequest(template);
            previewRequestBuilder.addTarget(surface);
            Log.d(TAG, "Camera " + cameraId + " Added preview surface to request");

            // 如果有录制Surface，也添加到输出目标
            java.util.List<Surface> surfaces = new java.util.ArrayList<>();
            surfaces.add(surface);
            if (recordSurface != null) {
                surfaces.add(recordSurface);
                previewRequestBuilder.addTarget(recordSurface);
                Log.d(TAG, "Added record surface to camera " + cameraId);
            }

            Log.d(TAG, "Camera " + cameraId + " Total surfaces: " + surfaces.size());

            // 创建会话
            Log.d(TAG, "Camera " + cameraId + " Creating capture session...");
            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "Camera " + cameraId + " Session configured!");
                    if (cameraDevice == null) {
                        Log.e(TAG, "Camera " + cameraId + " cameraDevice is null in onConfigured");
                        return;
                    }

                    captureSession = session;
                    try {
                        // 开始预览
                        Log.d(TAG, "Camera " + cameraId + " Setting repeating request...");
                        captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);

                        Log.d(TAG, "Camera " + cameraId + " preview started successfully!");
                        if (callback != null) {
                            callback.onCameraConfigured(cameraId);
                        }
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Failed to start preview for camera " + cameraId, e);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "Failed to configure camera " + cameraId + " session!");
                    if (callback != null) {
                        callback.onCameraError(cameraId, -3);
                    }
                }
            }, backgroundHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to create preview session for camera " + cameraId, e);
            Log.e(TAG, "Exception details: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected exception creating session for camera " + cameraId, e);
            Log.e(TAG, "Exception details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 重新创建会话（用于开始/停止录制时）
     */
    public void recreateSession() {
        if (cameraDevice != null) {
            if (captureSession != null) {
                captureSession.close();
                captureSession = null;
            }
            createCameraPreviewSession();
        }
    }

    /**
     * 关闭摄像头
     */
    public void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        stopBackgroundThread();

        Log.d(TAG, "Camera " + cameraId + " closed");
        if (callback != null) {
            callback.onCameraClosed(cameraId);
        }
    }
}

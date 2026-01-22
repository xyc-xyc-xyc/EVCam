package com.test.cam.camera;

/**
 * 摄像头回调接口
 */
public interface CameraCallback {
    /**
     * 摄像头打开成功
     */
    void onCameraOpened(String cameraId);

    /**
     * 摄像头配置完成，开始预览
     */
    void onCameraConfigured(String cameraId);

    /**
     * 摄像头关闭
     */
    void onCameraClosed(String cameraId);

    /**
     * 摄像头错误
     */
    void onCameraError(String cameraId, int errorCode);
}

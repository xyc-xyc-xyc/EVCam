package com.test.cam.camera;

/**
 * 录制回调接口
 */
public interface RecordCallback {
    /**
     * 录制开始
     */
    void onRecordStart(String cameraId);

    /**
     * 录制停止
     */
    void onRecordStop(String cameraId);

    /**
     * 录制错误
     */
    void onRecordError(String cameraId, String error);
}

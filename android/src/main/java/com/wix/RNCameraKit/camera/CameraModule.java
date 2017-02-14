package com.wix.RNCameraKit.camera;

import android.Manifest;
import android.hardware.Camera;
import android.support.v4.content.PermissionChecker;
import java.util.List;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeArray;
import com.wix.RNCameraKit.camera.commands.Capture;

public class CameraModule extends ReactContextBaseJavaModule {

    public CameraModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "CameraModule";
    }

    @ReactMethod
    public void hasFrontCamera(Promise promise) {

        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                promise.resolve(true);
                return;
            }
        }
        promise.resolve(false);
    }

    @ReactMethod
    public void hasFlashForCurrentCamera(Promise promise) {
        Camera camera = CameraViewManager.getCamera();
        promise.resolve(camera.getParameters().getSupportedFlashModes() != null);
    }

    @ReactMethod
    public void changeCamera(Promise promise) {
        promise.resolve(CameraViewManager.changeCamera());
    }

    @ReactMethod
    public void setFlashMode(String mode, Promise promise) {
        promise.resolve(CameraViewManager.setFlashMode(mode));
    }

    @ReactMethod
    public void getFlashMode(Promise promise) {
        Camera camera = CameraViewManager.getCamera();
        promise.resolve(camera.getParameters().getFlashMode());
    }

    @ReactMethod
    public void setImageSize(int width, int height, Promise promise) {
        promise.resolve(CameraViewManager.setImageSize(width, height));
    }

    @ReactMethod
    public void setPreviewSize(int width, int height, Promise promise) {
        promise.resolve(CameraViewManager.setPreviewSize(width, height));
    }

    @ReactMethod
    public void getImageSizes(Promise promise) {
        Camera camera = CameraViewManager.getCamera();
        List<Camera.Size> supportedPictureSizes = camera.getParameters().getSupportedPictureSizes();
        WritableArray array = new WritableNativeArray();
        for (Camera.Size size : supportedPictureSizes) {
            WritableArray sizeArray = new WritableNativeArray();
            sizeArray.pushInt((Integer)size.width);
            sizeArray.pushInt((Integer)size.height);
            array.pushArray(sizeArray);
        }
        
        promise.resolve(array);
    }

    @ReactMethod
    public void getPreviewSizes(Promise promise) {
        Camera camera = CameraViewManager.getCamera();
        List<Camera.Size> supportedPreviewSizes = camera.getParameters().getSupportedPreviewSizes();
        WritableArray array = new WritableNativeArray();
        for (Camera.Size size : supportedPreviewSizes) {
            WritableArray sizeArray = new WritableNativeArray();
            sizeArray.pushInt((Integer)size.width);
            sizeArray.pushInt((Integer)size.height);
            array.pushArray(sizeArray);
        }
        promise.resolve(array);
    }

    @ReactMethod
    public void capture(boolean saveToCameraRoll, final Promise promise) {
          new Capture(getReactApplicationContext(), saveToCameraRoll).execute(promise);
    }

    @ReactMethod
    public void hasCameraPermission(Promise promise) {
        boolean hasPermission = PermissionChecker.checkSelfPermission(getReactApplicationContext(), Manifest.permission.CAMERA)
                == PermissionChecker.PERMISSION_GRANTED;
        promise.resolve(hasPermission);
    }
}

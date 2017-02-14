package com.wix.RNCameraKit.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.util.Log;

import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;

import java.io.IOException;
import java.util.List;
import java.util.Stack;

public class CameraViewManager extends SimpleViewManager<CameraView> {

    private static Camera camera = null;
    private static int currentCamera = 0;
    private static String flashMode = Camera.Parameters.FLASH_MODE_AUTO;
    private static Stack<CameraView> cameraViews = new Stack<>();
    private static ThemedReactContext reactContext;
    private static int imgWidth = 1080;
    private static int imgHeight = 1920;
    private static int previewWidth = 1080;
    private static int previewHeight = 1920;
    private static boolean inPreview = false;

    public static boolean setImageSize(int width, int height)
    {
        imgWidth = width;
        imgHeight = height;
        return true;
    }

    public static boolean setPreviewSize(int width, int height)
    {
        previewWidth = width;
        previewHeight = height;
        return true;
    }

    public static Camera getCamera() {
        return camera;
    }

    public static String getFlashMode() {
        return flashMode;
    }

    @Override
    public String getName() {
        return "CameraView";
    }

    @Override
    protected CameraView createViewInstance(ThemedReactContext reactContext) {
        this.reactContext = reactContext;
        return new CameraView(reactContext);
    }

    public static void setCameraView(CameraView cameraView) {
        if(!cameraViews.isEmpty() && cameraViews.peek() == cameraView) return;
        CameraViewManager.cameraViews.push(cameraView);
        connectHolder();
    }

    public static boolean setFlashMode(String mode) {
        if (camera == null) {
            return false;
        }
        Camera.Parameters parameters = camera.getParameters();
        if(parameters.getSupportedFlashModes().contains(mode)) {
            flashMode = mode;
            parameters.setFlashMode(flashMode);
            stopPreview();
            camera.setParameters(parameters);
            startPreview();
            return true;
        } else {
            return false;
        }
    }

    public static boolean changeCamera() {
        if (Camera.getNumberOfCameras() == 1) {
            return false;
        }
        stopPreview();
        currentCamera++;
        currentCamera = currentCamera % Camera.getNumberOfCameras();
        initCamera();
        connectHolder();

        return true;
    }

    public static void initCamera() {
        if (camera != null) {
            camera.release();
        }
        try {
            camera = Camera.open(currentCamera);
            try {
                setCameraDisplayOrientation(((Activity) reactContext.getBaseContext()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            updateCameraSize();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public static void startPreview()
    {
        if (camera == null) return;
        if (!inPreview)
        {
            inPreview = true;
            camera.startPreview();
        }
    }

    public static void stopPreview()
    {
        if (camera == null) return;
        if (inPreview)
        {
            inPreview = false;
            camera.stopPreview();
        }
    }

    private static void connectHolder() {
        if (cameraViews.isEmpty()  || cameraViews.peek().getHolder() == null) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                if(camera == null) {
                    initCamera();
                }

                if(cameraViews.isEmpty()) {
                    return;
                }

                cameraViews.peek().post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            stopPreview();
                            camera.setPreviewDisplay(cameraViews.peek().getHolder());
                            startPreview();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }).start();
    }

    public static void removeCameraView() {
        if(!cameraViews.isEmpty()) {
            cameraViews.pop();
        }
        if(!cameraViews.isEmpty()) {
            connectHolder();
        } else if(camera != null){
            camera.release();
            camera = null;
        }
    }

    public static void setCameraDisplayOrientation(Activity activity) {
        int result = getRotation(activity);
        Camera.Parameters parameters = camera.getParameters();
        parameters.setRotation(result);
        parameters.set("orientation", "portrait");
        parameters.setPictureFormat(PixelFormat.JPEG);
        camera.setDisplayOrientation(result);
        camera.setParameters(parameters);
    }

    public static Camera.CameraInfo getCameraInfo() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(currentCamera, info);
        return info;
    }

    public static int getRotation(Activity activity) {
        Camera.CameraInfo info = getCameraInfo();

        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    // the camera sizes are always the original landscape mode sizes (widht>height)
    // the input is always portrait, so we have to change the sides to compare
    private static Camera.Size getOptimalSize(List<Camera.Size> sizes, int portraitWidth, int portraitHeight) {
        final double ASPECT_TOLERANCE = 0.1;
        if (sizes == null) return null;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = portraitWidth; // assume portrait mode
        int targetWidth = portraitHeight;
        double targetRatio=((double)targetWidth) / (double)targetHeight;
        for (Camera.Size size : sizes) {
            double ratio = ((double) size.width) / (double)size.height ;
            if (Math.abs(ratio - targetRatio) <= ASPECT_TOLERANCE)
            {
                int sizeDiff = Math.abs(size.height - targetHeight) + Math.abs(size.width - targetWidth);
                if (sizeDiff < minDiff) {
                    optimalSize = size;
                    minDiff = sizeDiff;
                }
            }
        }
        // fallback if no fitting aspect ratio resolution was found
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                int sizeDiff = Math.abs(size.height - targetHeight) + Math.abs(size.width - targetWidth);
                if (sizeDiff < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    private static void updateCameraSize() {
        try {
            if (camera == null) return;

            //WindowManager wm = (WindowManager) reactContext.getSystemService(Context.WINDOW_SERVICE);
            //Display display = wm.getDefaultDisplay();
            //Point displaySize = new Point();
            //display.getSize(displaySize);
            
            List<Camera.Size> supportedPreviewSizes = camera.getParameters().getSupportedPreviewSizes();
            List<Camera.Size> supportedPictureSizes = camera.getParameters().getSupportedPictureSizes();
            Camera.Size previewSize = getOptimalSize(supportedPreviewSizes, previewWidth, previewHeight);
            Camera.Size pictureSize = getOptimalSize(supportedPictureSizes, imgWidth, imgHeight);
            
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPreviewSize(previewSize.width, previewSize.height);
            parameters.setPictureSize(pictureSize.width, pictureSize.height);
            stopPreview();
            camera.setParameters(parameters);
            startPreview();
        } catch (RuntimeException e) {
            Log.e("Error", "Error Update Camera Size");
        }
    }

    public static void reconnect() {
        connectHolder();
    }
}

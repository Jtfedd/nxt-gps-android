package com.opencvtest.jake.opencvtest;

import android.content.Context;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener2, LocationListener {

    private CameraBridgeViewBase mOpenCvCameraView;

    private static final String TAG = "OpenCV Test App";

    private boolean initialized;
    private boolean hasGPS;

    private double latitude;
    private double longitude;

    private boolean hasCone;
    private double coneX;

    private String msgIn;
    private String msgOut;
    private NXTComm nxtComm;

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.e("OPENCVLOADER", "ERROR");
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        initialized = false;
        hasGPS = false;
        latitude = 0;
        longitude = 0;

        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setMaxFrameSize(480, 320);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

//        Camera cam = mOpenCvCameraView.getCamera();
//        Camera.Parameters params = cam.getParameters();
//
//        Log.d(TAG, "min comp:" + params.getMinExposureCompensation());
//        Log.d(TAG, "max comp:" + params.getMaxExposureCompensation());
//        Log.d(TAG, "current comp:" + params.getExposureCompensation());

        initLocationManager();

        msgOut = "init";
        msgIn = "Not Received";

        Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    msgIn = nxtComm.getMsgIn();
                }
            }
        };

        nxtComm = new NXTComm(mHandler);
        nxtComm.start();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        nxtComm.interrupt();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        // Here is where the image processing happens.
        // Currently set up for identifying largest orange object
        // Shows contour, bounding rect, and center
        Mat raw = inputFrame.rgba();
        Mat hsv = new Mat();
        Mat processedLight = new Mat();
        Mat processedDark = new Mat();
        Mat processed = new Mat();
        Imgproc.blur(raw, raw, new Size(5, 5));
        Imgproc.cvtColor(raw, hsv, Imgproc.COLOR_RGB2HSV, 4);

        int colorHue = 28/2;
        int sensitivity = 10;

        Core.inRange(hsv, new Scalar(colorHue-sensitivity, 185, 230), new Scalar(colorHue+sensitivity, 255, 255), processedLight);
        Core.inRange(hsv, new Scalar(colorHue-sensitivity, 230, 200), new Scalar(colorHue+sensitivity, 255, 255), processedDark);

        Core.add(processedLight, processedDark, processed);

        List<MatOfPoint> mContours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(processedLight, mContours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        double maxArea = 0;
        int maxAreaIdx = 0;

        for(int i = 0; i < mContours.size(); i++) {
            double area = Imgproc.contourArea(mContours.get(i));
            if (area > maxArea) {
                maxArea = area;
                maxAreaIdx = i;
            }
        }
        if (mContours.size() > 0) {
            Imgproc.drawContours(raw, mContours, maxAreaIdx, new Scalar(0, 255, 0), 2);

            MatOfPoint2f contour2f = new MatOfPoint2f(mContours.get(maxAreaIdx).toArray());

            MatOfPoint2f approxCurve = new MatOfPoint2f();
            double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

            MatOfPoint points = new MatOfPoint(approxCurve.toArray());
            Rect rect = Imgproc.boundingRect(points);

            Imgproc.rectangle(raw, rect.tl(), rect.br(), new Scalar(255, 0, 0), 2, 8, 0);

            double cx = rect.x+rect.width/2;
            double cy = rect.y+rect.height/2;

            Imgproc.circle(raw, new Point(cx, cy), 2, new Scalar(255, 255, 255), 4);

            hasCone = true;
            coneX = (cx-240)/240;
        }
        else {
            hasCone = false;
        }

        updateOutputMessage();

        return raw;
    }

    private void initLocationManager() {
        if (initialized) return;
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            initialized = true;
        }
        catch (SecurityException e) {
            Log.w(TAG, "GPS Not Available");
        }
    }

    private void updateOutputMessage() {
        String message = "";
        message += (int)(latitude*1000000) + ":" + (int)(longitude*1000000);
        if (hasCone) {
            message += ":" + (int)(coneX*100);
        }
        nxtComm.setMsgOut(message);
    }

    @Override
    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        hasGPS = true;

        updateOutputMessage();
    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }
}

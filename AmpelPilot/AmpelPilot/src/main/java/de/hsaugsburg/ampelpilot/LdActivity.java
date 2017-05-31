package de.hsaugsburg.ampelpilot;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class LdActivity extends Activity implements CvCameraViewListener2 {

    private static final String    TAG                 = "OCVSample::Activity";
    private static final Scalar GREEN_RECT_COLOR = new Scalar(0, 255, 0, 255);
    private static final Scalar    RED_RECT_COLOR     = new Scalar(255, 0, 0, 255);
    public static final int        JAVA_DETECTOR       = 0;

    int method = 0;

    private Mat                    mRgba;
    private Mat                    mGray;
    private File                   mCascadeFileGreen;
    private File                   mCascadeFileRed;
    private CascadeClassifier mJavaDetectorGreen;
    private CascadeClassifier mJavaDetectorRed;


    private int                    mDetectorType       = JAVA_DETECTOR;
    private String[]               mDetectorName;

    private int mTrafficLightSize = 0;

    private CameraBridgeViewBase   mOpenCvCameraView;
    private TextView mValue;

    double xCenter = -1;
    double yCenter = -1;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");


                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.cascade_green);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFileGreen = new File(cascadeDir, "cascade_green.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFileGreen);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        // load cascade file from application resources
                        InputStream ise = getResources().openRawResource(R.raw.cascade_red);
                        File cascadeDirGreen = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFileRed = new File(cascadeDirGreen, "cascade_red.xml");
                        FileOutputStream ose = new FileOutputStream(mCascadeFileRed);

                        while ((bytesRead = ise.read(buffer)) != -1) {
                            ose.write(buffer, 0, bytesRead);
                        }
                        ise.close();
                        ose.close();

                        mJavaDetectorGreen = new CascadeClassifier(mCascadeFileGreen.getAbsolutePath());
                        if (mJavaDetectorGreen.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetectorGreen = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFileGreen.getAbsolutePath());

                        mJavaDetectorRed = new CascadeClassifier(mCascadeFileRed.getAbsolutePath());
                        if (mJavaDetectorRed.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier for eye");
                            mJavaDetectorRed = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFileRed.getAbsolutePath());

                        cascadeDir.delete();
                        cascadeDirGreen.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }
                    mOpenCvCameraView.enableFpsMeter();
                    mOpenCvCameraView.setCameraIndex(0);
                    mOpenCvCameraView.setClickable(false);

                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public LdActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.trafficlights_detect_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tl_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();


        MatOfRect green = new MatOfRect();
        MatOfRect red = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetectorGreen != null){
                mJavaDetectorGreen.detectMultiScale(mGray, green, 20, 20, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mTrafficLightSize, mTrafficLightSize), new Size());
            }
            if(mJavaDetectorRed!= null){
                mJavaDetectorRed.detectMultiScale(mGray, red, 12, 12, 2,
                        new Size(mTrafficLightSize, mTrafficLightSize), new Size());
            }
        }
        else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] greenArray = green.toArray();
        for (int i = 0; i < greenArray.length; i++)
        {
            Imgproc.rectangle(mRgba, greenArray[i].tl(), greenArray[i].br(),
                    GREEN_RECT_COLOR, 3);
        }
        Rect[] redArray = red.toArray();
        for (int i = 0; i < redArray.length; i++)
        {
            Imgproc.rectangle(mRgba, redArray[i].tl(), redArray[i].br(),
                    RED_RECT_COLOR, 3);
        }

        return mRgba;
    }

}

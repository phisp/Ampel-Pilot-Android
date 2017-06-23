package de.hsaugsburg.ampelpilot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import android.Manifest;
import android.support.v4.app.ActivityCompat;


import android.os.Vibrator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import static android.R.attr.button;

public class LdActivity extends Activity implements CvCameraViewListener2, SensorEventListener{

    public static final int MY_PERMISSIONS_REQUEST_CAMERA = 100;
    private static final String    TAG                 = "OCVSample::Activity";
    private static final Scalar GREEN_RECT_COLOR = new Scalar(0, 255, 0, 255);
    private static final Scalar    RED_RECT_COLOR     = new Scalar(255, 0, 0, 255);
    public static final int        JAVA_DETECTOR       = 0;
    private SensorManager mSensorManager;
    private Sensor mSensor;

    private  LightPeriod lightgreen = new LightPeriod();
    private  LightPeriod lightred = new LightPeriod();
    long SytsemTime = System.currentTimeMillis();
    private TextToSpeech tts;
    //status check code
    private int MY_DATA_CHECK_CODE = 0;
    int method = 0;

    private Mat                    mRgba;
    private Mat                    mGray;
    private Mat                    mRgbaT;
    private Mat                    mRgbaF;
    private File                   mCascadeFileGreen;
    private File                   mCascadeFileRed;
    private CascadeClassifier mJavaDetectorGreen;
    private CascadeClassifier mJavaDetectorRed;

    float[] mGravity;
    float[] mGeomagnetic;
    Sensor accelerometer;
    Sensor magnetometer;
    Vibrator v;
    float pi = 3.142f;
    double billigerVersuch;


    private TextView x;
    private TextView y;
    private TextView z;

    private double                  scaleFactor;
    private int                     minNeighbours;

    private int                      Zoom;

    private double                  scaleFactorRED;
    private int                     minNeighboursRED;


    private int                    mDetectorType       = JAVA_DETECTOR;
    private String[]               mDetectorName;

    private int mTrafficLightSize = 0;

    private CameraBridgeViewBase   mOpenCvCameraView;
    private TextView mValue;

    long millis = System.currentTimeMillis();

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
                        InputStream is = getResources().openRawResource(R.raw.green_cascade);
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
                        InputStream ise = getResources().openRawResource(R.raw.red_cascade2);
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

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                MY_PERMISSIONS_REQUEST_CAMERA);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.GERMANY);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "This Language is not supported");
                    }
                    //speak("App gestartet");

                } else {
                    Log.e("TTS", "Initilization Failed!");
                }
            }
        });
        setContentView(R.layout.trafficlights_detect_surface_view);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tl_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        Button btnSettings = (Button) findViewById(R.id.settingsbtn);

        x = (TextView) findViewById(R.id.x);
        y = (TextView) findViewById(R.id.y);
        z = (TextView) findViewById(R.id.z);

        Intent i = getIntent();
        minNeighbours = i.getIntExtra("MinNeighbours",15);
        scaleFactor = i.getDoubleExtra("ScaleFactor",15);

        minNeighboursRED = i.getIntExtra("MinNeighboursRED",15);
        scaleFactorRED = i.getDoubleExtra("ScaleFactorRED",15);

        Zoom = i.getIntExtra("ZoomFactor",10);

        v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        v.vibrate(500);

        btnSettings.setOnClickListener(new View.OnClickListener(){
            public void onClick(View arg0){
                finish();
            }
        });


    }

    @Override
    public void onPause()
    {
        super.onPause();
        mSensorManager.unregisterListener(this);
        mSensorManager.unregisterListener(this);
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }



    @Override
    public void onResume()
    {
        super.onResume();
       mSensorManager.registerListener(this, accelerometer ,SensorManager.SENSOR_DELAY_UI);
       mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
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
        mSensorManager.unregisterListener(this);
        mSensorManager.unregisterListener(this);
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat(height, width, CvType.CV_8UC4);
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        //Core.transpose(mRgba, mRgbaT);
        //Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0,0, 0);
        //Core.flip(mRgbaF, mRgba, 1 );
        /*
        float zoom = (float)0;
        Size orig = mRgba.size();
        int offx = (int)(0.5 * (1.0-zoom) * orig.width);
        int offy = (int)(0.5 * (1.0-zoom) * orig.height);

        // crop the part, you want to zoom into:
        Mat cropped = mRgba.submat(offy, (int)orig.height-offy, offx, (int)orig.width-offx);

        // resize to original:
        Imgproc.resize(cropped, cropped, orig);

*/
        MatOfRect green = new MatOfRect();
        MatOfRect red = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetectorGreen != null){
                mJavaDetectorGreen.detectMultiScale(mRgba, green, scaleFactor, minNeighbours, 0, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(20, 40), new Size(200,400));
            }
            if(mJavaDetectorRed!= null){
                mJavaDetectorRed.detectMultiScale(mRgba, red, scaleFactorRED, minNeighboursRED, 0,
                        new Size(20, 40), new Size(200,400));
            }
        }
        else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] greenArray = green.toArray();
        lightgreen.addpoint(greenArray);
        if(lightgreen.checklight()){
            Log.w("step","onCameraFrame: "+ System.currentTimeMillis() +"   " +SytsemTime);
            if((System.currentTimeMillis() - SytsemTime )>2000){
                speak("Es ist Grün");
                SytsemTime=System.currentTimeMillis();
            }


            Log.w("step","onCameraFrame:  #################### Grün wurde erkannt bÄÄÄÄÄm");
        }
        for (int i = 0; i < greenArray.length; i++)
        {
            Imgproc.rectangle(mRgba, greenArray[i].tl(), greenArray[i].br(),
                    GREEN_RECT_COLOR, 3);
        }

        Rect[] redArray = red.toArray();
        lightred.addpoint(redArray);
        if(lightred.checklight()){
            if(( System.currentTimeMillis()- SytsemTime )>2000){
                speak("Es ist Rot");
                SytsemTime=System.currentTimeMillis();
            }
            Log.w("step","onCameraFrame:  #################### Red wurde erkannt bÄÄÄÄÄm");
        }
        for (int i = 0; i < redArray.length; i++)
        {
            Imgproc.rectangle(mRgba, redArray[i].tl(), redArray[i].br(),
                    RED_RECT_COLOR, 3);
        }

        return mRgba;
    }


    private void speak(String text){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }else{
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    public void onSensorChanged(SensorEvent event) {

        long newMillis = System.currentTimeMillis() ;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                float azimut = orientation[0]; // orientation contains: azimut, pitch and roll
                float pitch = orientation[1]; //roll
                float roll = orientation[2];


                x.setText("" + roll);
                y.setText("" + pitch);
                z.setText("" + azimut);


                double diffRoll = 0.6;
                double diffPitch = 0.2;


                //roll kleiner 1.4
                double valueRoll = 1.4;
                if ((abs(roll) <= valueRoll) && (newMillis > millis + 1500)) {
                    float t = ( 150/ (abs(roll)) -20);
                    v.vibrate((long) (t));
                    millis = newMillis;
                    if((System.currentTimeMillis() - SytsemTime )>5000){
                        speak("Winkel zu Niedrig");
                        SytsemTime=System.currentTimeMillis();
                    }
                }

                //roll größer 2
                if ((abs(roll) >= valueRoll + diffRoll) && (newMillis > millis + 1500)) {
                    float t = ( (abs(roll) *100) -50);
                    v.vibrate((long) (t));
                    millis = newMillis;
                    if((System.currentTimeMillis() - SytsemTime )>5000){
                        speak("Winkel zu Hoch");
                        SytsemTime=System.currentTimeMillis();
                    }
                }
                double valueazimut =0;

                //pitch größer 0,2
                double valuePitch = 0;
                if ((( pitch <= valuePitch - diffPitch)) && (newMillis > millis + 1500)) {
                    float t = ((abs(pitch) * 1000) - 50);
                    v.vibrate((long) (t));
                    millis = newMillis;
                    if((System.currentTimeMillis() - SytsemTime )>5000){
                        speak("Zu weit nach rechts geneigt");
                        SytsemTime=System.currentTimeMillis();
                    }
                }
                if (((pitch >= valuePitch + diffPitch)) && (newMillis > millis + 1500)) {
                    float t = ((abs(pitch) * 1000) - 50);
                    v.vibrate((long) (t));
                    millis = newMillis;
                    if((System.currentTimeMillis() - SytsemTime )>5000){
                        speak("Zu weit nach links geneigt");
                        SytsemTime=System.currentTimeMillis();
                    }
                }

                // Azimut größer 2
                double valueAzimut = 2.3;
                if ((abs(azimut) >= valueAzimut) && (newMillis > millis + 1500)) {
                    float t = ( 500);
                    v.vibrate((long) (t));
                    millis = newMillis;
                    if((System.currentTimeMillis() - SytsemTime )>5000){
                        speak("Drehen Sie das Handy");
                        SytsemTime=System.currentTimeMillis();
                    }
                }
            }
        }
    }

    public static float abs(float a) {
        return (a <= 0.0F) ? 0.0F - a : a;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

}

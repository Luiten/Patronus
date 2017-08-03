package luiten.patronus;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

/**
 * Created by LG on 2017-05-29.
 */

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, SensorEventListener {

    private Mat img_input;
    private Mat img_result;
    private static final String TAG = "opencv";
    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean bStart = false;

    private int VideoSize[][] = {
            { 1920, 1080 },
            { 1280, 720 },
            { 800, 600 },
            { 640, 480 } };

    // GPS 속도
    private LocationManager lm;
    private LocationListener ll;
    double mySpeed, maxSpeed;

    // 가속도
    SensorManager mSensorManager = null;
    Sensor accSensor = null;
    long checkStartTime = 0, checkEndTime = 0;
    double accelSpeed = 0.0;
    double nowSpeed = 0.0;

    public native int convertNativeLib(long matAddrInput, long matAddrResult);
    public native int PushbackAccel(float fValue);
    public native int SetSettings(int type, double value);

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        RelativeLayout main_layout = (RelativeLayout)findViewById(R.id.main_layout);
        final LinearLayout btn_layout = (LinearLayout)findViewById(R.id.main_layout_button);
        final Handler mHandler = new Handler();

        btn_layout.setVisibility(View.INVISIBLE);

        main_layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN :
                        btn_layout.setVisibility(View.VISIBLE);
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                btn_layout.setVisibility(View.INVISIBLE);
                            }
                        }, 3000);
                }
                return true;
            }
        });

        // Start
        Button button1 = (Button)findViewById(R.id.main_btn_start);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bStart = true;

                // 세팅 읽기
                SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
                mOpenCvCameraView.setMaxFrameSize(VideoSize[settings.getInt("resolution", 1)][0], VideoSize[settings.getInt("resolution", 1)][1]);

                mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
            }
        });

        // Setting
        Button button3 = (Button)findViewById(R.id.main_btn_setting);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOpenCvCameraView.setVisibility(SurfaceView.INVISIBLE);

                Intent intent = new Intent(getApplicationContext(), Setting.class);
                startActivity(intent);
            }
        });

        // Stop
        Button button4 = (Button)findViewById(R.id.main_btn_exit);
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bStart = false;
                mOpenCvCameraView.setVisibility(SurfaceView.INVISIBLE);
            }
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.INVISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(0); // front-camera(1),  back-camera(0)

        //InitializeNativeLib(1280, 720);

        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        // 속도 초기화
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        ll = new SpeedActionListener();
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, ll);

        // 가속도 초기화
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        // 주기 설명
        // SENSOR_DELAY_UI 갱신에 필요한 정도 주기
        // SENSOR_DELAY_NORMAL 화면 방향 전환 등의 일상적인  주기
        // SENSOR_DELAY_GAME 게임에 적합한 주기
        mSensorManager.registerListener(this, accSensor, mSensorManager.SENSOR_DELAY_NORMAL);

        if (bStart)
        {

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        img_input = inputFrame.rgba();
        img_result = new Mat();

        if (bStart)
        {
            convertNativeLib(img_input.getNativeObjAddr(), img_result.getNativeObjAddr());
        }

        return img_result;
    }

    //정확도에 대한 메소드 호출 (사용안함)
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // 센서값 얻어오기
    // 출처: http://h5bak.tistory.com/271
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        float accelX, accelY, accelZ;

        if (checkStartTime == 0)
        {
            checkStartTime = System.nanoTime();
        }
        else
        {
            checkStartTime = checkEndTime;
        }
        checkEndTime =  System.nanoTime();
        double AccelTime = (double) (checkEndTime - checkStartTime) / 1000000000;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelX = event.values[0];
            accelY = event.values[1];
            accelZ = event.values[2];
            accelSpeed = (Math.sqrt(Math.pow(accelX, 2) + Math.pow(accelY, 2) + Math.pow(accelZ, 2)) - 9.81);
            PushbackAccel((float)accelSpeed);

            if (Math.abs(accelSpeed) < 0.3)
            {
                accelSpeed = 0.0;
                nowSpeed = 0.0;
            }
            else
            {
                nowSpeed += accelSpeed * AccelTime * 3.6;
            }
        }

        TextView tv = (TextView)findViewById(R.id.main_text_speed);
        tv.setText("Speed: " + mySpeed + " km/h\n" + "nowSpeed: " + nowSpeed + "km/h");
    }

    private class SpeedActionListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                // GPS 위도, 경도   출처: http://mainia.tistory.com/1153
                SetSettings(100, location.getLatitude());
                SetSettings(101, location.getLongitude());

                // GPS 속도   출처: http://mainia.tistory.com/1772
                mySpeed = location.getSpeed() * 3.6;
                if (mySpeed > maxSpeed) {
                    maxSpeed = mySpeed;
                }
                TextView tv = (TextView)findViewById(R.id.main_text_speed);

                tv.setText("Speed: " + mySpeed + " km/h\n" + "nowSpeed: " + nowSpeed);
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub

        }
    }
}

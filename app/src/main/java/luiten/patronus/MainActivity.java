package luiten.patronus;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
import org.opencv.videoio.VideoCapture;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by LG on 2017-05-29.
 */

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, SensorEventListener {

    private final long FINISH_INTERVAL_TIME = 2000;
    private long backPressedTime = 0;

    private static final String TAG = "opencv";
    private CameraBridgeViewBase mOpenCvCameraView;
    private CameraBridgeViewBase mOpenCvCameraView2;
    private Mat matInput;
    private Mat matResult;
    private boolean bStart = false;

    private boolean frontCamera = false;
    private boolean backCamera = false;

    private int switchCamera = 0;
    private boolean changed = false;

/*    private int VideoSize[][] = {
            { 1920, 1080 },
            { 1280, 720 },
            { 800, 600 },
            { 640, 480 } };*/

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

    // 버튼 보여주기 상태
    private boolean bButtonShow = false;

    public native int InitializeNativeLib(int w, int h);
    public native int convertNativeLib(long matAddrInput, long matAddrResult, int iCamera); // iCamera: 0 = 후면, 1 = 전면
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
                    //mOpenCvCameraView.enableView();
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
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        RelativeLayout warning_layout = (RelativeLayout)findViewById(R.id.main_layout_warning);
        final Animation fade_warning = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        warning_layout.startAnimation(fade_warning);
        warning_layout.setVisibility(View.INVISIBLE);
        //애니메이션 fade-in-out 부분

        RelativeLayout main_layout = (RelativeLayout)findViewById(R.id.main_layout);
        final LinearLayout btn_layout = (LinearLayout)findViewById(R.id.main_layout_button);
        final Handler btn_Handler = new Handler();

        btn_layout.setVisibility(View.INVISIBLE);

        main_layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 버튼이 표지되어 있지 않으면 5초간 표시
                        if (bButtonShow == false) {
                            btn_layout.setVisibility(View.VISIBLE);
                            bButtonShow = true;
                            btn_Handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    btn_layout.setVisibility(View.INVISIBLE);
                                    bButtonShow = false;
                                }
                            }, 5000);
                        }
                        // 버튼이 표시되고 있으면 핸들러 제거후 버튼 안보이게 하기
                        else {
                            btn_Handler.removeMessages(0);
                            btn_layout.setVisibility(View.INVISIBLE);
                            bButtonShow = false;
                        }
                }
                return true;
            }
        });

        // Start
        final Button button1 = (Button)findViewById(R.id.main_btn_start);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!bStart) {
                    button1.setText("종료");

                    // 영상처리 시작
                    InitializeResolution();
                    StartProcessing();
                    bStart = true;
                }
                else {
                    button1.setText("시작");

                    // 영상처리 중지
                    StopProcessing();
                    bStart = false;
                }
            }
        });

        // 동영상 녹화
        Button button2 = (Button)findViewById(R.id.main_btn_capture);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        // Setting
        Button button3 = (Button)findViewById(R.id.main_btn_setting);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button1.setText("시작");
                StopProcessing();
                bStart = false;

                Intent intent = new Intent(getApplicationContext(), Setting.class);
                startActivity(intent);
            }
        });

        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.INVISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(0); // front-camera(1),  back-camera(0)

        mOpenCvCameraView2 = (CameraBridgeViewBase)findViewById(R.id.activity_surface_view2);
        mOpenCvCameraView2.setVisibility(SurfaceView.INVISIBLE);
        mOpenCvCameraView2.setCvCameraViewListener(this);
        mOpenCvCameraView2.setCameraIndex(1); // front-camera(1),  back-camera(0)

        // 성공적으로 연결되면 콜백 전달
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

        StopProcessing();

        if (mSensorManager != null)
            mSensorManager.unregisterListener(this);

        if (accSensor != null)
            accSensor = null;

        if (lm != null)
            lm.removeUpdates(ll);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (mOpenCvCameraView != null) {
            if (!OpenCVLoader.initDebug()) {
                Log.d(TAG, "onResume :: Internal OpenCV library not found.");
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
            } else {
                Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }

            if (bStart)
                StartProcessing();
        }

        // 속도 초기화
        if (lm == null) {
            lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            ll = new SpeedActionListener();
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, ll);
        }

        // 가속도 초기화
        if (mSensorManager == null)
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (accSensor == null)
            accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // 주기 설명
        // SENSOR_DELAY_UI 갱신에 필요한 정도 주기
        // SENSOR_DELAY_NORMAL 화면 방향 전환 등의 일상적인  주기
        // SENSOR_DELAY_GAME 게임에 적합한 주기
        if (mSensorManager != null)
            mSensorManager.registerListener(this, accSensor, mSensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

        if (mOpenCvCameraView2 != null)
            mOpenCvCameraView2.disableView();

        if (mSensorManager != null)
            mSensorManager.unregisterListener(this);

        if (accSensor != null)
            accSensor = null;

        if (lm != null)
            lm.removeUpdates(ll);
    }

    @Override
    public void onBackPressed(){
        long tempTime = System.currentTimeMillis();
        long intervalTime = tempTime - backPressedTime;

        if(0<=intervalTime && FINISH_INTERVAL_TIME >= intervalTime) {
            super.onBackPressed();
        }
        else
        {
            backPressedTime = tempTime;
            Toast.makeText(getApplicationContext(),"한 번 더 누르면 종료됩니다.",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    //------------------------------------------------------------------------------------------------//
    // 카메라 영상 처리
    //------------------------------------------------------------------------------------------------//
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
         matInput = inputFrame.rgba();

        if ( matResult != null ) matResult.release();
        matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());

        changed = true;

        if (bStart)
        {
            convertNativeLib(matInput.getNativeObjAddr(), matResult.getNativeObjAddr(), switchCamera);
        }

        return matResult;
    }

    //정확도에 대한 메소드 호출 (사용안함)
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //------------------------------------------------------------------------------------------------//
    // 센서값 얻어오기
    // 출처: http://h5bak.tistory.com/271
    //------------------------------------------------------------------------------------------------//
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

            if (bStart) {
                PushbackAccel((float)accelSpeed);
            }

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
        String strSpeed = (int)mySpeed + " km/h";
        tv.setText(strSpeed);
        //tv.setText("Speed: " + mySpeed + " km/h\n" + "nowSpeed: " + nowSpeed + "km/h");
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

                String strSpeed = (int)mySpeed + " km/h";
                tv.setText(strSpeed);
                //tv.setText("Speed: " + mySpeed + " km/h\n" + "nowSpeed: " + nowSpeed);
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

    //------------------------------------------------------------------------------------------------//
    // 카메라 스위치 비동기 작업   출처: http://webnautes.tistory.com/1085
    //------------------------------------------------------------------------------------------------//
    private class CameraSwitchTask extends AsyncTask<String, Integer, Integer> {
        private Context context;

        public CameraSwitchTask(Context context) {
            this.context = context;
        }

        @Override protected void onCancelled() {
            super.onCancelled();
        }

        // 시작하기 전
        @Override
        protected void onPreExecute() { //2
            super.onPreExecute();

        }

        // 진행
        @Override
        protected Integer doInBackground(String ... str) { //3
            int result = 0;
            boolean allwaysTrue = true;

            while (allwaysTrue) {
                try {
                    Thread.sleep(10);
                    // 화면 끌 경우 해당 작업도 종료
                    if (switchCamera == -1) {
                        break;
                    }
                    publishProgress(0);
                } catch (Exception e) {
                    Log.e("Error: ", e.getMessage());
                }
            }

            return result;
        }

        // 업데이트
        @Override
        protected void onProgressUpdate(Integer... update) { //4
            super.onProgressUpdate(update[0]);

            if (changed) {
                if (switchCamera == 0) {
                    mOpenCvCameraView.disableView();
                    switchCamera = switchCamera ^ 1;
                    //mOpenCvCameraView2.setCameraIndex(switchCamera); // front-camera(1),  back-camera(0)
                    mOpenCvCameraView2.enableView();
                }
                else {
                    mOpenCvCameraView2.disableView();
                    switchCamera = switchCamera ^ 1;
                    //mOpenCvCameraView.setCameraIndex(switchCamera); // front-camera(1),  back-camera(0)
                    mOpenCvCameraView.enableView();
                }
                changed = false;
            }
        }

        // 완료 후
        @Override
        protected void onPostExecute(Integer result) { //5
            super.onPostExecute(result);

        }
    }


    //------------------------------------------------------------------------------------------------//
    // 영상처리 전 OpenCV 해상도 설정
    //------------------------------------------------------------------------------------------------//
    private void InitializeResolution() {

        // 세팅 읽기
        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        int savedWidth = settings.getInt("resolutionwidth", 0);
        int savedHeight = settings.getInt("resolutionheight", 0);

        // 만약 saved 값이 0이면 최대값으로 초기화
        if (savedWidth <= 0 || savedHeight <= 0) {
            // 지원 해상도 알아내기
            Camera camera = Camera.open();
            Camera.Parameters parameters = camera.getParameters();
            List<Camera.Size> SupporetdSizes = parameters.getSupportedPreviewSizes();

            for (Camera.Size camSize : SupporetdSizes) {
                float raito = (float) camSize.width / camSize.height;
                // 비율(16:9 +-20%)과 일정 해상도(400)이상 만족시 표시 + 끝자리가 0일 경우 표시
                if (raito >= 1.77 * 0.8 && raito <= 1.77 * 1.2 && camSize.width > 400 && camSize.width % 10 == 0) {
                    int temp[] = {camSize.width, camSize.height};
                    savedWidth = camSize.width;
                    savedHeight = camSize.height;
                    break;
                }
            }
        }

        // 해상도 설정
        mOpenCvCameraView.setMinimumWidth(savedWidth);
        mOpenCvCameraView.setMinimumHeight(savedHeight);
        mOpenCvCameraView.setMaxFrameSize(savedWidth, savedHeight);
        mOpenCvCameraView2.setMinimumWidth(savedWidth);
        mOpenCvCameraView2.setMinimumHeight(savedHeight);
        mOpenCvCameraView2.setMaxFrameSize(savedWidth, savedHeight);

        InitializeNativeLib(savedWidth, savedHeight);
    }

    //------------------------------------------------------------------------------------------------//
    // 영상처리 시작
    //------------------------------------------------------------------------------------------------//
    private void StartProcessing() {

        // 세팅 읽기
        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);

        // 카메라 설정 읽기
        frontCamera = settings.getBoolean("frontcamera", false);
        backCamera = settings.getBoolean("backcamera", true);

        switchCamera = 0;

        // 후면 카메라만 설정되어 있는 경우 후면 카메라
        if (!frontCamera && backCamera) {
            mOpenCvCameraView.enableView();
            switchCamera = 0;
        }
        // 전면 카메라만 설정되어 있는 경우 전면 카메라만 보여준다
        else if (frontCamera && !backCamera) {
            mOpenCvCameraView.disableView();
            switchCamera = 1;
            //mOpenCvCameraView2.setCameraIndex(switchCamera); // front-camera(1),  back-camera(0)
            mOpenCvCameraView2.enableView();
        }

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView2.setVisibility(SurfaceView.VISIBLE);

        // 전면 카메라와 후면 카메라 모두 설정되어 있으면 듀얼 카메라이므로 주기적으로 교체
        if (frontCamera && backCamera) {
            final CameraSwitchTask cameraTask = new CameraSwitchTask(MainActivity.this);
            cameraTask.execute("");
        }
    }


    //------------------------------------------------------------------------------------------------//
    // 영상처리 중지
    //------------------------------------------------------------------------------------------------//
    private void StopProcessing() {
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.setVisibility(SurfaceView.INVISIBLE);
            mOpenCvCameraView.disableView();
        }

        if (mOpenCvCameraView2 != null) {
            mOpenCvCameraView2.setVisibility(SurfaceView.INVISIBLE);
            mOpenCvCameraView2.disableView();
        }
        switchCamera = -1; // 카메라 스위치 작업 종료
    }
}

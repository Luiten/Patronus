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
import android.media.Image;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
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
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by LG on 2017-05-29.
 */

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, SensorEventListener, SurfaceHolder.Callback {

    private String WarningType[] = { "자동차간 거리", "끼어들기", "차선 침범", "신호 위반", "신호 주시 안함",
            "표지판", "졸음 운전", "충돌", "운전 점수" };

    private String WarningDesc[] = { "앞차와 너무 가깝습니다", " 자동차를 주의하세요", "차선을 침범 중입니다", " 신호를 위반했습니다",
            " 신호를 확인하세요", "입니다", "졸음운전 경고입니다", "충돌이 감지됐습니다", "" };

    private Button btnStart;
    private Button btnRecord;

    private final long FINISH_INTERVAL_TIME = 2000;
    private long backPressedTime = 0;

    private static final String TAG = "opencv";
    private CameraBridgeViewBase mOpenCvCameraView;
    private CameraBridgeViewBase mOpenCvCameraView2;
    private Mat matInput;
    private Mat matResult;
    private boolean bStart = false;

    private VideoView mVideoView = null;
    private Camera mCamera;
    private MediaRecorder recorder = null;
    private static final int RECORDING_TIME = 0; // 녹화시간
    private int VIDEO_BITRATE = 3000000; // 동영상 비트레이트(3 Mbps)

    private boolean frontCamera = false;
    private boolean backCamera = false;

    private int switchCamera = 0;

    private int nWarningMode = 0; // 경고 알림 모드

/*    private int VideoSize[][] = {
            { 1920, 1080 },
            { 1280, 720 },
            { 800, 600 },
            { 640, 480 } };*/

    // Face
    private Face mFace;

    // GPS 속도
    private LocationManager lm;
    private LocationListener ll;
    double mySpeed, maxSpeed;

    // 가속도
    SensorManager mSensorManager = null;
    //SensorEventListener accL;
    TextView tv;
    Sensor accSensor = null;
    long checkStartTime = 0, checkEndTime = 0;
    double accelSpeed = 0.0;
    double speed = 0;
    double nowSpeed = 0.0;

    private Warning classWarning;
    public static Context mContext;

    private PowerManager.WakeLock mWakeLock;

    // 버튼 보여주기 상태
    private boolean bButtonShow = false;

    public native int InitializeNativeLib(int w, int h);
    public native int convertNativeLib(long matAddrInput, long matAddrResult, int iCamera); // iCamera: 0 = 후면, 1 = 전면
    public native int PushbackAccel(float fValue);
    public native int SetSettings(int type, double value);
    public native double GetValue(int type);
    public native int PrintLogForRecord(String strMsg);

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
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mContext = this;

        //사용자가 다운로드 중 파워 버튼을 누르더라도 CPU가 잠들지 않도록 해서
        //다시 파워버튼 누르면 그동안 다운로드가 진행되고 있게 됩니다.
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());

        //--------------------------------------------------------------------------//
        // 스플래시 이미지
        //--------------------------------------------------------------------------//
        final ImageView imageView = (ImageView) findViewById(R.id.main_image_background);
        // dpi와 density 구하는 방법
        DisplayMetrics outMetrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);

        // 변경하고 싶은 레이아웃의 파라미터 값을 가져 옴
        RelativeLayout.LayoutParams ImageParams = (RelativeLayout.LayoutParams) imageView.getLayoutParams();

        ImageParams.width = outMetrics.widthPixels / 5;
        ImageParams.height = outMetrics.heightPixels / 5;

        // 변경된 값의 파라미터를 해당 레이아웃 파라미터 값에 셋팅
        imageView.setLayoutParams(ImageParams);


        RelativeLayout warning_layout = (RelativeLayout)findViewById(R.id.main_layout_warning);
        warning_layout.setVisibility(View.INVISIBLE);

        RelativeLayout main_layout = (RelativeLayout)findViewById(R.id.main_layout);
        final LinearLayout btn_layout = (LinearLayout)findViewById(R.id.main_layout_button);
        final Handler btn_Handler = new Handler();

        btn_layout.setVisibility(View.INVISIBLE);

        // 녹화용 VideoView
        mVideoView = (VideoView)findViewById(R.id.activity_videoview);
        mVideoView.setVisibility(View.INVISIBLE);

        setPreview();

        // 화면 클릭 리스너
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
        btnStart = (Button)findViewById(R.id.main_btn_start);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!bStart) {
                    btnRecord.setBackgroundResource(R.drawable.recordstart);
                    mVideoView.setVisibility(View.INVISIBLE);
                    if(recorder != null){
                        recorder.stop();
                        recorder.release();
                        recorder = null;
                    }
                    if(mCamera == null){
                        setCameraPreview(mVideoView.getHolder());
                        mCamera.startPreview();
                    }

                    btnStart.setBackgroundResource(R.drawable.drivestop);

                    // 백그라운드 동작
                    if (!mWakeLock.isHeld()) mWakeLock.acquire();

                    // 영상처리 시작
                    InitializeResolution();
                    StartProcessing();
                    bStart = true;

                    final ImageView imgBackground = (ImageView) findViewById(R.id.main_image_background);
                    imgBackground.setVisibility(View.INVISIBLE);
                }
                else {
                    //button1.setText("시작");
                    btnStart.setBackgroundResource(R.drawable.drivestart);

                    // 백그라운드 동작 종료
                    if (mWakeLock.isHeld()) mWakeLock.release();

                    // 영상처리 중지
                    StopProcessing();
                    bStart = false;

                    final ImageView imgBackground = (ImageView) findViewById(R.id.main_image_background);
                    imgBackground.setVisibility(View.VISIBLE);
                }
            }
        });

        // 동영상 녹화
        btnRecord = (Button)findViewById(R.id.main_btn_capture);
        btnRecord.setOnClickListener(new View.OnClickListener() {
            boolean bRecord = false;

            @Override
            public void onClick(View v) {
                if (bRecord) {
                    btnRecord.setBackgroundResource(R.drawable.recordstart);
                    mVideoView.setVisibility(View.INVISIBLE);
                    if(recorder != null){
                        recorder.stop();
                        recorder.release();
                        recorder = null;
                    }
                    if(mCamera == null){
                        setCameraPreview(mVideoView.getHolder());
                        mCamera.startPreview();
                    }

                    // 백그라운드 동작 종료
                    if (mWakeLock.isHeld()) mWakeLock.release();
                } else {
                    btnStart.setBackgroundResource(R.drawable.drivestart);

                    // 영상처리 중지
                    StopProcessing();
                    bStart = false;

                    // 백그라운드 동작
                    if (!mWakeLock.isHeld()) mWakeLock.acquire();

                    btnRecord.setBackgroundResource(R.drawable.recordstop);
                    mVideoView.setVisibility(View.VISIBLE);
                    beginRecording(mVideoView.getHolder());
                }
                bRecord = !bRecord;
            }
        });

        // Setting
        Button btnSetting = (Button)findViewById(R.id.main_btn_setting);
        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnStart.setBackgroundResource(R.drawable.drivestart);

                StopProcessing();
                bStart = false;

                btnRecord.setBackgroundResource(R.drawable.recordstart);
                mVideoView.setVisibility(View.INVISIBLE);
                if(recorder != null){
                    recorder.stop();
                    recorder.release();
                    recorder = null;
                }
                if(mCamera == null){
                    setCameraPreview(mVideoView.getHolder());
                    mCamera.startPreview();
                }

                // 백그라운드 동작 종료
                if (mWakeLock.isHeld()) mWakeLock.release();

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
        mOpenCvCameraView2.setCvCameraViewListener(new OpenCVView());
        mOpenCvCameraView2.setCameraIndex(1); // front-camera(1),  back-camera(0)

/*       if (!mOpenCvCameraView.isHardwareAccelerated())
        {
            Toast.makeText(getApplicationContext(), "하드웨어 가속을 지원하지 않습니다.", Toast.LENGTH_LONG).show();
        }*/

        // 성공적으로 연결되면 콜백 전달
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        // 속도 초기화
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        ll = new SpeedActionListener();
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, ll);

        // 가속도 초기화
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //accL = new accListener();
        tv = (TextView)findViewById(R.id.main_text_speed);

        // 경고 클래스 초기화   출처: http://victor8481.tistory.com/72
        classWarning = new Warning(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        //StopProcessing();

        if (mSensorManager != null)
            mSensorManager.unregisterListener(this);

        if (accSensor != null)
            accSensor = null;

        if (lm != null)
            lm.removeUpdates(ll);

        //mSensorManager.unregisterListener(accL);
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

            //if (bStart)
                //StartProcessing();
        }

        //mSensorManager.registerListener(accL, accSensor, 10000, 10001);

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

    private void setCameraPreview(SurfaceHolder holder){
        try{
            mCamera = Camera.open();
            Camera.Parameters parameters = mCamera.getParameters();
            mCamera.setParameters(parameters);
            mCamera.setPreviewDisplay(holder);
        }catch (Exception e){
        }
    }

    private void setPreview(){
        mVideoView = (VideoView)findViewById(R.id.activity_videoview);
        final SurfaceHolder holder = mVideoView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private void beginRecording(SurfaceHolder holder){
        if(recorder != null){
            recorder.stop();
            recorder.release();
        }
        String state = android.os.Environment.getExternalStorageState();
        if(!state.equals(Environment.MEDIA_MOUNTED)){
            Log.e("CAM TEST","I/O Exception");
        }

        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Patronus/video/";
        File file = new File(dirPath);

        // 일치하는 폴더가 없으면 생성
        if (!file.exists()) {
            file.mkdirs();
        }

        // 현재 시간 구하기
        String time = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date(System.currentTimeMillis()));
        String OUTPUT_FILE = dirPath + time + ".mp4"; // 저장위치
        PrintLogForRecord(time + ".mp4");

        if(mCamera != null){
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        try{
            recorder = new MediaRecorder();
            recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setVideoSize(1280, 720);
            recorder.setVideoFrameRate(30);
            recorder.setVideoEncodingBitRate(VIDEO_BITRATE);
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setMaxDuration(RECORDING_TIME);
            recorder.setPreviewDisplay(holder.getSurface());
            recorder.setOutputFile(OUTPUT_FILE);
            recorder.prepare();
            recorder.start();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        setCameraPreview(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(mCamera != null){
            mCamera.startPreview();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if(mCamera != null){
            mCamera.stopPreview();
            mCamera = null;
        }
    }

    @Override
    public void onBackPressed(){
        long tempTime = System.currentTimeMillis();
        long intervalTime = tempTime - backPressedTime;

        if(0 <= intervalTime && FINISH_INTERVAL_TIME >= intervalTime) {
            super.onBackPressed();
        }
        else
        {
            backPressedTime = tempTime;
            Toast.makeText(getApplicationContext(),"뒤로 버튼을 한 번 더 누르면 종료됩니다.",Toast.LENGTH_SHORT).show();
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

        //--------------------------------------------------------------------------//
        // 영상 처리
        //--------------------------------------------------------------------------//
        if (matResult != null) matResult.release();
        matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());

        if (bStart)
        {
            convertNativeLib(matInput.getNativeObjAddr(), matResult.getNativeObjAddr(), switchCamera);
        }

        //--------------------------------------------------------------------------//
        // 경고가 있을 경우 진동이나 TTS와 이미지로 알림
        //--------------------------------------------------------------------------//
        this.handler.sendMessage(Message.obtain(handler, 1, (int) GetValue(1))); // 경고 이미지

        if (nWarningMode == 0) {
            this.handler.sendMessage(Message.obtain(handler, 2, (int) GetValue(1))); // TTS
        } else if (nWarningMode == 1) {
            this.handler.sendMessage(Message.obtain(handler, 3, (int) GetValue(1))); // 진동
        }

        //ShowAlert((int) GetValue(1)); // 이미지 Fade in/out 경고
        //classWarning.TTS((int) GetValue(1), "");
        SetSettings(200, -1); // 최근 로그 값을 사용했으므로 초기화

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

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelX = event.values[0];
            accelY = event.values[1];
            accelZ = event.values[2];
            accelSpeed = (Math.sqrt(Math.pow(accelX, 2) + Math.pow(accelY, 2) + Math.pow(accelZ, 2)) - 9.81);

            if (bStart) {
                PushbackAccel((float)accelSpeed);
            }
        }

        //tv = (TextView)findViewById(R.id.main_text_speed);
        //String strSpeed = (int)mySpeed + " km/h";
        //tv.setText(strSpeed);
        //tv.setText("Speed: " + mySpeed + " km/h\n" + "nowSpeed: " + nowSpeed + "km/h");
    }

    private class SpeedActionListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            TextView tv = (TextView)findViewById(R.id.main_text_speed);

            if (location != null) {
                // GPS 위도, 경도   출처: http://mainia.tistory.com/1153
                SetSettings(100, location.getLatitude());
                SetSettings(101, location.getLongitude());

                // GPS 속도   출처: http://mainia.tistory.com/1772
                mySpeed = location.getSpeed() * 3.6;
                if (mySpeed > maxSpeed) {
                    maxSpeed = mySpeed;
                }

                // 현재 속도 전달
                SetSettings(102, mySpeed);

                String strSpeed = (int)mySpeed + " km/h";
                tv.setText(strSpeed);
                //tv.setText("Speed: " + mySpeed + " km/h\n" + "nowSpeed: " + nowSpeed);
            }
            else {
                String strSpeed = "0 km/h";
                tv.setText(strSpeed);
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
        mOpenCvCameraView2.setMaxFrameSize(400, 300);

        InitializeNativeLib(savedWidth, savedHeight);

        // 경고 모드 읽기
        nWarningMode = settings.getInt("sound", 0);
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
        int dualcamera = settings.getInt("dualcamera", 0);

        switchCamera = 0;

        // 후면 카메라 동작
        if (backCamera) {
            mOpenCvCameraView.enableView();
        }
        // 전면 카메라 동작
        if (frontCamera) {
            if (dualcamera == 2 || !mOpenCvCameraView.isEnabled()) {
                mOpenCvCameraView2.enableView();
                if (mFace == null)
                    mFace = new Face();

                mFace.LoadCascade();
            }
        }

        // 둘 다 작동하지 않게 설정되면 Toast 출력 후 전면만 작동
        if (!backCamera && !frontCamera) {
            Toast.makeText(getApplicationContext(), "전면, 후면 카메라 모두 사용하지 않게 설정되어 있어,\n녹화와 경고 기능을 사용하지 않고 후면 카메라만 보여줍니다.", Toast.LENGTH_LONG).show();
            mOpenCvCameraView.enableView();
            switchCamera = -1;
        }

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView2.setVisibility(SurfaceView.VISIBLE);
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

    //------------------------------------------------------------------------------------------------//
    // 경고를 위한 핸들러(UI는 스레드(opencv 스레드)에서 수정할 수 없다)
    //------------------------------------------------------------------------------------------------//
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int type = (int)msg.obj;

            switch (msg.what) {
                case 1:  // 메시지로 넘겨받은 파라미터, 이 값으로 어떤 처리를 할지 결정
                    ShowAlert(type);
                    break;

                case 2: // TTS
                    classWarning.TTS(type, "");
                    break;

                case 3: // 진동
                    classWarning.Vibrate(type);
                    break;
            }
        }
    };

    //------------------------------------------------------------------------------------------------//
    // 경고 이미지 보여주는 함수
    //------------------------------------------------------------------------------------------------//
    private void ShowAlert(int nType) {
        if (nType < 0) return;

        //((StandardActivity)StandardActivity.mContext).SetImage(img_result);

        RelativeLayout warning_layout = (RelativeLayout)findViewById(R.id.main_layout_warning);
        final Animation fade_warning = AnimationUtils.loadAnimation(MainActivity.mContext, R.anim.fade_in); // 애니메이션 fade-in-out 부분

        final TextView txtWarning = (TextView)findViewById(R.id.main_text_warning);
        txtWarning.setText(WarningDesc[nType - 1]);

        warning_layout.setVisibility(View.VISIBLE);
        warning_layout.startAnimation(fade_warning);

        warning_layout.startAnimation(fade_warning);
        warning_layout.setVisibility(View.INVISIBLE);
    }

    //------------------------------------------------------------------------------------------------//
    // OpenCV View 클래스
    //------------------------------------------------------------------------------------------------//
    public class OpenCVView implements CameraBridgeViewBase.CvCameraViewListener2 {
        private Mat matInput;
        private Mat matResult;

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
            final ArrayList<Integer> m_BlinkList = new ArrayList<Integer>();
            int nEyeStatus = 0;
            matInput = inputFrame.rgba();

            //--------------------------------------------------------------------------//
            // 영상 처리
            //--------------------------------------------------------------------------//
            if (matResult != null) matResult.release();
            matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());
            matInput.copyTo(matResult);

            matResult = mFace.ProcessFace(matInput);
            nEyeStatus = mFace.GetBlinkList();
            if (m_BlinkList.size() > (30 * 60)) {
                m_BlinkList.remove(0);
            }
            m_BlinkList.add(nEyeStatus);


            int blink = 0;
            for (int i = 0; i < m_BlinkList.size(); i++) {
                if (m_BlinkList.get(i) == 2) {
                    blink++;
                }
            }


//            Imgproc.putText(matResult, "blink: " + ((double)(m_BlinkList.size() / 30) / blink), new Point(200, 30),
//                    Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 0, 0, 255));

            if ((m_BlinkList.size () > 30 * 5) && ((double)(m_BlinkList.size() / 30) / blink) < 1)
            {
                // 졸음운전 여부 데이터 전송
                SetSettings(300, 0);
                m_BlinkList.clear();
            }

            return matResult;
        }

    }
}
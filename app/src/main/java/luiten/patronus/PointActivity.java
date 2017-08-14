package luiten.patronus;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

/**
 * Created by LG on 2017-05-29.
 */

public class PointActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private Mat img_input;
    private Mat img_result;
    private static final String TAG = "opencv";
    private CameraBridgeViewBase mOpenCvCameraView;

    private int VideoSize[][] = {
            { 1920, 1080 },
            { 1280, 720 },
            { 800, 600 },
            { 640, 480 } };

    public native int InitializeNativeLib(int w, int h);
    public native int convertNativeLib(long matAddrInput, long matAddrResult, int iCamera);
    public native int CaptureImage(long matAddrResult);

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.point_main);

        // Start
        Button button1 = (Button)findViewById(R.id.point_btn_capture);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                img_result = new Mat();
                CaptureImage(img_result.getNativeObjAddr());
                ((StandardActivity)StandardActivity.mContext).SetImage(img_result);
                finish();
            }
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.point_cameraview);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(0); // front-camera(1),  back-camera(0)

        Intent intent = getIntent();
        mOpenCvCameraView.setMaxFrameSize(VideoSize[intent.getIntExtra("resolution", 1)][0], VideoSize[intent.getIntExtra("resolution", 1)][1]);

        InitializeNativeLib(VideoSize[intent.getIntExtra("resolution", 1)][0], VideoSize[intent.getIntExtra("resolution", 1)][1]);

        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
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
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
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

        convertNativeLib(img_input.getNativeObjAddr(), img_result.getNativeObjAddr(), 0);

        return img_result;
    }
}

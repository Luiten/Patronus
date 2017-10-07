//https://github.com/auejin/winklick
package luiten.patronus;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
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
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

public class Face {

    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    private String TAG = "FACE";
    public static final int JAVA_DETECTOR = 0;
    private static final int TM_SQDIFF = 0;
    private static final int TM_SQDIFF_NORMED = 1;
    private static final int TM_CCOEFF = 2;
    private static final int TM_CCOEFF_NORMED = 3;
    private static final int TM_CCORR = 4;
    private static final int TM_CCORR_NORMED = 5;

    private int learn_frames = 0;
    private Mat teplateR;
    private Mat teplateL;
    //int method = 0;

    // matrix for zooming
    //private Mat mZoomWindow;
    //private Mat mZoomWindow2;

    //private MenuItem               mItemFace50;
    //private MenuItem               mItemFace40;
    //private MenuItem               mItemFace30;
    //private MenuItem               mItemFace20;
    // private MenuItem               mItemType;

    private Mat mRgba;
    private Mat mGray;
    private File mCascadeFile;
    //private File                   mCascadeFileEye;
    private CascadeClassifier mJavaDetector;
    //private CascadeClassifier      mJavaDetectorEye;


    //private int                    mDetectorType       = JAVA_DETECTOR;
    private String[] mDetectorName;

    private float mRelativeFaceSize = 0.5f;
    private int mAbsoluteFaceSize = 0;

    private static int cameraWidth = 400; //1280*720
    private static int cameraHeight = 300; //352*288

    private WindowManager.LayoutParams mParams; //뷰의 위치 및 크기
    private WindowManager mWindowManager;
    //private RelativeLayout mPopupView;//항상 보이게할 뷰(nativecam_view.xml의 레이아웃)

    double xCenter = -1;
    double yCenter = -1;

    public void LoadCascade() {
        // load cascade file from application resources
        mCascadeFile = new File("/sdcard/Patronus/lbpcascade_frontalface.xml");

        File cascadeFileT = new File("/sdcard/Patronus/haarcascade_eye.xml");
        //-- end --//

        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        if (mJavaDetector.empty()) {
            Log.e(TAG, "Failed to load cascade classifier");
            mJavaDetector = null;
        } else
            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());


        mJavaDetectorTest = new CascadeClassifier(cascadeFileT.getAbsolutePath());
        if (mJavaDetectorTest.empty()) {
            Log.e(TAG, "Failed to load cascade classifier");
            mJavaDetectorTest = null;
        } else
            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

        mGray = new Mat();
        mRgba = new Mat();
    }

    public Face() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    public Mat ProcessFace(Mat inputFrame) {

        mRgba = inputFrame.clone();
        Imgproc.cvtColor(inputFrame, mGray, Imgproc.COLOR_BGR2GRAY);

        Core.flip(mRgba, mRgba, 1);
        Core.flip(mGray, mGray, 1);//(352,288)

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }

        }

//        if (mZoomWindow == null || mZoomWindow2 == null)
//            CreateAuxiliaryMats();


        MatOfRect faces = new MatOfRect();

        if (mJavaDetector != null)
            mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2,
                    2,
                    new Size(mAbsoluteFaceSize, mAbsoluteFaceSize),
                    new Size());

        Rect[] facesArray = faces.toArray();


//        if (mDetectorType == JAVA_DETECTOR) {
//            if (mJavaDetector != null)
//                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
//                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
//        }
//        else {
//            Log.e(TAG, "Detection method is not selected!");
//        }

        if (facesArray.length > 0) {
            if (isLineVisible)
                Imgproc.rectangle(mRgba, facesArray[0].tl(), facesArray[0].br(), FACE_RECT_COLOR, 3);
            xCenter = (facesArray[0].x + facesArray[0].width + facesArray[0].x) / 2;
            yCenter = (facesArray[0].y + facesArray[0].y + facesArray[0].height) / 2;
            eye_area = facesArray[0];

            if (isLineVisible) {
                Point center = new Point(xCenter, yCenter);//얼굴 중점 그리고 중점에 미니원을 그린다
                Imgproc.circle(mRgba, center, 10, new Scalar(255, 0, 0, 255), 3);
                Imgproc.putText(mRgba, "[" + center.x + "," + center.y + "]",
                        new Point(center.x + 20, center.y + 20),
                        Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 255, 255,
                                255));//중점 옆에 위치좌표를 보여주는 텍스트 표시
            }

            Rect r = facesArray[0];

            //TODO : 여기서부터 변경함
            Rect eyearea_left = returnEyeArea(r, true);
            Rect eyearea_right = returnEyeArea(r, false);

            Imgproc.rectangle(mRgba, eyearea_left.tl(), eyearea_left.br(),
                    new Scalar(255, 0, 0, 255), 2);
            Imgproc.rectangle(mRgba, eyearea_right.tl(), eyearea_right.br(),
                    new Scalar(255, 0, 0, 255), 2);
            Rect eye_left = returnEye(eyearea_left);
            Rect eye_right = returnEye(eyearea_right);

            //traceUpdateEyesarray(r);//핵심 코드

//            final Rect eyearea_L = returnEyeArea(r, true); // 왼쪽눈영역
//            final Rect eyearea_R = returnEyeArea(r, false); // 오른쪽눈영역
//
//            Imgproc.rectangle(mRgba, eyearea_L.tl(), eyearea_L.br(),
//                    new Scalar(0, 0, 255, 255), 2);
//            Imgproc.rectangle(mRgba, eyearea_R.tl(), eyearea_R.br(),
//                    new Scalar(0, 0, 255, 255), 2);


            area_check(r, true);
            area_check(r, false);
            //area_check(r, false);
            //area_check(r, false);
            //Mat m = mGray.submat(eyearea_left);
            //Rect[] eyesArray = detectedEyesOnEyeArea(eyearea_left);

//            Imgproc.rectangle(mRgba,eyesArray[0].tl(), eyesArray[0].br(),
//                    new Scalar(0, 0, 255, 255), 2);

            //Imgproc.rectangle(mRgba, eye_left.tl(), eye_left.br(), FACE_RECT_COLOR, 3);


            if (blink_L || blink_R) {

                Log.e("WINK", "-----Eye Blink----");
                Point center = new Point(xCenter, yCenter);//얼굴 중점 그리고 중점에 원을 그린다
                Imgproc.putText(mRgba, "Eye Blink", new Point(center.x - 20, center.y - 30),
                        Core.FONT_HERSHEY_SIMPLEX, 1.2, new Scalar(255, 0, 0,
                                255));
            } else {
                Log.e("WINK", "-------------");
                Point center = new Point(xCenter, yCenter);//얼굴 중점 그리고 중점에 원을 그린다
                Imgproc.putText(mRgba, "Eye Not Blink", new Point(center.x - 20, center.y - 30),
                        Core.FONT_HERSHEY_SIMPLEX, 1.2, new Scalar(0, 0, 255,
                                255));
            }


        }


        Imgproc.putText(mRgba, "numBlink: " + Num_Blink, new Point(100, 30),
                Core.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255, 0, 0, 255));

        return mRgba;
    }

    // TODO : 추가
    private static boolean isLineVisible = true;
    public Rect prev_detected_eyearea_left = new Rect(1, 1, 2, 2);
    public Rect prev_detected_eyearea_right = new Rect(1, 1, 2, 2);//prev보단 prev_detected가 더 커야됨. 안크면 submat시 오류.
    private int mAbsoluteEyeSize = 0;
    private float mRelativeEyeSize = 0.3f;
    private CascadeClassifier mJavaDetectorTest;
    private Rect eye_area;
    public Rect prev_facearea = new Rect(1, 1, 3, 3); //얼굴 미발견시 추적
    private Rect eye_a_area;
    private Mat eye_a, eye_b, eye_temp;
    private boolean isMainEyeWink = false;
    private boolean isWink = false;//outOfArea가 마우스 속도에 영향을 주지 않게 하기 위해 따로 bool 만들자
    private boolean blink_L = false; // 왼쪽눈 감음?
    private boolean blink_R = false; // 오른쪽눈 감음?
    private int maxH = -1;

    private int Num_Blink = 0;

    private static int a_hei_t, a_hei_m, a_hei_b, avg_thr_t, avg_thr_m, avg_thr_b, maxH_t, maxH_avg, maxH_b;// avg_bri_b
    private static int learn_framesMAX = 30;//이 기간 이상 고정시 확정
    boolean isBlink = true;//현재 타깃 눈의 눈이 감겨있는가?
    private int isLeft = 0, isUp = 0;
    public Rect prev_eye_a_area = new Rect(1, 1, 1, 1);//머신러닝으로 측정된 눈 이미지에서 검출된 진짜 "눈" 영역. 회색 사각테두리.
    private int check = 0;
    public int eyeLMissingFrame = 0, eyeRMissingFrame = 0, faceMissingFrame = 0;
    private boolean isClickEyeLeft = true;//클릭인식 기준 눈이 왼쪽인가


    private ArrayList<Integer> hei_t = new ArrayList<Integer>();//마이닝되니 데이터 저장//위//rList
    private ArrayList<Integer> hei_m = new ArrayList<Integer>();//마이닝되니 데이터 저장//아래//rList
    private ArrayList<Integer> hei_b = new ArrayList<Integer>();//마이닝되니 데이터 저장//아래

    private ArrayList<Integer> thr_t = new ArrayList<Integer>();//마이닝되니 데이터 저장//위
    private ArrayList<Integer> thr_m = new ArrayList<Integer>();//마이닝되니 데이터 저장//중간
    private ArrayList<Integer> thr_b = new ArrayList<Integer>();//마이닝되니 데이터 저장//아래

    private ArrayList<Integer> maxH_list = new ArrayList<Integer>();//윙크인식용

    private int learn_course = 0;//0:없음, 1:가운데, 2:위 얻음->작동 시작

    private Rect returnEye(Rect eye) {
        eye = new Rect(eye.x + eye.width / 16
                + (eye.width - 2 * eye.width / 16) / 3,
                (int) (eye.y + (eye.height / 4)),
                (eye.width - 2 * eye.width / 16) / 2, (int) (eye.height / 3.0));
        return eye;
    }


    private void area_check(Rect face, boolean isLeftEye) {
        Rect prev_detected_eyearea = new Rect();
        boolean isWinkTmp = false;//현재 타깃 눈의 눈이 감겨있는가?
        final Rect eye = returnEyeArea(face, isLeftEye); // 왼쪽눈영역
        if (isLeftEye) {
            prev_detected_eyearea = prev_detected_eyearea_left;
        } else {
            prev_detected_eyearea = prev_detected_eyearea_right;
        }

        Rect[] eyesArray = detectedEyesOnEyeArea(eye);


        if (eyesArray.length == 0 || eyesArray[0].width < eye.width / 2
                || (eyesArray[0].width < prev_detected_eyearea.width * 2 && eyesArray[0].height < prev_detected_eyearea.height * 2)) {//크기 너무 작거나 정사각형인건 일단 배제
            if (isLeftEye) {
                eyeLMissingFrame++;
            } else {
                eyeRMissingFrame++;
            }
            Rect tempEyeArea_L = new Rect(1, 1, 1, 1);//추적이 안된경우, 이전 영역을 바탕으로 추정함 (빨간색)
            tempEyeArea_L.x = prev_detected_eyearea.x * face.width / prev_facearea.width;
            tempEyeArea_L.y = prev_detected_eyearea.y * face.height / prev_facearea.height;
            tempEyeArea_L.width = prev_detected_eyearea.width * face.width / prev_facearea.width;
            tempEyeArea_L.height = prev_detected_eyearea.height * face.height / prev_facearea.height;

            //여기서 ratio가 face를 기준으로 했는데, 지금 eye측정 유무의 주기랑 face유무 주기랑 달라서 범위 초과로 assertion failed 오류났던 적이 있다.
            //근데 이 코드는 얼굴을 찾았을 때 face인수가 있을 때 발생하는거라서....//그러니까 prev가 face로 덮어지면서 같아지니까 오류가 발생하지!!!!!

            Imgproc.rectangle(mRgba
                    , new Point(eye.tl().x + tempEyeArea_L.tl().x, eye.tl().y + tempEyeArea_L.tl().y)
                    , new Point(eye.tl().x + tempEyeArea_L.br().x, eye.tl().y + tempEyeArea_L.br().y)
                    , new Scalar(255, 0, 0, 255), 3);//빨간색

            Mat m = mGray.submat(eye);

            if (m.height() < tempEyeArea_L.y + tempEyeArea_L.height || m.width() < tempEyeArea_L.x + tempEyeArea_L.width) {
                isWinkTmp = false;
            } else {
                eye_temp = m.submat(tempEyeArea_L);//.clone() 아직까진 따로 잘리게 나올 필요는 없어서 clone 안함
                isWinkTmp = getPupilBlink(eye_temp);

                if (isLeftEye == true) {
                    if (blink_L && (blink_L != isWinkTmp)) Num_Blink++;
                    blink_L = isWinkTmp;
                } else {
                    if (blink_R && (blink_R != isWinkTmp)) Num_Blink++;
                    blink_R = isWinkTmp;

                }
            }


        } else {
            if (isLeftEye) {
                eyeLMissingFrame = 0;
            } else {
                eyeRMissingFrame = 0;
            }
            if (isLeftEye) {
                prev_detected_eyearea_left = eyesArray[0];
            } else {
                prev_detected_eyearea_right = eyesArray[0];
            }
            //Imgproc.rectangle(mRgba, eyesArray[0].tl(), eyesArray[0].br(), FACE_RECT_COLOR, 3);

            //이전꺼 업뎃
            Imgproc.rectangle(mRgba
                    , new Point(eye.tl().x + eyesArray[0].tl().x, eye.tl().y + eyesArray[0].tl().y)
                    , new Point(eye.tl().x + eyesArray[0].br().x, eye.tl().y + eyesArray[0].br().y)
                    , FACE_RECT_COLOR, 3);
            Mat m = mGray.submat(eye);
            eye_temp = m.submat(eyesArray[0]);//.clone()하면 원래 mGray에서 잘리게 나옴. 지우면 히스토그램부터 블러처리까지 된걸 자른 mat이 리턴된다.
            isWinkTmp = getPupilBlink(eye_temp);//Imgproc.resize(setPupilWink_mat(m.submat(eyesArray[0])), mZoomWindow2,mZoomWindow2.size());//표시하는거//setPupilWink_mat
            if (isLeftEye) {
                if (blink_L && (blink_L != isWinkTmp)) Num_Blink++;
                blink_L = isWinkTmp;

            } else {
                if (blink_R && (blink_R != isWinkTmp)) Num_Blink++;
                blink_R = isWinkTmp;
            }
        }


        //getPupilMove(isLeftEye);//LL || RR일때 윙크 안하면 동공추적


        prev_facearea = face;
        faceMissingFrame = 0;


    }

    private Rect returnEyeArea(Rect face, boolean isLeft) {

        if (!isLeft) {
            face = new Rect(face.x + face.width / 16
                    + (face.width - 2 * face.width / 16) / 2,
                    (int) (face.y + (face.height / 4.5)),
                    (face.width - 2 * face.width / 16) / 2, (int) (face.height / 3.0));
        } else {
            face = new Rect(face.x + face.width / 16,
                    (int) (face.y + (face.height / 4.5)),
                    (face.width - 2 * face.width / 16) / 2, (int) (face.height / 3.0));
        }


        return face;
    }

    public void traceUpdateEyesarray(Rect face) {//checkWink
        //final boolean isTargetEyeLeft = (isMainEye == isClickEyeLeft);
        //final Rect eyearea = returnEyeArea(face, isTargetEyeLeft);//얼굴 영역에서 일정 비율 자름
        final Rect eyearea_L = returnEyeArea(face, true); // 왼쪽눈영역
        final Rect eyearea_R = returnEyeArea(face, false); // 오른쪽눈영역
        boolean isBlink = false;//현재 타깃 눈의 눈이 감겨있는가?
        Rect prev_detected_eyearea_L = new Rect();//이걸 없애고 LEFT와 RIGHT 둘다 검사하도록 해야한다.
        Rect prev_detected_eyearea_R = new Rect();
        prev_detected_eyearea_L = prev_detected_eyearea_left;
        prev_detected_eyearea_R = prev_detected_eyearea_right;


        Rect[] eyesArray_L = detectedEyesOnEyeArea(eyearea_L);
        Rect[] eyesArray_R = detectedEyesOnEyeArea(eyearea_R);

        if (eyesArray_L.length == 0 || eyesArray_L[0].width < eyearea_L.width / 2
                || (eyesArray_L[0].width < prev_detected_eyearea_L.width * 2 && eyesArray_L[0].height < prev_detected_eyearea_L.height * 2)) {//크기 너무 작거나 정사각형인건 일단 배제
            Rect tempEyeArea_L = new Rect(1, 1, 1, 1);//추적이 안된경우, 이전 영역을 바탕으로 추정함 (빨간색)
            tempEyeArea_L.x = prev_detected_eyearea_L.x * face.width / prev_facearea.width;
            tempEyeArea_L.y = prev_detected_eyearea_L.y * face.height / prev_facearea.height;
            tempEyeArea_L.width = prev_detected_eyearea_L.width * face.width / prev_facearea.width;
            tempEyeArea_L.height = prev_detected_eyearea_L.height * face.height / prev_facearea.height;

            //여기서 ratio가 face를 기준으로 했는데, 지금 eye측정 유무의 주기랑 face유무 주기랑 달라서 범위 초과로 assertion failed 오류났던 적이 있다.
            //근데 이 코드는 얼굴을 찾았을 때 face인수가 있을 때 발생하는거라서....//그러니까 prev가 face로 덮어지면서 같아지니까 오류가 발생하지!!!!!

            Imgproc.rectangle(mRgba
                    , new Point(eyearea_L.tl().x + tempEyeArea_L.tl().x, eyearea_L.tl().y + tempEyeArea_L.tl().y)
                    , new Point(eyearea_L.tl().x + tempEyeArea_L.br().x, eyearea_L.tl().y + tempEyeArea_L.br().y)
                    , new Scalar(255, 0, 0, 255), 3);//빨간색

            Mat m = mGray.submat(eyearea_L);
            if (m.height() < tempEyeArea_L.y + tempEyeArea_L.height || m.width() < tempEyeArea_L.x + tempEyeArea_L.width) {
                blink_L = false;//윙클릭 처음 작동시의 오류. 그냥 무시.
            } else {
                eye_temp = m.submat(tempEyeArea_L);//.clone() 아직까진 따로 잘리게 나올 필요는 없어서 clone 안함
                blink_L = getPupilBlink(eye_temp);
            }

        } else {
            //이전꺼 업뎃
            Imgproc.rectangle(mRgba
                    , new Point(eyearea_L.tl().x + eyesArray_L[0].tl().x, eyearea_L.tl().y + eyesArray_L[0].tl().y)
                    , new Point(eyearea_L.tl().x + eyesArray_L[0].br().x, eyearea_L.tl().y + eyesArray_L[0].br().y)
                    , FACE_RECT_COLOR, 3);
            Mat m = mGray.submat(eyearea_L);
            eye_temp = m.submat(eyesArray_L[0]);//.clone()하면 원래 mGray에서 잘리게 나옴. 지우면 히스토그램부터 블러처리까지 된걸 자른 mat이 리턴된다.
            blink_L = getPupilBlink(eye_temp);//Imgproc.resize(setPupilWink_mat(m.submat(eyesArray[0])), mZoomWindow2,mZoomWindow2.size());//표시하는거//setPupilWink_mat

        }


        if (eyesArray_R.length == 0 || eyesArray_R[0].width < eyearea_R.width / 2
                || (eyesArray_R[0].width < prev_detected_eyearea_R.width * 2 && eyesArray_R[0].height < prev_detected_eyearea_R.height * 2)) {//크기 너무 작거나 정사각형인건 일단 배제
            Rect tempEyeArea_R = new Rect(1, 1, 1, 1);//추적이 안된경우, 이전 영역을 바탕으로 추정함 (빨간색)
            tempEyeArea_R.x = prev_detected_eyearea_R.x * face.width / prev_facearea.width;
            tempEyeArea_R.y = prev_detected_eyearea_R.y * face.height / prev_facearea.height;
            tempEyeArea_R.width = prev_detected_eyearea_R.width * face.width / prev_facearea.width;
            tempEyeArea_R.height = prev_detected_eyearea_R.height * face.height / prev_facearea.height;

            //여기서 ratio가 face를 기준으로 했는데, 지금 eye측정 유무의 주기랑 face유무 주기랑 달라서 범위 초과로 assertion failed 오류났던 적이 있다.
            //근데 이 코드는 얼굴을 찾았을 때 face인수가 있을 때 발생하는거라서....//그러니까 prev가 face로 덮어지면서 같아지니까 오류가 발생하지!!!!!

            Imgproc.rectangle(mRgba
                    , new Point(eyearea_L.tl().x + tempEyeArea_R.tl().x, eyearea_L.tl().y + tempEyeArea_R.tl().y)
                    , new Point(eyearea_L.tl().x + tempEyeArea_R.br().x, eyearea_L.tl().y + tempEyeArea_R.br().y)
                    , new Scalar(255, 0, 0, 255), 3);//빨간색

            Mat m = mGray.submat(eye_area);
            if (m.height() < tempEyeArea_R.y + tempEyeArea_R.height || m.width() < tempEyeArea_R.x + tempEyeArea_R.width) {
                blink_R = false;//윙클릭 처음 작동시의 오류. 그냥 무시.
            } else {
                eye_temp = m.submat(tempEyeArea_R);//.clone() 아직까진 따로 잘리게 나올 필요는 없어서 clone 안함
                blink_R = getPupilBlink(eye_temp);
            }

        } else {
            //이전꺼 업뎃
            Imgproc.rectangle(mRgba
                    , new Point(eyearea_R.tl().x + eyesArray_L[0].tl().x, eyearea_R.tl().y + eyesArray_R[0].tl().y)
                    , new Point(eyearea_R.tl().x + eyesArray_L[0].br().x, eyearea_R.tl().y + eyesArray_R[0].br().y)
                    , FACE_RECT_COLOR, 3);
            Mat m = mGray.submat(eyearea_R);
            eye_temp = m.submat(eyesArray_R[0]);//.clone()하면 원래 mGray에서 잘리게 나옴. 지우면 히스토그램부터 블러처리까지 된걸 자른 mat이 리턴된다.
            blink_R = getPupilBlink(eye_temp);//Imgproc.resize(setPupilWink_mat(m.submat(eyesArray[0])), mZoomWindow2,mZoomWindow2.size());//표시하는거//setPupilWink_mat

        }

        if (blink_L && blink_R) {
            isBlink = true;
        }

    }

    public void getPupilMove(boolean isTargetEyeLeft) {
        if (eye_b == null || eye_b.width() < 2 || eye_b.height() < 2) return;

        //세로이동 시 머신러닝 할 것. (상, 중, 하를 볼 때의 ~~를 머신러닝)
        //만약 여기서 accEyeArea.y를 사용하지 않는다면, 윙크시에도 세로인식 되게 한다.

        final int thr = (int) getThreshRatio1000(eye_a.submat(eye_a_area), eye_b.submat(eye_a_area), 1);//이건 eqHist랑 관계x

        if (eye_temp.width() > 10) {//원본으로부터 잘린 eye_temp의 유일한 변형지점
            Imgproc.dilate(eye_temp, eye_temp, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));//살붙이기
            Imgproc.equalizeHist(eye_temp, eye_temp);
        }


        //final int a_bri = (int)getColorAvg(eye_temp.submat(eye_a_area), 1);//클수록 위에본거//getColorAvg는 threshhold된 eye_a가 아닌 흑백이미지서 해야 함
        //final int a_bri = (int)getColorAvgWithThresh(eye_temp.submat(eye_a_area), eye_a.submat(eye_a_area), 1);

        final Rect dg = getAccEyeArea(eye_b.submat(eye_a_area));//dg와 eye_a_area는 모두 인풋 eye_a(eye_b) 내에서 추적된 영역
        dg.x += eye_a_area.x;
        dg.y += eye_a_area.y;//화면표시 때문에 어쩔 수 없다
        //String k = "";for(int i=0; i<dg.width*10/dg.height; i+= 1) k += "=";Log.d("세로", "밝기비율 = "+ k + thr);


        int y1 = 0;//Point E1 = new Point(0,0);
        int y2 = 0;//Point E2 = new Point(0,0);
        E1Loop:
        for (int x = 0; x < eye_a.width(); x++) {
            for (int y = eye_a.height(); y > 0; y--) {
                double colorArr[] = eye_a.get(y, x);
                if (colorArr != null && colorArr[0] < 10) {
                    y1 = y;//E1 = new Point(x,y);
                    Imgproc.circle(eye_temp, new Point(x, y), 3, new Scalar(100, 100, 100, 255), 1);
                    break E1Loop;
                }
            }
        }//눈 왼쪽
        E2Loop:
        for (int x = eye_a.width(); x > 0; x--) {
            for (int y = eye_a.height(); y > 0; y--) {
                double colorArr[] = eye_a.get(y, x);
                if (colorArr != null && colorArr[0] < 10) {
                    y2 = y;//E2 = new Point(x,y);
                    Imgproc.circle(eye_temp, new Point(x, y), 3, new Scalar(100, 100, 100, 255), 1);
                    break E2Loop;
                }
            }
        }//눈 오른쪽


        final int hei = (dg.y - (y1 + y2) / 2) * 20;//20곱한건 dst와 영향력 비슷하게 하기 위함

        boolean learnEyeArea = true;
        if (learn_course <= 4) {
            final int x = dg.x + dg.width / 2;
            if (x < eye_a_area.width / 3 + eye_a_area.tl().x) {
                isLeft = 1;//왼쪽
            } else if (x > eye_a_area.width * 2 / 3 + eye_a_area.tl().x) {
                isLeft = -1;//오른쪽
            } else {
                learnEyeArea = false;
                //이건 가로세로 인식이 너무 예민해. 그리고 세로가 묻힘.
                //화면 밖으로 나가는 경우가 있어서 assertion failed가 뜬다.
                if (dg.width > eye_a_area.width / 2) {//눈에 비해 동공 큼
                    if (x > prev_eye_a_area.width / 2 + prev_eye_a_area.x) {
                        isLeft = 1;
                    } else {
                        isLeft = -1;
                    }
                } else {//눈에 비해 동공 작음
                    if (x < eye_a_area.width / 2 + eye_a_area.tl().x) {
                        isLeft = 1;//왼쪽
                    } else {
                        isLeft = -1;//오른쪽
                    }
                }


                if (eye_a_area.width > eye_a_area.height * 1.5) {
                    isLeft = 0;//가운데
                } else {//세로 사각형//자꾸 왔다갔다 함
                    if (dg.width > eye_a_area.width / 2) {//눈에 비해 동공 큼
                        learnEyeArea = false;
                        if (x > prev_eye_a_area.width / 2 + prev_eye_a_area.x) {
                            isLeft = -1;
                        } else {
                            isLeft = 1;
                        }
                    } else {//눈에 비해 동공 작음
                        if (x < eye_a_area.width / 2 + eye_a_area.tl().x) {
                            isLeft = 1;//왼쪽
                        } else {
                            isLeft = -1;//오른쪽
                        }
                    }
                }


                if (eye_a_area.width > eye_a_area.height * 1.5) {
                    isLeft = 0;//가운데
                } else {//세로 사각형//자꾸 왔다갔다 함
                    learnEyeArea = false;
                    if (dg.width > eye_a_area.width / 2) {//눈에 비해 동공 큼
                        if (x > prev_eye_a_area.width / 2 + prev_eye_a_area.x) {
                            isLeft = -1;
                        } else {
                            isLeft = 1;
                        }
                    } else {//눈에 비해 동공 작음
                        if (x < eye_a_area.width / 2 + eye_a_area.tl().x) {
                            isLeft = 1;//왼쪽
                        } else {
                            isLeft = -1;//오른쪽
                        }
                    }
                }
            }
        }
        if (learnEyeArea) {
            prev_eye_a_area = eye_a_area;
            //Log.i("TEST", "-------------");
        } else {
            eye_a_area = prev_eye_a_area.clone();
            //Log.i("TEST", "영역 정사각형");
        }

        if (learn_course < 4) {
            if (learn_course == 0) {//가운데
                //sayText(6);
                if (hei != 0) hei_m.add(hei);
                if (thr != 0) thr_m.add(thr);
                if (hei_m.size() > learn_framesMAX && thr_m.size() > learn_framesMAX) {
                    a_hei_m = mode(hei_m);
                    avg_thr_m = mode(thr_m);
                    hei_m.clear();
                    thr_m.clear();
                    learn_course++;
                }
            } else if (learn_course == 1) {//윙크
                //getPupilBlink에서 실행함
            } else if (learn_course == 2) {//위
                //sayText(7);
                if (hei != 0) hei_t.add(hei);
                if (thr != 0) thr_t.add(thr);
                if (hei_t.size() > learn_framesMAX && thr_t.size() > learn_framesMAX) {
                    a_hei_t = mode(hei_t);
                    avg_thr_t = mode(thr_t);
                    hei_t.clear();
                    thr_t.clear();
                    learn_course++;
                }
            } else if (learn_course == 3) {//아래
                //sayText(8);
                if (hei != 0) hei_b.add(hei);
                if (thr != 0) thr_b.add(thr);
                if (hei_b.size() > learn_framesMAX && thr_b.size() > learn_framesMAX) {
                    a_hei_b = mode(hei_b);
                    avg_thr_b = mode(thr_b);
                    hei_b.clear();
                    thr_b.clear();
                    learn_course++;//sayText(69);
                    if (a_hei_t == a_hei_m) a_hei_t -= 5;
                    if (a_hei_b == a_hei_m) a_hei_b += 5;//hei끼리 비교후 같은게 있으면 보정하기
                }
            }
        } else if (isLeft == 0) {// if(isLeft == 0) 가로 세로 중 하나만 됨
/*             if(a_bri > avg_bri_t){//머2때의 코드 원본
		        	isUp = 1; //Log.d("세로", "위");
		        }else{
		            eye_b.convertTo(eye_b, -1, 1, 100);
		            if(!isMainEyeWink && dg.width > dg.height * 3){//잘 작동됨
		            	isUp = -1; //Log.d("세로", "아래");
		            }else{
		            	isUp = 0; //Log.d("세로", "-");
		            }
		        }*/

            //Log.d("세로", "t, m, m, b"+ avg_bri_t + ", " + avg_bri_m + " ; " + avg_thr_m + ", " + avg_thr_b);

            Log.d("세로", "Height ; " + hei + " : " + a_hei_t + ", " + a_hei_m + ", " + a_hei_b);
            Log.d("세로", "Thresh ; " + thr + " : " + avg_thr_t + ", " + avg_thr_m + ", " + avg_thr_b);
            //t, m, b -> 86, 77, 82

            final int dstT = (int) (Math.pow(a_hei_t - hei, 2) + Math.pow(avg_thr_t - thr, 2));//3NN알고리즘. 효과 없음
            final int dstM = (int) (Math.pow(a_hei_m - hei, 2) + Math.pow(avg_thr_m - thr, 2));
            final int dstB = (int) (Math.pow(a_hei_b - hei, 2) + Math.pow(avg_thr_b - thr, 2));
            final int min = Math.min(dstT, Math.min(dstM, dstB));

            if (min == dstM) {
                isUp = 0;
            } else if (min == dstB) {
                isUp = -1;
            } else if (min == dstT) {
                isUp = 1;
            }

            if (min == dstM) {
                isUp = 0;
            } else if (dstT == dstB) {
                isUp = 0;
            } else if (min == dstB) {
                isUp = -1;
            } else if (min == dstT) {
                isUp = 1;
            }

        }


        //TODO : 가운데 쳐다보게 머신러닝된 값중 최대값인 a_bri_midmax와 a_bri값을 비교해 위 쳐다봄 -> 아니면 dg값으로 아래 쳐다봄 검사.

        eye_temp = eye_a;//스레시홀드된 눈 영역 리턴

        Imgproc.rectangle(eye_temp, eye_a_area.tl(), eye_a_area.br(), new Scalar(100, 100, 100, 255), 1);//추정된 눈영역 리턴
        Imgproc.rectangle(eye_temp, dg.tl(), dg.br(), new Scalar(200, 200, 200, 255), 1);//추정된 동공영역 리턴


        if (eye_temp.height() > 2 && eye_temp.width() > 2 && eye_temp.type() == 0)
            Imgproc.cvtColor(eye_temp, eye_temp, Imgproc.COLOR_GRAY2RGBA);//표시를 위해 흑백을 rgba로 변환
        //이거 안쓰면 아에 창이 안보임
        //Imgproc.resize(eye_temp, mRgba,mRgba.size());//eye_temp //미니 창으로 표시
    }

    private double getThreshRatio1000(Mat eye_a, Mat eye_b, int roughRate) {
        double p = 0;
        if (eye_a.width() != eye_b.width() || eye_a.height() != eye_b.height()) return 0;
        //int size = mat.width()*mat.height()/(roughRate*roughRate);
        int a = 0, b = 0;
        for (int x = 1; x < eye_a.width() / roughRate; x++) {
            for (int y = 1; y < eye_a.height() / roughRate; y++) {
                double eye_aArr[] = eye_a.get(y * roughRate, x * roughRate);
                double eye_bArr[] = eye_b.get(y * roughRate, x * roughRate);
                if (eye_aArr[0] < 10) a++;
                if (eye_bArr[0] < 10) b++;

            }
        }
        return a == 0 ? 0 : 1000 * b / a;
    }

    private boolean getPupilBlink(Mat eye) {
        updateEyeAB(eye);


        boolean isEmpty = false;
        boolean isBlink = false;

        if (eye_a_area.width > eye_a_area.height * 2) {//윙크검사
            isEmpty = true;
            L:
            for (int x = (int) eye_a_area.tl().x; x < (int) eye_a_area.br().x + 1; x++) {
                for (int y = (int) eye_a_area.tl().y; y < (int) eye_a_area.br().y + 1; y++) {
                    double colorArr[] = eye_b.get(y, x);
                    if (colorArr != null && colorArr[0] < 10) {
                        isEmpty = false;
                        break L;
                    }
                }
            }
            if (isEmpty) isBlink = true;
        } else {
            Floodfill:
            for (int x = 1; x < eye_b.width(); x++) {
                for (int y = 1; y < eye_b.height() / 3; y++) {//정사각형 위쪽 1/3지점을 floodfill//눈썹제거
                    double colorArr[] = eye_b.get(y, x);
                    if (colorArr != null && colorArr[0] < 10) {
                        Imgproc.floodFill(eye_b, Mat.zeros(eye_b.rows() + 2, eye_b.cols() + 2, CvType.CV_8U), new Point(x, y), new Scalar(255, 255, 255));
                        //Log.e("WINK", "-------플러드 필 진행함-------");
                        //break Floodfill;
                    }
                }
            }
        }
        maxH = 0;
        for (int x = 1; x < eye_b.width(); x++) {
            int maxHtemp = 0;
            for (int y = 1; y < eye_b.height(); y++) {
                double colorArr[] = eye_b.get(y, x);
                if (colorArr != null && colorArr[0] < 10) maxHtemp++;
                if (maxHtemp > maxH) maxH = maxHtemp;
            }
        }

        getPupilMove(true);


        if (learn_course == 1) {//윙크 머신러닝

            Log.e("WINK", "윙크 머신러닝 중");
            if (maxH != -1) maxH_list.add(maxH);
            if (maxH_list.size() > learn_framesMAX) {
                maxH_avg = mode(maxH_list);
                maxH_b = Collections.min(maxH_list);
                maxH_t = Collections.max(maxH_list);
                //Log.i("TEST", "maxH_list = "+maxH_list);
                maxH_list.clear();
                learn_course++;
            }

        } else if (learn_course == 4) {//maxH_t는 당연히 일반눈이 나온다

            if (maxH <= maxH_avg) isBlink = true;


            Log.i("WINK", "maxH = " + maxH + ", maxH_t = " + maxH_t + ", maxH_avg = " + maxH_avg + ", maxH_b = " + maxH_b);
        }

        return isBlink;
    }


    private void updateEyeAB(Mat eye) {
        eye.convertTo(eye, -1, 1, 10);
        Imgproc.equalizeHist(eye, eye);//detected area 불러옴
        Imgproc.erode(eye, eye, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));//살붙이기
        Imgproc.GaussianBlur(eye, eye, new Size(5, 5), 5);
        Mat eyeA = new Mat();
        Imgproc.threshold(eye, eyeA, 40, 255, Imgproc.THRESH_BINARY);//눈영역

        for (int y = 0; y < 5; y++) {
            Floodfill:
            for (int x = 1; x < eyeA.width(); x++) {
                double colorArr[] = eyeA.get(y, x);
                if (colorArr != null && colorArr[0] < 10) {
                    Imgproc.floodFill(eyeA, Mat.zeros(eyeA.rows() + 2, eyeA.cols() + 2, CvType.CV_8U), new Point(x, y), new Scalar(255, 255, 255));
                    break Floodfill;
                }
            }
        }
        //eye_a, eye_b는 바이너리된 영역, eye_a_area는 눈의 크기이다. 이게 끝임. 흑백 이미지는 eye_temp에 저장됨. 그건 이거랑 관계x
        eye_a_area = getAccEyeArea(eyeA);//eyeA에서 눈 영역 알아냄
        eye_a = eyeA;
        eye_b = new Mat();
        Imgproc.threshold(eye, eye_b, 7, 255, Imgproc.THRESH_BINARY);//B에 동공표시


    }

    private Rect getAccEyeArea(Mat eyeA) {
        Rect accEyeArea = new Rect();//eyeA에서 알아낸 눈영역
        for (int x = 1; x < eyeA.width(); x++) {
            l:
            for (int y = 1; y < eyeA.height(); y++) {
                double colorArr[] = eyeA.get(y, x);
                if (colorArr != null && colorArr[0] < 10) {
                    if (accEyeArea.x == 0) {
                        accEyeArea.x = x;

                    } else {
                        accEyeArea.width = x - accEyeArea.x;
                    }
                    break l;
                }
            }
        }
        for (int y = 1; y < eyeA.height(); y++) {
            l:
            for (int x = 1; x < eyeA.width(); x++) {
                double colorArr[] = eyeA.get(y, x);
                if (colorArr != null && colorArr[0] < 10) {
                    if (accEyeArea.y == 0) {
                        accEyeArea.y = y;
                    } else {
                        accEyeArea.height = y - accEyeArea.y;
                    }
                    break l;
                }
            }
        }
        return accEyeArea;
    }

    private Rect[] detectedEyesOnEyeArea(Rect eyeArea) {
        //Imgproc.equalizeHist(mGray, mGray);
        Mat testMat = mGray.submat(eyeArea);
        Imgproc.equalizeHist(testMat, testMat);//원본은 이거 주석 안됨

        MatOfRect eyeTest = new MatOfRect();
        if (mAbsoluteEyeSize == 0) {
            int height = testMat.rows();
            if (Math.round(height * mRelativeEyeSize) > 0) {
                mAbsoluteEyeSize = Math.round(height * mRelativeEyeSize);
            }
        }
        if (mJavaDetectorTest != null)
            mJavaDetectorTest.detectMultiScale(testMat, eyeTest, 1.1, 2,
                    2, // objdetect.CV_HAAR_SCALE_IMAGE
                    new Size(mAbsoluteEyeSize, mAbsoluteEyeSize),
                    new Size());
        return eyeTest.toArray();
    }

    private int mode(ArrayList<Integer> arr) {
        // arr에서 최빈값을 반환
        ArrayList<Integer> freqArr = new ArrayList<Integer>();
        for (int k = 0; k < arr.size(); k++) {
            freqArr.add(Collections.frequency(arr, arr.get(k)));
        }

        //arr.indexOf(object)
        //Collections.frequency(arr, 2);
        return arr.get(freqArr.indexOf(Collections.max(freqArr)));
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        Log.i(TAG, "called onCreateOptionsMenu");
//        mItemFace50 = menu.add("Face size 50%");
//        mItemFace40 = menu.add("Face size 40%");
//        mItemFace30 = menu.add("Face size 30%");
//        mItemFace20 = menu.add("Face size 20%");
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
//        if (item == mItemFace50)
//            setMinFaceSize(0.5f);
//        else if (item == mItemFace40)
//            setMinFaceSize(0.4f);
//        else if (item == mItemFace30)
//            setMinFaceSize(0.3f);
//        else if (item == mItemFace20)
//            setMinFaceSize(0.2f);
//
//        return true;
//    }

    private void setMinFaceSize(float faceSize) {
        mRelativeFaceSize = faceSize;
        mAbsoluteFaceSize = 0;
    }

//    private void CreateAuxiliaryMats() {
//        if (mGray.empty())
//            return;
//
//        int rows = mGray.rows();
//        int cols = mGray.cols();
//
//        if (mZoomWindow == null) {
//            mZoomWindow = mRgba.submat(rows / 2 + rows / 10, rows, cols / 2
//                    + cols / 10, cols);
//            mZoomWindow2 = mRgba.submat(0, rows / 2 - rows / 10, cols / 2
//                    + cols / 10, cols);
//        }
//
//    }

    private void match_eye(Rect area, Mat mTemplate, int type) {
        Point matchLoc;
        Mat mROI = mGray.submat(area);
        int result_cols = mROI.cols() - mTemplate.cols() + 1;
        int result_rows = mROI.rows() - mTemplate.rows() + 1;
        // Check for bad template size
        if (mTemplate.cols() == 0 || mTemplate.rows() == 0) {
            return;
        }
        Mat mResult = new Mat(result_cols, result_rows, CvType.CV_8U);

        switch (type) {
            case TM_SQDIFF:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_SQDIFF);
                break;
            case TM_SQDIFF_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult,
                        Imgproc.TM_SQDIFF_NORMED);
                break;
            case TM_CCOEFF:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCOEFF);
                break;
            case TM_CCOEFF_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult,
                        Imgproc.TM_CCOEFF_NORMED);
                break;
            case TM_CCORR:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCORR);
                break;
            case TM_CCORR_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult,
                        Imgproc.TM_CCORR_NORMED);
                break;
        }

        Core.MinMaxLocResult mmres = Core.minMaxLoc(mResult);
        // there is difference in matching methods - best match is max/min value
        if (type == TM_SQDIFF || type == TM_SQDIFF_NORMED) {
            matchLoc = mmres.minLoc;
        } else {
            matchLoc = mmres.maxLoc;
        }

        Point matchLoc_tx = new Point(matchLoc.x + area.x, matchLoc.y + area.y);
        Point matchLoc_ty = new Point(matchLoc.x + mTemplate.cols() + area.x,
                matchLoc.y + mTemplate.rows() + area.y);

        Imgproc.rectangle(mRgba, matchLoc_tx, matchLoc_ty, new Scalar(255, 255, 0,
                255));
        Rect rec = new Rect(matchLoc_tx, matchLoc_ty);


    }

    private Mat get_template(CascadeClassifier clasificator, Rect area, int size) {
        Mat template = new Mat();
        Mat mROI = mGray.submat(area);
        MatOfRect eyes = new MatOfRect();
        Point iris = new Point();
        Rect eye_template = new Rect();
        clasificator.detectMultiScale(mROI, eyes, 1.1, 2,
                Objdetect.CASCADE_FIND_BIGGEST_OBJECT
                        | Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30),
                new Size());

        Rect[] eyesArray = eyes.toArray();
        for (int i = 0; i < eyesArray.length; ) {
            Rect e = eyesArray[i];
            e.x = area.x + e.x;
            e.y = area.y + e.y;
            Rect eye_only_rectangle = new Rect((int) e.tl().x,
                    (int) (e.tl().y + e.height * 0.4), (int) e.width,
                    (int) (e.height * 0.6));
            mROI = mGray.submat(eye_only_rectangle);
            Mat vyrez = mRgba.submat(eye_only_rectangle);


            Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);

            Imgproc.circle(vyrez, mmG.minLoc, 2, new Scalar(255, 255, 255, 255), 2);
            iris.x = mmG.minLoc.x + eye_only_rectangle.x;
            iris.y = mmG.minLoc.y + eye_only_rectangle.y;
            eye_template = new Rect((int) iris.x - size / 2, (int) iris.y
                    - size / 2, size, size);
            Imgproc.rectangle(mRgba, eye_template.tl(), eye_template.br(),
                    new Scalar(255, 0, 0, 255), 2);
            template = (mGray.submat(eye_template)).clone();
            return template;
        }
        return template;
    }

    public void onRecreateClick(View v) {
        learn_course = 0;
    }

}
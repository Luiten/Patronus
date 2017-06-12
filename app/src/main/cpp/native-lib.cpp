
#include "Manager.cpp"
#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/video/tracking.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/opencv.hpp>

using namespace cv;
using namespace std;

Manager mgr;

extern "C"
{
JNIEXPORT jint JNICALL
Java_luiten_patronus_MainActivity_InitializeNativeLib(JNIEnv *, jobject)
{
    mgr.Initialize();
    return 0;
}

JNIEXPORT jint JNICALL
Java_luiten_patronus_FirstActivity_InitializeNativeLib(JNIEnv *, jobject)
{
    mgr.Initialize();
    return 0;
}

JNIEXPORT jint JNICALL
Java_luiten_patronus_FirstActivity_convertNativeLib(JNIEnv *, jobject, jlong addrInput, jlong addrResult)
{
    Mat &img_input = *(Mat *) addrInput;
    Mat &img_result = *(Mat *) addrResult;

    mgr.Execute(img_input, img_result);
    return 0;
}

// FirstActivity에서 사용하는 Pushback_Collision: 매니저 클래스에 가속도 값을 넣는다
JNIEXPORT jint JNICALL
Java_luiten_patronus_FirstActivity_PushbackAccel(JNIEnv *, jobject, jfloat value)
{
    mgr.Pushback_Collision(value);
    return 0;
}

JNIEXPORT jint JNICALL
Java_luiten_patronus_PointActivity_InitializeNativeLib(JNIEnv *, jobject)
{
    return 0;
}

JNIEXPORT jint JNICALL
Java_luiten_patronus_PointActivity_convertNativeLib(JNIEnv *, jobject, jlong addrInput, jlong addrResult)
{
    Mat &img_input = *(Mat *) addrInput;
    Mat &img_result = *(Mat *) addrResult;

    mgr.Execute(img_input, img_result);
    return 0;
}

JNIEXPORT jint JNICALL
Java_luiten_patronus_PointActivity_CaptureImage(JNIEnv *, jobject, jlong addrResult)
{
    Mat &img_result = *(Mat *) addrResult;

    //detectcars.get_result(img_result);
    mgr.GetDetectedCar(img_result);
    return 0;
}

// Manager 클래스에 세팅 값 적용
JNIEXPORT jint JNICALL
Java_luiten_patronus_MainActivity_SetSettings(JNIEnv *, jobject, jint type, jdouble value)
{
    mgr.SetSettings(type, value);
    return 0;
}
}
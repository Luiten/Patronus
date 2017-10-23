
#include "Manager.cpp"

using namespace cv;
using namespace std;

Manager mgr;

extern "C"
{
JNIEXPORT jint JNICALL
Java_luiten_patronus_MainActivity_InitializeNativeLib(JNIEnv *, jobject, jint w, jint h)
{
    mgr.Initialize(w, h);
    return 0;
}

JNIEXPORT jint JNICALL
Java_luiten_patronus_MainActivity_convertNativeLib(JNIEnv *, jobject, jlong addrInput, jlong addrResult, jint iCamera)
{
    Mat &img_input = *(Mat *) addrInput;
    Mat &img_result = *(Mat *) addrResult;

    mgr.Execute(img_input, img_result, iCamera);
    return 0;
}

// FirstActivity에서 사용하는 Pushback_Collision: 매니저 클래스에 가속도 값을 넣는다
JNIEXPORT jint JNICALL
Java_luiten_patronus_MainActivity_PushbackAccel(JNIEnv *, jobject, jfloat value)
{
    mgr.Pushback_Collision(value);
    return 0;
}

JNIEXPORT jint JNICALL
Java_luiten_patronus_PointActivity_InitializeNativeLib(JNIEnv *, jobject, jint w, jint h)
{
    mgr.Initialize(w, h);
    return 0;
}

JNIEXPORT jint JNICALL
Java_luiten_patronus_PointActivity_convertNativeLib(JNIEnv *, jobject, jlong addrInput, jlong addrResult, jint iCamera)
{
    Mat &img_input = *(Mat *) addrInput;
    Mat &img_result = *(Mat *) addrResult;

    mgr.Execute(img_input, img_result, iCamera);
    return 0;
}

JNIEXPORT jintArray JNICALL
Java_luiten_patronus_PointActivity_GetToStandardLane(JNIEnv *env, jobject, jint nLaneType)
{
    int arr[2];
    jintArray result;
    result = env->NewIntArray(2);

    mgr.GetStandardLane(arr, nLaneType);

    env->SetIntArrayRegion(result, 0, 2, arr);
    return result;
}

// Manager 클래스에 세팅 값 적용
JNIEXPORT jint JNICALL
Java_luiten_patronus_SplashActivity_SetSettings(JNIEnv *, jobject, jint type, jdouble value)
{
    mgr.SetSettings(type, value);
    return 0;
}

// Manager 클래스에 세팅 값 적용(데이터용)
JNIEXPORT jint JNICALL
Java_luiten_patronus_MainActivity_SetSettings(JNIEnv *, jobject, jint type, jdouble value)
{
    mgr.SetSettings(type, value);
    return 0;
}

// Manager 클래스에 세팅 값 적용
JNIEXPORT jint JNICALL
Java_luiten_patronus_Setting_SetSettings(JNIEnv *, jobject, jint type, jdouble value)
{
    mgr.SetSettings(type, value);
    return 0;
}

// 수동 녹화용 로그
JNIEXPORT jint JNICALL
Java_luiten_patronus_MainActivity_PrintLogForRecord(JNIEnv *env, jobject, jstring javaString)
{
    // jstring to string(char*) 변경   출처: https://stackoverflow.com/questions/4181934/jni-converting-jstring-to-char
    const char *nativeString = env->GetStringUTFChars(javaString, JNI_FALSE);

    mgr.PrintLogForRecord(nativeString);

    env->ReleaseStringUTFChars(javaString, nativeString);
    return 0;
}

// 값 읽어오기
JNIEXPORT jdouble JNICALL
Java_luiten_patronus_MainActivity_GetValue(JNIEnv *, jobject, jint type)
{
    return mgr.GetValue(type);
}
}
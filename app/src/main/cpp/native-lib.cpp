#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>

using namespace cv;

extern "C"
{
    int process(Mat img_input, Mat &img_result)
    {
        cvtColor( img_input, img_result, CV_RGBA2GRAY);

        return(0);
    }

    JNIEXPORT jint JNICALL
    Java_luiten_patronus_MainActivity_convertNativeLib(JNIEnv*, jobject, jlong addrInput, jlong addrResult)
    {
        Mat &img_input = *(Mat *) addrInput;
        Mat &img_result = *(Mat *) addrResult;

        int conv = process(img_input, img_result);
        int ret = (jint) conv;

        return ret;
    }
}
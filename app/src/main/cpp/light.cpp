//
// Created by power on 2017-06-12.
//

#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include "lib/cbloblabelingrobit.h"
#include "lib/cvblob.h"

using namespace std;
using namespace cv;
using namespace cvb;


struct LightInfo
{
    Rect rect;
    int type;
};

#define PATRONUS_LIGHT_TYPE_RED         1
#define PATRONUS_LIGHT_TYPE_YELLOW      2
#define PATRONUS_LIGHT_TYPE_GREEN       3

class Light
{
public:
    void ExecuteLight(Mat &img_input, Mat &img_result, vector<LightInfo> &listLight)
    {
        Mat frame, gray, matResize;
        Mat matOutput;
        int resizeWidth = 640;
        int resizeHeight = 360;

        listLight.clear();

        resize(img_input, matResize, Size(resizeWidth, resizeHeight));

        Rect Roi(Point(matResize.cols / 6, 0), Point(5 * matResize.cols / 6, matResize.rows / 2));
        frame = matResize(Roi);

        Mat HSV(Roi.size(), CV_8U);
        cvtColor(frame, gray, CV_BGR2GRAY);
        cvtColor(frame, HSV, CV_BGR2HSV);

        Mat red, red1, red2, green, bin, bin_erode, bin_dilate, black, afterBlack;

        //Morph operation 모폴로지 연산
        inRange(HSV, Scalar(0, 0, 80), Scalar(180, 255, 255), bin);

        Mat RectKernel = getStructuringElement(MORPH_RECT, Size(3, 3));

        erode(bin, bin_erode, RectKernel, Point(-1, -1), 1);
        dilate(bin, bin_dilate, RectKernel, Point(-1, -1), 1);

        inRange(HSV, Scalar(0, 0, 0), Scalar(180, 255, 40), black);

        black.copyTo(afterBlack);
        afterBlack = Scalar(0);

        Mat rectKernel = getStructuringElement(MORPH_RECT, Size(3, 3));
        Mat elipseKernel = getStructuringElement(MORPH_ELLIPSE, Size(2, 2));

        morphologyEx(black, black, MORPH_CLOSE, rectKernel, Point(-1, -1), 2);
        morphologyEx(black, black, MORPH_CLOSE, elipseKernel, Point(-1, -1), 2);

        //블랍 레이블링
        CvBlob blobBlack; // Blob Lebeling
        CBlobLabelingRobit imgLabelingBlack(black, 50, afterBlack.cols / 5, afterBlack.rows / 8);

        imgLabelingBlack.doLabeling();

        for (int i = 0; i < imgLabelingBlack.m_nBlobs; i++)
        {

            if (imgLabelingBlack.m_recBlobs[i].width < 2 * imgLabelingBlack.m_recBlobs[i].height ||
                (imgLabelingBlack.m_recBlobs[i].height > afterBlack.cols / 4 || imgLabelingBlack.m_recBlobs[i].width > afterBlack.rows / 4)) {

            }
        }

        afterBlack.copyTo(afterBlack);

        //red
        inRange(HSV, Scalar(0, 100, 50), Scalar(14, 255, 255), red1);
        inRange(HSV, Scalar(162, 100, 50), Scalar(180, 255, 255), red2);
        red = red1 | red2;

        dilate(red, red, RectKernel, Point(-1, -1), 1);

        //green
        inRange(HSV, Scalar(70, 50, 50, 0), Scalar(98, 255, 255, 0), green);
        dilate(green, green, RectKernel, Point(-1, -1), 1);

        //black
        //inRange(HSV, Scalar(0, 0, 50), Scalar(180, 255, 255), trafficLight);

        Mat YCrCb;
        Mat Y_red = HSV, Y_green = HSV;

        cvtColor(frame, YCrCb, CV_RGB2YCrCb);

        //red
        inRange(YCrCb, Scalar(0, 70, 170), Scalar(190, 150, 240), Y_red);
        dilate(Y_red, Y_red, RectKernel, Point(-1, -1), 1);

        //green
        inRange(YCrCb, Scalar(120, 100, 50), Scalar(255, 160, 180), Y_green);
        dilate(Y_green, Y_green, RectKernel, Point(-1, -1), 1);

        Mat AndRed, AndGreen;
        AndRed = red & Y_red;
        AndGreen = green & Y_green;

        Mat R, G, set;
        Mat filterR, filterG;
        frame.copyTo(R);
        frame.copyTo(G);
        frame.copyTo(set);

        vector<Vec3b> rgb;
        //split(set, rgb);
        //set.setTo(rgb[0] - rgb[1] + rgb[0] - rgb[2] - (rgb[1] - rgb[2]));
        for (int i = 0; i < frame.cols; i++) {
            for (int j = 0; j < frame.rows; j++) {
                int r = frame.at<Vec3b>(j, i)[2];
                int g = frame.at<Vec3b>(j, i)[1];
                int b = frame.at<Vec3b>(j, i)[0];
                if (r - g + r - b - abs(g - b) <= 0) {
                    R.at<Vec3b>(j, i)[2] = 0;
                }
                else if (r - g + r - b - abs(g - b) >= 255) {
                    R.at<Vec3b>(j, i)[2] = 255;
                }
                else {
                    R.at<Vec3b>(j, i)[2] = r - g + r - b - abs(g - b);
                }
                R.at<Vec3b>(j, i)[1] = 0;
                R.at<Vec3b>(j, i)[0] = 0;
            }
        }

        for (int i = 0; i < frame.cols; i++) {
            for (int j = 0; j < frame.rows; j++) {
                int r = frame.at<Vec3b>(j, i)[2];
                int g = frame.at<Vec3b>(j, i)[1];
                int b = frame.at<Vec3b>(j, i)[0];
                if (r < 200 && g > 200 && b > 200) {
                    b = 150;
                }

                if ((g - r + (g - b) - abs(r - b)) <= 0) {
                    G.at<Vec3b>(j, i)[1] = 0;
                }
                else if ((g - r + (g - b) - abs(r - b)) >= 255) {
                    G.at<Vec3b>(j, i)[1] = 255;
                }
                else {
                    G.at<Vec3b>(j, i)[1] = (g - r + (g - b) - abs(r - b));
                }
                G.at<Vec3b>(j, i)[2] = 0;
                G.at<Vec3b>(j, i)[0] = 0;
            }
        }

        inRange(R, Scalar(0, 0, 120), Scalar(1, 1, 255), filterR);
        inRange(G, Scalar(0, 20, 0), Scalar(1, 255, 1), filterG);

        filterR = filterR & AndRed;
        filterG = filterG & AndGreen;

        Mat beforeErosion_R, beforeErosion_G;
        Mat ellipseKernel = getStructuringElement(MORPH_ELLIPSE, Size(3, 3));

        filterR.copyTo(beforeErosion_R);
        filterG.copyTo(beforeErosion_G);

        dilate(filterR, filterR, RectKernel, Point(-1, -1), 1);
        dilate(filterG, filterG, RectKernel, Point(-1, -1), 1);

        CvBlob blobR; // Blob Lebeling
        CBlobLabelingRobit imgLabelingR(filterR, 20, frame.cols, frame.rows);

        imgLabelingR.doLabeling();

        for (int i = 0; i<imgLabelingR.m_nBlobs; i++)
        {

            int width, height;
            width = imgLabelingR.m_recBlobs[i].width;
            height = imgLabelingR.m_recBlobs[i].height;
            float light_avg = 0, range_avg = 0, black_avg = 0;

            if (width < frame.cols / 5 && height < frame.rows / 8)
            {
                if (imgLabelingR.m_recBlobs[i].x - 5 > 0 && imgLabelingR.m_recBlobs[i].x + 6 * width < frame.cols && imgLabelingR.m_recBlobs[i].y - 5 > 0 && imgLabelingR.m_recBlobs[i].y < frame.rows)
                {
                    Rect rectOrigLoc;
                    rectOrigLoc = imgLabelingR.m_recBlobs[i];
                    rectOrigLoc.x *= ((float)img_input.cols / resizeWidth);
                    rectOrigLoc.y *= ((float)img_input.rows / resizeHeight);
                    rectOrigLoc.width *= ((float)img_input.cols / resizeWidth);
                    rectOrigLoc.height *= ((float)img_input.rows / resizeHeight);

                    rectOrigLoc.x += img_input.cols / 6;

                    rectangle(img_result, imgLabelingR.m_recBlobs[i], Scalar(0, 255, 255), 2);
                    for (int j = imgLabelingR.m_recBlobs[i].x; j < imgLabelingR.m_recBlobs[i].x + width; j++)
                    {
                        for (int k = imgLabelingR.m_recBlobs[i].y; k < imgLabelingR.m_recBlobs[i].y + height; k++)
                        {
                            light_avg += afterBlack.at<uchar>(k, j);
                        }
                    }
                    light_avg = light_avg / (float)(width * height);


                    for (int j = imgLabelingR.m_recBlobs[i].x + width; j < imgLabelingR.m_recBlobs[i].x + 4 * width; j++)
                    {
                        for (int k = imgLabelingR.m_recBlobs[i].y; k < imgLabelingR.m_recBlobs[i].y + height; k++)
                        {
                            range_avg += bin_erode.at<uchar>(k, j);
                            //black_avg += black.at<uchar>(k, j);
                        }
                    }
                    range_avg = range_avg / (float)((height - 4)* 2 * width);
                    black_avg = black_avg / (float)((height - 4)* 2 * width);

                }
            }
            if (light_avg > 0.75 * 255 && range_avg < 0.15 * 255)
            {
                Rect rectOrigLoc;
                rectOrigLoc = Rect(imgLabelingR.m_recBlobs[i].x - 5, imgLabelingR.m_recBlobs[i].y - 5, height * 4.5, height + 10);
                rectOrigLoc.x *= ((float)img_input.cols / resizeWidth);
                rectOrigLoc.y *= ((float)img_input.rows / resizeHeight);
                rectOrigLoc.width *= ((float)img_input.cols / resizeWidth);
                rectOrigLoc.height *= ((float)img_input.rows / resizeHeight);

                rectOrigLoc.x += img_input.cols / 6;

                rectangle(img_result, Rect(imgLabelingR.m_recBlobs[i].x - 5, imgLabelingR.m_recBlobs[i].y - 5, height * 4.5, height + 10), Scalar(0, 0, 255), 2);

                listLight.push_back({rectOrigLoc, PATRONUS_LIGHT_TYPE_RED});
            }
        }


        CBlobLabelingRobit imgLabelingG(filterG, 20, frame.cols, frame.rows);

        imgLabelingG.doLabeling();

        for (int i = 0; i<imgLabelingG.m_nBlobs; i++)
        {
            float width, height;
            width = imgLabelingG.m_recBlobs[i].width;
            height = imgLabelingG.m_recBlobs[i].height;

            float light_avg = 0, range_avg = 0, black_avg = 0;

            if (width < frame.cols / 5 && height < frame.rows / 8)
            {
                if (imgLabelingG.m_recBlobs[i].x - width * 3.5 > 0 && imgLabelingG.m_recBlobs[i].y - 5 > 0 && imgLabelingG.m_recBlobs[i].x + 2.5 * width < frame.cols && imgLabelingG.m_recBlobs[i].y + height + 5 < frame.rows) {
                    Rect rectOrigLoc;
                    rectOrigLoc = imgLabelingG.m_recBlobs[i];
                    rectOrigLoc.x *= ((float)img_input.cols / resizeWidth);
                    rectOrigLoc.y *= ((float)img_input.rows / resizeHeight);
                    rectOrigLoc.width *= ((float)img_input.cols / resizeWidth);
                    rectOrigLoc.height *= ((float)img_input.rows / resizeHeight);

                    rectOrigLoc.x += img_input.cols / 6;

                    rectangle(img_result, rectOrigLoc, Scalar(0, 255, 255), 2);
                    for (int j = imgLabelingG.m_recBlobs[i].x; j < imgLabelingG.m_recBlobs[i].x + width; j++)
                    {
                        for (int k = imgLabelingG.m_recBlobs[i].y; k < imgLabelingG.m_recBlobs[i].y + height; k++)
                        {
                            light_avg += afterBlack.at<uchar>(k, j);


                        }
                    }

                    light_avg = light_avg / (float)(width * height);

                    for (int j = imgLabelingG.m_recBlobs[i].x - 2 * width; j < imgLabelingG.m_recBlobs[i].x; j++)
                    {
                        for (int k = imgLabelingG.m_recBlobs[i].y; k < imgLabelingG.m_recBlobs[i].y + height; k++)
                        {
                            range_avg += bin_erode.at<uchar>(k, j);
                            black_avg += black.at<uchar>(k, j);

                        }
                    }

                    range_avg = range_avg / (float)((height) * width * 2);
                    black_avg = black_avg / (float)((height) * width * 2);
                }
            }

            if (light_avg > 0.75 * 255 && range_avg < 0.1 * 255)
            {
                Rect rectOrigLoc;
                rectOrigLoc = Rect(imgLabelingG.m_recBlobs[i].x - height * 3, imgLabelingG.m_recBlobs[i].y - 5, height * 4.5, height + 10);
                rectOrigLoc.x *= ((float)img_input.cols / resizeWidth);
                rectOrigLoc.y *= ((float)img_input.rows / resizeHeight);
                rectOrigLoc.width *= ((float)img_input.cols / resizeWidth);
                rectOrigLoc.height *= ((float)img_input.rows / resizeHeight);

                rectOrigLoc.x += img_input.cols / 6;

                rectangle(img_result, Rect(imgLabelingG.m_recBlobs[i].x - height * 3, imgLabelingG.m_recBlobs[i].y - 5, height * 4.5, height + 10), Scalar(0, 255, 0), 2);

                listLight.push_back({rectOrigLoc, PATRONUS_LIGHT_TYPE_GREEN});
            }
        }
    }
};
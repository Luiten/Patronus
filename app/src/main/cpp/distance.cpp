
#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/opencv.hpp>
#include <fstream>
#include "IPM.h"

using namespace cv;
using namespace std;



struct CarInfo
{
    Point start;
    Point end;
    float distance;
};
class Distance
{
private:
    CascadeClassifier classifier;
    CascadeClassifier classifier2;

    int averageSat = 0;
    int averageVal = 0;

    int width, height;
    int resizeWidth, resizeHeight; // 최적화할 영상 크기
    bool bInit = false;

    //clock_t time_begin, time_end;

    //------------------------------------------------------------------------------------------------//
    // 검출된 자동차 상자 그리기
    //------------------------------------------------------------------------------------------------//
    void draw_locations(Mat &img, const vector<Rect> &locations, const Scalar &color)
    {
        if (!locations.empty())
        {
            vector<Rect>::const_iterator loc = locations.begin();
            vector<Rect>::const_iterator end = locations.end();
            for (; loc != end; ++loc)
            {
                rectangle(img, *loc, color, 2);
            }
        }
    }

    //------------------------------------------------------------------------------------------------//
    // 로그
    //------------------------------------------------------------------------------------------------//
    void SaveLog(int ps, int detect, vector<float> vecDist, double dLatitude, double dLongitude)
    {
        static ofstream offile;
        time_t now = time(0); //현재 시간을 time_t 타입으로 저장
        struct tm tstruct;
        char buf[80];
        tstruct = *localtime(&now);

        if (ps < 0)
        {

            if (offile)
            {
                offile.close();
            }

            strftime(buf, sizeof(buf), "%Y-%m-%d_%H-%M", &tstruct); // YYYY-MM-DD.HH:mm:ss 형태의 스트링

            stringstream filename;
            filename << "/sdcard/Patronus/";
            filename << "DistanceLog_" << buf << ".csv";

            offile.open(filename.str(), ofstream::in | ofstream::out | ofstream::app);
            offile << "Width,Height" << endl;
            offile << width << "," << height << endl;
            offile << "Time,Processing Speed(ms),Detected,Latitude,Longitude,Distance" << endl;
            return;
        }

        strftime(buf, sizeof(buf), "%H:%M:%S", &tstruct); // YYYY-MM-DD.HH:mm:ss 형태의 스트링

        offile << buf << "," << ps << "," << detect << "," << dLatitude << "," << dLongitude;
        for (int i = 0; i < vecDist.size(); i++)
        {
            offile << "," << vecDist[i];
        }
        offile << endl;
        //offile.close();
    }

    //------------------------------------------------------------------------------------------------//
    // 두 선분의 교차 검사
    // 출처: http://neoplanetz.tistory.com/entry/OpenCV-2개의-라인에서-교점-찾기Calculate-Intersection-between-Lines
    //------------------------------------------------------------------------------------------------//
    bool GetIntersection(const Point2f AP1, const Point2f AP2, const Point2f BP1, const Point2f BP2, Point2f &result)
    {
        double t;
        double s;
        double under = (BP2.y - BP1.y) * (AP2.x - AP1.x) - (BP2.x - BP1.x) * (AP2.y - AP1.y);
        if (under == 0) return false;

        double _t = (BP2.x - BP1.x) * (AP1.y - BP1.y) - (BP2.y - BP1.y) * (AP1.x - BP1.x);
        double _s = (AP2.x - AP1.x) * (AP1.y - BP1.y) - (AP2.y - AP1.y) * (AP1.x - BP1.x);

        t = _t / under;
        s = _s / under;

        if (t < 0.0 || t > 1.0 || s < 0.0 || s > 1.0) return false;
        if (_t == 0 && _s == 0) return false;

        result.x = AP1.x + t * (double) (AP2.x - AP1.x);
        result.y = AP1.y + t * (double) (AP2.y - AP1.y);

        return true;
    }

public:
    void Initialize(int w, int h)
    {
        if (bInit == false)
        {
            classifier.load("/sdcard/Patronus/cars.xml");
            classifier2.load("/sdcard/Patronus/checkcas.xml");
            bInit = true;
        }

        width = w;
        height = h;

        resizeWidth = 640;
        resizeHeight = 360;

        averageSat = 0;
        averageVal = 0;

        //vector<float> vecDist;
        //SaveLog(-1, 0, vecDist, 0, 0);
        //time_begin = clock();
    }

    void ExecuteDistance(Mat &matInput, Mat &matResult, vector<CarInfo>& listCar, double dLatitude, double dLongitude, Point ptCenter)
    {
        Mat matGray, matResize;
        vector<Rect> vFound;
        vector<Rect> vFound2;
        vector<Rect> vFoundResult;
        Mat matHSV;
        Mat matBinTtack;
        Mat matCanny;
        vector<float> vecDist;
        int nDetect = 0;
        Mat matRoadTarget, matRoadBin;

        vector<Point2f> origPoints;
        vector<Point2f> dstPoints;
        IPM *ipm;

        Mat roi;
        Mat matROIBin;

        width = matInput.cols;
        height = matInput.rows;

        listCar.clear();

        resize(matInput, matResize, Size(resizeWidth, resizeHeight));
        //matResize.copyTo(matOutput);

        //--------------------------------------------------------------------------//
        // HSV 및 이진화   출처: http://webnautes.tistory.com/945
        //--------------------------------------------------------------------------//
        //HSV로 변환
        cvtColor(matInput, matHSV, COLOR_BGR2HSV);

        //지정한 HSV 범위를 이용하여 영상을 이진화
        inRange(matHSV, Scalar(0, 0, 120), Scalar(180, 255, 255), matBinTtack);

        //--------------------------------------------------------------------------//
        // 도로 이진화   출처: http://vgg.fiit.stuba.sk/2016-06/car-detection-in-videos/
        //--------------------------------------------------------------------------//
        matRoadTarget = matHSV(Rect(width / 2 - 30, height * 6 / 7, 60, 10)).clone();
        matRoadBin = matHSV.clone();

        int sumHue = 0, sumSat = 0, sumVal = 0;

        for (int i = 0; i < matRoadTarget.rows; i++)
        {
            for (int j = 0; j < matRoadTarget.cols; j++)
            {
                sumHue += matRoadTarget.at<Vec3b>(i, j)[0];
                sumSat += matRoadTarget.at<Vec3b>(i, j)[1];
                sumVal += matRoadTarget.at<Vec3b>(i, j)[2];
            }
        }

        //averageHue = ((averageHue * 9) + (sumHue / (matRoadTarget.rows * matRoadTarget.cols))) / 10;
        averageSat = ((averageSat * 9) + (sumSat / (matRoadTarget.rows * matRoadTarget.cols))) / 10;
        averageVal = ((averageVal * 9) + (sumVal / (matRoadTarget.rows * matRoadTarget.cols))) / 10;

        inRange(matRoadBin, Scalar(0, averageSat - 30, averageVal - 50), Scalar(180, averageSat + 30, averageVal + 50), matRoadBin);

        rectangle(matResult, Rect(width / 2 - 30, height * 6 / 7, 60, 10), Scalar(0, 0, 255), 2);

        //--------------------------------------------------------------------------//
        // Bird's Eye View   출처: https://marcosnietoblog.wordpress.com/2014/02/22/source-code-inverse-perspective-mapping-c-opencv/
        //--------------------------------------------------------------------------//
        int w = width;
        int h = height / 2;
        Rect rectLaneROI(Point(0, matInput.rows / 2), Point(matInput.cols, matInput.rows));

        Point2f Bird1(0, height);
        Point2f Bird2(width, height);

        // The 4-points at the input image
        origPoints.push_back(Bird1); // *** 운전학원 카메라
        origPoints.push_back(Bird2);
        origPoints.push_back(Point(width, ptCenter.y));
        origPoints.push_back(Point(0, ptCenter.y));

        // 변환할 BEV 영역의 높이
        int nViewHeight = h + ptCenter.y;

        // 변환된 BEV 내에서 중심점에 따라서 dstPoints 3, 4번째 x 이동
        int nBEVMoveX = ((float) ((width / 2) - ptCenter.x) / (width / 2) * (nViewHeight * 8.4));

        // The 4-points correspondences in the destination image
        dstPoints.push_back(Point2f((width / 2) - (width / 13), 720));
        dstPoints.push_back(Point2f((width / 2) + (width / 13), 720));
        dstPoints.push_back(Point2f(width + nBEVMoveX + (nViewHeight * 8.4), 0));
        dstPoints.push_back(Point2f(nBEVMoveX - (nViewHeight * 8.4), 0));

        // IPM object
        ipm = new IPM(Size(width, height), Size(width, height), origPoints, dstPoints);

        //matBirdView = matResize.clone();

        // Process
        //ipm->applyHomography(matBirdView, matBirdOutput);

        // 결과 화면에 Bird's Eye View 영역 보기
        ipm->drawPoints(origPoints, matResult);

        //--------------------------------------------------------------------------//
        // Haar 검출
        //--------------------------------------------------------------------------//
        // Apply the classifier to the frame
        cvtColor(matResize, matGray, COLOR_BGR2GRAY);
        equalizeHist(matGray, matGray);
        classifier.detectMultiScale(matGray, vFound, 1.1, 2, 0 | CASCADE_SCALE_IMAGE, Size(30, 30)); // cars1.xml

        if (!vFound.empty())
        {
            vector< Rect >::const_iterator loc = vFound.begin();
            vector< Rect >::const_iterator end = vFound.end();

            for (; loc != end; ++loc)
            {
                roi = matGray(*loc);
                //classifier2.detectMultiScale(roi, vFound2, 1.1, 2, 0 | CASCADE_SCALE_IMAGE, Size(30, 30)); // checkcas.xml

                //if (!vFound2.empty())
                {
                    Rect rectOrigLoc;
                    rectOrigLoc = *loc;
                    rectOrigLoc.x *= ((float)width / resizeWidth);
                    rectOrigLoc.y *= ((float)height / resizeHeight);
                    rectOrigLoc.width *= ((float)width / resizeWidth);
                    rectOrigLoc.height *= ((float)height / resizeHeight);

                    //--------------------------------------------------------------------------//
                    // 자동차, 도로 검출
                    //--------------------------------------------------------------------------//
                    bool bCarFind = false;

                    matROIBin = matRoadBin(rectOrigLoc);

                    sumHue = 0;
                    for (int i = matROIBin.rows / 4 * 3; i < matROIBin.rows; i++)
                    {
                        for (int j = 0; j < matROIBin.cols; j++)
                        {
                            sumHue += matROIBin.at<uchar>(i, j);
                        }
                    }

                    int avg = sumHue / ((matROIBin.rows / 4) * matROIBin.cols);

                    if (avg > 150)
                    {
                        sumHue = 0;
                        for (int i = matROIBin.rows / 4 * 2; i < matROIBin.rows / 4 * 3; i++)
                        {
                            for (int j = matROIBin.cols / 4 * 1; j < matROIBin.cols / 4 * 3; j++)
                            {
                                sumHue += matROIBin.at<uchar>(i, j);
                            }
                        }

                        avg = sumHue / ((matROIBin.rows / 4) * matROIBin.cols);

                        if (avg < 60)
                        {
                            bCarFind = true;
                        }
                    }

                    rectangle(matROIBin, Rect(0, matROIBin.rows / 4 * 3, matROIBin.cols, matROIBin.rows / 4), Scalar(0), 2);
                    rectangle(matROIBin, Rect(matROIBin.cols / 4, matROIBin.rows / 4 * 2, matROIBin.cols / 4 * 2, matROIBin.rows / 4), Scalar(0), 2);

                    if (!bCarFind) continue;

                    nDetect++;
                    vFoundResult.push_back(rectOrigLoc);

                    //--------------------------------------------------------------------------//
                    // 차선 뷰(Bird's Eye View) 관심영역에 드는지 검사
                    //--------------------------------------------------------------------------//
                    bCarFind = false;
                    //----------------------------------------------------//
                    // 박스 아래 선
                    //----------------------------------------------------//
                    Point2f temp;
                    // 뷰 왼쪽 선
                    if (GetIntersection(origPoints[0], origPoints[3],
                                        Point2f(rectOrigLoc.x, rectOrigLoc.y + rectOrigLoc.height), Point2f(rectOrigLoc.x + rectOrigLoc.width, rectOrigLoc.y + rectOrigLoc.height), temp))
                    {
                        bCarFind = true;
                    }
                    // 뷰 오른쪽 선
                    if (GetIntersection(origPoints[1], origPoints[2],
                                        Point2f(rectOrigLoc.x, rectOrigLoc.y + rectOrigLoc.height), Point2f(rectOrigLoc.x + rectOrigLoc.width, rectOrigLoc.y + rectOrigLoc.height), temp))
                    {
                        bCarFind = true;
                    }
                    // 뷰 중앙 위쪽 선
                    if (GetIntersection(origPoints[3], origPoints[2],
                                        Point2f(rectOrigLoc.x, rectOrigLoc.y + rectOrigLoc.height), Point2f(rectOrigLoc.x + rectOrigLoc.width, rectOrigLoc.y + rectOrigLoc.height), temp))
                    {
                        bCarFind = true;
                    }

                    //----------------------------------------------------//
                    // 박스 왼쪽 선
                    //----------------------------------------------------//
                    if (GetIntersection(origPoints[0], origPoints[3],
                                        Point2f(rectOrigLoc.x, rectOrigLoc.y), Point2f(rectOrigLoc.x, rectOrigLoc.y + rectOrigLoc.height), temp))
                    {
                        bCarFind = true;
                    }
                    if (GetIntersection(origPoints[1], origPoints[2],
                                        Point2f(rectOrigLoc.x, rectOrigLoc.y), Point2f(rectOrigLoc.x, rectOrigLoc.y + rectOrigLoc.height), temp))
                    {
                        bCarFind = true;
                    }
                    // 뷰 중앙 위쪽 선
                    if (GetIntersection(origPoints[3], origPoints[2],
                                        Point2f(rectOrigLoc.x, rectOrigLoc.y), Point2f(rectOrigLoc.x, rectOrigLoc.y + rectOrigLoc.height), temp))
                    {
                        bCarFind = true;
                    }

                    //----------------------------------------------------//
                    // 박스 오른쪽 선
                    //----------------------------------------------------//
                    if (GetIntersection(origPoints[0], origPoints[3],
                                        Point2f(rectOrigLoc.x + rectOrigLoc.width, rectOrigLoc.y), Point2f(rectOrigLoc.x + rectOrigLoc.width, rectOrigLoc.y + rectOrigLoc.height), temp))
                    {
                        bCarFind = true;
                    }
                    if (GetIntersection(origPoints[1], origPoints[2],
                                        Point2f(rectOrigLoc.x + rectOrigLoc.width, rectOrigLoc.y), Point2f(rectOrigLoc.x + rectOrigLoc.width, rectOrigLoc.y + rectOrigLoc.height), temp))
                    {
                        bCarFind = true;
                    }
                    // 뷰 중앙 위쪽 선
                    if (GetIntersection(origPoints[3], origPoints[2],
                                        Point2f(rectOrigLoc.x + rectOrigLoc.width, rectOrigLoc.y), Point2f(rectOrigLoc.x + rectOrigLoc.width, rectOrigLoc.y + rectOrigLoc.height), temp))
                    {
                        bCarFind = true;
                    }

                    //--------------------------------------------------------------------------//
                    // 자동차를 찾은 경우 추적 및 거리를 계산한다
                    //--------------------------------------------------------------------------//
                    if (bCarFind)
                    {
                        //--------------------------------------------------------------------------//
                        // 거리 계산
                        //--------------------------------------------------------------------------//
                        //----------------------------------------------------//
                        // 자동차 끝부분 검출
                        //----------------------------------------------------//
                        matCanny = matInput(rectOrigLoc).clone();
                        cvtColor(matCanny, matCanny, COLOR_BGR2HSV);

                        //지정한 HSV 범위를 이용하여 영상을 이진화

                        //cvtColor(matResult, matResult, COLOR_BGR2HSV);
                        inRange(matCanny, Scalar(0, 0, 50), Scalar(180, 255, 255), matCanny);
                        //inRange(matCanny, Scalar(50, 50, 50), Scalar(255, 255, 255), matCanny);
                        Canny(matCanny, matCanny, 10, 200, 3);

                        vector<Vec2f> lines;
                        int threshold = 30; // r,θ 평면에서 몇개의 곡선이 한점에서 만났을 때 직선으로 판단할지에 대한 최소값
                        HoughLines(matCanny, lines, 1, CV_PI / 180, threshold);

                        //matCanny = matResize(rectOrigLoc);

                        //----------------------------------------------------//
                        // 선 검출   출처: http://hongkwan.blogspot.kr/2013/01/opencv-7-2-example.html
                        //----------------------------------------------------//
                        int max = -1;
                        Point maxpt1, maxpt2;

                        // 선 벡터를 반복해 선 그리기
                        vector<Vec2f>::const_iterator it = lines.begin();
                        while (it != lines.end())
                        {
                            float rho = (*it)[0];   // 첫 번째 요소는 rho 거리
                            float theta = (*it)[1]; // 두 번째 요소는 델타 각도
                            if (theta > 1.8 * CV_PI / 4. && theta < 2.2 * CV_PI / 4.)
                            {
                                // 수평 행
                                Point pt1(0, rho / sin(theta)); // 첫 번째 열에서 해당 선의 교차점
                                Point pt2(matCanny.cols, (rho - matCanny.cols * cos(theta)) / sin(theta));
                                if (pt1.y + pt2.y > max)
                                {
                                    max = pt1.y + pt2.y;
                                    maxpt1 = pt1;
                                    maxpt2 = pt2;
                                }
                            }
                            ++it;
                        }

                        //----------------------------------------------------//
                        // Bird's Eye View 내에서 거리 계산
                        //----------------------------------------------------//
                        if (max > 0)
                        {
                            // 해당 직선을 수평으로 맞추기
                            maxpt1.y = maxpt2.y = MIN(maxpt1.y, maxpt2.y) + (abs(maxpt1.y - maxpt2.y) / 2);

                            // 기존 영상 크기에 맞게 직선 위치 변환
                            maxpt1.y = maxpt2.y = maxpt1.y + rectOrigLoc.y;
                            maxpt1.x = 0; maxpt2.x = width;

                            // 마지막 열에서 해당 선의 교차점
                            line(matResult, maxpt1, maxpt2, cv::Scalar(255, 0, 0), 2); // 빨간 선으로 그리기

                            // 해당 직선이 거리계산 관심영역(IPM)에 들면 IPM에 맞는 직선 변환
                            if (origPoints[0].y >= maxpt1.y && origPoints[3].y <= maxpt1.y)
                            {
                                maxpt1 = ipm->applyHomography(maxpt1);
                                maxpt2 = ipm->applyHomography(maxpt2);

                                float tempDist = 0.2 * (dstPoints[0].y - maxpt1.y);

                                vecDist.push_back(tempDist);

                                stringstream text;
                                text << tempDist;
                                text << "m";

                                putText(matResult, text.str(), Point(rectOrigLoc.x, rectOrigLoc.y), 2, 1.7, Scalar(255, 0, 0), 2);
                            }
                        }
                    }
                }
            }
        }

        draw_locations(matResult, vFoundResult, Scalar(0, 255, 0));

        delete ipm;

        matGray.release();
        matResize.release();
        matHSV.release();
        matBinTtack.release();
        matCanny.release();
        matRoadTarget.release();
        matRoadBin.release();
        roi.release();
        matROIBin.release();
        vFound.clear();
        vFound2.clear();
        vFoundResult.clear();
        origPoints.clear();
        dstPoints.clear();
        vecDist.clear();

        //time_end = clock();
        //SaveLog((time_end - time_begin) * 1000 / CLOCKS_PER_SEC, nDetect, vecDist, fLatitude, fLongitude);
        //time_begin = time_end;
    }
};
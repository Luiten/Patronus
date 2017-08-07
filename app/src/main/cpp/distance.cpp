
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
    Point ptCenter;

    int width, height;
    bool bInit = false;

    timeval time_begin, time_end;

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

    void SaveLog(int ps, int detect, vector<float> vecDist, float fLatitude, float fLongitude)
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

        offile << buf << "," << ps << "," << detect << "," << fLatitude << "," << fLongitude;
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

    void eliminateOutLiers(vector<Vec2f> &aLines)
    {
        int num = aLines.size();
        if (num == 0 || num == 1 || num == 2)
            return;

        for (int i = 0; i < aLines.size(); i++)
        {
            float i_theta = aLines[i][1];
            float i_degree = i_theta * 180 / CV_PI;

            for (int j = i + 1; j < aLines.size();)
            {
                float j_theta = aLines[j][1];
                float j_degree = j_theta * 180 / CV_PI;

                if (abs(i_degree - j_degree) < 5)
                {
                    aLines.erase(aLines.begin() + j);
                }
                else
                {
                    j++;
                }
            }
        }
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

        averageSat = 0;
        averageVal = 0;
        ptCenter = Point(width / 2, (height / 2) / 3);

        vector<float> vecDist;
        SaveLog(-1, 0, vecDist, 0, 0);
        gettimeofday(&time_begin, NULL);
    }

    void ExecuteDistance(Mat &matInput, Mat &matResult, vector<CarInfo>& listCar, float fLatitude, float fLongitude)
    {
        Mat matGray, matResize, matBirdOutput;
        vector<Rect> vFound;
        vector<Rect> vFound2;
        vector<Rect> vFoundResult;

        Mat matHSV;
        Mat matBirdView;
        Mat matBinTtack;
        Mat matCanny;

        vector<float> vecDist;
        int nDetect = 0;

        //int averageHue = 0;

        listCar.clear();

        resize(matInput, matResize, Size(width, height));
        //matResize.copyTo(matOutput);

        //--------------------------------------------------------------------------//
        // CamShift를 위한 HSV 및 이진화   출처: http://webnautes.tistory.com/945
        //--------------------------------------------------------------------------//
        //HSV로 변환
        cvtColor(matResize, matHSV, COLOR_BGR2HSV);

        //지정한 HSV 범위를 이용하여 영상을 이진화
        inRange(matHSV, Scalar(0, 0, 120), Scalar(180, 255, 255), matBinTtack);

        //--------------------------------------------------------------------------//
        // 도로 이진화   출처: http://vgg.fiit.stuba.sk/2016-06/car-detection-in-videos/
        //--------------------------------------------------------------------------//
        Mat matRoadTarget = matHSV(Rect(width / 2 - 30, height - 100, 60, 10)).clone();
        Mat matRoadBin = matHSV.clone();

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

        rectangle(matResult, Rect(width / 2 - 30, height - 100, 60, 10), Scalar(0, 0, 255), 2);

        //--------------------------------------------------------------------------//
        // 차선 검출
        //--------------------------------------------------------------------------//
        //Rect Roi(Point(matResize.cols / 5, 65 * matResize.rows / 100), Point(4 * matResize.cols / 5, matResize.rows));
        Rect Roi(Point(0, matResize.rows * 50 / 100), Point(matResize.cols, matResize.rows));
        Mat matLaneROI = matResult(Roi);

        cvtColor(matLaneROI, matGray, CV_BGR2GRAY);

        GaussianBlur(matGray, matGray, Size(3, 3), 1.5, 1.5);

        cv::Mat grad;
        int scale = 1;
        int delta = 0;
        int ddepth = CV_16S;

        // Generate grad_x and grad_y
        Mat grad_x, grad_y;
        Mat sobel_dx, sobel_dy;

        // Gradient X
        Sobel(matGray, grad_x, ddepth, 1, 0, 3, scale, delta, BORDER_DEFAULT);
        convertScaleAbs(grad_x, sobel_dx);

        // Gradient Y
        Sobel(matGray, grad_y, ddepth, 0, 1, 3, scale, delta, BORDER_DEFAULT);
        convertScaleAbs(grad_y, sobel_dy);

        // Total Gradient (approximate)
        cv::addWeighted(sobel_dx, 2, sobel_dy, 2, 0, grad);

        Mat non_maxima = matGray.clone();

        Mat sobel;

        grad.copyTo(sobel);

        int i, j, h, w, tan255, tan675, index, magni;
        float slope;
        bool maxima;
        tan255 = 424;
        tan675 = 2472;
        h = grad.rows;
        w = grad.cols;
        for (i = 1; i < h; i++)
        {
            for (j = 1; j < w; j++)
            {
                index = w * i + j;
                maxima = true;
                magni = grad.at<uchar>(i, j);
                non_maxima.at<uchar>(i, j) = 0;
                if (magni > 30)
                {
                    if (sobel_dx.at<uchar>(i, j) != 0)
                    {

                        slope = (sobel_dy.at<uchar>(i, j) << 10) / sobel_dx.at<uchar>(i, j);
                        if (slope > 0)
                        {
                            if (slope < tan255)
                            {
                                if (magni < sobel.at<uchar>(i, j - 1) ||
                                    magni < sobel.at<uchar>(i, j + 1))
                                    maxima = false;
                            }
                            else if (slope < tan675)
                            {
                                if (magni < sobel.at<uchar>(i - 1, j + 1) ||
                                    magni < sobel.at<uchar>(i - 1, j - 1))
                                    maxima = false;
                            }
                            else //2
                            {
                                if (magni < sobel.at<uchar>(i - 1, j) ||
                                    magni < sobel.at<uchar>(i + 1, j))
                                    maxima = false;
                            }
                        } else
                        {
                            if (slope < -tan675)
                            {
                                if (magni < sobel.at<uchar>(i + 1, j) ||
                                    magni < sobel.at<uchar>(i - 1, j))
                                    maxima = false;
                            }
                            else if (slope < -tan255)
                            {
                                if (magni < sobel.at<uchar>(i - 1, j - 1) ||
                                    magni < sobel.at<uchar>(i + 1, j + 1))
                                    maxima = false;
                            }
                            else
                            {
                                if (magni < sobel.at<uchar>(i, j - 1) ||
                                    magni < sobel.at<uchar>(i, j + 1))
                                    maxima = false;
                            }
                        }
                        if (maxima)
                            non_maxima.at<uchar>(i, j) = sobel.at<uchar>(i, j);

                    }
                }
            }
        }

        threshold(non_maxima, non_maxima, 200, 255, 0);

        Mat right_border;

        non_maxima.copyTo(right_border);

        for (int i = 1; i < h - 1; i++)
        {
            for (int j = 1; j < w - 5; j++)
            {
                if (right_border.at<uchar>(i, j) == 255)
                {
                    for (int t = 4; t <= 50; t++)
                    {
                        if (j + t < w)
                        {
                            right_border.at<uchar>(i, j + t) = 0;
                        } else
                        {
                            right_border.at<uchar>(i, j) = 0;
                        }
                    }
                    if (j + 50 < w)
                        j = j + 50;
                    else
                        j = w - 1;
                }
            }
        }

        Mat left_border;

        non_maxima.copyTo(left_border);

        for (int i = 1; i < h - 1; i++)
        {
            for (int j = w - 1; j > 3; j--)
            {
                if (left_border.at<uchar>(i, j) == 255)
                {
                    for (int t = 4; t <= 50; t++)
                    {
                        if (j - t > 0)
                        {
                            left_border.at<uchar>(i, j - t) = 0;
                        } else
                        {
                            left_border.at<uchar>(i, j) = 0;
                        }
                    }
                    if (j - 50 > 0)
                        j = j - 50;
                    else
                        j = 4;
                }
            }
        }

        vector<Vec2f> s_lines;
        vector<Vec2f> left_lines;
        vector<Vec2f> right_lines;
        int hough_threshold = w / 15;

        HoughLines(right_border, s_lines, 1, CV_PI / 180, hough_threshold, 0, 0);

        for (int i = 0; i < s_lines.size(); i++)
        {
            float r = s_lines[i][0], theta = s_lines[i][1];
            float degree = theta * 180 / CV_PI;

            if (90 < degree && degree < 170) // lane on leftside
            {
                right_lines.push_back(s_lines[i]);
            }
            if (10 < degree && degree < 90) // lane on leftside
            {
                left_lines.push_back(s_lines[i]);
            }
        }

        eliminateOutLiers(right_lines);
        eliminateOutLiers(left_lines);

        //drawLines(matLaneROI, right_lines);
        //drawLines(matLaneROI, left_lines);

        vector<Vec2f> vLines;
        for (int i = 0; i < right_lines.size(); i++)
        {
            vLines.push_back(right_lines[i]);
        }
        for (int i = 0; i < left_lines.size(); i++)
        {
            vLines.push_back(left_lines[i]);
        }

        // 소실점 검출
        vector<Point2f> vInterPoint;

        // 교차점 구하기
        int num = vLines.size();
        for (int i = 0; i < num - 1; i++)
        {
            float rho = vLines[i][0];   // 첫 번째 요소는 rho 거리
            float theta = vLines[i][1]; // 두 번째 요소는 델타 각도

            Point pt1(rho / cos(theta), 0); // 첫 행에서 해당 선의 교차점
            Point pt2((rho - h * sin(theta)) / cos(theta), h);

            for (int j = i + 1; j < num; j++)
            {
                float rho = vLines[j][0];   // 첫 번째 요소는 rho 거리
                float theta = vLines[j][1]; // 두 번째 요소는 델타 각도

                Point pt3(rho / cos(theta), 0); // 첫 행에서 해당 선의 교차점
                Point pt4((rho - h * sin(theta)) / cos(theta), h);

                Point2f result;

                if (GetIntersection(pt1, pt2, pt3, pt4, result))
                {
                    vInterPoint.push_back(result);
                    //circle(matLaneROI, result, 3, Scalar(255, 0, 0), CV_FILLED, CV_AA);
                }
            }
        }

        // 중심으로부터 가장 가까운 점 찾기
        float minDis, raito;
        minDis = raito = sqrt(pow(w, 2) + pow(h, 2)) * 0.07;
        Point2f minPt;
        for (i = 0; i < vInterPoint.size(); i++)
        {
            if (minDis > sqrt(pow(ptCenter.x - vInterPoint[i].x, 2) + pow(ptCenter.y - vInterPoint[i].y, 2)))
            {
                minPt = vInterPoint[i];
                minDis = sqrt(pow(ptCenter.x - vInterPoint[i].x, 2) + pow(ptCenter.y - vInterPoint[i].y, 2));
            }
        }

        vector<Point2f> vResultPoint;
        // 만약 중심과 일정 범위 내 가까운 점을 찾았다면
        if (minDis < raito)
        {
            // 그 근처 점들을 모으기
            for (i = 0; i < vInterPoint.size(); i++)
            {
                if (raito > sqrt(pow(minPt.x - vInterPoint[i].x, 2) + pow(minPt.y - vInterPoint[i].y, 2)))
                {
                    vResultPoint.push_back(vInterPoint[i]);
                }

            }
        }

        //circle(matLaneROI, minPt, 10, Scalar(0, 0, 255), CV_FILLED, CV_AA);

        if (!vResultPoint.empty())
        {
            // 점들 평균 구하기
            Point2f avgPt;
            for (i = 0; i < vResultPoint.size(); i++)
            {
                avgPt.x += vResultPoint[i].x;
                avgPt.y += vResultPoint[i].y;
            }

            avgPt.x = avgPt.x / vResultPoint.size();
            avgPt.y = avgPt.y / vResultPoint.size();

            //for (i = 0; i < vResultPoint.size(); i++)
            //{
            //	circle(matLaneROI, vResultPoint[i], 3, Scalar(0, 0, 255), CV_FILLED, CV_AA);
            //}

            //circle(matLaneROI, avgPt, raito, Scalar(0, 0, 255), 3, CV_AA);

            // 비율로 센터 점 옮기기
            ptCenter.x = (ptCenter.x * 9 + avgPt.x) / 10;
            ptCenter.y = (ptCenter.y * 9 + avgPt.y) / 10;
        }

        circle(matLaneROI, ptCenter, 10, Scalar(255, 0, 0), CV_FILLED, CV_AA);

        //--------------------------------------------------------------------------//
        // Bird's Eye View   출처: https://marcosnietoblog.wordpress.com/2014/02/22/source-code-inverse-perspective-mapping-c-opencv/
        //--------------------------------------------------------------------------//
        Point2f Bird1(0, height);
        Point2f Bird2(width, height);
        vector<Point2f> origPoints;
        vector<Point2f> dstPoints;
        IPM *ipm;

        // The 4-points at the input image
        origPoints.push_back(Bird1); // *** 운전학원 카메라
        origPoints.push_back(Bird2);
        origPoints.push_back(Point(width, ptCenter.y + (height - h)));
        origPoints.push_back(Point(0, ptCenter.y + (height - h)));

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

        matBirdView = matResize.clone();

        // Process
        ipm->applyHomography(matBirdView, matBirdOutput);

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
            vector<Rect>::const_iterator loc = vFound.begin();
            vector<Rect>::const_iterator end = vFound.end();

            for (; loc != end; ++loc)
            {
                Mat roi = matGray(*loc);
                classifier2.detectMultiScale(roi, vFound2, 1.1, 2, 0 | CASCADE_SCALE_IMAGE, Size(30, 30)); // checkcas.xml

                if (!vFound2.empty())
                {
                    //--------------------------------------------------------------------------//
                    // 자동차, 도로 검출
                    //--------------------------------------------------------------------------//
                    bool bCarFind = false;

                    Mat matROIBin = matRoadBin(*loc);

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
                            for (int j = matROIBin.cols / 4 * 1;
                                 j < matROIBin.cols / 4 * 3; j++)
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
                                        Point2f((*loc).x, (*loc).y + (*loc).height), Point2f((*loc).x + (*loc).width, (*loc).y + (*loc).height), temp))
                    {
                        bCarFind = true;
                    }
                    // 뷰 오른쪽 선
                    if (GetIntersection(origPoints[1], origPoints[2],
                                        Point2f((*loc).x, (*loc).y + (*loc).height), Point2f((*loc).x + (*loc).width, (*loc).y + (*loc).height), temp))
                    {
                        bCarFind = true;
                    }
                    // 뷰 중앙 위쪽 선
                    if (GetIntersection(origPoints[3], origPoints[2],
                                        Point2f((*loc).x, (*loc).y + (*loc).height), Point2f((*loc).x + (*loc).width, (*loc).y + (*loc).height), temp))
                    {
                        bCarFind = true;
                    }

                    //----------------------------------------------------//
                    // 박스 왼쪽 선
                    //----------------------------------------------------//
                    if (GetIntersection(origPoints[0], origPoints[3],
                                        Point2f((*loc).x, (*loc).y), Point2f((*loc).x, (*loc).y + (*loc).height), temp))
                    {
                        bCarFind = true;
                    }
                    if (GetIntersection(origPoints[1], origPoints[2],
                                        Point2f((*loc).x, (*loc).y), Point2f((*loc).x, (*loc).y + (*loc).height), temp))
                    {
                        bCarFind = true;
                    }
                    // 뷰 중앙 위쪽 선
                    if (GetIntersection(origPoints[3], origPoints[2],
                                        Point2f((*loc).x, (*loc).y), Point2f((*loc).x, (*loc).y + (*loc).height), temp))
                    {
                        bCarFind = true;
                    }

                    //----------------------------------------------------//
                    // 박스 오른쪽 선
                    //----------------------------------------------------//
                    if (GetIntersection(origPoints[0], origPoints[3],
                                        Point2f((*loc).x + (*loc).width, (*loc).y), Point2f((*loc).x + (*loc).width, (*loc).y + (*loc).height), temp))
                    {
                        bCarFind = true;
                    }
                    if (GetIntersection(origPoints[1], origPoints[2],
                                        Point2f((*loc).x + (*loc).width, (*loc).y), Point2f((*loc).x + (*loc).width, (*loc).y + (*loc).height), temp))
                    {
                        bCarFind = true;
                    }
                    // 뷰 중앙 위쪽 선
                    if (GetIntersection(origPoints[3], origPoints[2],
                                        Point2f((*loc).x + (*loc).width, (*loc).y), Point2f((*loc).x + (*loc).width, (*loc).y + (*loc).height), temp))
                    {
                        bCarFind = true;
                    }

                    //--------------------------------------------------------------------------//
                    // 자동차를 찾은 경우 추적 및 거리를 계산한다
                    //--------------------------------------------------------------------------//
                    if (bCarFind)
                    {
                        vFoundResult.push_back(*loc);

                        //--------------------------------------------------------------------------//
                        // 거리 계산
                        //--------------------------------------------------------------------------//
                        //----------------------------------------------------//
                        // 자동차 끝부분 검출
                        //----------------------------------------------------//
                        matCanny = matResize(*loc).clone();
                        cvtColor(matCanny, matCanny, COLOR_BGR2HSV);

                        //지정한 HSV 범위를 이용하여 영상을 이진화

                        //cvtColor(matResult, matResult, COLOR_BGR2HSV);
                        inRange(matCanny, Scalar(0, 0, 50), Scalar(180, 255, 255), matCanny);
                        //inRange(matCanny, Scalar(50, 50, 50), Scalar(255, 255, 255), matCanny);
                        Canny(matCanny, matCanny, 10, 200, 3);

                        vector<Vec2f> lines;
                        int threshold = 30; // r,θ 평면에서 몇개의 곡선이 한점에서 만났을 때 직선으로 판단할지에 대한 최소값
                        HoughLines(matCanny, lines, 1, CV_PI / 180, threshold);

                        //matCanny = matResize(*loc);

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
                            maxpt1.y = maxpt2.y = maxpt1.y + (*loc).y;
                            maxpt1.x = 0;
                            maxpt2.x = width;

                            // 마지막 열에서 해당 선의 교차점
                            line(matResult, maxpt1, maxpt2, cv::Scalar(255, 0, 0), 1); // 빨간 선으로 그리기

                            // 해당 직선이 거리계산 관심영역(IPM)에 들면 IPM에 맞는 직선 변환
                            if (origPoints[0].y >= maxpt1.y && origPoints[3].y <= maxpt1.y)
                            {
                                maxpt1 = ipm->applyHomography(maxpt1);
                                maxpt2 = ipm->applyHomography(maxpt2);

                                // 마지막 열에서 해당 선의 교차점
                                line(matBirdOutput, maxpt1, maxpt2, cv::Scalar(255, 0, 0), 2); // 빨간 선으로 그리기

                                // 거리 계산
                                float tempDist = 0.4 * (dstPoints[0].y - maxpt1.y);

                                vecDist.push_back(tempDist);

                                stringstream text;
                                text << tempDist;
                                text << "m";

                                putText(matResult, text.str(), Point((*loc).x, (*loc).y), 2, 1.3, Scalar(255, 0, 0), 2);
                            }
                        }
                    }
                }
            }
        }

        draw_locations(matResult, vFoundResult, Scalar(0, 255, 0));

        delete ipm;

        gettimeofday(&time_end, NULL);
        SaveLog((time_end.tv_usec - time_begin.tv_usec) / 1000, nDetect, vecDist, fLatitude, fLongitude);
        time_begin = time_end;
    }
};
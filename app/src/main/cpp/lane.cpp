
#include <vector>
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>



using namespace std;
using namespace cv;



struct LaneInfo
{
    Point start;
    Point end;
    int type; // 1 = 중앙선, 0 = 흰색 실선
};

class Lane {
private:
    int width, height;
    int resizeWidth, resizeHeight; // 최적화할 영상 크기
    int statWidth, statHeight; // 이전 영상 크기
    Point ptCenter;

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

    //------------------------------------------------------------------------------------------------//
    // 불필요한 선 제거
    //------------------------------------------------------------------------------------------------//
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

    //------------------------------------------------------------------------------------------------//
    // 원-직선 교차 확인
    // 출처: https://stackoverflow.com/questions/6091728/line-segment-circle-intersection
    //------------------------------------------------------------------------------------------------//
    bool CircleLineIntersection(float x1, float y1, float x2, float y2, float cx, float cy, float cr ) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float a = dx * dx + dy * dy;
        float b = 2 * (dx * (x1 - cx) + dy * (y1 - cy));
        float c = cx * cx + cy * cy;
        c += x1 * x1 + y1 * y1;
        c -= 2 * (cx * x1 + cy * y1);
        c -= cr * cr;
        float bb4ac = b * b - 4 * a * c;

        if (bb4ac < 0) {
            return false;    // No collision
        } else {
            return true;      //Collision
        }
    }

public:
    Point GetCenterPoint()
    {
        return Point(ptCenter.x, ptCenter.y + (height / 2));
    }

    void ExecuteLane(Mat &matInput, Mat &matResult, vector<LaneInfo>& listLane)
    {
        Mat matGray, matResize;
        Mat matHSV;
        Mat matLaneROI;

        // Generate grad_x and grad_y
        Mat grad_x, grad_y;
        Mat sobel_dx, sobel_dy;
        Mat non_maxima;
        Mat sobel;
        Mat right_border;
        //Mat left_border;

        width = matInput.cols;
        height = matInput.rows;
        resizeWidth = 640;
        resizeHeight = 360;

        listLane.clear();

        // 초기값이 설정되지 않은 경우 값 설정
        if (ptCenter.x <= 0 && ptCenter.y <= 0)
        {
            ptCenter = Point(width / 2, (height / 2) / 3);
            statWidth = width;
            statHeight = height;
        }

        // 영상 크기가 달라진 경우 센터점 영상에 맞게 옮기기
        if (width != statWidth || height != statHeight)
        {
            ptCenter.x *= (float) width / statWidth;
            ptCenter.y *= (float) height / statHeight;
            statWidth = width;
            statHeight = height;
        }

        resize(matInput, matResize, Size(resizeWidth, resizeHeight));

        //--------------------------------------------------------------------------//
        // 차선 검출
        //--------------------------------------------------------------------------//
        //Rect Roi(Point(matResize.cols / 5, 65 * matResize.rows / 100), Point(4 * matResize.cols / 5, matResize.rows));
        Rect rectResizeROI(Point(0, matResize.rows / 2), Point(matResize.cols, matResize.rows));
        //Mat matLaneROI = matOutput(Roi);
        matLaneROI = matResize(rectResizeROI);

        cvtColor(matLaneROI, matGray, CV_BGR2GRAY);

        GaussianBlur(matGray, matGray, Size(3, 3), 1.5, 1.5);

        cv::Mat grad;
        int scale = 1;
        int delta = 0;
        int ddepth = CV_16S;

        // Gradient X
        Sobel(matGray, grad_x, ddepth, 1, 0, 3, scale, delta, BORDER_DEFAULT);
        convertScaleAbs(grad_x, sobel_dx);

        // Gradient Y
        Sobel(matGray, grad_y, ddepth, 0, 1, 3, scale, delta, BORDER_DEFAULT);
        convertScaleAbs(grad_y, sobel_dy);

        // Total Gradient (approximate)
        cv::addWeighted(sobel_dx, 2, sobel_dy, 2, 0, grad);

        non_maxima = matGray.clone();

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
                index = w*i + j;
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
                                if (magni < sobel.at<uchar>(i, j - 1) || magni < sobel.at<uchar>(i, j + 1))
                                    maxima = false;
                            }
                            else if (slope < tan675)
                            {
                                if (magni < sobel.at<uchar>(i - 1, j + 1) || magni < sobel.at<uchar>(i - 1, j - 1))
                                    maxima = false;
                            }
                            else //2
                            {
                                if (magni < sobel.at<uchar>(i - 1, j) || magni < sobel.at<uchar>(i + 1, j))
                                    maxima = false;
                            }
                        }
                        else
                        {
                            if (slope < -tan675)
                            {
                                if (magni < sobel.at<uchar>(i + 1, j) || magni < sobel.at<uchar>(i - 1, j))
                                    maxima = false;
                            }
                            else if (slope < -tan255)
                            {
                                if (magni < sobel.at<uchar>(i - 1, j - 1) || magni < sobel.at<uchar>(i + 1, j + 1))
                                    maxima = false;
                            }
                            else
                            {
                                if (magni < sobel.at<uchar>(i, j - 1) || magni < sobel.at<uchar>(i, j + 1))
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

        non_maxima.copyTo(right_border);

        for (int i = 1; i < h - 1; i++)
        {
            for (int j = 1; j < w - 10; j++)
            {
                if (right_border.at<uchar>(i, j) == 255)
                {
                    for (int t = 4; t <= 50; t++)
                    {
                        if (j + t < w)
                        {
                            right_border.at<uchar>(i, j + t) = 0;
                        }
                        else
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

/*        non_maxima.copyTo(left_border);

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
                        }
                        else
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
        }*/

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
            right_lines[i][0] = right_lines[i][0] * ((float)width / resizeWidth); // 해상도에 따른 거리 보정
            vLines.push_back(right_lines[i]);
        }
        for (int i = 0; i < left_lines.size(); i++)
        {
            left_lines[i][0] = left_lines[i][0] * ((float)width / resizeWidth); // 해상도에 따른 거리 보정
            vLines.push_back(left_lines[i]);
        }

        // 소실점 검출
        vector<Point2f> vInterPoint;

        w = width;
        h = height / 2;
        Rect rectLaneROI(Point(0, matInput.rows / 2), Point(matInput.cols, matInput.rows));
        matLaneROI = matResult(rectLaneROI);

        //drawLines(matLaneROI, vLines);

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



        vector<Vec2f>::const_iterator it = vLines.begin();
        while (it != vLines.end())
        {
            float rho = (*it)[0];   // 첫 번째 요소는 rho 거리
            float theta = (*it)[1]; // 두 번째 요소는 델타 각도
            //if (theta > 1.0 * CV_PI / 4. && theta < 3.0 * CV_PI / 4.)
            {
                // 수평 행
                //Point pt1(0, rho / sin(theta)); // 첫 번째 열에서 해당 선의 교차점
                //Point pt2(non_maxima.cols, (rho - non_maxima.cols * cos(theta)) / sin(theta));


                float rho = (*it)[0];
                float theta = (*it)[1];

                Point pt1(rho / cos(theta), 0); // point of intersection of the line with first row
                Point pt2((rho - matLaneROI.rows*sin(theta)) / cos(theta), matLaneROI.rows); // point of interseaction of the line with last row
                //line(frame, pt1, pt2, Scalar(0, 255, 0), 2);
                //line(hough, pt1, pt2, Scalar(255), 8);

                // cout<<"line : (" << rho <<", " <<theta<<")"<<endl;

                if (CircleLineIntersection(pt1.x, pt1.y, pt2.x, pt2.y, ptCenter.x, ptCenter.y, raito)) {
                    pt1.y += matLaneROI.rows;
                    pt2.y += matLaneROI.rows;

                    listLane.push_back({ pt1, pt2, 0 });
                }

                //line(non_maxima, maxpt1, maxpt2, cv::Scalar(0, 0, 255), 1); // 빨간 선으로 그리기
            }
            ++it;
        }


    }

};
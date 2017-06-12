//
// Created by power on 2017-06-12.
//

#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/objdetect.hpp>
using namespace std;
using namespace cv;


struct LightInfo
{
    Point center;
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
        Mat HSV;
        Mat filterR, filterG;
        Mat Red, Green;

        listLight.clear();
        cvtColor(img_input, HSV, COLOR_RGB2HSV);

        img_input.copyTo(Red);
        //int r = 0;
        //putText(frame, "rows : " + r, Point(10, 10), 5, 3, Scalar(0, 255, 0), 2);


        Mat Red_Img, Green_Img, Yellow_Img;

        Mat HSV_Roi = HSV(Rect(1 * Red.cols / 6, 0, 5 * Red.cols / 6, 1 * Red.rows / 2));

        Mat Roi = img_input(Rect(1 * Red.cols / 6, 0, 5 * Red.cols / 6, 1 * Red.rows / 2));


        Mat red;
        Mat red1(Roi.size(), Roi.type());
        Mat red2(Roi.size(), Roi.type());
        Mat green(Roi.size(), Roi.type());
        Mat yellow(Roi.size(), Roi.type());

        //red
        cv::inRange(HSV_Roi, cv::Scalar(0, 80, 80, 0), cv::Scalar(20, 255, 160, 0), red1);
        cv::inRange(HSV_Roi, cv::Scalar(150, 80, 80, 0), cv::Scalar(180, 255, 160, 0), red2);
        red = red1 | red2;

        //yellow
        cv::inRange(HSV_Roi, cv::Scalar(90, 110, 100, 0), cv::Scalar(120, 255, 180, 0), yellow);

        //green
        cv::inRange(HSV_Roi, cv::Scalar(33, 80, 80, 0), cv::Scalar(96, 255, 160, 0), green);


        Mat red_canny, green_canny, yellow_canny;

        blur(green, green_canny, Size(3, 3));
        Canny(green_canny, green_canny, 125, 125 * 3, 3);


        GaussianBlur(green, green, Size(3, 3), 2, 2);
        Mat mask = cv::getStructuringElement(cv::MORPH_RECT, cv::Size(3, 3), cv::Point(1, 1));
        morphologyEx(green, green, cv::MorphTypes::MORPH_CLOSE, mask);
        vector<Vec3f> circles_g;
        vector<Vec3f> circles_g1;
        HoughCircles(green_canny, circles_g, CV_HOUGH_GRADIENT, 2, img_input.rows / 4, 20, 10,
                     img_input.rows / 150, img_input.rows / 20);
        HoughCircles(green, circles_g1, CV_HOUGH_GRADIENT, 2, img_input.rows / 4, 20, 10,
                     img_input.rows / 150, img_input.rows / 20);


        for (int i = 0; i < circles_g.size(); i++) {
            Point center(cvRound(circles_g[i][0]), cvRound(circles_g[i][1]));
            int radius = cvRound(circles_g[i][2]);
            bool isTrafficLight = false;
            double color = 0.0;
            for (int w = 0; w < radius * 4; w++) {
                for (int h = 0; h < radius * 1.2; h++) {
                    int x, y;
                    if (center.x - radius - w <= 0) {
                        x = 0;
                    } else {
                        x = center.x - radius - w;
                    }
                    if (center.y - 0.6 * radius + h <= 0) {
                        y = 0;
                    } else if (center.y - 0.6 * radius + h >= HSV.rows) {
                        y = HSV_Roi.rows;
                    } else {
                        y = center.y - 0.6 * radius + h;
                    }
                    if (HSV_Roi.at<Vec3b>(y, x)[2] < 80 && HSV_Roi.at<Vec3b>(y, x)[1] < 80)
                        color++;
                }
            }
            if (color / double(radius * radius * 4.8) > 0.7) {
                isTrafficLight = true;
                circle(Roi, center, 3, Scalar(0, 255, 0), -1, 8, 0);
                circle(Roi, center, radius, Scalar(0, 255, 0), 3, 8, 0);

                listLight.push_back( { { center.x + (Red.cols / 6), center.y }, PATRONUS_LIGHT_TYPE_GREEN } );
            }
            rectangle(Roi, Point(center.x - 2 * radius, center.y - 0.6 * radius),
                      Point(center.x - 2 * radius - radius * 4, center.y + 1.2 * radius),
                      Scalar(0, 0, 0), 3);
            //circle(Roi, center, radius, Scalar(0, 0, 0), 3, 8, 0);

        }
        for (int i = 0; i < circles_g1.size(); i++) {
            Point center(cvRound(circles_g1[i][0]), cvRound(circles_g1[i][1]));
            int radius = cvRound(circles_g1[i][2]);
            bool isTrafficLight = false;
            double color = 0.0;
            for (int w = 0; w < radius * 4; w++) {
                for (int h = 0; h < 1.2 * radius; h++) {
                    int x, y;
                    if (center.x - 2 * radius - w <= 1) {
                        x = 1;
                    } else {
                        x = center.x - 2 * radius - w;
                    }
                    if (center.y - 0.6 * radius + h <= 1) {
                        y = 1;
                    } else if (center.y - 0.6 * radius + h >= HSV.rows) {
                        y = HSV_Roi.rows - 1;
                    } else {
                        y = center.y - 0.6 * radius + h;
                    }
                    if (HSV_Roi.at<Vec3b>(y, x)[2] < 80 && HSV_Roi.at<Vec3b>(y, x)[1] < 80)
                        color++;
                }
            }
            rectangle(Roi, Point(center.x - 2 * radius, center.y - 0.6 * radius),
                      Point(center.x - 2 * radius - radius * 4, center.y + 1.2 * radius),
                      Scalar(0, 255, 0), 3);
            if (color / double(radius * radius * 4.8) > 0.7) {
                isTrafficLight = true;
                //circle(Roi, center, 3, Scalar(0, 255, 0), -1, 8, 0);
                circle(Roi, center, radius, Scalar(0, 255, 0), 3, 8, 0);
                listLight.push_back( { { center.x + (Red.cols / 6), center.y },  PATRONUS_LIGHT_TYPE_GREEN } );
            }
            //circle(Roi, center, radius, Scalar(0, 255, 0), 3, 8, 0);
        }
        blur(red, red_canny, Size(3, 3));
        Canny(red_canny, red_canny, 125, 125 * 3, 3);
        vector<Vec3f> circles_r;
        vector<Vec3f> circles_r1;
        HoughCircles(red_canny, circles_r, CV_HOUGH_GRADIENT, 2, img_input.rows / 4, 20, 10,
                     img_input.rows / 150, img_input.rows / 20);
        HoughCircles(red, circles_r1, CV_HOUGH_GRADIENT, 2, img_input.rows / 4, 20, 10,
                     img_input.rows / 150, img_input.rows / 20);
        for (int i = 0; i < circles_r.size(); i++) {
            Point center(cvRound(circles_r[i][0]), cvRound(circles_r[i][1]));
            int radius = cvRound(circles_r[i][2]);
            bool isTrafficLight = false;
            double color = 0.0;
            for (int w = 0; w < radius * 4; w++) {
                for (int h = 0; h < radius * 1.4; h++) {
                    int x, y;
                    if (center.x + 2 * radius + w >= HSV.cols) {
                        x = HSV_Roi.cols - 1;
                    } else {
                        x = center.x + 2 * radius + w;
                    }
                    if (center.y - 0.7 * radius + h <= 1) {
                        y = 1;
                    } else if (center.y - 0.7 * radius + h >= HSV.rows) {
                        y = HSV_Roi.rows - 1;
                    } else {
                        y = center.y - 0.7 * radius + h;
                    }
                    if (HSV_Roi.at<Vec3b>(y, x)[2] < 80 && HSV_Roi.at<Vec3b>(y, x)[1] < 80)
                        color++;
                }
            }
            if (color / double(radius * radius * 6) > 0.7) {
                isTrafficLight = true;
                circle(Roi, center, 3, Scalar(255, 0, 0), -1, 8, 0);
                circle(Roi, center, radius, Scalar(255, 0, 0), 3, 8, 0);
                listLight.push_back( { { center.x + (Red.cols / 6), center.y },  PATRONUS_LIGHT_TYPE_RED } );
            }
            rectangle(Roi, Point(center.x + 2 * radius, center.y - 0.7 * radius),
                      Point(center.x + 6 * radius, center.y + 0.7 * radius), Scalar(0, 0, 0), 3);


        }
        for (int i = 0; i < circles_r1.size(); i++) {
            Point center(cvRound(circles_r1[i][0]), cvRound(circles_r1[i][1]));
            int radius = cvRound(circles_r1[i][2]);
            bool isTrafficLight = false;
            double color = 0.0;
            for (int w = 0; w < radius * 4; w++) {
                for (int h = 0; h < radius * 1.4; h++) {
                    int x, y;
                    if (center.x + 2 * radius + w >= HSV.cols) {
                        x = HSV_Roi.cols - 1;
                    } else {
                        x = center.x + 2 * radius + w;
                    }
                    if (center.y - 0.7 * radius + h <= 1) {
                        y = 1;
                    } else if (center.y - 0.7 * radius + h >= HSV.rows) {
                        y = HSV_Roi.rows - 1;
                    } else {
                        y = center.y - 0.7 * radius + h;
                    }
                    if (HSV_Roi.at<Vec3b>(y, x)[2] < 80 && HSV_Roi.at<Vec3b>(y, x)[1] < 80)
                        color++;
                }
            }
            if (color / double(radius * radius * 6) > 0.7) {
                isTrafficLight = true;
                circle(Roi, center, 3, Scalar(255, 0, 0), -1, 8, 0);
                circle(Roi, center, radius, Scalar(255, 0, 0), 3, 8, 0);

                listLight.push_back( { { center.x + (Red.cols / 6), center.y },  PATRONUS_LIGHT_TYPE_RED } );
            }
            rectangle(Roi, Point(center.x + 2 * radius, center.y - 0.7 * radius),
                      Point(center.x + 6 * radius, center.y + 0.7 * radius), Scalar(255, 0, 0), 3);
        }

        blur(yellow, yellow_canny, Size(3, 3));
        Canny(yellow_canny, yellow_canny, 125, 125 * 3, 3);
        vector<Vec3f> circles_y;
        vector<Vec3f> circles_y1;
        HoughCircles(yellow_canny, circles_r, CV_HOUGH_GRADIENT, 2, red_canny.rows / 4, 20, 10,
                     red_canny.rows / 60, red_canny.rows / 20);
        HoughCircles(yellow, circles_r1, CV_HOUGH_GRADIENT, 2, red_canny.rows / 4, 20, 10,
                     red_canny.rows / 60, red_canny.rows / 20);
        for (int i = 0; i < circles_y.size(); i++) {
            Point center(cvRound(circles_y[i][0]), cvRound(circles_y[i][1]));
            int radius = cvRound(circles_y[i][2]);
            bool isTrafficLight = false;
            double color = 0.0;
            for (int w = 0; w < radius * 2; w++) {
                for (int h = 0; h < radius * 1.4; h++) {
                    if (HSV.at<Vec3b>(center.y - 0.7 * radius + h, center.x + radius + w)[2] < 80 &&
                        HSV.at<Vec3b>(center.y - 0.7 * radius + h, center.x + radius + w)[1] < 80)
                        color++;
                    if (HSV.at<Vec3b>(center.y - 0.7 * radius + h, center.x - radius - w)[2] < 80 &&
                        HSV.at<Vec3b>(center.y - 0.7 * radius + h, center.x - radius - w)[1] < 80)
                        color++;
                }
            }
            if (color / double(radius * radius * 6) > 0.8) {
                isTrafficLight = true;
                //circle(image, center, 3, Scalar(0, 255, 255), -1, 8, 0);
                circle(Roi, center, radius, Scalar(0, 255, 255), 3, 8, 0);

                listLight.push_back( { { center.x + (Red.cols / 6), center.y }, PATRONUS_LIGHT_TYPE_YELLOW } );
            }
            //circle(Roi, center, radius, Scalar(0, 255, 255), 3, 8, 0);
        }
        for (int i = 0; i < circles_y1.size(); i++) {
            Point center(cvRound(circles_y1[i][0]), cvRound(circles_y1[i][1]));
            int radius = cvRound(circles_y1[i][2]);
            bool isTrafficLight = false;
            double color = 0.0;
            for (int w = 0; w < radius * 2; w++) {
                for (int h = 0; h < radius * 1.4; h++) {
                    if (HSV.at<Vec3b>(center.y - 0.7 * radius + h, center.x + radius + w)[2] < 80 &&
                        HSV.at<Vec3b>(center.y - 0.7 * radius + h, center.x + radius + w)[1] < 80)
                        color++;
                    if (HSV.at<Vec3b>(center.y - 0.7 * radius + h, center.x - radius - w)[2] < 80 &&
                        HSV.at<Vec3b>(center.y - 0.7 * radius + h, center.x - radius - w)[1] < 80)
                        color++;
                }
            }
            if (color / double(radius * radius * 6) > 0.8) {
                isTrafficLight = true;
                //circle(image, center, 3, Scalar(0, 255, 255), -1, 8, 0);
                circle(Roi, center, radius, Scalar(0, 255, 255), 3, 8, 0);

                listLight.push_back( { { center.x + (Red.cols / 6), center.y }, PATRONUS_LIGHT_TYPE_YELLOW } );
            }
            //circle(Roi, center, radius, Scalar(0, 255, 255), 3, 8, 0);
        }

        //img_input.copyTo(img_result);
    }
};

// https://github.com/abhi-kumar/CAR-DETECTION

#include <opencv2/core/core.hpp>
#include <opencv2/video/tracking.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/opencv.hpp>
#include <iterator>
//#include "IPM.h"

using namespace cv;
using namespace std;


struct CarInfo
{
    Point start;
    Point end;
    float distance;
};

class cars     //main class
{
public:	    //variables kept public but precaution taken all over the code

    Mat image_input;          //main input image
    Mat image_main_result;    //the final result
    Mat storage;              //introduced to stop detection of same car more than once

    CascadeClassifier cascade;    //the main cascade classifier
    CascadeClassifier checkcascade;        //a test classifier,car detected by both main and test is stated as car
    //Ptr<Tracker> tracker;

    int num;

    void getimage(Mat src) //getting the input image
    {

        if(! src.data )
        {
            //cout <<  "src not filled" << endl ;
        }

        else
        {
            image_input = src.clone();
            storage = src.clone();              //initialising storage
            image_main_result = src.clone();    //initialising result

            //cout << "got image" <<endl;
        }
    }


    void cascade_load(string cascade_string)            //loading the main cascade
    {
        //cascade.load(cascade_string);

        if( !cascade.load(cascade_string) )
        {
            //cout << endl << "Could not load classifier cascade" << endl;
        }
        else
        {
            //cout << "cascade : " << cascade_string << " loaded" << endl;
        }

    }

    void checkcascade_load(string checkcascade_string)               //loading the test/check cascade
    {
        // Instead of MIL, you can also use BOOSTING, KCF, TLD, MEDIANFLOW or GOTURN
        //tracker = Tracker::create( "MIL" );

        //checkcascade.load(checkcascade_string);

        if( !checkcascade.load(checkcascade_string) )
        {
            //cout << endl << "Could not load classifier checkcascade" << endl;
        }
        else
        {
            //cout<< "checkcascade : " << checkcascade_string << " loaded" << endl;
        }
    }


    void get_result(Mat& img_result)             // function to display input
    {
        if(!image_main_result.empty() )
        {
            image_main_result.copyTo(img_result);
        }
    }

    void get_input(Mat& img_result)            //function to display output
    {
        if(!image_input.empty() )
        {
            image_input.copyTo(img_result);
        }
    }

    void setnum()
    {
        num = 0;
    }

    void findcars(vector <CarInfo>& listCar)                 //main function
    {
        int i = 0;

        listCar.clear();

        Mat img = storage.clone();
        Mat temp;                    //for region of interest.If a car is detected(after testing) by one classifier,then it will not be available for other one

        if(img.empty() )
        {
            //cout << endl << "detect not successful" << endl;
        }
        int cen_x;
        int cen_y;
        vector<Rect> cars;
        const static Scalar colors[] =  { CV_RGB(0,0,255),CV_RGB(0,255,0),CV_RGB(255,0,0),CV_RGB(255,255,0),CV_RGB(255,0,255),CV_RGB(0,255,255),CV_RGB(255,255,255),CV_RGB(128,0,0),CV_RGB(0,128,0),CV_RGB(0,0,128),CV_RGB(128,128,128),CV_RGB(0,0,0)};

        Mat gray;

        cvtColor(img, gray, CV_BGR2GRAY);

        //Mat resize_image(cvRound (img.rows), cvRound(img.cols), CV_8UC1 );

        //resize( gray, resize_image, resize_image.size(), 0, 0, INTER_LINEAR );
        equalizeHist(gray, gray);


        cascade.detectMultiScale(gray, cars, 1.1, 2, 0 | CASCADE_SCALE_IMAGE, Size(30, 30));                 //detection using main classifier


        for( vector<Rect>::const_iterator main = cars.begin(); main != cars.end(); main++, i++ )
        {
            Mat resize_image_reg_of_interest;
            vector<Rect> nestedcars;
            Point center;
            Scalar color = colors[i%8];


            //getting points for bouding a rectangle over the car detected by main
            int x0 = cvRound(main->x);
            int y0 = cvRound(main->y);
            int x1 = cvRound((main->x + main->width-1));
            int y1 = cvRound((main->y + main->height-1));



            if(checkcascade.empty())
            {
                continue;
            }

            //resize_image_reg_of_interest = gray(*main);
            //checkcascade.detectMultiScale( resize_image_reg_of_interest, nestedcars, 1.1, 2,0, Size(30, 30));

            //for( vector<Rect>::const_iterator sub = nestedcars.begin(); sub != nestedcars.end(); sub++ )      //testing the detected car by main using checkcascade
            {
                //center.x = cvRound((main->x + sub->x + sub->width*0.5));        //getting center points for bouding a circle over the car detected by checkcascade
                //cen_x = center.x;
                //center.y = cvRound((main->y + sub->y + sub->height*0.5));
                //cen_y = center.y;
                //if(cen_x>(x0+15) && cen_x<(x1-15) && cen_y>(y0+15) && cen_y<(y1-15))         //if centre of bounding circle is inside the rectangle boundary over a threshold the the car is certified
                {
                    //Rect roi = Rect(cvPoint(x0, y0), cvPoint(x1, y1));
                    //Mat ROIimg = image_input(roi);

                    rectangle( image_main_result, cvPoint(x0,y0),
                               cvPoint(x1,y1),
                               color ,3, 8, 0);               //detecting boundary rectangle over the final result

                    //getdistance(ROIimg);

                    listCar.push_back({cvPoint(x0, y0), cvPoint(x1, y1), ((float)400 / (x1 - x0)) * ((float)400 / (x1 - x0))});







                    Mat matROI, matHSV, matBin;
                    matHSV = image_input.clone();

                    //----------------------------------------------------//
                    // Left
                    //----------------------------------------------------//
                    Rect tempRect = *main;

                    // 자동차 영역
                    tempRect.x += (tempRect.width / 6);
                    tempRect.y += (tempRect.height / 4);
                    tempRect.width -= (tempRect.width / 4);
                    tempRect.height -= (tempRect.height / 3);

                    // 램프 영역
                    tempRect.x += 0;
                    tempRect.y += 0;
                    tempRect.width -= (tempRect.width / 2) + (tempRect.width / 6);
                    tempRect.height -= (tempRect.height / 4);

                    cvtColor(matHSV, matHSV, COLOR_BGR2HSV);
                    matROI = matHSV(tempRect);

                    // HSV   출처: http://webnautes.tistory.com/942
                    //지정한 HSV 범위를 이용하여 영상을 이진화
                    inRange(matROI, Scalar(170, 0, 0), Scalar(179, 255, 255), matBin);

                    //morphological matBin 작은 점들을 제거
                    erode(matBin, matBin, getStructuringElement(MORPH_ELLIPSE, Size(5, 5)));
                    dilate(matBin, matBin, getStructuringElement(MORPH_ELLIPSE, Size(5, 5)));

                    //morphological closing 영역의 구멍 메우기
                    dilate(matBin, matBin, getStructuringElement(MORPH_ELLIPSE, Size(5, 5)));
                    erode(matBin, matBin, getStructuringElement(MORPH_ELLIPSE, Size(5, 5)));

                    //라벨링
                    Mat img_labels, stats, centroids;
                    int numOfLables1 = connectedComponentsWithStats(matBin, img_labels, stats, centroids, 8, CV_32S);
                    //rectangle(image_main_result, tempRect, Scalar(255, 255, 255), 2);

                    //----------------------------------------------------//
                    // Right
                    //----------------------------------------------------//
                    Rect tempRect2 = *main;

                    // 자동차 영역
                    tempRect2.x += tempRect2.width - (tempRect2.width / 6);
                    tempRect2.y += (tempRect2.height / 4);
                    tempRect2.width -= (tempRect2.width / 4);
                    tempRect2.height -= (tempRect2.height / 3);

                    // 램프 영역
                    tempRect2.y += 0;
                    tempRect2.width -= (tempRect2.width / 2) + (tempRect2.width / 6);
                    tempRect2.height -= (tempRect2.height / 4);
                    tempRect2.x -= tempRect2.width;

                    //cvtColor(mResize, matHSV, COLOR_BGR2HSV);
                    matROI = matHSV(tempRect2);

                    // HSV   출처: http://webnautes.tistory.com/942
                    //지정한 HSV 범위를 이용하여 영상을 이진화
                    inRange(matROI, Scalar(170, 0, 0), Scalar(179, 255, 255), matBin);

                    //morphological matBin 작은 점들을 제거
                    erode(matBin, matBin, getStructuringElement(MORPH_ELLIPSE, Size(5, 5)));
                    dilate(matBin, matBin, getStructuringElement(MORPH_ELLIPSE, Size(5, 5)));

                    //morphological closing 영역의 구멍 메우기
                    dilate(matBin, matBin, getStructuringElement(MORPH_ELLIPSE, Size(5, 5)));
                    erode(matBin, matBin, getStructuringElement(MORPH_ELLIPSE, Size(5, 5)));

                    //라벨링
                    int numOfLables2 = connectedComponentsWithStats(matBin, img_labels, stats, centroids, 8, CV_32S);
                    //rectangle(image_main_result, tempRect2, Scalar(255, 255, 255), 2);




                    // ***** 해결해야 할 점: 무조건 첫번째로 대칭되는 점만 헤드라이트로 인식하는데, 가끔 다른 영역이 잘못 검출되므로
                    //							영역 크기나 추적을 통해 이전에 검출되었던 해당 위치를 집중적으로 인식해야함

                    //영역박스 그리기
                    for (int i = 1; i < numOfLables1; i++)
                    {
                        int area1 = stats.at<int>(i, CC_STAT_AREA);

                        int left1 = stats.at<int>(i, CC_STAT_LEFT);
                        int top1 = stats.at<int>(i, CC_STAT_TOP);
                        int width1 = stats.at<int>(i, CC_STAT_WIDTH);
                        int height1 = stats.at<int>(i, CC_STAT_HEIGHT);

                        for (int j = 1; j < numOfLables2; j++)
                        {
                            int area2 = stats.at<int>(j, CC_STAT_AREA);

                            int left2 = stats.at<int>(j, CC_STAT_LEFT);
                            int top2 = stats.at<int>(j, CC_STAT_TOP);
                            int width2 = stats.at<int>(j, CC_STAT_WIDTH);
                            int height2 = stats.at<int>(j, CC_STAT_HEIGHT);

                            // 높이가 5픽셀 이하로 비슷한 경우
                            if (abs(top1 - top2) < 5)
                            {
                                rectangle(image_main_result, Point(left1 + tempRect.x, top1 + tempRect.y),
                                          Point(left1 + width1 + tempRect.x, top1 + height1 + tempRect.y), Scalar(0, 255, 255), 2);
                                rectangle(image_main_result, Point(left2 + tempRect2.x, top2 + tempRect2.y),
                                          Point(left2 + width2 + tempRect2.x, top2 + height2 + tempRect2.y), Scalar(0, 255, 255), 2);

                                line(image_main_result, Point(left1 + tempRect.x, top1 + tempRect.y + (height1 / 2)),
                                     Point(left2 + tempRect2.x + width2, top2 + tempRect2.y + (height2 / 2)), Scalar(0, 255, 0), 2);



                                // 임시 거리 계산
                                float meter = abs((left1 + tempRect.x) - (left2 + tempRect2.x + width2));
                                meter = 400 / meter;

                                stringstream text;
                                text << meter;
                                text << "m";

                                putText(image_main_result, text.str(), Point((*main).x, (*main).y), 2, 0.7, Scalar::all(255));
                                //vFoundResult.push_back(*loc);

                                i = numOfLables1;
                                break;
                            }
                        }
                    }









/*                    stringstream text;
                    text << ((float)400 / (x1 - x0)) * ((float)400 / (x1 - x0));
                    text << "m";

                    //sprintf("%.1fm", text, (float)(x1 - x0) / 50);
                    putText(image_main_result, text.str(), cvPoint(x0, y0), FONT_HERSHEY_SIMPLEX, 1.0, Scalar(255, 0, 0), 3);*/

                    //masking the detected car to detect second car if present

                    Rect region_of_interest = Rect(x0, y0, x1-x0, y1-y0);
                    temp = storage(region_of_interest);
                    temp = Scalar(255,255,255);

                    num = num+1;     //num if number of cars detected

                }
            }

        }

        if(image_main_result.empty() )
        {
            //cout << endl << "result storage not successful" << endl;
        }
    }

        void getdistance(Mat roi)
        {
            Mat HSV;


            cvtColor(roi, HSV, COLOR_RGB2HSV);

            //Mat Red_Img, Green_Img, Yellow_Img;

            //cvtColor(img_input, Red_Img, COLOR_RGB2GRAY);
            Mat red1 = HSV, red2 = HSV;
            Mat red;
//    Scalar RedA = (0, 80, 80);
//    Scalar RedB = (18, 255, 160);
//    Scalar RedC = (156, 80, 80);
//    Scalar RedD = (180, 255, 160);
//    Scalar GreenA = (30, 20480, 20480);
//    Scalar GreenB = (78, 65535, 40960); // 40~105
//    Scalar YellowA = (0, 0, 0);
//    Scalar YellowB = (180, 255, 255);
//    inRange(HSV, RedA, RedB, RED1);
//    inRange(HSV, RedA, RedB, RED2);
//    IplImage *RedTemp1 = new IplImage(RED1);
//    IplImage *RedTemp2 = new IplImage(RED2);
//    IplImage *RedResult;
        //RedResult = cvCreateImage(cvGetSize(RedTemp1), IPL_DEPTH_8U, 3);
        //cvAdd(RedTemp1, RedTemp2, RedResult);
        //cvReleaseImage(&RedTemp1);
        //cvReleaseImage(&RedTemp2);

        //Mat RED = cvarrToMat(RedResult);
        //cvReleaseImage(&RedResult);



//    inRange(HSV, GreenA ,GreenB , img_result);
        //inRange(HSV, YellowA, YellowB, YELLOW);


        //red
        cv::inRange(HSV, cv::Scalar(0, 80, 80, 0), cv::Scalar(10, 255, 160, 0), red1);
        cv::inRange(HSV, cv::Scalar(150, 80, 80, 0), cv::Scalar(180, 255, 160, 0), red2);
        red = red1 | red2;

        //GaussianBlur(red, image_main_result, Size(3, 3), 2, 2);
        //Mat mask = cv::getStructuringElement(cv::MORPH_RECT, cv::Size(3, 3), cv::Point(1, 1));
        //morphologyEx(green, green, cv::MorphTypes::MORPH_CLOSE, mask);
/*
        vector<Vec3f> circles;
        HoughCircles(red, circles, CV_HOUGH_GRADIENT, 2, 100);
        for(int i=0; i<circles.size(); i++)
        {
            Point center((int)(circles[i][0]+0.5), (int)(circles[i][1]+0.5));
            int radius=(int)(circles[i][2]);
            circle(roi, center, radius, Scalar(255,0,0), 3);
        }
*/


//        bitwise_not(green, green);

        //Mat circles;
//       Mat srcImage;
//       cvtColor(img_input, srcImage, COLOR_RGB2GRAY);
//
//        vector<Vec3f> circles;
//        HoughCircles(srcImage, circles, HOUGH_GRADIENT, 1, 50);
//        Mat dstImage(srcImage.size(), CV_8UC3);
//        //cvtColor(green, dstImage, COLOR_GRAY2BGR);
//
//        Vec3f params;
//        int cx, cy, r;
//        //for(int k = 0; k < circles.cols; k++);
//        for(int k = 0; k < circles.size(); k++){
//            params = circles[k];
//            cx = cvRound(params[0]);
//            cy = cvRound(params[1]);
//            r = cvRound(params[2]);
//
//            Point center(cx, cy);
//            circle(srcImage, center, r, Scalar(0, 0, 255), 2);
//            rectangle(srcImage, Point(50, 50), Point(100, 100), Scalar(0, 255, 0), 3);
//
//        }


        red.copyTo(image_main_result);
    }
};
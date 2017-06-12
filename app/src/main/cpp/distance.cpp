
// https://github.com/abhi-kumar/CAR-DETECTION

#include <opencv2/core/core.hpp>
#include <opencv2/video/tracking.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/opencv.hpp>
#include <iterator>

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

        cvtColor( img, gray, CV_BGR2GRAY );

        Mat resize_image(cvRound (img.rows), cvRound(img.cols), CV_8UC1 );

        resize( gray, resize_image, resize_image.size(), 0, 0, INTER_LINEAR );
        equalizeHist( resize_image, resize_image );


        cascade.detectMultiScale( resize_image, cars, 1.1, 2,0, Size(30, 30));                 //detection using main classifier


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

            resize_image_reg_of_interest = resize_image(*main);
            checkcascade.detectMultiScale( resize_image_reg_of_interest, nestedcars, 1.1, 2,0, Size(30,30));

            for( vector<Rect>::const_iterator sub = nestedcars.begin(); sub != nestedcars.end(); sub++ )      //testing the detected car by main using checkcascade
            {
                center.x = cvRound((main->x + sub->x + sub->width*0.5));        //getting center points for bouding a circle over the car detected by checkcascade
                cen_x = center.x;
                center.y = cvRound((main->y + sub->y + sub->height*0.5));
                cen_y = center.y;
                if(cen_x>(x0+15) && cen_x<(x1-15) && cen_y>(y0+15) && cen_y<(y1-15))         //if centre of bounding circle is inside the rectangle boundary over a threshold the the car is certified
                {
                    Rect roi = Rect(cvPoint(x0, y0), cvPoint(x1, y1));
                    Mat ROIimg = image_input(roi);

                    rectangle( image_main_result, cvPoint(x0,y0),
                               cvPoint(x1,y1),
                               color ,3, 8, 0);               //detecting boundary rectangle over the final result

                    //getdistance(ROIimg);

                    listCar.push_back({cvPoint(x0, y0), cvPoint(x1, y1), ((float)400 / (x1 - x0)) * ((float)400 / (x1 - x0))});

                    stringstream text;
                    text << ((float)400 / (x1 - x0)) * ((float)400 / (x1 - x0));
                    text << "m";

                    //sprintf("%.1fm", text, (float)(x1 - x0) / 50);
                    putText(image_main_result, text.str(), cvPoint(x0, y0), FONT_HERSHEY_SIMPLEX, 1.0, Scalar(255, 0, 0), 3);

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

#include <vector>
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>



using namespace std;
using namespace cv;



#define PI 3.1415926
#define MAX_KERNEL_LENGTH 5


struct LaneInfo
{
    Point start;
    Point end;
    int type; // 1 = 중앙선, 0 = 흰색 실선
};

class Lane {
public:
    void ExecuteLane(Mat &img_input, Mat &img_result, vector<LaneInfo>& listLane)
    {
        Mat frame;
        Mat HSV;

        //TODO : Lane Detect

        listLane.clear();

        img_input.copyTo(frame);
        cvtColor(frame, HSV, COLOR_BGR2HSV);
        //frame = imread("1.jpg", CV_LOAD_IMAGE_COLOR);
        //resize(frame, frame, Size(1280, 720));
        //imshow("image", frame);



        Mat currFrame; //stores the upcoming frame
        Mat temp;      //stores intermediate results
        Mat temp2;     //stores the final lane segments

        int diff, diffL, diffR;
        int laneWidth;
        int diffThreshTop;
        int diffThreshLow;
        int ROIrows;
        int vertical_left;
        int vertical_right;
        int vertical_top;
        int smallLaneArea;
        int longLane;
        int vanishingPt;
        float maxLaneWidth;

        //to store various blob properties
        Mat binary_image; //used for blob removal
        int minSize;
        int ratio;
        float contour_area;
        float blob_angle_deg;
        float bounding_width;
        float bounding_length;
        Size2f sz;
        vector<vector<Point> > contours;
        vector<Vec4i> hierarchy;
        RotatedRect rotated_rect;

        currFrame = Mat(img_input.rows, img_input.cols, CV_8UC1,
                        0.0);                        //initialised the image size to 320x480  //TODO : 해상도 변경
        resize(frame, currFrame, currFrame.size());             // resize the input to required size
        cvtColor(currFrame, currFrame, CV_BGR2GRAY);

        temp = Mat(currFrame.rows, currFrame.cols, CV_8UC1, 0.0);//stores possible lane markings
        temp2 = Mat(currFrame.rows, currFrame.cols, CV_8UC1,
                    0.0);//stores finally selected lane marks

        vanishingPt = currFrame.rows / 2;                           //for simplicity right now
        ROIrows = currFrame.rows - vanishingPt;               //rows in region of interest
        minSize = 0.00015 *
                  (currFrame.cols *
                   currFrame.rows);  //min size of any region to be selected as lane
        maxLaneWidth = 0.025 *
                       currFrame.cols;                     //approximate max lane width based on image size
        smallLaneArea = 7 * minSize;
        longLane = 0.3 * currFrame.rows;
        ratio = 4;

        //these mark the possible ROI for vertical lane segments and to filter vehicle glare
        //vertical_left = 2 * currFrame.cols / 5;
        vertical_left = currFrame.cols;
        //vertical_right = 3 * currFrame.cols / 5;
        vertical_right = currFrame.cols;
        //vertical_top = 2 * currFrame.rows / 3;
        vertical_top = currFrame.rows / 2;


        //temp, temp2 검은색으로 초기화
        for (int i = vanishingPt; i < currFrame.rows; i++)
            for (int j = 0; j < currFrame.cols; j++) {
                temp.at<uchar>(i, j) = 0;
                temp2.at<uchar>(i, j) = 0;
            }

        //imshow("currframe", currFrame);


        for (int i = vanishingPt; i < currFrame.rows; i++) {
            //IF COLOUR IMAGE IS GIVEN then additional check can be done
            // lane markings RGB values will be nearly same to each other(i.e without any hue)

            //min lane width is taken to be 5
            laneWidth = 5 + maxLaneWidth * (i - vanishingPt) / ROIrows;
            for (int j = laneWidth; j < currFrame.cols - laneWidth; j++) {

                diffL = currFrame.at<uchar>(i, j) - currFrame.at<uchar>(i, j - laneWidth);
                diffR = currFrame.at<uchar>(i, j) - currFrame.at<uchar>(i, j + laneWidth);
                diff = diffL + diffR - abs(diffL - diffR);

                //1 right bit shifts to make it 0.5 times
                diffThreshLow = currFrame.at<uchar>(i, j) >> 1;
                //diffThreshTop = 1.2*currFrame.at<uchar>(i,j);

                //both left and right differences can be made to contribute
                //at least by certain threshold (which is >0 right now)
                //total minimum Diff should be atleast more than 5 to avoid noise
                if (diffL > 0 && diffR > 0 && diff > 5)
                    if (diff >= diffThreshLow /*&& diff<= diffThreshTop*/)
                        temp.at<uchar>(i, j) = 255;
            }
        }


        // find all contours in the binary image
        temp.copyTo(binary_image);
        findContours(binary_image, contours,
                     hierarchy, CV_RETR_CCOMP,
                     CV_CHAIN_APPROX_SIMPLE);

        // for removing invalid blobs
        if (!contours.empty()) {
            for (size_t i = 0; i < contours.size(); ++i) {
                //====conditions for removing contours====//

                contour_area = contourArea(contours[i]);

                //blob size should not be less than lower threshold
                if (contour_area > minSize) {
                    rotated_rect = minAreaRect(contours[i]);
                    sz = rotated_rect.size;
                    bounding_width = sz.width;
                    bounding_length = sz.height;


                    //openCV selects length and width based on their orientation
                    //so angle needs to be adjusted accordingly
                    blob_angle_deg = rotated_rect.angle;
                    if (bounding_width < bounding_length)
                        blob_angle_deg = 90 + blob_angle_deg;

                    //if such big line has been detected then it has to be a (curved or a normal)lane
                    if (bounding_length > longLane || bounding_width > longLane) {


                        //drawContours(frame, contours, i, Scalar(255, 255, 0), CV_FILLED, 8);
                        //drawContours(temp2, contours, i, Scalar(255), CV_FILLED, 8);
                        int min = 0, minX = contours[i][0].x, max = 0, maxX = 0, color = 0, type = 0;
                        for (int j = 0; j < contours[i].size(); j++) {
                            int h = HSV.at<Vec3b>(contours[i][j].y, contours[i][j].x)[0];
                            int s = HSV.at<Vec3b>(contours[i][j].y, contours[i][j].x)[1];
                            int v = HSV.at<Vec3b>(contours[i][j].y, contours[i][j].x)[2];
                            if (s < 50) {
                                color++;
                            }
                            if ((double) color / (double) contours[i].size() > 0.5) {
                                type = 1;
                            }
                            if (minX >= contours[i][j].x) {
                                minX = contours[i][j].x;
                                min = j;
                            }
                            if (maxX <= contours[i][j].x) {
                                maxX = contours[i][j].x;
                                max = j;
                            }
                        }
                        listLane.push_back({contours[i][min], contours[i][max], type});
//                        int min = 0, minX = contours[i][0].x, max = 0, maxX = 0;
//                        for (int j = 1; j < contours[i].size(); j++) {
//                            if (minX >= contours[i][j].x) {
//                                minX = contours[i][j].x;
//                                min = j;
//                            }
//                            if (maxX <= contours[i][j].x) {
//                                maxX = contours[i][j].x;
//                                max = j;
//                            }
//                        }
//                        listLane.push_back({ contours[i][min], contours[i][max], 0 });
                    }

                        //angle of orientation of blob should not be near horizontal or vertical
                        //vertical blobs are allowed only near center-bottom region, where centre lane mark is present
                        //length:width >= ratio for valid line segments
                        //if area is very small then ratio limits are compensated
                    else if ((blob_angle_deg < -10 || blob_angle_deg > -10) &&
                             ((blob_angle_deg > -70 && blob_angle_deg < 70) ||
                              (rotated_rect.center.y > vertical_top &&
                               rotated_rect.center.x > vertical_left &&
                               rotated_rect.center.x < vertical_right))) {

                        if ((bounding_length / bounding_width) >= ratio ||
                            (bounding_width / bounding_length) >= ratio
                            || (contour_area < smallLaneArea &&
                                ((contour_area / (bounding_width * bounding_length)) > .75) &&
                                ((bounding_length / bounding_width) >= 2 ||
                                 (bounding_width / bounding_length) >= 2))) {
                            //drawContours(frame, contours, i, Scalar(255, 255, 0), CV_FILLED, 8);
                            //drawContours(temp2, contours, i, Scalar(255), CV_FILLED, 8);
                            int min = 0, minX = contours[i][0].x, max = 0, maxX = 0, color = 0, type = 0;
                            for (int j = 0; j < contours[i].size(); j++) {
                                int h = HSV.at<Vec3b>(contours[i][j].y, contours[i][j].x)[0];
                                int s = HSV.at<Vec3b>(contours[i][j].y, contours[i][j].x)[1];
                                int v = HSV.at<Vec3b>(contours[i][j].y, contours[i][j].x)[2];
                                if (s < 50) {
                                    color++;
                                }
                                if ((double) color / (double) contours[i].size() > 0.5) {
                                    type = 1;
                                }
                                if (minX >= contours[i][j].x) {
                                    minX = contours[i][j].x;
                                    min = j;
                                }
                                if (maxX <= contours[i][j].x) {
                                    maxX = contours[i][j].x;
                                    max = j;
                                }
                            }
                            listLane.push_back({contours[i][min], contours[i][max], type});
//                            int min = 0, minX = contours[i][0].x, max = 0, maxX = 0;
//                            for (int j = 1; j < contours[i].size(); j++) {
//                                if (minX >= contours[i][j].x) {
//                                    minX = contours[i][j].x;
//                                    min = j;
//                                }
//                                if (maxX <= contours[i][j].x) {
//                                    maxX = contours[i][j].x;
//                                    max = j;
//                                }
//                            }
//                            listLane.push_back({ contours[i][min], contours[i][max], 0 });
                        }
                    }
                }
            }
        }

        // listLane 출력
/*        for (int i = 0; i < listLane.size(); i++) {
            if (listLane[i].type == 1) {
                line(img_result, listLane[i].start, listLane[i].end, Scalar(0, 0, 255), 5);
            } else {
                line(img_result, listLane[i].start, listLane[i].end, Scalar(0, 0, 0), 5);
            }
        }*/
    }
};
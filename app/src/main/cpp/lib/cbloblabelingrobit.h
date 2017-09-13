#ifndef CBLOBLABELINGROBIT_H
#define CBLOBLABELINGROBIT_H

/*==============================================================================
 *    	Date           	: 2016.09.20
 *		Modified       	: 2016.09.21 By Kwon, Yonghye
 *		Author         	: Kwon, Yonghye  (ROBIT 10th, Kwangwoon University 2014)
 *      E-mail          : robotmanyh@naver.com
 *		NOTE : Neighbor labeling
==============================================================================*/

#include <opencv2/opencv.hpp>
#include <vector>

using namespace std;
using namespace cv;

enum{UP,DOWN,LEFT,RIGHT};

class CBlobLabelingRobit
{
public:

        unsigned int            m_nBlobs;
        unsigned int            m_nCount;
        unsigned char*          m_ImgData;
		unsigned int			area;

        vector<Rect>            m_recBlobs;
		vector<uchar>			areas;

        CBlobLabelingRobit();
        CBlobLabelingRobit(const Mat& Img,const unsigned int nThreshold, const int width, const int height);

        ~CBlobLabelingRobit();

        void            setParam(const Mat &Img, const unsigned int nThreshold);
        void            doLabeling();

private:
        Mat            m_Img;
        bool*          m_isChecked;
        Point *        Pt_visited;

        unsigned int m_num;
        unsigned int m_nThreshold;

        int m_width;
        int m_height;
		int	height;
		int	width;

        void _labeling();
        const unsigned int _check_Four_Neighbor(unsigned int &StartX, unsigned int &StartY, unsigned int &EndX, unsigned int &EndY);
};
#endif // CBLOBLABELINGROBIT_H

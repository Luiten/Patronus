//------------------------------------------------------------------------------------------------//
//--------------------------------------------------------------------------//
//----------------------------------------------------//

#include <vector>
#include <list>
#include <opencv2/core/core.hpp>
#include <opencv2/video/tracking.hpp>
#include <opencv2/opencv.hpp>
#include "distance.cpp"
#include "lane.cpp"
#include "light.cpp"
#include "log.cpp"



using namespace cv;
using namespace std;

class Manager
{

public:
    void Initialize(int w, int h)
    {
        width = w;
        height = h;

        m_Detectcars.Initialize(width, height);
    }

    //------------------------------------------------------------------------------------------------//
    // 실행
    //------------------------------------------------------------------------------------------------//
    void Execute(Mat& input, Mat& output, int iCamera)
    {
        input.copyTo(img_input);
        input.copyTo(img_result);

        //--------------------------------------------------------------------------//
        // 후면 카메라
        //--------------------------------------------------------------------------//
        if (iCamera == 0)
        {
            //----------------------------------------------------//
            // Lane
            //----------------------------------------------------//
            if (m_bLane)
            {
                m_Lane.ExecuteLane(input, img_result, m_listLane);
            }

            //----------------------------------------------------//
            // Distance
            //----------------------------------------------------//
            if (m_bDistance)
            {
                // 차선 검출을 하지 않는 경우 여기선 필요하므로 검출 알고리즘을 사용한다
                if (!m_bLane)
                {
                    m_Lane.ExecuteLane(input, img_result, m_listLane);
                }

                m_Detectcars.ExecuteDistance(input, img_result, m_listCar, m_dLatitude, m_dLongitude, m_Lane.GetCenterPoint());
            }

            //----------------------------------------------------//
            // Light
            //----------------------------------------------------//
            if (m_bLight)
            {
                m_Light.ExecuteLight(input, img_result, m_listLight);
            }
        }
        //--------------------------------------------------------------------------//
        // 전면 카메라일 경우
        //--------------------------------------------------------------------------//
        else if (iCamera == 1)
        {

        }

        //--------------------------------------------------------------------------//
        // Calculate
        //--------------------------------------------------------------------------//
        if (m_bLane) CalculateLane();
        if (m_bDistance) CalculateDistance();
        if (m_bLight) CalculateLight();
        CalculateCollision();

        img_result.copyTo(output);
    }

    void GetDetectedCar(Mat& output)
    {
        //m_Detectcars.get_result(img_result);          //get the final result
        img_result.copyTo(output);
    }

    //------------------------------------------------------------------------------------------------//
    // 차선 침범 계산
    //------------------------------------------------------------------------------------------------//
    bool CalculateLane()
    {
        for (int i = 0; i < m_listLane.size(); i++)
        {
            // 임시로 경고 차선 설정
            // 나중에 시간을 추가해 3초이상 선이 해당 영역일 경우 경고
            if ((m_listLane[i].start.x > (1 * width / 3)) && (m_listLane[i].end.x < (2 * width / 3)))
            {
                putText(img_result, "Warning: Lane", cvPoint(100, 100), FONT_HERSHEY_SIMPLEX, 1.3, Scalar(255, 0, 0), 3);

                if (m_listLane[i].type == 1)
                {
                    line(img_result, m_listLane[i].start, m_listLane[i].end, Scalar(255, 0, 0), 5);
                    m_Log.Write(PATRONUS_LOG_TYPE_LANE, "중앙선", "", m_dLatitude, m_dLongitude);
                } else
                {
                    line(img_result, m_listLane[i].start, m_listLane[i].end, Scalar(255, 255, 0), 5);
                    m_Log.Write(PATRONUS_LOG_TYPE_LANE, "흰색 실선", "", m_dLatitude, m_dLongitude);
                }
            }
        }
        return true;
    }

    //------------------------------------------------------------------------------------------------//
    // 차간 거리 계산
    //------------------------------------------------------------------------------------------------//
    bool CalculateDistance()
    {
        for (int i = 0; i < m_listCar.size(); i++)
        {
            // 임시로 거리 설정
            // 원래대로라면 현재 속도에 따라 경고
            if (m_listCar[i].distance < 3)
            {
                putText(img_result, "Too close", cvPoint(100, 50), FONT_HERSHEY_SIMPLEX, 1.0, Scalar(255, 0, 0), 3);

                m_Log.Write(PATRONUS_LOG_TYPE_DISTANCE, tostr(m_listCar[i].distance));
            }
        }
        return true;
    }

    //------------------------------------------------------------------------------------------------//
    // 신호등 계산
    //------------------------------------------------------------------------------------------------//
    bool CalculateLight()
    {
        for (int i = 0; i < m_listLight.size(); i++)
        {
            // 임시로 신호등 표시
            if (m_listLight[i].type == PATRONUS_LIGHT_TYPE_RED)
            {
                circle(img_result, m_listLight[i].center, 5, Scalar(255, 0, 0), -1, 8, 0);
                //m_Log.Write(PATRONUS_LOG_TYPE_LIGHT, "빨간불", "", m_dLatitude, m_dLongitude);
            }
            else if (m_listLight[i].type == PATRONUS_LIGHT_TYPE_YELLOW)
            {
                circle(img_result, m_listLight[i].center, 5, Scalar(255, 255, 0), -1, 8, 0);
                //m_Log.Write(PATRONUS_LOG_TYPE_LIGHT, "노란불", "", m_dLatitude, m_dLongitude);
            }
            else if (m_listLight[i].type == PATRONUS_LIGHT_TYPE_GREEN)
            {
                circle(img_result, m_listLight[i].center, 5, Scalar(0, 255, 0), -1, 8, 0);
                //m_Log.Write(PATRONUS_LOG_TYPE_LIGHT, "초록불", "", m_dLatitude, m_dLongitude);
            }
        }

        return true;
    }

    //------------------------------------------------------------------------------------------------//
    // 충돌 계산
    //------------------------------------------------------------------------------------------------//
    bool CalculateCollision()
    {
        list<float>::iterator it = m_listCollision.begin();
        list<float>::iterator it_end = m_listCollision.end();
        while (it != it_end)
        {
            // 임시로 충돌 값 설정
            // 나중에 퍼센트에 따른 경고 추가
            if (*it >= m_fCollision)
            {
                putText(img_result, "Collision", cvPoint(100, 200), FONT_HERSHEY_SIMPLEX, 1.3, Scalar(255, 0, 0), 3);
                m_Log.Write(PATRONUS_LOG_TYPE_COLLISION, tostr(*it), "", m_dLatitude, m_dLongitude);
            }

            it++;
            m_listCollision.pop_front();
        }

        //m_listCollision.clear();
        return true;
    }

    void SetSettings(int type, double value)
    {
        int VideoSize[][2] = {
                { 1920, 1080 },
                { 1280, 720 },
                { 800, 600 },
                { 640, 480 } };

        switch (type)
        {
            // 차선
            case 1:
                m_bLane = value;
                break;

            // 자동차 거리
            case 2:
                m_bDistance = value;
                break;

            // 신호등
            case 3:
                m_bLight = value;
                break;

            // 얼굴
            case 4:
                m_bFace = value;
                break;

            // 표지판
            case 5:
                m_bSign = value;
                break;

            // 충돌 기준 가속도 값
            case 6:
                m_fCollision = (float) (value + 1) * 5.8f;
                break;

            // 동영상 크기 - unused
            case 7:
                // 아래 두 크기 직접 입력으로 사용하지 않음
                //width = VideoSize[(int)value][0];
                //height = VideoSize[(int)value][1];
                break;

            // 동영상 너비
            case 8:
                width = value;
                break;

            // 동영상 높이
            case 9:
                height = value;
                break;

            // GPS 위도 값
            case 100:
                m_dLatitude = value;
                break;

            // GPS 경도 값
            case 101:
                m_dLongitude = value;
                break;
        }
    }

    void Pushback_Collision(float value)
    {
        m_listCollision.push_back(value);
    }

private:
    Distance m_Detectcars;
    Lane m_Lane;
    Light m_Light;
    Log m_Log;
    bool m_bLane; // 차선
    bool m_bDistance; // 자동차간 거리
    bool m_bLight; // 신호등
    bool m_bFace;
    bool m_bSign;
    float m_fCollision;
    int height;
    int width;
    double m_dLatitude; // 위도
    double m_dLongitude; // 경도
    Mat img_input;
    Mat img_result;
    vector <LaneInfo> m_listLane;
    vector <CarInfo> m_listCar;
    vector <LightInfo> m_listLight;
    list <float> m_listCollision;

protected:
    // int/float to string 출처: https://stackoverflow.com/questions/2125880/convert-float-to-stdstring-in-c
    template <typename T> string tostr(const T& t)
    {
        ostringstream os;
        os << t;
        return os.str();
    }
};

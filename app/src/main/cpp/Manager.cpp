
#include <vector>
#include <list>
#include <opencv2/core/core.hpp>
#include <opencv2/video/tracking.hpp>
#include <opencv2/highgui/highgui.hpp>
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
    void Initialize()
    {
        string checkcas = "/sdcard/checkcas.xml";

        m_Detectcars.setnum();                  //set number of cars detected as 0
        m_Detectcars.checkcascade_load(checkcas);      //load the test cascade

        //Applying various cascades for a finer search.
        string cas = "/sdcard/cas1.xml";
        m_Detectcars.cascade_load(cas);

    /*    cas = "/sdcard/cas2.xml";
        detectcars.cascade_load(cas);

        cas = "/sdcard/cas3.xml";
        detectcars.cascade_load(cas);

        cas = "/sdcard/cas4.xml";
        detectcars.cascade_load(cas);*/

        width = 1280;
        height = 720;
    }

    //-----------------------------------------------------------------------------------------------//
    // 실행
    //-----------------------------------------------------------------------------------------------//
    void Execute(Mat& input, Mat& output)
    {
        input.copyTo(img_input);
        input.copyTo(img_result);

        //------------------------------------------------------------//
        // Lane
        //------------------------------------------------------------//
        if (m_bLane)
        {
            m_Lane.ExecuteLane(input, img_result, m_listLane);
        }

        //------------------------------------------------------------//
        // Distance
        //------------------------------------------------------------//
        if (m_bDistance)
        {
            m_Detectcars.setnum();
            m_Detectcars.getimage(input);           //get the image

            m_Detectcars.findcars(m_listCar);

            if (m_Detectcars.num == 0)
            {
                //cout << endl << "cars not found" << endl;
            }
            m_Detectcars.get_result(img_result);          //get the final result
        }

        //------------------------------------------------------------//
        // Light
        //------------------------------------------------------------//
        if (m_bLight)
        {
            m_Light.ExecuteLight(input, img_result, m_listLight);
        }

        //------------------------------------------------------------//
        // Calculate
        //------------------------------------------------------------//
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

    //-----------------------------------------------------------------------------------------------//
    // 차선 침범 계산
    //-----------------------------------------------------------------------------------------------//
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
                    m_Log.Write(PATRONUS_LOG_TYPE_LANE, "중앙선", "", m_fLatitude, m_fLongitude);
                } else
                {
                    line(img_result, m_listLane[i].start, m_listLane[i].end, Scalar(255, 255, 0), 5);
                    m_Log.Write(PATRONUS_LOG_TYPE_LANE, "흰색 실선", "", m_fLatitude, m_fLongitude);
                }
            }
        }
        return true;
    }

    //-----------------------------------------------------------------------------------------------//
    // 차간 거리 계산
    //-----------------------------------------------------------------------------------------------//
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

    //-----------------------------------------------------------------------------------------------//
    // 신호등 계산
    //-----------------------------------------------------------------------------------------------//
    bool CalculateLight()
    {
        for (int i = 0; i < m_listLight.size(); i++)
        {
            // 임시로 신호등 표시
            if (m_listLight[i].type == PATRONUS_LIGHT_TYPE_RED)
            {
                circle(img_result, m_listLight[i].center, 5, Scalar(255, 0, 0), -1, 8, 0);
                //m_Log.Write(PATRONUS_LOG_TYPE_LIGHT, "빨간불", "", m_fLatitude, m_fLongitude);
            }
            else if (m_listLight[i].type == PATRONUS_LIGHT_TYPE_YELLOW)
            {
                circle(img_result, m_listLight[i].center, 5, Scalar(255, 255, 0), -1, 8, 0);
                //m_Log.Write(PATRONUS_LOG_TYPE_LIGHT, "노란불", "", m_fLatitude, m_fLongitude);
            }
            else if (m_listLight[i].type == PATRONUS_LIGHT_TYPE_GREEN)
            {
                circle(img_result, m_listLight[i].center, 5, Scalar(0, 255, 0), -1, 8, 0);
                //m_Log.Write(PATRONUS_LOG_TYPE_LIGHT, "초록불", "", m_fLatitude, m_fLongitude);
            }
        }

        return true;
    }

    //-----------------------------------------------------------------------------------------------//
    // 충돌 계산
    //-----------------------------------------------------------------------------------------------//
    bool CalculateCollision()
    {
        list<float>::iterator it;
        for (it = m_listCollision.begin(); it != m_listCollision.end(); it++)
        {
            // 임시로 충돌 값 설정
            // 나중에 퍼센트에 따른 경고 추가
            if (*it >= m_fCollision)
            {
                putText(img_result, "Collision", cvPoint(100, 200), FONT_HERSHEY_SIMPLEX, 1.3, Scalar(255, 0, 0), 3);
                m_Log.Write(PATRONUS_LOG_TYPE_COLLISION, tostr(*it), "", m_fLatitude, m_fLongitude);
            }
        }

        m_listCollision.clear();
        return true;
    }

    void SetSettings(int type, double value)
    {
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

            // GPS 위도 값
            case 100:
                m_fLatitude = value;
                break;

            // GPS 경도 값
            case 101:
                m_fLongitude = value;
                break;
        }
    }

    void Pushback_Collision(float value)
    {
        m_listCollision.push_back(value);
    }

private:
    cars m_Detectcars;                      //creating a object
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
    double m_fLatitude; // 위도
    double m_fLongitude; // 경도
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
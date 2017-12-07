//------------------------------------------------------------------------------------------------//
//--------------------------------------------------------------------------//
//----------------------------------------------------//

#include <vector>
#include <list>
#include <time.h>
#include <ctime>
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

        m_dSpeed = 0;
        bSleeping = false;

        m_Detectcars.Initialize(width, height);

        if (m_bRecord)
        {
            video = new VideoWriter("/sdcard/Patronus/video/blackbox.avi", CV_FOURCC('M', 'J', 'P', 'G'), 30, Size(width, height), true);
        }
        bIsRecord = true;
        bCrashed = false;


        time_t rawtime;
        time(&rawtime);

        m_timeDistance = clock();
        m_timeLane = clock();
        m_timeLight = clock();
        m_timeAttention =clock();
        m_timeCrash = clock();
    }

    //------------------------------------------------------------------------------------------------//
    // 실행
    //------------------------------------------------------------------------------------------------//
    void Execute(Mat& input, Mat& output, int iCamera)
    {
        clock_t end = clock();
        double elapsed_ms = 0;

        input.copyTo(img_input);
        input.copyTo(img_result);

        cvtColor(img_input, img_input, CV_RGB2BGR);
        cvtColor(img_result, img_result, CV_RGB2BGR);

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
                m_Lane.ExecuteLane(img_input, img_result, m_listLane);

                // 좌측 선
                if (ptStandardLane[0].x != 0 && ptStandardLane[1].x != 0)
                {
                    line(img_result, ptStandardLane[0], ptStandardLane[1], Scalar(0, 0, 255), 5);
                }
                // 우측 선
                if (ptStandardLane[2].x != 0 && ptStandardLane[3].x != 0)
                {
                    line(img_result, ptStandardLane[2], ptStandardLane[3], Scalar(0, 0, 255), 5);
                }
            }

            //----------------------------------------------------//
            // Distance
            //----------------------------------------------------//
            if (m_bDistance)
            {
                // 차선 검출을 하지 않는 경우 여기선 필요하므로 검출 알고리즘을 사용한다
                if (!m_bLane)
                {
                    m_Lane.ExecuteLane(img_input, img_result, m_listLane);
                }

                m_Detectcars.ExecuteDistance(img_input, img_result, m_listCar, m_dLatitude, m_dLongitude, m_Lane.GetCenterPoint());
            }

            //----------------------------------------------------//
            // Light
            //----------------------------------------------------//
            if (m_bLight || m_bAttention)
            {
                m_Light.ExecuteLight(img_input, img_result, m_listLight);
            }
        }
        //--------------------------------------------------------------------------//
        // 전면 카메라일 경우
        //--------------------------------------------------------------------------//
        else if (iCamera == 1)
        {

        }
        //--------------------------------------------------------------------------//
        // 기준점 찍기
        //--------------------------------------------------------------------------//
        else if (iCamera == 2)
        {
            m_Lane.ExecuteLane(img_input, img_result, m_listLane);

            Point pt[4];
            float th[2];
            th[0] = 180;
            th[1] = -180;
            for (int i = 0; i < m_listLane.size(); i++)
            {
                // 왼쪽 선
                if (m_listLane[i].theta < th[0] && m_listLane[i].theta > 2.0)
                {
                    th[0] = m_listLane[i].theta;
                    pt[0] = m_listLane[i].start;
                    pt[1] = m_listLane[i].end;
                }
                else if (m_listLane[i].theta > th[1] && m_listLane[i].theta < 1.2)
                {
                    th[1] = m_listLane[i].theta;
                    pt[2] = m_listLane[i].start;
                    pt[3] = m_listLane[i].end;
                }

//                stringstream text;
//                text << "theta:" << m_listLane[i].theta;
//
//                putText(img_result, text.str(), m_listLane[i].start, 2, 1.7, Scalar(255, 0, 0), 2);
//                line(img_result, m_listLane[i].start, m_listLane[i].end, Scalar(0, 255, 0), 5);
            }

            ptTempStandardLane[0] = pt[0];
            ptTempStandardLane[1] = pt[1];
            ptTempStandardLane[2] = pt[2];
            ptTempStandardLane[3] = pt[3];
            line(img_result, pt[0], pt[1], Scalar(0, 0, 255), 5);
            line(img_result, pt[2], pt[3], Scalar(0, 0, 255), 5);

            cvtColor(img_result, img_result, CV_BGR2RGB);

            img_result.copyTo(output);
            return;
        }

        //--------------------------------------------------------------------------//
        // Calculate
        //--------------------------------------------------------------------------//
        // "자동차간 거리", "끼어들기", "차선 침범", "신호 위반", "신호 주시 안함", "표지판", "졸음 운전", "충돌"
        if (m_bDistance && (float)(end - m_timeDistance) / CLOCKS_PER_SEC > 5) CalculateDistance();
        if (m_bLane && (float)(end - m_timeLane) / CLOCKS_PER_SEC > 5) CalculateLane();
        if (m_bDistance && m_bLane) CalculateInterference();
        if (m_bLight && (float)(end - m_timeLight) / CLOCKS_PER_SEC > 5) CalculateLight();
        if (m_bAttention && (float)(end - m_timeAttention) / CLOCKS_PER_SEC > 5) CalculateAttention();
        if (m_bFace) CalculateSleepiness();
        if ((float)(end - m_timeCrash) / CLOCKS_PER_SEC > 5) CalculateCollision();

        cvtColor(img_result, img_result, CV_BGR2RGB);
        img_result.copyTo(output);

        if (m_bRecord)
        {
            if (bIsRecord)
            {
                // 충돌한 뒤 시간을 체크해 지정된 시간까지만 녹화
                m_EndTime = clock();
                if ((((m_EndTime - m_StartTime) / CLOCKS_PER_SEC) > m_nRecordTime) && bCrashed)
                {
                    // 지정 시간을 초과하면 녹화 종료
                    bIsRecord = false;
                    bCrashed = false;
                    m_StartTime = 0;
                    m_EndTime = 0;

                    // 파일 이름 변경
                    string strPath = "/sdcard/Patronus/video/";
                    strPath += m_RecordFile;
                    rename("/sdcard/Patronus/video/blackbox.avi", strPath.c_str());
                }

                // 충돌 전후를 위한 녹화
                video->write(img_result);
            }
        }
    }

    //------------------------------------------------------------------------------------------------//
    // 차선 침범 계산
    //------------------------------------------------------------------------------------------------//
    bool CalculateLane()
    {
        // 속도가 30km/h 이하면 경고하지 않음
        //if (m_dSpeed < 30) return false;
        Point pt[4];
        float th[2];
        th[0] = 180;
        th[1] = -180;

        for (int i = 0; i < m_listLane.size(); i++)
        {
            //line(img_result, m_listLane[i].start, m_listLane[i].end, Scalar(0, 255, 0), 5);

            // 임시로 경고 차선 설정
            // 나중에 시간을 추가해 3초이상 선이 해당 영역일 경우 경고
/*            if ((m_listLane[i].start.x > (1 * width / 3)) && (m_listLane[i].start.x < (2 * width / 3)) &&
                    (m_listLane[i].end.x > (1 * width / 3)) && (m_listLane[i].end.x < (2 * width / 3)))
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
            }*/

            // 왼쪽 선
            if (m_listLane[i].theta < th[0] && m_listLane[i].theta > 2.0)
            {
                th[0] = m_listLane[i].theta;
                pt[0] = m_listLane[i].start;
                pt[1] = m_listLane[i].end;
            }
            // 오른쪽 선
            else if (m_listLane[i].theta > th[1] && m_listLane[i].theta < 1.2)
            {
                th[1] = m_listLane[i].theta;
                pt[2] = m_listLane[i].start;
                pt[3] = m_listLane[i].end;
            }
        }

        line(img_result, pt[0], pt[1], Scalar(0, 255, 0), 5);
        line(img_result, pt[2], pt[3], Scalar(0, 255, 0), 5);
        if ((pt[0].x > (1 * width / 3)) && (pt[0].x < (2 * width / 3)) && (pt[1].x > (1 * width / 3)) && (pt[1].x < (2 * width / 3)))
        {
            putText(img_result, "Warning: Lane", cvPoint(100, 100), FONT_HERSHEY_SIMPLEX, 1.3, Scalar(0, 0, 255), 3);

            line(img_result, pt[0], pt[1], Scalar(0, 0, 255), 5);
            m_Log.Write(PATRONUS_LOG_TYPE_LANE, "실선", "", m_dLatitude, m_dLongitude);
            m_timeLane = clock();
        }
        if ((pt[2].x > (1 * width / 3)) && (pt[2].x < (2 * width / 3)) && (pt[3].x > (1 * width / 3)) && (pt[3].x < (2 * width / 3)))
        {
            putText(img_result, "Warning: Lane", cvPoint(100, 100), FONT_HERSHEY_SIMPLEX, 1.3, Scalar(0, 0, 255), 3);

            line(img_result, pt[2], pt[3], Scalar(0, 0, 255), 5);
            m_Log.Write(PATRONUS_LOG_TYPE_LANE, "실선", "", m_dLatitude, m_dLongitude);
            m_timeLane = clock();
        }
        return true;
    }

    //------------------------------------------------------------------------------------------------//
    // 차간 거리 계산
    //------------------------------------------------------------------------------------------------//
    bool CalculateDistance()
    {
        // 속도가 30km/h 이하면 경고하지 않음
        //if (m_dSpeed < 30) return false;

        for (int i = 0; i < m_listCar.size(); i++)
        {
            // 임시로 거리 설정
            // 원래대로라면 현재 속도에 따라 경고
            if (m_listCar[i].distance < 5)
            {
                putText(img_result, "Too close", cvPoint(100, 50), FONT_HERSHEY_SIMPLEX, 1.0, Scalar(255, 0, 0), 3);

                m_Log.Write(PATRONUS_LOG_TYPE_DISTANCE, tostr(m_listCar[i].distance));
                m_timeDistance = clock();
            }
        }
        return true;
    }

    //------------------------------------------------------------------------------------------------//
    // 끼어들기 계산
    //------------------------------------------------------------------------------------------------//
    bool CalculateInterference()
    {
        // 속도가 30km/h 이하면 경고하지 않음
        if (m_dSpeed < 30) return false;

        return true;
    }

    //------------------------------------------------------------------------------------------------//
    // 신호주시 계산
    //------------------------------------------------------------------------------------------------//
    bool CalculateAttention()
    {
        // 속도가 10km/h 이상이면 경고하지 않음
        //if (m_dSpeed >= 10) return false;

        for (int i = 0; i < m_listLight.size(); i++)
        {
            // 임시로 신호등 표시
            if (m_listLight[i].type == PATRONUS_LIGHT_TYPE_GREEN)
            {
                rectangle(img_result, m_listLight[i].rect, Scalar(0, 255, 0), 3);
                m_Log.Write(PATRONUS_LOG_TYPE_ATTENTION, "초록불", "", m_dLatitude, m_dLongitude);
            }
        }

        return true;
    }

    //------------------------------------------------------------------------------------------------//
    // 신호 위반 계산
    //------------------------------------------------------------------------------------------------//
    bool CalculateLight()
    {
        //if (m_dSpeed < 30) return false;

        for (int i = 0; i < m_listLight.size(); i++)
        {
            // 임시로 신호등 표시
            if (m_listLight[i].type == PATRONUS_LIGHT_TYPE_RED)
            {
                rectangle(img_result, m_listLight[i].rect, Scalar(0, 0, 255), 3);
                m_Log.Write(PATRONUS_LOG_TYPE_LIGHT, "빨간불", "", m_dLatitude, m_dLongitude);
            }
            else if (m_listLight[i].type == PATRONUS_LIGHT_TYPE_YELLOW)
            {
                rectangle(img_result, m_listLight[i].rect, Scalar(255, 255, 0), 3);
                //m_Log.Write(PATRONUS_LOG_TYPE_LIGHT, "노란불", "", m_dLatitude, m_dLongitude);
            }
            else if (m_listLight[i].type == PATRONUS_LIGHT_TYPE_GREEN)
            {
                rectangle(img_result, m_listLight[i].rect, Scalar(0, 255, 0), 3);
                //m_Log.Write(PATRONUS_LOG_TYPE_LIGHT, "초록불", "", m_dLatitude, m_dLongitude);
            }
        }

        return true;
    }

    //------------------------------------------------------------------------------------------------//
    // 졸음운전 계산
    //------------------------------------------------------------------------------------------------//
    bool CalculateSleepiness()
    {
        // 속도가 30km/h 이하면 경고하지 않음
        //if (m_dSpeed < 30) return false;

        // 졸음운전일 경우(임시)
        if (bSleeping) {
            m_Log.Write(PATRONUS_LOG_TYPE_DROWSINESS, "", "", m_dLatitude, m_dLongitude);
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
                time_t rawtime;
                tm * timeinfo;

                time(&rawtime);
                timeinfo = localtime(&rawtime);

                // 충돌 녹화가 설정되었을 때만 녹화파일 입력
                if (m_bRecord)
                {
                    strftime(m_RecordFile, 80, "%Y%m%d_%H%M%S.avi", timeinfo);
                }
                else
                {
                    strcpy(m_RecordFile, "");
                }

                putText(img_result, "Collision", cvPoint(100, 200), FONT_HERSHEY_SIMPLEX, 1.3, Scalar(255, 0, 0), 3);
                m_Log.Write(PATRONUS_LOG_TYPE_COLLISION, tostr((*it / 0.58) + 10), m_RecordFile, m_dLatitude, m_dLongitude);
                bCrashed = true;
                m_StartTime = clock();
                m_timeCrash = clock();
            }

            it++;
            m_listCollision.pop_front();
        }

        //m_listCollision.clear();
        return true;
    }

    //------------------------------------------------------------------------------------------------//
    // 외부 세팅 값 적용
    //------------------------------------------------------------------------------------------------//
    void SetSettings(int type, double value)
    {
/*        int VideoSize[][2] = {
                { 1920, 1080 },
                { 1280, 720 },
                { 800, 600 },
                { 640, 480 } };*/

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

            // 신호 주시
            case 3:
                m_bAttention = value;
                break;

            // 신호 위반
            case 4:
                m_bLight = value;
                break;

            // 얼굴
            case 5:
                m_bFace = value;
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

             // 동영상 녹화 길이
            case 10:
                m_nRecordTime = value;
                break;

            // 충돌 녹화
            case 11:
                m_bRecord = value;
                break;

            // GPS 위도 값
            case 100:
                m_dLatitude = value;
                break;

            // GPS 경도 값
            case 101:
                m_dLongitude = value;
                break;

            // 현재 속도
            case 102:
                m_dSpeed = value;
                break;

            // 최근 로그 값 초기화
            case 200:
                m_Log.InitLastType();
                break;

            // 졸음 인식
            case 300:
                bSleeping = true;
                break;

                // 기준선1 x1
            case 400:
                ptStandardLane[0].x = value;
                break;

                // 기준선1 y1
            case 401:
                ptStandardLane[0].y = value;
                break;

                // 기준선1 x2
            case 402:
                ptStandardLane[1].x = value;
                break;

                // 기준선1 y2
            case 403:
                ptStandardLane[1].y = value;

                // 기준선2 x1
            case 404:
                ptStandardLane[2].x = value;
                break;

                // 기준선2 y1
            case 405:
                ptStandardLane[2].y = value;
                break;

                // 기준선2 x2
            case 406:
                ptStandardLane[3].x = value;
                break;

                // 기준선2 y2
            case 407:
                ptStandardLane[3].y = value;
                break;
        }
    }

    void PrintLogForRecord(string str)
    {
        m_Log.Write(PATRONUS_LOG_TYPE_RECORD, "", str, m_dLatitude, m_dLongitude);
    }

    //------------------------------------------------------------------------------------------------//
    // 클래스 내 값 읽어오기
    //------------------------------------------------------------------------------------------------//
    double GetValue(int type)
    {
        double value = 0.0;

        // 현재 발생한 로그 종류 읽어오기
        if (type == 1)
        {
            value = m_Log.GetLastType();
        }
        return value;
    }

    void Pushback_Collision(float value)
    {
        m_listCollision.push_back(value);
    }

    void GetStandardLane(int* arr, int nLaneType)
    {
        // 좌측 선
        if (nLaneType == 0)
        {
            arr[0] = ptTempStandardLane[0].x;
            arr[1] = ptTempStandardLane[0].y;
            arr[2] = ptTempStandardLane[1].x;
            arr[3] = ptTempStandardLane[1].y;
        }
        // 우측 선
        else if (nLaneType == 1)
        {
            arr[0] = ptTempStandardLane[2].x;
            arr[1] = ptTempStandardLane[2].y;
            arr[2] = ptTempStandardLane[3].x;
            arr[3] = ptTempStandardLane[3].y;
        }
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
    bool m_bAttention; // 신호 주시
    float m_fCollision;
    int height;
    int width;
    double m_dLatitude; // 위도
    double m_dLongitude; // 경도
    double m_dSpeed; // 현재 속도
    Mat img_input;
    Mat img_result;
    vector <LaneInfo> m_listLane;
    vector <CarInfo> m_listCar;
    vector <LightInfo> m_listLight;
    list <float> m_listCollision;
    bool bSleeping;

    Point ptStandardLane[4]; // 차선임을 판별하는 기준점 차선
    Point ptTempStandardLane[4]; // 차선임을 판별하는 임시 기준점 차선

    VideoWriter* video;
    bool bIsRecord; // 현재 녹화 상태
    bool m_bRecord; // 충돌 녹화 유무
    bool bCrashed;
    char m_RecordFile[256];
    int m_nRecordTime; // second
    clock_t m_StartTime;
    clock_t m_EndTime;
    clock_t m_timeDistance; // 차량 거리 경고 시간(5초)
    clock_t m_timeLane; // 차선 경고 시간(5초)
    clock_t m_timeAttention; // 신호 주시 경고 시간(5초)
    clock_t m_timeLight; // 신호 위반 경고 시간(5초)
    clock_t m_timeCrash; // 충돌 경고 시간(5초)

protected:
    // int/float to string 출처: https://stackoverflow.com/questions/2125880/convert-float-to-stdstring-in-c
    template <typename T> string tostr(const T& t)
    {
        ostringstream os;
        os << t;
        return os.str();
    }
};

//
// Created by power on 2017-06-12.
//
#include <iostream>
#include <fstream>
#include <stdio.h>
#include <sstream>
#include <string>
#include <time.h>
#include <math.h>

#define PATRONUS_LOG_TYPE_DISTANCE          1
#define PATRONUS_LOG_TYPE_INTERFERENCE      2
#define PATRONUS_LOG_TYPE_LANE              3
#define PATRONUS_LOG_TYPE_LIGHT             4
#define PATRONUS_LOG_TYPE_ATTENTION         5
#define PATRONUS_LOG_TYPE_SIGN              6
#define PATRONUS_LOG_TYPE_DROWSINESS        7
#define PATRONUS_LOG_TYPE_COLLISION         8
#define PATRONUS_LOG_TYPE_SCORE             9


using namespace std;

class Log
{
public:
    void Write(int nType, string strMsg, string strDesc = "", double x = 0.0, double y = 0.0)
    {
        static int nCheckFlag = 0;
        stringstream strLog;
        string csvName = "logs.csv";

        // 시간 측정   출처: http://www.cplusplus.com/reference/ctime/strftime/
        time_t rawtime;
        struct tm * timeinfo;
        char buffer[80];

        time(&rawtime);
        timeinfo = localtime(&rawtime);

        // 1초 경과한 경우 로그 플래그 초기화(1초마다 쓰는 이유는 짧은 시간내(1초) 중복 로그 방지)
        if (prevSec != timeinfo->tm_sec)
        {
            nCheckFlag = 0;
        }
        prevSec = timeinfo->tm_sec;

        // 1 값이 있으면 이미 해당 시간에 로그를 썼으므로 종료
        if (nCheckFlag & (int)pow(2, nType))
        {
            return;
        }
        nCheckFlag |= (int)pow(2, nType);

        lastType = nType;
        strftime(buffer, 80, "%F,%T", timeinfo);

        strLog << buffer << "," << nType << "," << strMsg << "," << strDesc << "," << x << "," << y << "\n";

        // 파일 저장
        string dirPath = "/sdcard/Patronus/" + csvName;
        ofstream file;
        file.open(dirPath, ofstream::in | ofstream::out | ofstream::app);

        // 일치하는 폴더가 없으면 생성
        if (file == nullptr)
        {
            return;
        }

        // Save csv file
        file << strLog.str();
        file.close();
    }

    //------------------------------------------------------------------------------------------------//
    // 가장 최근에 기록된 로그 타입 리턴
    //------------------------------------------------------------------------------------------------//
    int GetLastType()
    {
        return lastType;
    }

    //------------------------------------------------------------------------------------------------//
    // 가장 최근에 기록된 로그 타입 초기화
    //------------------------------------------------------------------------------------------------//
    void InitLastType()
    {
        lastType = -1;
    }

private:
    int prevSec;
    int lastType = -1;
};
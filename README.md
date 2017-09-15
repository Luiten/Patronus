### Patronus: Advanced Driver Assistance Systems(ADAS) for Android
Version: 0.92.0 (2017-09-13)

<br>

### How to build
#### 1. Change "Patronus/app/CMakeLists.txt"<br>
set(pathOpenCv "Your OpenCV 3.1 Android SDK Path")<br>
set(pathProject "This Project Path")
<br>
#### 2. Build it!
<br>

---
#### Problem
1. 듀얼 카메라 작동시 카메라 전환 속도가 너무 느린 문제(스냅드래곤 800대 AP는 듀얼카메라 지원)
---
#### 0.92.1 (2017-09-16)
1. setting.java 파일에 WarningAdapter와 Warning 리스트뷰 추가
2. warning_item.xml 추가 및 setting.xml에 warning 리스트뷰 추가
---
#### 0.92.0 (2017-09-13)
1. 신호등 인식 추가
2. TTS 경고 추가
3. MainActivity: 전면 카메라 추가
4. Setting: 듀얼 카메라 지원에 따라 전면 카메라 체크 설정
5. Capture_lengthAdapter: 듀얼 카메라 구분을 위한 Enabled 추가
6. SplashActivity: 폴더가 없을 경우 폴더 생성
7. MainActivity: 경고 애니메이션 적용
8. RecordCrash: 버튼 기능 일부 변경
9. RecordDrive: 그래프 색상 변경
10. RecordDrive: CSV 파일을 읽어 운전 점수 적용
*. TTS: 클래스가 초기화되지 않았는지 오류
---
#### 0.90.2 (2017-09-04)
1. 속도 측정 개선
---
#### 0.90.1 (2017-08-31)
1. RecordCrash: 동영상 미디어 컨트롤러 play,stop 버튼 추가(seekbar 문제 고쳐야 함)
---
#### 0.90.0 (2017-08-24)
1. Lane.cpp: 소실점 검출 처리
2. Distance.cpp: Lane 클래스로부터 연산된 소실점으로 Bird's Eye View 변환
3. standard_main.xml: [다시 찍기], [닫기] 버튼 제거
4. StandardActivity: 기준점 파일이 없을 경우 Toast 메시지 추가
5. PointActivity: 메모리 누수(matResult) 해결
6. PointActivity: 기준점 촬영시 파일이 저장되지 않는 문제 수정
7. Setting: 지원하는 영상 크기가 끝자리가 0이 아닌 크기 제외
8. AlarmAdapter: 레이아웃이 짤리는 문제 해결
9. colors.xml: 메인 색상 변경(#FFC000 -> #FF8040)
---
#### 0.89.4 (2017-08-23)
1. MainActivity: 메모리 누수(matResult) 해결
2. MainActivity: onPause(), onResume() 시 화면이 안나오는 문제 수정
---
#### 0.89.3 (2017-08-21)
1. MainActivity: 시작, 종료 통합
---
#### 0.89.2 (2017-08-18)
1. LogAdapter: record_log_time에 시간이 나오게 수정
2. 전체 레이아웃 폰트 크기 수정(20dp -> 18dp)
---
#### 0.89.2 (2017-08-20)
1. 메인 액티비티 시작, 종료 버튼 합치기(처음 시작과 종료는 됫느데 두번째 시작 부터 켜지지 않음)
---
#### 0.89.1 (2017-08-18)
1. 모든 액티비티 세로 화면 고정
2. record_log_content 레이아웃 시간과 종류 둘 다 나오게 수정
---
#### 0.89.0 (2017-08-16)
1. MainActivity: 속도, 종료 Toast 텍스트 변경
2. distance.cpp: 영상처리(낮은 영상 크기 사용, IPM) 최적화
3. MainActivity, Setting, PointActivity, Manager.cpp: 기기가 지원하는 영상 크기로 변경
4. activity_main.xml: 전면 카메라 위치 변경
---
#### 0.88.1 (2017-08-15)
1. MainActivity: 버튼 투명도 조절 및 속도 텍스트 크기 조절
2. MainActivity: Warning faed-in-out 추가 
3. MainActivity: 두번 누를시 종료 추가
4. res/anim: fade-in.xml 추가 및 activity_main.xml 레이아웃 수정
---
#### 0.88.0 (2017-08-14)
1. MainActivity, native-lib.cpp, Manager.cpp: 듀얼 카메라 추가 * 
1. RecordCrash, record_crash.xml: 동영상이 없을 경우 팝업창 크기 수정
2. setting_main.xml 제거 후 setting.xml -> setting_main.xml로 이름 변경
3. setting_main.xml: capscore, sensiscore 오른쪽 정렬 및 색상 변경
---
#### 0.87.1 (2017-08-13)
1. alarm_item.xml: 설명이 여러 줄이 될 경우 레이아웃 분배 수정 *
2. Setting: 알림 설명 변경
3. setting.xml: 타이틀 텍스트 색상 검정으로 변경
---
#### 0.87.0 (2017-08-12)
1. SplashActivity: 파일 검사 및 다운로드 추가
2. RecordDrive: 빠졌던 CustomValueFormatter 추가
---
#### 0.86.3 (2017-08-10)
1. sensitivity_test.xml, standard_main.xml: 타이틀바 추가 및 크기 조정
2. Record_drive.java: null 포인트 오류 해결, 라벨 제거, value format값 수정
---
#### 0.86.2 (2017-08-10)
1. MainActivity: 버튼 View 다시 터치시 사라지기 및 시간 3초 -> 5초로 수정
---
#### 0.86.1 (2017-08-07)
1. record_drive.xml: 패딩, 마진, 텍스트 뷰 추가
2. record_drive.xml: 그래프 색깔 변화 및 데이터 값 텍스트 사이즈 라벨 위치 수정
3. record_drive.java: 그래프 클릭시 해당 클릭 인덱스 번호에 따라 데이터값이 들어가게 수정
---
#### 0.86.0 (2017-08-07)
1. record_signal.xml: 지도 높이 350dp -> 300dp로 수정
2. distance.cpp: 차 검출 및 거리 추정 알고리즘 전체 수정(자체 로그파일 출력 추가)
3. Manager.cpp: distance.cpp 변경에 따른 초기화, 검출 수정
4. CMakeLists.txt: diatance.cpp에서 사용하는 IPM.cpp, IPM.h 추가
5. MainActivity: 시작 버튼 터치시 Manager 초기화 추가
6. PointActivity: 시작시 Manager 초기화 추가
---
#### 0.85.3 (2017-08-05)
1. build.gradle에 MPAndroid 라이브러리 추가
2. 레이아웃 record_drive 수정
3. 자바파일 RecordDrive 그래프 및 터치이벤트(수정중)
---
#### 0.85.2 (2017-08-03)
1. 자바파일 Sensitivity_SeekAdapter, Sensitivity_SeekBarAdapter 삭제
2. 레이아웃 record_crash, record_signal, sensitivity_test, standard_main 의 width 길이 300dp로 변경


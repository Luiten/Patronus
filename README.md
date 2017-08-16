### Patronus: Advanced Driver Assistance Systems(ADAS) for Android
Version: 0.89.0 (2017-08-16)

<br>

### Change "CMakeLists.txt"
set(pathOpenCv "Your OpenCV 3.1 Android SDK Path")<br>
set(pathProject "This Project Path")

<br>
<br>

---
#### Problem
1. StandardActivity: [다시 찍기] 터치시 설정 해상도가 적용이 안되는 문제
2. 너비나 크기를 dp로 설정시 글자 크기, 화면 크기에 따라 레이아웃 일부가 짤릴 수 있는 문제
3. 듀얼 카메라 작동시 카메라 전환 속도가 너무 느린 문제
---
#### 0.89.0 (2017-08-16)
1. MainActivity: 속도, 종료 Toast 텍스트 변경
2. distance.cpp: 영상처리(저해상도 사용, IPM) 최적화
3. MainActivity, Setting, PointActivity, Manager.cpp: 기기가 지원하는 해상도로 변경
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


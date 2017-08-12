### Patronus: Advanced Driver Assistance Systems(ADAS) for Android
Version: 0.87.0 (2017-08-12)

---
#### Change "CMakeLists.txt"
set(pathOpenCv "Your OpenCV 3.1 Android SDK Path")
set(pathProject "This Project Path")

---
#### Problem
1. StandardActivity: [다시 찍기] 터치시 설정 해상도가 적용이 안되는 문제
2. MainActivity: 800x600 해상도 실행시 팅기는 문제
3. RecordCrash: 동영상이 없을 경우 화면이 너무 커지는 문제
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


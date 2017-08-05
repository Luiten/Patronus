### Patronus: Advanced Driver Assistance Systems(ADAS) for Android

Version: 0.85.3 (2017-08-03)


## Change "CMakeLists.txt"
set(pathOpenCv "Your OpenCV 3.1 Android SDK Path")
set(pathProject "This Project Path")

## 2017.08.03
1. 자바파일 Sensitivity_SeekAdapter, Sensitivity_SeekBarAdapter 삭제
2. 레이아웃 record_crash, record_signal, sensitivity_test, standard_main 의 width 길이 300dp로 변경

##2017.08.05
1. build.gradle에 MPAndroid 라이브러리 추가
2. 레이아웃 record_drive 수정
3. 자바파일 RecordDrive 그래프 및 터치이벤트(수정중)

### Patronus: Advanced Driver Assistance Systems(ADAS) for Android
Version: 0.97.0 (2017-12-07)

<br>

### How to build
#### 1. Change "Patronus/app/CMakeLists.txt"<br>
set(pathOpenCv "Your OpenCV 3.1 Android SDK Path")<br>
set(pathProject "This Project Path")
<br>
#### 2. Build it!
<br>
---
To do
1. 졸음 운전 판별 미완성
2. 차선 기준점 활용
3. 신호등 검출 미완성
4. 끼어들기, 신호주시, 신호위반 미완성
5. OpenCV 녹화 스레드 추가 필요
7. 로그 자동 삭제, 모두 삭제 버튼
8. 중지 후 재시작시 속도 0km/h 문제
---
#### 0.97.0 (2017-12-07)
1. Light.cpp: 신호등 검출 알고리즘 변경
2. Setting: 신호 주시, 충돌 녹화 설정 추가
3. setting_main.xml: 녹화 레이아웃 변경
4. PointActivity: 기준점 데이터 저장
5. Manager.cpp; 기준선에 따라 선 검출
6. Manual_Recording/Adapter: 동영상 뷰 추가